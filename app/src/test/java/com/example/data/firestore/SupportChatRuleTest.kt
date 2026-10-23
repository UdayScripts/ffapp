package com.example.data.firestore

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportChatRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun sendSupportMessage_withPhoto_createsThreadAndMessage() = runBlocking {
    val aliceUid = signInTestUser("alice_support@test.com")
    val repo = FirestoreRepository(firestore)

    val sampleBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    val result = withTimeout(5000L) {
      repo.sendSupportMessage(
        threadUserId = aliceUid,
        userName = "AlicePro",
        userEmail = "alice_support@test.com",
        userFfIgn = "AliceFF",
        userFfUid = "12345678",
        senderId = aliceUid,
        senderName = "AlicePro",
        isAdminSender = false,
        messageText = "Attached my match result screenshot",
        imageBase64 = sampleBase64
      )
    }

    assertTrue("Expected message send success", result.isSuccess)
    val sentMsg = result.getOrThrow()
    assertEquals("Attached my match result screenshot", sentMsg.messageText)
    assertEquals(sampleBase64, sentMsg.imageBase64)

    val messages = withTimeout(3000L) {
      repo.observeSupportMessages(aliceUid).first { it.isNotEmpty() }
    }
    assertTrue(messages.any { it.id == sentMsg.id && it.imageBase64 == sampleBase64 })

    val thread = withTimeout(3000L) {
      repo.observeSupportThread(aliceUid).first { it != null }
    }
    assertNotNull(thread)
    assertEquals("OPEN", thread?.status)
    assertTrue(thread?.hasImage == true)
  }

  @Test
  fun crossUserAccess_bobCannotReadAliceSupportThread() = runBlocking {
    val aliceUid = signInTestUser("alice_private@test.com")
    val repo = FirestoreRepository(firestore)

    withTimeout(5000L) {
      repo.sendSupportMessage(
        threadUserId = aliceUid,
        userName = "AlicePrivate",
        senderId = aliceUid,
        senderName = "AlicePrivate",
        isAdminSender = false,
        messageText = "Private support question"
      ).getOrThrow()
    }

    signInTestUser("bob_intruder@test.com")
    var denied = false
    try {
      withTimeout(5000L) {
        firestore.collection("support_chats").document(aliceUid).get().await()
      }
    } catch (e: FirebaseFirestoreException) {
      denied = (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED || e.code == FirebaseFirestoreException.Code.UNAVAILABLE)
    }
    assertTrue("Cross-user access should fail with PERMISSION_DENIED or UNAVAILABLE", denied)
  }

  @Test
  fun unauthenticatedUser_cannotReadSupportChats() = runBlocking {
    auth.signOut()
    var denied = false
    var actualError: Throwable? = null
    try {
      withTimeout(5000L) {
        firestore.collection("support_chats").document("any_user").get().await()
      }
    } catch (e: Throwable) {
      actualError = e
      denied = (e is FirebaseFirestoreException && (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED || e.code == FirebaseFirestoreException.Code.UNAVAILABLE))
    }
    assertTrue("Unauthenticated access should fail with PERMISSION_DENIED or UNAVAILABLE, but got: $actualError", denied)
  }
}
