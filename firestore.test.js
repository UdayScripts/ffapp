const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "udaynath381@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read tournaments, users, or support chats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tournaments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("support_chats").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("support_chats").doc(ALICE_UID).collection("messages").get());
});

test("Authenticated user: can read tournaments", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tournaments").doc("tourney_1").set({
      id: "tourney_1",
      title: "Bermuda Clash Solo",
      matchType: "Solo",
      map: "Bermuda",
      entryFee: 50,
      prizePool: 500,
      perKill: 10,
      maxSlots: 48,
      joinedSlots: 10,
      status: "open"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tournaments").doc("tourney_1").get());
  await assertSucceeds(aliceDb.collection("tournaments").get());
});

test("Regular user cannot create tournament, but admin can", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tournaments").doc("tourney_2").set({
    id: "tourney_2",
    title: "Unauthorized Tournament",
    matchType: "Solo",
    map: "Bermuda",
    entryFee: 0,
    prizePool: 100,
    perKill: 0,
    maxSlots: 48,
    joinedSlots: 0,
    status: "upcoming"
  }));

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(adminDb.collection("tournaments").doc("tourney_2").set({
    id: "tourney_2",
    title: "Authorized Tournament",
    matchType: "Solo",
    map: "Bermuda",
    entryFee: 0,
    prizePool: 100,
    perKill: 0,
    maxSlots: 48,
    joinedSlots: 0,
    status: "upcoming"
  }));
});

test("User can create their own profile and read it, but cannot read others", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    email: "alice@example.com",
    username: "AliceFF",
    inGameName: "AlicePro",
    inGameId: "987654321",
    balance: 100,
    role: "user"
  }));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("User can create deposit transaction and read their own transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("transactions").doc("tx_alice_1").set({
    id: "tx_alice_1",
    userId: ALICE_UID,
    type: "DEPOSIT",
    amount: 100,
    title: "Instant Top-Up",
    status: "SUCCESS"
  }));

  await assertSucceeds(
    aliceDb.collection("transactions").where("userId", "==", ALICE_UID).get()
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("transactions").doc("tx_alice_1").get()
  );
});

test("Support Chat: User can create their own thread, send text & photo messages, and read them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const threadRef = aliceDb.collection("support_chats").doc(ALICE_UID);

  await assertSucceeds(threadRef.set({
    userId: ALICE_UID,
    userName: "AliceFF",
    userEmail: "alice@example.com",
    userFfIgn: "AlicePro",
    userFfUid: "987654321",
    lastMessage: "📷 Sent a screenshot of my match",
    lastSenderId: ALICE_UID,
    lastSenderIsAdmin: false,
    lastMessageTimestamp: 1700000000000,
    unreadByAdmin: 1,
    unreadByUser: 0,
    status: "OPEN",
    hasImage: true
  }));

  await assertSucceeds(threadRef.collection("messages").doc("msg_1").set({
    id: "msg_1",
    threadUserId: ALICE_UID,
    senderId: ALICE_UID,
    senderName: "AliceFF",
    isAdminSender: false,
    messageText: "Here is my payment screenshot",
    imageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
    timestamp: 1700000000000
  }));

  await assertSucceeds(threadRef.get());
  await assertSucceeds(threadRef.collection("messages").get());
});

test("Support Chat: Another user cannot read or send messages in Alice's thread, but Admin can", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("support_chats").doc(ALICE_UID).set({
      userId: ALICE_UID,
      userName: "AliceFF",
      lastMessage: "Need help with withdrawal",
      lastSenderId: ALICE_UID,
      lastSenderIsAdmin: false,
      lastMessageTimestamp: 1700000000000,
      unreadByAdmin: 1,
      unreadByUser: 0,
      status: "OPEN"
    });
    await db.collection("support_chats").doc(ALICE_UID).collection("messages").doc("msg_1").set({
      id: "msg_1",
      threadUserId: ALICE_UID,
      senderId: ALICE_UID,
      senderName: "AliceFF",
      isAdminSender: false,
      messageText: "Need help with withdrawal",
      timestamp: 1700000000000
    });
  });

  // Bob cannot read or write to Alice's support chat
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("support_chats").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("support_chats").doc(ALICE_UID).collection("messages").get());
  await assertFails(bobDb.collection("support_chats").doc(ALICE_UID).collection("messages").doc("msg_bob").set({
    id: "msg_bob",
    threadUserId: ALICE_UID,
    senderId: BOB_UID,
    senderName: "Bob",
    isAdminSender: false,
    messageText: "Hijacking thread",
    timestamp: 1700000005000
  }));

  // Admin can list all support threads, read Alice's messages, and reply with photo
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(adminDb.collection("support_chats").get());
  await assertSucceeds(adminDb.collection("support_chats").doc(ALICE_UID).collection("messages").get());
  await assertSucceeds(adminDb.collection("support_chats").doc(ALICE_UID).collection("messages").doc("msg_admin_1").set({
    id: "msg_admin_1",
    threadUserId: ALICE_UID,
    senderId: ADMIN_UID,
    senderName: "Support Team",
    isAdminSender: true,
    messageText: "We have verified your screenshot and approved your request!",
    imageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
    timestamp: 1700000010000
  }));
});
