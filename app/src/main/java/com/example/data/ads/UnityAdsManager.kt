package com.example.data.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.services.core.properties.ClientProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

data class AdsAccountDetails(
    val adsEnabled: Boolean = true,
    val accountName: String = UnityAdsManager.DEFAULT_ACCOUNT_NAME,
    val accountEmail: String = UnityAdsManager.DEFAULT_ACCOUNT_EMAIL,
    val organizationId: String = UnityAdsManager.DEFAULT_ORGANIZATION_ID,
    val gameId: String = UnityAdsManager.DEFAULT_GAME_ID,
    val rewardedPlacementId: String = UnityAdsManager.DEFAULT_PLACEMENT_ID,
    val interstitialPlacementId: String = UnityAdsManager.DEFAULT_INTERSTITIAL_ID,
    val bannerPlacementId: String = UnityAdsManager.DEFAULT_BANNER_ID,
    val testMode: Boolean = UnityAdsManager.DEFAULT_TEST_MODE,
    val dailyAdLimit: Int = UnityAdsManager.DEFAULT_DAILY_LIMIT,
    val rewardPerAd: Double = UnityAdsManager.DEFAULT_REWARD_PER_AD
)

object UnityAdsManager {
    private const val TAG = "UnityAdsManager"
    private const val PREFS_NAME = "unity_ads_config_v2_prefs"
    private const val LEGACY_GAME_ID = "5723485"
    private const val LEGACY_PLACEMENT_ID = "Rewarded_Android"

    // Default Unity Ads Configuration:
    // Android Game ID: 800408446 | Rewarded placement ID: esports | Test mode: enabled for development
    const val DEFAULT_ACCOUNT_NAME = "Unity Ads Android"
    const val DEFAULT_ACCOUNT_EMAIL = "udaynath381@gmail.com"
    const val DEFAULT_ORGANIZATION_ID = ""
    const val DEFAULT_GAME_ID = "800408446"
    const val DEFAULT_PLACEMENT_ID = "esports"
    const val DEFAULT_INTERSTITIAL_ID = ""
    const val DEFAULT_BANNER_ID = ""
    const val DEFAULT_TEST_MODE = true
    const val DEFAULT_DAILY_LIMIT = 5
    const val DEFAULT_REWARD_PER_AD = 0.5
    const val SDK_DEPENDENCY_NOTATION = "implementation(\"com.unity3d.ads:unity-ads:4.21.0\")"

    fun normalizeGameId(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        return if (trimmed.isBlank() || trimmed == LEGACY_GAME_ID) DEFAULT_GAME_ID else trimmed
    }

    fun normalizePlacementId(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        return if (trimmed.isBlank() || trimmed == LEGACY_PLACEMENT_ID) DEFAULT_PLACEMENT_ID else trimmed
    }

    private var activeGameId: String = DEFAULT_GAME_ID
    private var activePlacementId: String = DEFAULT_PLACEMENT_ID
    private var activeInterstitialId: String = DEFAULT_INTERSTITIAL_ID
    private var activeBannerId: String = DEFAULT_BANNER_ID
    private var activeTestMode: Boolean = DEFAULT_TEST_MODE
    private var activeAdsEnabled: Boolean = true
    private var isInitialized = false
    private var isInitializing = false
    private var isAdLoaded = false
    private var isAdLoading = false
    private var lifecycleCallbacksRegistered = false
    private var prefsLoaded = false
    private var safeHostActivityRef: WeakReference<Activity>? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _accountDetails = MutableStateFlow(AdsAccountDetails())
    val accountDetails: StateFlow<AdsAccountDetails> = _accountDetails.asStateFlow()

    private val _adState = MutableStateFlow<AdState>(AdState.Idle)
    val adState: StateFlow<AdState> = _adState.asStateFlow()

    sealed class AdState {
        object Idle : AdState()
        object Initializing : AdState()
        object Loading : AdState()
        object Ready : AdState()
        object Showing : AdState()
        data class Error(val message: String) : AdState()
    }

    private fun ensurePrefsLoaded(context: Context) {
        if (prefsLoaded) return
        prefsLoaded = true
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedGameId = normalizeGameId(prefs.getString("unity_game_id", DEFAULT_GAME_ID))
            val savedPlacementId = normalizePlacementId(prefs.getString("unity_placement_id", DEFAULT_PLACEMENT_ID))
            val savedInterstitialId = prefs.getString("unity_interstitial_id", DEFAULT_INTERSTITIAL_ID)
                ?.trim().orEmpty()
            val savedBannerId = prefs.getString("unity_banner_id", DEFAULT_BANNER_ID)
                ?.trim().orEmpty()
            val savedAccountName = prefs.getString("ads_account_name", DEFAULT_ACCOUNT_NAME)
                ?.trim().orEmpty().ifBlank { DEFAULT_ACCOUNT_NAME }
            val savedAccountEmail = prefs.getString("ads_account_email", DEFAULT_ACCOUNT_EMAIL)
                ?.trim().orEmpty().ifBlank { DEFAULT_ACCOUNT_EMAIL }
            val savedOrgId = prefs.getString("ads_organization_id", DEFAULT_ORGANIZATION_ID)
                ?.trim().orEmpty()
            val savedTestMode = prefs.getBoolean("unity_test_mode", DEFAULT_TEST_MODE)
            val savedAdsEnabled = prefs.getBoolean("ads_enabled", true)
            val savedDailyLimit = prefs.getInt("daily_ad_limit", DEFAULT_DAILY_LIMIT).coerceIn(1, 50)
            val savedRewardPerAd = prefs.getFloat("reward_per_ad", DEFAULT_REWARD_PER_AD.toFloat()).toDouble()
                .takeIf { it > 0.0 } ?: DEFAULT_REWARD_PER_AD

            activeGameId = savedGameId
            activePlacementId = savedPlacementId
            activeInterstitialId = savedInterstitialId
            activeBannerId = savedBannerId
            activeTestMode = savedTestMode
            activeAdsEnabled = savedAdsEnabled

            _accountDetails.value = AdsAccountDetails(
                adsEnabled = savedAdsEnabled,
                accountName = savedAccountName,
                accountEmail = savedAccountEmail,
                organizationId = savedOrgId,
                gameId = savedGameId,
                rewardedPlacementId = savedPlacementId,
                interstitialPlacementId = savedInterstitialId,
                bannerPlacementId = savedBannerId,
                testMode = savedTestMode,
                dailyAdLimit = savedDailyLimit,
                rewardPerAd = savedRewardPerAd
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read cached ads account prefs: ${e.message}")
        }
    }

    private fun savePrefs(context: Context, details: AdsAccountDetails) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("ads_enabled", details.adsEnabled)
                .putString("ads_account_name", details.accountName)
                .putString("ads_account_email", details.accountEmail)
                .putString("ads_organization_id", details.organizationId)
                .putString("unity_game_id", details.gameId)
                .putString("unity_placement_id", details.rewardedPlacementId)
                .putString("unity_interstitial_id", details.interstitialPlacementId)
                .putString("unity_banner_id", details.bannerPlacementId)
                .putBoolean("unity_test_mode", details.testMode)
                .putInt("daily_ad_limit", details.dailyAdLimit)
                .putFloat("reward_per_ad", details.rewardPerAd.toFloat())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist ads account prefs: ${e.message}")
        }
    }

    /**
     * Registers lifecycle callbacks so that whenever Unity's internal AdUnitActivity starts/resumes,
     * UnityIntentApi.setActiveActivity(...) points to our host Activity which safely handles
     * missing Google Play Store (com.android.vending) intents on emulators/devices without Play Store.
     */
    fun registerSafeIntentHost(activity: Activity) {
        safeHostActivityRef = WeakReference(activity)
        bindSafeActivityToUnity(activity)

        if (lifecycleCallbacksRegistered) return
        val app = activity.applicationContext as? Application ?: return
        lifecycleCallbacksRegistered = true

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(act: Activity, savedInstanceState: Bundle?) {
                if (act.javaClass.name.startsWith("com.example")) {
                    safeHostActivityRef = WeakReference(act)
                }
                mainHandler.post { bindSafeActivityToUnity() }
            }

            override fun onActivityStarted(act: Activity) {
                bindSafeActivityToUnity()
            }

            override fun onActivityResumed(act: Activity) {
                if (act.javaClass.name.startsWith("com.example")) {
                    safeHostActivityRef = WeakReference(act)
                }
                bindSafeActivityToUnity()
                mainHandler.post { bindSafeActivityToUnity() }
            }

            override fun onActivityPaused(act: Activity) {
                bindSafeActivityToUnity()
            }

            override fun onActivityStopped(act: Activity) {}
            override fun onActivitySaveInstanceState(act: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(act: Activity) {
                bindSafeActivityToUnity()
            }
        })
    }

    private fun bindSafeActivityToUnity(explicitActivity: Activity? = null) {
        val target = explicitActivity ?: safeHostActivityRef?.get() ?: return
        try {
            ClientProperties.setActivity(target)
            runCatching {
                val intentApiClass = Class.forName("com.unity3d.services.core.api.Intent")
                val method = intentApiClass.getMethod("setActiveActivity", Activity::class.java)
                method.invoke(null, target)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to bind safe host activity to Unity SDK: ${e.message}")
        }
    }

    fun configure(
        context: Context,
        gameId: String,
        placementId: String,
        interstitialPlacementId: String = _accountDetails.value.interstitialPlacementId,
        bannerPlacementId: String = _accountDetails.value.bannerPlacementId,
        accountName: String = _accountDetails.value.accountName,
        accountEmail: String = _accountDetails.value.accountEmail,
        organizationId: String = _accountDetails.value.organizationId,
        testMode: Boolean = _accountDetails.value.testMode,
        adsEnabled: Boolean = _accountDetails.value.adsEnabled,
        dailyAdLimit: Int = _accountDetails.value.dailyAdLimit,
        rewardPerAd: Double = _accountDetails.value.rewardPerAd
    ) {
        ensurePrefsLoaded(context)
        if (context is Activity) {
            registerSafeIntentHost(context)
        }
        val cleanGameId = normalizeGameId(gameId)
        val cleanPlacementId = normalizePlacementId(placementId)
        val cleanInterstitialId = interstitialPlacementId.trim()
        val cleanBannerId = bannerPlacementId.trim()
        val cleanAccountName = accountName.trim().ifBlank { DEFAULT_ACCOUNT_NAME }
        val cleanAccountEmail = accountEmail.trim().ifBlank { DEFAULT_ACCOUNT_EMAIL }
        val cleanOrgId = organizationId.trim()
        val cleanLimit = dailyAdLimit.coerceIn(1, 50)
        val cleanReward = if (rewardPerAd > 0.0) rewardPerAd else DEFAULT_REWARD_PER_AD

        val gameIdOrModeChanged = cleanGameId != activeGameId || testMode != activeTestMode
        val placementChanged = cleanPlacementId != activePlacementId

        activeGameId = cleanGameId
        activePlacementId = cleanPlacementId
        activeInterstitialId = cleanInterstitialId
        activeBannerId = cleanBannerId
        activeTestMode = testMode
        activeAdsEnabled = adsEnabled

        val updatedDetails = AdsAccountDetails(
            adsEnabled = adsEnabled,
            accountName = cleanAccountName,
            accountEmail = cleanAccountEmail,
            organizationId = cleanOrgId,
            gameId = cleanGameId,
            rewardedPlacementId = cleanPlacementId,
            interstitialPlacementId = cleanInterstitialId,
            bannerPlacementId = cleanBannerId,
            testMode = testMode,
            dailyAdLimit = cleanLimit,
            rewardPerAd = cleanReward
        )
        _accountDetails.value = updatedDetails
        savePrefs(context, updatedDetails)

        if (!adsEnabled) {
            _adState.value = AdState.Idle
            return
        }

        if (gameIdOrModeChanged && !isInitializing) {
            isInitialized = false
            isAdLoaded = false
            initialize(context, forceReinit = true)
        } else if (placementChanged) {
            isAdLoaded = false
            if (isInitialized || UnityAds.isInitialized) {
                loadRewardedAd(activePlacementId)
            } else {
                initialize(context)
            }
        } else {
            initialize(context)
        }
    }

    /**
     * Prevents Chromium WebView's `variations_seed_loader.cc` from logging
     * `ERROR:variations_seed_loader.cc(37)] Seed missing signature.` on emulators/devices
     * where `app_webview/variations_seed` or `variations_seed_new` is written without a GMS Finch signature.
     */
    fun sanitizeWebViewVariationsSeed(context: Context) {
        try {
            val appCtx = context.applicationContext ?: context
            val dataDir = appCtx.applicationInfo?.dataDir?.let { java.io.File(it) }
                ?: appCtx.filesDir?.parentFile
                ?: return

            val primaryWebViewDir = java.io.File(dataDir, "app_webview")
            if (!primaryWebViewDir.exists()) {
                primaryWebViewDir.mkdirs()
            }

            val webViewDirs = (dataDir.listFiles { file ->
                file.isDirectory && file.name.startsWith("app_webview")
            }?.toList().orEmpty() + primaryWebViewDir).distinctBy { it.absolutePath }

            val now = System.currentTimeMillis()
            for (dir in webViewDirs) {
                listOf("variations_seed", "variations_seed_new").forEach { seedName ->
                    val seedFile = java.io.File(dir, seedName)
                    if (seedFile.exists()) {
                        seedFile.delete()
                    }
                }
                // Touch variations_stamp with the current timestamp so Chromium's VariationsSeedLoader
                // throttles background seed fetches from VariationsSeedServer.
                val stampFile = java.io.File(dir, "variations_stamp")
                if (!stampFile.exists()) {
                    stampFile.createNewFile()
                }
                stampFile.setLastModified(now)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to sanitize WebView variations seed: ${e.message}")
        }
    }

    fun initialize(
        context: Context,
        forceReinit: Boolean = false,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        sanitizeWebViewVariationsSeed(context)
        ensurePrefsLoaded(context)
        if (context is Activity) {
            registerSafeIntentHost(context)
        }
        if (!activeAdsEnabled) {
            _adState.value = AdState.Idle
            onComplete?.invoke()
            return
        }
        if (!forceReinit && (isInitialized || UnityAds.isInitialized)) {
            isInitialized = true
            onComplete?.invoke()
            if (!isAdLoaded && !isAdLoading) {
                loadRewardedAd(activePlacementId)
            }
            return
        }

        if (isInitializing) return
        isInitializing = true
        _adState.value = AdState.Initializing

        val targetGameId = normalizeGameId(activeGameId)
        val targetTestMode = activeTestMode

        try {
            UnityAds.initialize(
                context.applicationContext,
                targetGameId,
                targetTestMode,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        sanitizeWebViewVariationsSeed(context)
                        Log.d(
                            TAG,
                            "Unity Ads initialized (Game ID: $targetGameId, Placement: $activePlacementId, TestMode: $targetTestMode)"
                        )
                        isInitialized = true
                        isInitializing = false
                        loadRewardedAd(activePlacementId)
                        onComplete?.invoke()
                    }

                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError?,
                        message: String?
                    ) {
                        Log.e(TAG, "Unity Ads initialization failed ($targetGameId): $error - $message")
                        isInitializing = false
                        isInitialized = false
                        val errMsg = message?.takeIf { it.isNotBlank() }
                            ?: "Unity Ads initialization failed ($error)."
                        _adState.value = AdState.Error(errMsg)
                        onError?.invoke(errMsg)
                    }
                }
            )
            // If Unity SDK was already initialized earlier in process, trigger placement load immediately
            if (UnityAds.isInitialized && isInitializing) {
                isInitialized = true
                isInitializing = false
                loadRewardedAd(activePlacementId)
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing Unity Ads", e)
            isInitializing = false
            isInitialized = false
            val errMsg = e.localizedMessage ?: "Failed to initialize Unity Ads SDK."
            _adState.value = AdState.Error(errMsg)
            onError?.invoke(errMsg)
        }
    }

    fun loadRewardedAd(
        placementId: String = activePlacementId,
        onLoaded: (() -> Unit)? = null,
        onLoadError: ((String) -> Unit)? = null
    ) {
        if (!isInitialized && !UnityAds.isInitialized) {
            onLoadError?.invoke("Unity Ads SDK is not initialized yet.")
            return
        }
        isInitialized = true

        val targetPlacement = normalizePlacementId(placementId).ifBlank { activePlacementId }
        if (isAdLoaded && targetPlacement == activePlacementId && onLoaded != null) {
            onLoaded.invoke()
            return
        }

        isAdLoading = true
        _adState.value = AdState.Loading

        try {
            UnityAds.load(
                targetPlacement,
                UnityAdsLoadOptions(),
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placement: String) {
                        Log.d(TAG, "Unity Rewarded Ad loaded: $placement")
                        isAdLoaded = true
                        isAdLoading = false
                        _adState.value = AdState.Ready
                        onLoaded?.invoke()
                    }

                    override fun onUnityAdsFailedToLoad(
                        placement: String,
                        error: UnityAds.UnityAdsLoadError?,
                        message: String?
                    ) {
                        Log.w(TAG, "Unity Ads failed to load $placement: $error - $message")
                        isAdLoaded = false
                        isAdLoading = false
                        val errMsg = message?.takeIf { it.isNotBlank() }
                            ?: "No video ad available right now ($error). Please try again shortly."
                        _adState.value = AdState.Error(errMsg)
                        onLoadError?.invoke(errMsg)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading Unity Ad", e)
            isAdLoaded = false
            isAdLoading = false
            val errMsg = e.localizedMessage ?: "Unable to load rewarded video ad."
            _adState.value = AdState.Error(errMsg)
            onLoadError?.invoke(errMsg)
        }
    }

    fun showRewardedAd(
        activity: Activity,
        placementId: String = activePlacementId,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onAdDismissed: () -> Unit = {},
        onError: (String) -> Unit
    ) {
        registerSafeIntentHost(activity)
        if (!activeAdsEnabled) {
            onError("Rewarded video ads are currently paused by the administrator.")
            return
        }
        val targetPlacement = normalizePlacementId(placementId).ifBlank { activePlacementId }

        if (!isInitialized && !UnityAds.isInitialized) {
            initialize(
                context = activity,
                onComplete = {
                    loadAndDisplayAd(
                        activity = activity,
                        placementId = targetPlacement,
                        onRewardEarned = onRewardEarned,
                        onAdDismissed = onAdDismissed,
                        onError = onError
                    )
                },
                onError = { errMsg ->
                    onError(errMsg)
                }
            )
            return
        }

        isInitialized = true
        loadAndDisplayAd(
            activity = activity,
            placementId = targetPlacement,
            onRewardEarned = onRewardEarned,
            onAdDismissed = onAdDismissed,
            onError = onError
        )
    }

    private fun loadAndDisplayAd(
        activity: Activity,
        placementId: String,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onAdDismissed: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isAdLoaded) {
            displayAd(
                activity = activity,
                placementId = placementId,
                onRewardEarned = onRewardEarned,
                onAdDismissed = onAdDismissed,
                onError = onError
            )
        } else {
            loadRewardedAd(
                placementId = placementId,
                onLoaded = {
                    displayAd(
                        activity = activity,
                        placementId = placementId,
                        onRewardEarned = onRewardEarned,
                        onAdDismissed = onAdDismissed,
                        onError = onError
                    )
                },
                onLoadError = { errMsg ->
                    onError(errMsg)
                }
            )
        }
    }

    private fun displayAd(
        activity: Activity,
        placementId: String,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onAdDismissed: () -> Unit,
        onError: (String) -> Unit
    ) {
        registerSafeIntentHost(activity)
        isAdLoaded = false
        _adState.value = AdState.Showing

        try {
            UnityAds.show(
                activity,
                placementId,
                UnityAdsShowOptions(),
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowStart(placement: String) {
                        Log.d(TAG, "Unity Ad started showing: $placement")
                        bindSafeActivityToUnity(activity)
                    }

                    override fun onUnityAdsShowClick(placement: String) {
                        Log.d(TAG, "Unity Ad clicked: $placement")
                    }

                    override fun onUnityAdsShowComplete(
                        placement: String,
                        state: UnityAds.UnityAdsShowCompletionState?
                    ) {
                        Log.d(TAG, "Unity Ad completed: $placement, state=$state")
                        isAdLoaded = false
                        _adState.value = AdState.Idle
                        // Preload next ad in background
                        loadRewardedAd(placementId)

                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            val randomReward = (1..5).random()
                            onRewardEarned(randomReward)
                        } else if (state == UnityAds.UnityAdsShowCompletionState.SKIPPED) {
                            onError("Ad was skipped before completion. Complete the full video ad to earn your reward.")
                            onAdDismissed()
                        } else {
                            onAdDismissed()
                        }
                    }

                    override fun onUnityAdsShowFailure(
                        placement: String,
                        error: UnityAds.UnityAdsShowError?,
                        message: String?
                    ) {
                        Log.w(TAG, "Unity Ad show failed: $placement, error=$error - $message")
                        isAdLoaded = false
                        _adState.value = AdState.Idle
                        loadRewardedAd(placementId)
                        val errMsg = message?.takeIf { it.isNotBlank() }
                            ?: "Unable to play video ad ($error). Please try again."
                        onError(errMsg)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing Unity Ad", e)
            isAdLoaded = false
            _adState.value = AdState.Idle
            loadRewardedAd(placementId)
            onError(e.localizedMessage ?: "Failed to display video ad.")
        }
    }
}
