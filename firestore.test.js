const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

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

// 1. Default deny catch-all and unauthenticated rejection on protected collections
test("Unauthenticated caller cannot read or write protected officer collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauth.collection("citizen_disaster_reports").get());
  await assertFails(unauth.collection("officer_credentials").get());
  await assertFails(unauth.collection("device_security_sessions").get());
  await assertFails(unauth.collection("unknown_collection").doc("doc1").set({ foo: "bar" }));
});

// 2. Citizen disaster reports: cross-device officer read & status update + schema validation
test("Citizen disaster reports allow cross-device officer read and status update while enforcing schema", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  const validReport = {
    id: "LAP-2026-001",
    authorUid: ALICE_UID,
    disasterType: "Banjir",
    district: "Kota Mukomuko",
    status: "Diterima PUSDALOPS",
    timestamp: Date.now(),
  };

  // Citizen can create
  await assertSucceeds(alice.collection("citizen_disaster_reports").doc("LAP-2026-001").set(validReport));

  // Unfiltered list succeeds so officer devices can read all incoming citizen reports
  await assertSucceeds(alice.collection("citizen_disaster_reports").get());
  await assertSucceeds(bob.collection("citizen_disaster_reports").get());

  // Officer (Bob) can read and update status of citizen (Alice) report
  await assertSucceeds(bob.collection("citizen_disaster_reports").doc("LAP-2026-001").get());
  await assertSucceeds(
    bob.collection("citizen_disaster_reports").doc("LAP-2026-001").update({
      status: "Divalidasi",
      updatedAt: Date.now(),
    })
  );

  // Non-owner cannot delete report
  await assertFails(bob.collection("citizen_disaster_reports").doc("LAP-2026-001").delete());

  // Ghost field shadow update rejected
  await assertFails(
    alice.collection("citizen_disaster_reports").doc("LAP-2026-001").set(
      { ...validReport, ghostAdminFlag: true },
      { merge: true }
    )
  );
});

// 3. Officer credentials: single-doc NIP lookup allowed for Cloud PIN verification, block list, plaintext initialSecurePin & cross-user overwrite
test("Officer credentials allow single-doc NIP lookup while blocking list, plaintext initialSecurePin, and cross-user tampering", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  const validCred = {
    nip: "197906072006041011",
    ownerUid: ALICE_UID,
    fullName: "Petugas Resmi",
    pinHash: "pbkdf2$10000$" + "a".repeat(64),
    salt: "0123456789abcdef0123456789abcdef",
    isCustomPinSet: true,
    isPinChanged: true,
    initialSecurePin: "",
    updatedAt: Date.now(),
  };

  await assertSucceeds(alice.collection("officer_credentials").doc("197906072006041011").set(validCred));

  // Single-document lookup by exact NIP succeeds for fresh-install Cloud PIN verification
  await assertSucceeds(unauth.collection("officer_credentials").doc("197906072006041011").get());
  // Collection listing remains strictly denied for unauthenticated callers
  await assertFails(unauth.collection("officer_credentials").get());

  // Reject non-empty initialSecurePin
  await assertFails(
    alice.collection("officer_credentials").doc("197906072006041012").set({
      ...validCred,
      nip: "197906072006041012",
      initialSecurePin: "041012",
    })
  );

  // Bob cannot overwrite Alice's Google-owned credential, and nobody can delete it
  await assertFails(
    bob.collection("officer_credentials").doc("197906072006041011").set({
      ...validCred,
      ownerUid: BOB_UID,
      pinHash: "pbkdf2$10000$" + "b".repeat(64),
    })
  );
  await assertFails(alice.collection("officer_credentials").doc("197906072006041011").delete());
});

// 4. Device security sessions & failures (Play Integrity audit)
test("Device security sessions & append-only failures enforce ownerUid isolation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    alice.collection("device_security_sessions").doc("197906072006041011").set({
      sessionId: "sess-001",
      ownerUid: ALICE_UID,
      officerNip: "197906072006041011",
      accessGranted: true,
      timestamp: Date.now(),
    })
  );
  await assertFails(bob.collection("device_security_sessions").doc("197906072006041011").get());
  await assertFails(
    bob.collection("device_security_sessions").doc("197906072006041011").update({ accessGranted: false })
  );
  await assertFails(
    bob.collection("device_security_sessions").doc("197906072006041011").delete()
  );
  await assertSucceeds(
    alice.collection("device_security_sessions").doc("197906072006041011").delete()
  );

  // Append-only audit log
  await assertSucceeds(
    alice.collection("device_security_failures").doc("fail-001").set({
      failureId: "fail-001",
      ownerUid: ALICE_UID,
      officerNip: "197906072006041011",
      reason: "Play Integrity failed",
      timestamp: Date.now(),
    })
  );
  await assertFails(
    alice.collection("device_security_failures").doc("fail-001").update({ reason: "tampered" })
  );
  await assertFails(alice.collection("device_security_failures").doc("fail-001").delete());
});

// 5. Heartbeats, Pings, Officers, Backups & ID poisoning
test("Heartbeats, pings, officers, and ID poisoning checks", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    alice.collection("device_heartbeats").doc("dev_01").set({
      deviceId: "dev_01",
      ownerUid: ALICE_UID,
      status: "ONLINE",
      lastSeen: Date.now(),
    })
  );
  await assertSucceeds(
    alice.collection("network_pings").doc("dev_01").set({
      deviceId: "dev_01",
      ownerUid: ALICE_UID,
      pingTimestamp: Date.now(),
    })
  );
  await assertFails(bob.collection("network_pings").doc("dev_01").get());

  // Path ID poisoning (>128 chars or invalid chars)
  await assertFails(
    alice.collection("officers").doc("bad id with spaces!").set({
      ownerUid: ALICE_UID,
      fullName: "X",
      nip: "123",
      role: "Operator",
    })
  );
});
