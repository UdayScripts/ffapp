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

test("Unauthenticated user: cannot read tournaments or users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tournaments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
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
