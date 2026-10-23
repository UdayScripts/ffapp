package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class AuthenticatedSession(
    val uid: String,
    val username: String,
    val email: String,
    val isAdmin: Boolean,
    val ffIgn: String = "",
    val ffUid: String = "",
    val passwordHash: String = ""
)

object AuthManager {

    private const val TAG = "AuthManager"
    private const val PREFS_NAME = "esports_auth_session"
    const val MASTER_ADMIN_UID = "admin_uday_master"
    const val MASTER_ADMIN_USERNAME = "Uday"
    const val MASTER_ADMIN_EMAIL = "uday@esportsarena.app"

    private val _activeSession = MutableStateFlow<AuthenticatedSession?>(null)
    val activeSession: StateFlow<AuthenticatedSession?> = _activeSession.asStateFlow()

    fun currentFirebaseUser(): FirebaseUser? = FirebaseAuth.getInstance().currentUser

    fun currentSession(): AuthenticatedSession? = _activeSession.value

    private fun getFirestore(): FirebaseFirestore {
        return try {
            val app = FirebaseApp.getInstance()
            val context = app.applicationContext
            val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
            val dbId = if (resId != 0) context.getString(resId) else "(default)"
            if (dbId.isNotBlank() && dbId != "(default)") {
                FirebaseFirestore.getInstance(app, dbId)
            } else {
                FirebaseFirestore.getInstance(app)
            }
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isMasterAdminIdentity(
        email: String? = null,
        username: String? = null,
        uid: String? = null
    ): Boolean {
        if (uid == MASTER_ADMIN_UID) return true
        if (!username.isNullOrBlank() && username.trim().equals(MASTER_ADMIN_USERNAME, ignoreCase = true)) return true
        if (!email.isNullOrBlank() && (
                email.trim().equals("udaynath381@gmail.com", ignoreCase = true) ||
                email.trim().equals(MASTER_ADMIN_EMAIL, ignoreCase = true)
            )
        ) {
            return true
        }
        return false
    }

    fun loadSavedSession(context: Context): AuthenticatedSession? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUid = prefs.getString("session_uid", null)
        if (!savedUid.isNullOrBlank()) {
            val username = prefs.getString("session_username", "Player") ?: "Player"
            val email = prefs.getString("session_email", "") ?: ""
            val isAdmin = prefs.getBoolean("session_is_admin", false) ||
                isMasterAdminIdentity(email = email, username = username, uid = savedUid)
            val ffIgn = prefs.getString("session_ff_ign", "") ?: ""
            val ffUid = prefs.getString("session_ff_uid", "") ?: ""
            val session = AuthenticatedSession(
                uid = savedUid,
                username = username,
                email = email,
                isAdmin = isAdmin,
                ffIgn = ffIgn,
                ffUid = ffUid
            )
            _activeSession.value = session
            return session
        }

        val fbUser = FirebaseAuth.getInstance().currentUser
        if (fbUser != null) {
            val email = fbUser.email ?: ""
            val username = fbUser.displayName?.ifBlank { email.substringBefore("@") } ?: "Player"
            val isAdmin = isMasterAdminIdentity(email = email, username = username, uid = fbUser.uid)
            val session = AuthenticatedSession(
                uid = fbUser.uid,
                username = username,
                email = email,
                isAdmin = isAdmin
            )
            saveSession(context, session)
            return session
        }

        return null
    }

    fun saveSession(context: Context, session: AuthenticatedSession) {
        _activeSession.value = session
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("session_uid", session.uid)
            .putString("session_username", session.username)
            .putString("session_email", session.email)
            .putBoolean("session_is_admin", session.isAdmin)
            .putString("session_ff_ign", session.ffIgn)
            .putString("session_ff_uid", session.ffUid)
            .commit()
    }

    fun updateSessionGamingProfile(
        context: Context,
        ffIgn: String,
        ffUid: String
    ): AuthenticatedSession? {
        val current = _activeSession.value ?: loadSavedSession(context) ?: return null
        val updated = current.copy(
            ffIgn = ffIgn.trim(),
            ffUid = ffUid.trim()
        )
        saveSession(context, updated)
        return updated
    }

    fun clearSavedSession(context: Context) {
        _activeSession.value = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    fun resolveEmailForUsername(usernameOrEmail: String): String {
        val trimmed = usernameOrEmail.trim()
        return if (trimmed.contains("@")) {
            trimmed
        } else {
            val clean = trimmed.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
            "${clean}@esportsarena.app"
        }
    }

    fun normalizeUsernameKey(username: String): String {
        return username.trim().lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
    }

    suspend fun registerWithUsernameAndPassword(
        context: Context,
        username: String,
        email: String,
        password: String,
        ffIgn: String = "",
        ffUid: String = ""
    ): Result<AuthenticatedSession> = withContext(Dispatchers.IO) {
        val trimmedUsername = username.trim()
        val cleanKey = normalizeUsernameKey(trimmedUsername)
        if (cleanKey.length < 3) {
            return@withContext Result.failure(Exception("Username must have at least 3 letters or numbers."))
        }

        // Check if registering as the Master Admin "Uday"
        if (trimmedUsername.equals(MASTER_ADMIN_USERNAME, ignoreCase = true) ||
            email.trim().equals(MASTER_ADMIN_EMAIL, ignoreCase = true)
        ) {
            if (password == "123456") {
                val db = getFirestore()
                var storedIgn = ffIgn.trim()
                var storedUid = ffUid.trim()
                try {
                    val adminDoc = db.collection("users").document(MASTER_ADMIN_UID).get().await()
                    if (adminDoc.exists()) {
                        if (storedIgn.isBlank()) {
                            storedIgn = (adminDoc.getString("ffIgn") ?: adminDoc.getString("inGameName") ?: "").trim()
                        }
                        if (storedUid.isBlank()) {
                            storedUid = (adminDoc.getString("ffUid") ?: adminDoc.getString("inGameId") ?: "").trim()
                        }
                    }
                } catch (_: Exception) {}
                val adminSession = AuthenticatedSession(
                    uid = MASTER_ADMIN_UID,
                    username = MASTER_ADMIN_USERNAME,
                    email = MASTER_ADMIN_EMAIL,
                    isAdmin = true,
                    ffIgn = storedIgn.ifBlank { "Uday_Admin" },
                    ffUid = storedUid,
                    passwordHash = sha256("123456")
                )
                saveSession(context, adminSession)
                return@withContext Result.success(adminSession)
            } else {
                return@withContext Result.failure(
                    Exception("Username 'Uday' is reserved for the Administrator.")
                )
            }
        }

        val resolvedEmail = if (email.isNotBlank() && email.contains("@")) {
            email.trim()
        } else {
            "${cleanKey}@esportsarena.app"
        }
        val passHash = sha256(password)
        val db = getFirestore()
        val deterministicUid = "usr_$cleanKey"

        try {
            // Check if username already exists in Cloud Firestore
            val existingDoc = db.collection("users").document(deterministicUid).get().await()
            if (existingDoc.exists()) {
                return@withContext Result.failure(
                    Exception("Username '$trimmedUsername' is already registered. Please sign in instead.")
                )
            }
            val existingByUsernameQuery = db.collection("users")
                .whereEqualTo("usernameLower", cleanKey)
                .limit(1)
                .get()
                .await()
            if (!existingByUsernameQuery.isEmpty) {
                return@withContext Result.failure(
                    Exception("Username '$trimmedUsername' is already registered. Please sign in instead.")
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Username check warning: ${e.message}")
        }

        val isAdmin = isMasterAdminIdentity(email = resolvedEmail, username = trimmedUsername, uid = deterministicUid)

        // Persist initial user credentials to Cloud Firestore immediately
        try {
            val nowMs = System.currentTimeMillis()
            val initialUserDoc = mapOf(
                "id" to deterministicUid,
                "userId" to deterministicUid,
                "username" to trimmedUsername,
                "usernameLower" to cleanKey,
                "passwordHash" to passHash,
                "email" to resolvedEmail,
                "ffIgn" to ffIgn.trim(),
                "ffUid" to ffUid.trim(),
                "inGameName" to ffIgn.trim(),
                "inGameId" to ffUid.trim(),
                "walletBalance" to 0,
                "winningBalance" to 0,
                "totalWithdrawn" to 0,
                "balance" to 0,
                "isAdmin" to isAdmin,
                "isSuspended" to false,
                "matchesPlayed" to 0,
                "totalKills" to 0,
                "totalWinnings" to 0,
                "bonusClaimed" to false,
                "signupTimestamp" to nowMs
            )
            db.collection("users").document(deterministicUid).set(initialUserDoc).await()
        } catch (e: Exception) {
            Log.w(TAG, "Initial user profile save warning: ${e.message}")
        }

        val session = AuthenticatedSession(
            uid = deterministicUid,
            username = trimmedUsername,
            email = resolvedEmail,
            isAdmin = isAdmin,
            ffIgn = ffIgn.trim(),
            ffUid = ffUid.trim(),
            passwordHash = passHash
        )
        saveSession(context, session)
        Result.success(session)
    }

    suspend fun signInWithUsernameOrEmail(
        context: Context,
        usernameOrEmail: String,
        password: String
    ): Result<AuthenticatedSession> = withContext(Dispatchers.IO) {
        val trimmedInput = usernameOrEmail.trim()
        val cleanKey = normalizeUsernameKey(trimmedInput.substringBefore("@"))
        val passHash = sha256(password)
        val db = getFirestore()

        // 1. Built-in Master Admin "Uday" / "123456" with Full Access
        val isUdayInput = trimmedInput.equals(MASTER_ADMIN_USERNAME, ignoreCase = true) ||
            trimmedInput.equals(MASTER_ADMIN_EMAIL, ignoreCase = true) ||
            trimmedInput.equals("udaynath381@gmail.com", ignoreCase = true)

        if (isUdayInput) {
            if (password == "123456") {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                var savedIgn = if (prefs.getString("session_uid", null) == MASTER_ADMIN_UID) {
                    prefs.getString("session_ff_ign", "") ?: ""
                } else ""
                var savedUid = if (prefs.getString("session_uid", null) == MASTER_ADMIN_UID) {
                    prefs.getString("session_ff_uid", "") ?: ""
                } else ""

                try {
                    val adminDoc = db.collection("users").document(MASTER_ADMIN_UID).get().await()
                    if (adminDoc.exists()) {
                        val cloudIgn = (adminDoc.getString("ffIgn") ?: adminDoc.getString("inGameName") ?: "").trim()
                        val cloudUid = (adminDoc.getString("ffUid") ?: adminDoc.getString("inGameId") ?: "").trim()
                        if (cloudIgn.isNotBlank()) savedIgn = cloudIgn
                        if (cloudUid.isNotBlank()) savedUid = cloudUid
                    } else {
                        val nowMs = System.currentTimeMillis()
                        db.collection("users").document(MASTER_ADMIN_UID).set(
                            mapOf(
                                "id" to MASTER_ADMIN_UID,
                                "userId" to MASTER_ADMIN_UID,
                                "username" to MASTER_ADMIN_USERNAME,
                                "usernameLower" to "uday",
                                "passwordHash" to passHash,
                                "email" to MASTER_ADMIN_EMAIL,
                                "ffIgn" to savedIgn.ifBlank { "Uday_Admin" },
                                "ffUid" to savedUid,
                                "inGameName" to savedIgn.ifBlank { "Uday_Admin" },
                                "inGameId" to savedUid,
                                "walletBalance" to 0,
                                "winningBalance" to 0,
                                "totalWithdrawn" to 0,
                                "isAdmin" to true,
                                "isSuspended" to false,
                                "matchesPlayed" to 0,
                                "totalKills" to 0,
                                "totalWinnings" to 0,
                                "bonusClaimed" to true,
                                "signupTimestamp" to nowMs
                            )
                        ).await()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Admin profile check warning: ${e.message}")
                }

                val adminSession = AuthenticatedSession(
                    uid = MASTER_ADMIN_UID,
                    username = MASTER_ADMIN_USERNAME,
                    email = MASTER_ADMIN_EMAIL,
                    isAdmin = true,
                    ffIgn = savedIgn.ifBlank { "Uday_Admin" },
                    ffUid = savedUid,
                    passwordHash = passHash
                )
                saveSession(context, adminSession)
                return@withContext Result.success(adminSession)
            } else {
                return@withContext Result.failure(Exception("Incorrect password for Admin Uday."))
            }
        }

        // 2. Check Cloud Firestore users collection for matching username or email
        try {
            val deterministicUid = "usr_$cleanKey"
            var matchedDoc = db.collection("users").document(deterministicUid).get().await()

            if (!matchedDoc.exists()) {
                val byLower = db.collection("users")
                    .whereEqualTo("usernameLower", cleanKey)
                    .limit(1)
                    .get()
                    .await()
                if (!byLower.isEmpty) {
                    matchedDoc = byLower.documents.first()
                }
            }

            if (!matchedDoc.exists()) {
                val byExactUsername = db.collection("users")
                    .whereEqualTo("username", trimmedInput)
                    .limit(1)
                    .get()
                    .await()
                if (!byExactUsername.isEmpty) {
                    matchedDoc = byExactUsername.documents.first()
                }
            }

            if (!matchedDoc.exists() && trimmedInput.contains("@")) {
                val byEmail = db.collection("users")
                    .whereEqualTo("email", trimmedInput)
                    .limit(1)
                    .get()
                    .await()
                if (!byEmail.isEmpty) {
                    matchedDoc = byEmail.documents.first()
                }
            }

            if (matchedDoc.exists()) {
                val storedHash = matchedDoc.getString("passwordHash") ?: ""
                if (storedHash.isNotBlank() && storedHash != passHash) {
                    return@withContext Result.failure(Exception("Incorrect password. Please try again."))
                }

                // If account was created before passwordHash was set, bind passwordHash now
                if (storedHash.isBlank()) {
                    try {
                        db.collection("users").document(matchedDoc.id).update(
                            mapOf(
                                "passwordHash" to passHash,
                                "usernameLower" to cleanKey
                            )
                        ).await()
                    } catch (_: Exception) {}
                }

                val uid = matchedDoc.id
                val uname = matchedDoc.getString("username")?.ifBlank { trimmedInput } ?: trimmedInput
                val uemail = matchedDoc.getString("email") ?: resolveEmailForUsername(uname)
                val isAdmin = (matchedDoc.getBoolean("isAdmin") == true) ||
                    isMasterAdminIdentity(email = uemail, username = uname, uid = uid)
                val ffIgn = (matchedDoc.getString("ffIgn") ?: matchedDoc.getString("inGameName") ?: "").trim()
                val ffUid = (matchedDoc.getString("ffUid") ?: matchedDoc.getString("inGameId") ?: "").trim()

                val session = AuthenticatedSession(
                    uid = uid,
                    username = uname,
                    email = uemail,
                    isAdmin = isAdmin,
                    ffIgn = ffIgn,
                    ffUid = ffUid,
                    passwordHash = passHash
                )
                saveSession(context, session)
                return@withContext Result.success(session)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore lookup during sign-in: ${e.message}")
            return@withContext Result.failure(
                Exception("Unable to verify credentials right now. Please check your connection and try again.")
            )
        }

        Result.failure(
            Exception("No account found with username '$trimmedInput'. Please switch to the Register tab to create an account.")
        )
    }

    suspend fun sendPasswordReset(emailOrUsername: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = emailOrUsername.trim()
        if (trimmed.equals(MASTER_ADMIN_USERNAME, ignoreCase = true)) {
            return@withContext Result.success("Master Admin 'Uday' uses the configured administrator password.")
        }
        val cleanKey = normalizeUsernameKey(trimmed.substringBefore("@"))
        return@withContext try {
            val db = getFirestore()
            val doc = db.collection("users").document("usr_$cleanKey").get().await()
            if (doc.exists()) {
                Result.success("Account '@${doc.getString("username") ?: trimmed}' verified. Please contact Admin Support in Live Chat to reset your password.")
            } else {
                Result.failure(Exception("No account found for '$trimmed'. Please register a new account."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Please contact Admin Support to reset your password."))
        }
    }

    // Silent Auto-Sign-In on App Startup
    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: (AuthenticatedSession) -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        val existingSession = loadSavedSession(context)
        if (existingSession != null) {
            onAuthSuccess(existingSession)
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w(TAG, "default_web_client_id not found: ${e.message}")
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        val email = user.email ?: ""
                        val uname = user.displayName?.ifBlank { email.substringBefore("@") } ?: "Player"
                        val session = AuthenticatedSession(
                            uid = user.uid,
                            username = uname,
                            email = email,
                            isAdmin = isMasterAdminIdentity(email = email, username = uname, uid = user.uid)
                        )
                        saveSession(context, session)
                        onAuthSuccess(session)
                    } else {
                        onUnauthenticated()
                    }
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                onUnauthenticated()
            }
        }
    }

    // Interactive Google Sign-In
    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: (AuthenticatedSession) -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val activity = context as? Activity
                    ?: throw IllegalStateException("Context must be an Activity for CredentialManager interactive prompt")
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        val email = user.email ?: ""
                        val uname = user.displayName?.ifBlank { email.substringBefore("@") } ?: "Player"
                        val session = AuthenticatedSession(
                            uid = user.uid,
                            username = uname,
                            email = email,
                            isAdmin = isMasterAdminIdentity(email = email, username = uname, uid = user.uid)
                        )
                        saveSession(context, session)
                        onAuthSuccess(session)
                    } else {
                        onAuthError("Sign in failed: empty user")
                    }
                } else {
                    onAuthError("Unexpected credential type: ${credential::class.java.simpleName}")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    // Sign-Out Flow
    fun signOut(
        context: Context,
        credentialManager: CredentialManager,
        onSignOutComplete: () -> Unit,
        scope: CoroutineScope
    ) {
        clearSavedSession(context)
        FirebaseAuth.getInstance().signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state", e)
            } finally {
                onSignOutComplete()
            }
        }
    }
}
