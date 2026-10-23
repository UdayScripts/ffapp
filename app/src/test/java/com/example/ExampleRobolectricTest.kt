package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthManager
import com.example.auth.AuthenticatedSession
import com.example.viewmodel.AppViewModel
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(application).isEmpty()) {
            FirebaseApp.initializeApp(application)
        }
        AuthManager.clearSavedSession(application)
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("eSports Arena", appName)
    }

    @Test
    fun `user can update Free Fire IGN and UID and persist across session`() {
        val playerSession = AuthenticatedSession(
            uid = "user_rahul_ff",
            username = "RahulPro",
            email = "rahulpro@esportsarena.app",
            isAdmin = false,
            ffIgn = "",
            ffUid = ""
        )
        AuthManager.saveSession(application, playerSession)

        val viewModel = AppViewModel(application)
        viewModel.onSessionAuthenticated(playerSession, isExplicitSignIn = false)
        ShadowLooper.idleMainLooper()

        val initialUser = viewModel.currentUser.value
        assertNotNull("currentUser should be non-null when signed in", initialUser)
        assertEquals("RahulPro", initialUser?.username)

        // Update Free Fire IGN and UID (as triggered from ProfileScreen -> EditProfileModalDialog)
        viewModel.updateGamingProfile("THUNDER_RAHUL", "9876543210")
        ShadowLooper.idleMainLooper()

        val updatedUser = viewModel.currentUser.value
        assertNotNull(updatedUser)
        assertEquals("THUNDER_RAHUL", updatedUser?.ffIgn)
        assertEquals("9876543210", updatedUser?.ffUid)

        // Verify persisted in AuthManager SharedPreferences session
        val savedSession = AuthManager.loadSavedSession(application)
        assertNotNull(savedSession)
        assertEquals("THUNDER_RAHUL", savedSession?.ffIgn)
        assertEquals("9876543210", savedSession?.ffUid)
    }

    @Test
    fun `master admin Uday has full admin access and users list is populated`() {
        val adminSession = AuthenticatedSession(
            uid = AuthManager.MASTER_ADMIN_UID,
            username = AuthManager.MASTER_ADMIN_USERNAME,
            email = AuthManager.MASTER_ADMIN_EMAIL,
            isAdmin = true,
            ffIgn = "UDAY_BOSS",
            ffUid = "1122334455"
        )
        AuthManager.saveSession(application, adminSession)

        val viewModel = AppViewModel(application)
        viewModel.onSessionAuthenticated(adminSession, isExplicitSignIn = false)
        ShadowLooper.idleMainLooper()

        assertTrue("Master Admin Uday must be recognized as real admin", viewModel.isRealAdmin.value)
        assertTrue("isUserRealAdmin() helper must return true for Uday", viewModel.isUserRealAdmin())

        val usersList = viewModel.allUsers.value
        assertTrue("Admin users list must include at least the active admin user", usersList.isNotEmpty())
        val udayInList = usersList.firstOrNull { it.id == AuthManager.MASTER_ADMIN_UID }
        assertNotNull("Master Admin user should be present in allUsers", udayInList)
        assertEquals("Uday", udayInList?.username)

        // Admin updates gaming ID
        viewModel.adminUpdateUserGamingProfile(AuthManager.MASTER_ADMIN_UID, "UDAY_LEGEND", "9988776655")
        ShadowLooper.idleMainLooper()
        assertEquals("UDAY_LEGEND", viewModel.currentUser.value?.ffIgn)
        assertEquals("9988776655", viewModel.currentUser.value?.ffUid)
        val udayUpdatedInAllUsers = viewModel.allUsers.value.firstOrNull { it.id == AuthManager.MASTER_ADMIN_UID }
        assertEquals("UDAY_LEGEND", udayUpdatedInAllUsers?.ffIgn)
        assertEquals("9988776655", udayUpdatedInAllUsers?.ffUid)
    }
}
