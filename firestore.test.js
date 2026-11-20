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
  const unauth = testEnv.unauthenticatedContext().firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  const validReport = {
    id: "LAP-2026-001",
    authorUid: "PUBLIC_CITIZEN_MUKOMUKO",
    disasterType: "Banjir",
    district: "Kota Mukomuko",
    status: "Diproses",
    timestamp: Date.now(),
    updatedAt: Date.now(),
  };

  // Unauthenticated citizen can create a disaster report
  await assertSucceeds(unauth.collection("citizen_disaster_reports").doc("LAP-2026-001").set(validReport));

  // Unfiltered list succeeds so officer devices (Google-signed-in or NIP-only) can read all incoming citizen reports
  await assertSucceeds(alice.collection("citizen_disaster_reports").get());
  await assertSucceeds(bob.collection("citizen_disaster_reports").get());
  await assertSucceeds(unauth.collection("citizen_disaster_reports").get());

  // Active officer (Bob or NIP-only officer) can read and update status + notes + active officer attribution
  await assertSucceeds(bob.collection("citizen_disaster_reports").doc("LAP-2026-001").get());
  await assertSucceeds(
    bob.collection("citizen_disaster_reports").doc("LAP-2026-001").update({
      status: "Divalidasi",
      additionalNotes: "Tim TRC sudah meluncur ke lokasi banjir.",
      handledByOfficerName: "YUDHA SOFRIANTO, S.A.P.",
      handledByOfficerNip: "199807202025061002",
      handledByOfficerRole: "Operator Pusdalops",
      updatedAt: Date.now(),
    })
  );
  await assertSucceeds(
    unauth.collection("citizen_disaster_reports").doc("LAP-2026-001").update({
      status: "Selesai",
      handledByOfficerName: "MANAJER PUSDALOPS",
      handledByOfficerNip: "197906072006041011",
      handledByOfficerRole: "Manajer Pusdalops-PB",
      updatedAt: Date.now(),
    })
  );

  // Mutating immutable authorUid on update must be rejected
  await assertFails(
    bob.collection("citizen_disaster_reports").doc("LAP-2026-001").update({
      authorUid: BOB_UID,
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

  // Firebase Storage HTTPS download URL in fotoUrl, fotoLampiran & photoUri succeeds on create & officer read
  const storageUrl = "https://firebasestorage.googleapis.com/v0/b/jazzakallah.firebasestorage.app/o/citizen_reports%2FLAP-2026-STORAGE.jpg?alt=media";
  await assertSucceeds(
    unauth.collection("citizen_disaster_reports").doc("LAP-2026-STORAGE").set({
      ...validReport,
      id: "LAP-2026-STORAGE",
      fotoUrl: storageUrl,
      fotoLampiran: storageUrl,
      photoUri: storageUrl,
    })
  );
  await assertSucceeds(
    bob.collection("citizen_disaster_reports").doc("LAP-2026-STORAGE").get()
  );

  // Inline compressed JPEG Data URI photo evidence (e.g. 20 KB Base64) succeeds on create & officer read
  const sampleDataUri = "data:image/jpeg;base64," + "A".repeat(20000);
  await assertSucceeds(
    unauth.collection("citizen_disaster_reports").doc("LAP-2026-PHOTO-OK").set({
      ...validReport,
      id: "LAP-2026-PHOTO-OK",
      fotoUrl: sampleDataUri,
      photoUri: sampleDataUri,
    })
  );
  await assertSucceeds(
    bob.collection("citizen_disaster_reports").doc("LAP-2026-PHOTO-OK").get()
  );

  // Oversized fotoUrl / photoUri (> 400000 chars) must be rejected to protect Firestore 1 MiB document budget
  const oversizedDataUri = "data:image/jpeg;base64," + "A".repeat(400100);
  await assertFails(
    unauth.collection("citizen_disaster_reports").doc("LAP-2026-PHOTO-BIG").set({
      ...validReport,
      id: "LAP-2026-PHOTO-BIG",
      fotoUrl: oversizedDataUri,
      photoUri: oversizedDataUri,
    })
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

  // Self-service first-time officer activation & PIN update (unauthenticated device with ownerUid = officer_nip_{nip})
  const selfNip = "199807202025061002";
  const selfCred = {
    nip: selfNip,
    ownerUid: `officer_nip_${selfNip}`,
    fullName: "YUDHA SOFRIANTO,S.A.P",
    pinHash: "pbkdf2$10000$" + "c".repeat(64),
    salt: "fedcba9876543210fedcba9876543210",
    isCustomPinSet: false,
    isPinChanged: false,
    initialSecurePin: "",
    updatedAt: Date.now(),
  };
  await assertSucceeds(unauth.collection("officer_credentials").doc(selfNip).set(selfCred));
  await assertSucceeds(
    unauth.collection("officer_credentials").doc(selfNip).set(
      {
        ...selfCred,
        pinHash: "pbkdf2$10000$" + "d".repeat(64),
        isCustomPinSet: true,
        isPinChanged: true,
        updatedAt: Date.now(),
      },
      { merge: true }
    )
  );

  // Reject non-empty initialSecurePin
  await assertFails(
    alice.collection("officer_credentials").doc("197906072006041012").set({
      ...validCred,
      nip: "197906072006041012",
      initialSecurePin: "041012",
    })
  );

  // Login metadata update (lastLoginAt / lastLoginDevice) preserving ownerUid & pinHash succeeds
  await assertSucceeds(
    unauth.collection("officer_credentials").doc("197906072006041011").set(
      {
        ...validCred,
        lastLoginAt: Date.now(),
        lastLoginDevice: "Pixel Test Device",
        updatedAt: Date.now(),
      },
      { merge: true }
    )
  );

  // First-time activation of an admin-seeded HITATUN ARAZAK record with empty pinHash/salt succeeds while preserving ownerUid
  const hitatunNip = "197401282006041006";
  // Seed unhashed record via rules-bypass and verify unauthenticated first-time hash activation works
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("officer_credentials").doc(hitatunNip).set({
      nip: hitatunNip,
      ownerUid: ALICE_UID,
      fullName: "HITATUN ARAZAK",
      pinHash: "",
      salt: "",
      isCustomPinSet: false,
      isPinChanged: false,
      initialSecurePin: "",
      updatedAt: Date.now(),
    });
  });
  await assertSucceeds(
    unauth.collection("officer_credentials").doc(hitatunNip).set(
      {
        nip: hitatunNip,
        ownerUid: ALICE_UID,
        fullName: "HITATUN ARAZAK",
        pinHash: "pbkdf2$10000$" + "e".repeat(64),
        salt: "11223344556677889900aabbccddeeff",
        isCustomPinSet: false,
        isPinChanged: false,
        initialSecurePin: "",
        lastLoginAt: Date.now(),
        updatedAt: Date.now(),
      },
      { merge: true }
    )
  );

  // Full fresh-state normalization of uncustomized initial HITATUN ARAZAK record succeeds
  await assertSucceeds(
    unauth.collection("officer_credentials").doc(hitatunNip).set({
      nip: hitatunNip,
      ownerUid: ALICE_UID,
      fullName: "HITATUN ARAZAK",
      role: "Manajer Pusdalops-PB",
      unit: "Pusdalops BPBD Kab. Mukomuko",
      shift: "Manajerial 24 Jam",
      phone: "",
      pinHash: "pbkdf2$10000$" + "f".repeat(64),
      salt: "ffeeddccbbaa00998877665544332211",
      isCustomPinSet: false,
      isPinChanged: false,
      initialSecurePin: "",
      lastLoginAt: 0,
      lastLoginDevice: "",
      updatedAt: Date.now(),
    })
  );

  // Officer can upgrade an uncustomized Admin-seeded record to a personal custom PIN (preserving ownerUid)
  await assertSucceeds(
    unauth.collection("officer_credentials").doc(hitatunNip).set({
      nip: hitatunNip,
      ownerUid: ALICE_UID,
      fullName: "HITATUN ARAZAK",
      role: "Manajer Pusdalops-PB",
      unit: "Pusdalops BPBD Kab. Mukomuko",
      shift: "Manajerial 24 Jam",
      phone: "",
      pinHash: "pbkdf2$10000$" + "9".repeat(64),
      salt: "11223344556677889900aabbccddeeff",
      isCustomPinSet: true,
      isPinChanged: true,
      initialSecurePin: "",
      lastLoginAt: Date.now(),
      lastLoginDevice: "Pixel Officer Device",
      updatedAt: Date.now(),
    })
  );

  // CRITICAL ANTI-REVERT: Once isCustomPinSet == true, unauthenticated client/reinstall MUST NOT downgrade back to default!
  await assertFails(
    unauth.collection("officer_credentials").doc(hitatunNip).set({
      nip: hitatunNip,
      ownerUid: ALICE_UID,
      fullName: "HITATUN ARAZAK",
      role: "Manajer Pusdalops-PB",
      unit: "Pusdalops BPBD Kab. Mukomuko",
      shift: "Manajerial 24 Jam",
      phone: "",
      pinHash: "pbkdf2$10000$" + "0".repeat(64),
      salt: "ffeeddccbbaa00998877665544332211",
      isCustomPinSet: false,
      isPinChanged: false,
      initialSecurePin: "",
      lastLoginAt: 0,
      lastLoginDevice: "",
      updatedAt: Date.now(),
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

// 4. Device security sessions, login activities & failures (Play Integrity audit)
test("Device security sessions, login activities & append-only failures enforce schema and delete ownership", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    alice.collection("device_security_sessions").doc("197906072006041011").set({
      sessionId: "sess-001",
      ownerUid: ALICE_UID,
      officerNip: "197906072006041011",
      officerName: "PETUGAS CONTOH",
      officerRole: "Operator Pusdalops",
      integrityToken: "classic-token-payload-12345",
      tokenHash: "classic-token-payload",
      tokenLength: 27,
      tokenSource: "PLAY_INTEGRITY_CLASSIC",
      classicNonce: "nonce-abc-123",
      verdict: "MEETS_STRONG_INTEGRITY",
      deviceVerdict: "HARDWARE_BACKED_EVALUATION",
      deviceId: "android-hw-id-01",
      deviceModel: "Google Pixel 8",
      deviceBrand: "google",
      deviceManufacturer: "Google",
      deviceHardware: "shiba",
      deviceFingerprintHash: "a1b2c3d4e5f6",
      isEmulator: false,
      osVersion: "Android 14 (API 34)",
      sdkInt: 34,
      appVersion: "1.0.0",
      loginMethod: "PIN_CLOUD",
      loginStatus: "SUCCESS",
      loginAction: "LOGIN_SUCCESSFUL_CLOUD_VERIFIED",
      loginCount: 2,
      lastActivityDetail: "Login PIN Cloud terverifikasi",
      isVerified: true,
      accessGranted: true,
      adminVerified: true,
      adminVerifiedBy: "Admin Pusdalops",
      adminVerifiedAt: Date.now(),
      adminVerificationNote: "Hardware terverifikasi asli",
      targetModule: "RUANG_KERJA",
      timestamp: Date.now(),
    })
  );
  // Single-doc get allowed for Ruang Kerja verification; unauthenticated collection list blocked
  await assertSucceeds(unauth.collection("device_security_sessions").doc("197906072006041011").get());
  await assertFails(unauth.collection("device_security_sessions").get());
  // Authenticated admin can list all sessions
  await assertSucceeds(bob.collection("device_security_sessions").get());
  // Admin can update verification & access permission fields
  await assertSucceeds(
    bob.collection("device_security_sessions").doc("197906072006041011").update({
      adminVerified: true,
      adminVerifiedBy: "Admin Bob",
      adminVerifiedAt: Date.now(),
      accessGranted: false,
      loginStatus: "REVOKED",
    })
  );
  // Non-owner cannot delete session, owner can delete
  await assertFails(
    bob.collection("device_security_sessions").doc("197906072006041011").delete()
  );
  await assertSucceeds(
    alice.collection("device_security_sessions").doc("197906072006041011").delete()
  );

  // Append-only device_login_activities: officer can append with signed-in UID or officer_nip_{nip}
  const activityPayload = {
    activityId: "act_001",
    ownerUid: ALICE_UID,
    sessionId: "sess-001",
    officerNip: "197906072006041011",
    officerName: "PETUGAS CONTOH",
    officerRole: "Operator Pusdalops",
    loginMethod: "PIN_CLOUD",
    loginStatus: "SUCCESS",
    action: "LOGIN_SUCCESSFUL_CLOUD_VERIFIED",
    verdict: "MEETS_STRONG_INTEGRITY",
    deviceVerdict: "HARDWARE_BACKED_EVALUATION",
    tokenSource: "PLAY_INTEGRITY_CLASSIC",
    tokenHash: "classic-token-hash",
    tokenLength: 120,
    classicNonce: "nonce-abc-123",
    deviceId: "android-hw-id-01",
    deviceModel: "Google Pixel 8",
    deviceBrand: "google",
    deviceHardware: "shiba",
    deviceFingerprintHash: "a1b2c3d4e5f6",
    osVersion: "Android 14 (API 34)",
    sdkInt: 34,
    isEmulator: false,
    appVersion: "1.0.0",
    accessGranted: true,
    detail: "Login berhasil",
    timestamp: Date.now(),
  };
  await assertSucceeds(alice.collection("device_login_activities").doc("act_001").set(activityPayload));
  // Unauthenticated NIP-only officer login can append activity with matching officer_nip_{nip} ownerUid
  await assertSucceeds(
    unauth.collection("device_login_activities").doc("act_002").set({
      ...activityPayload,
      activityId: "act_002",
      ownerUid: "officer_nip_197906072006041011",
      loginMethod: "BIOMETRIK",
    })
  );
  // Unauthenticated cannot read/list device_login_activities
  await assertFails(unauth.collection("device_login_activities").get());
  await assertFails(unauth.collection("device_login_activities").doc("act_001").get());
  // Authenticated admin can read & list all login activities
  await assertSucceeds(bob.collection("device_login_activities").get());
  // Append-only: nobody can update an existing activity
  await assertFails(alice.collection("device_login_activities").doc("act_001").update({ loginStatus: "TAMPERED" }));
  // Non-owner cannot delete; owner can delete
  await assertFails(bob.collection("device_login_activities").doc("act_001").delete());
  await assertSucceeds(alice.collection("device_login_activities").doc("act_001").delete());
  // Ghost field rejected on device_login_activities
  await assertFails(
    alice.collection("device_login_activities").doc("act_ghost").set({
      ...activityPayload,
      activityId: "act_ghost",
      ghostField: "bad",
    })
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
  // Reject ghost field on network_pings
  await assertFails(
    alice.collection("network_pings").doc("dev_02").set({
      deviceId: "dev_02",
      ownerUid: ALICE_UID,
      pingTimestamp: Date.now(),
      injectedAdmin: true,
    })
  );

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
