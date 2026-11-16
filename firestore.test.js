const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-farsijoo-5394a3ff-c0e1-4cf3-87c3-3cb1ca4dfda0";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ts = () => new Date(Date.now() - 2000);

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

test("Unauthenticated user cannot read or write user profile", async () => {
  const db = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(db.collection("users").doc(ALICE_UID).get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("lesson_progress").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("daily_usage").get());
});

test("Authenticated owner can create and update their profile, non-owner cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Dara",
      totalStars: 0,
      completedLessonsCount: 0,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      totalStars: 60,
      updatedAt: ts(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: ts(),
    })
  );
});

test("Lesson progress enforces owner isolation and valid ranges", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lesson_progress").doc("lesson_1").set({
      userId: ALICE_UID,
      lessonId: 1,
      completed: true,
      stars: 3,
      bestScore: 5,
      totalQuestions: 5,
      updatedAt: ts(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("lesson_progress").doc("lesson_1").update({
      stars: 99,
      updatedAt: ts(),
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("lesson_progress").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lesson_progress").get()
  );
});

test("Daily chat usage enforces max 10 messages/day and monotonic increase", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("daily_usage").doc("2026-11-16");

  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-16",
      messageCount: 1,
      updatedAt: ts(),
    })
  );

  await assertFails(
    docRef.update({
      messageCount: 0,
      updatedAt: ts(),
    })
  );

  await assertFails(
    docRef.update({
      messageCount: 11,
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    docRef.update({
      messageCount: 10,
      updatedAt: ts(),
    })
  );
});

test("Chat reports are append-only and can be deleted during account cleanup", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const reportRef = aliceDb.collection("users").doc(ALICE_UID).collection("chat_reports").doc("rep_1");

  await assertSucceeds(
    reportRef.set({
      userId: ALICE_UID,
      promptText: "Salam",
      responseText: "Sample reply",
      reason: "Off-topic",
      createdAt: ts(),
    })
  );

  await assertFails(
    reportRef.update({ reason: "Edited" })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("chat_reports").doc("rep_1").delete()
  );

  await assertSucceeds(reportRef.delete());
});
