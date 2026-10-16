const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "joshua270614@gmail.com";

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

test("Unauthenticated user: cannot read pipelines", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("pipelines").get());
});

test("Authenticated user: cannot read another user's pipeline", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("pipelines").doc("bob_p1").set({
      id: "bob_p1",
      name: "Bob Fraud Pipeline",
      description: "Tabular model",
      category: "Tabular",
      status: "COMPLETED",
      userId: BOB_UID,
      userEmail: "bob@example.com",
      targetCluster: "NVIDIA H100",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("pipelines").doc("bob_p1").get());
});

test("Authenticated user: can read own pipeline", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("pipelines").doc("alice_p1").set({
      id: "alice_p1",
      name: "Alice Vision Pipeline",
      description: "ResNet-50 fine-tuning",
      category: "Computer Vision",
      status: "RUNNING",
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      targetCluster: "Google TPU v5e",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("pipelines").doc("alice_p1").get());
});

test("Admin: can read any pipeline", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("pipelines").doc("bob_p1").set({
      id: "bob_p1",
      name: "Bob Pipeline",
      description: "Processing pipeline",
      category: "NLP",
      status: "COMPLETED",
      userId: BOB_UID,
      userEmail: "bob@example.com",
      targetCluster: "NVIDIA H100",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(adminDb.collection("pipelines").doc("bob_p1").get());
});

test("Non-admin: cannot write to banned_users collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("banned_users").doc("toxic_user").set({
    userId: "toxic_user",
    email: "toxic@example.com",
    reason: "Abusive language",
    bannedBy: ALICE_UID,
    bannedAt: new Date()
  }));
});

test("Admin: can ban toxic users", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(adminDb.collection("banned_users").doc("toxic_user").set({
    userId: "toxic_user",
    email: "toxic@example.com",
    reason: "Toxic behavior in comments",
    bannedBy: ADMIN_UID,
    bannedAt: new Date()
  }));
});
