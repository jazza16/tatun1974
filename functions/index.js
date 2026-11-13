const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { GoogleAuth } = require("google-auth-library");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

const EXPECTED_PACKAGE_NAME = "com.aistudio.cuacamukomuko.wmzq";
const MAX_TOKEN_AGE_MS = 5 * 60 * 1000; // Maksimal 5 menit
const COLLECTION_SECURITY_SESSIONS = "device_security_sessions";
const COLLECTION_NONCE_STORE = "play_integrity_nonces";

/**
 * 1. Endpoint Penanganan Nonce:
 * Menghasilkan nonce kriptografis acak (URL-safe Base64) untuk dikirim ke aplikasi Android.
 */
exports.generateIntegrityNonce = functions.https.onRequest(async (req, res) => {
  try {
    if (req.method !== "POST" && req.method !== "GET") {
      return res.status(405).json({ error: "Method Not Allowed" });
    }

    const nip = (req.body && req.body.nip) || req.query.nip || "ANONYMOUS";
    const deviceId = (req.body && req.body.deviceId) || req.query.deviceId || "UNKNOWN";

    // Generate 32-byte random cryptographic nonce (URL-safe Base64 tanpa padding)
    const randomPart = crypto.randomBytes(24).toString("base64url");
    const rawPayload = `${nip}:${deviceId}:${Date.now()}:${randomPart}`;
    const nonce = crypto.createHash("sha256").update(rawPayload).digest("base64url");
    const now = Date.now();

    await db.collection(COLLECTION_NONCE_STORE).doc(nonce).set({
      nonce,
      nip,
      deviceId,
      createdAt: now,
      expiresAt: now + MAX_TOKEN_AGE_MS,
      used: false,
    });

    return res.status(200).json({
      nonce,
      expiresAt: now + MAX_TOKEN_AGE_MS,
    });
  } catch (error) {
    console.error("Error generating nonce:", error);
    return res.status(500).json({ error: "Gagal membuat nonce keamanan." });
  }
});

/**
 * 2. Cloud Function verifyIntegrityToken:
 * Menerima token & nonce dari aplikasi, memanggil playintegrity.googleapis.com untuk mendekripsi token,
 * memvalidasi klaim Play Integrity Classic, dan menyimpan sesi ke koleksi 'device_security_sessions'.
 */
exports.verifyIntegrityToken = functions.https.onRequest(async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({ error: "Method Not Allowed" });
    }

    const {
      integrityToken,
      nonce,
      nip,
      officerName = "",
      officerRole = "",
      deviceId = "",
      deviceModel = "",
      osVersion = "",
      sdkInt = 0,
    } = req.body || {};

    if (!integrityToken || !nonce || !nip) {
      return res.status(400).json({
        isAuthorized: false,
        message: "Parameter integrityToken, nonce, dan nip wajib diisi.",
      });
    }

    // Dekripsi token melalui Google Play Integrity API (playintegrity.googleapis.com)
    const auth = new GoogleAuth({
      scopes: ["https://www.googleapis.com/auth/playintegrity"],
    });
    const client = await auth.getClient();
    const url = `https://playintegrity.googleapis.com/v1/${EXPECTED_PACKAGE_NAME}:decodeIntegrityToken`;

    const apiResponse = await client.request({
      url,
      method: "POST",
      data: { integrityToken },
    });

    const payload = apiResponse.data && apiResponse.data.tokenPayloadExternal;
    if (!payload) {
      return res.status(403).json({
        isAuthorized: false,
        message: "Payload token Play Integrity kosong atau gagal didekripsi.",
      });
    }

    const { requestDetails, appIntegrity, deviceIntegrity } = payload;

    // Validasi 1: Pastikan requestPackageName sesuai
    if (!requestDetails || requestDetails.requestPackageName !== EXPECTED_PACKAGE_NAME) {
      return res.status(403).json({
        isAuthorized: false,
        message: `Package name tidak sesuai: ${requestDetails?.requestPackageName}`,
      });
    }

    // Validasi 2: Pastikan nonce cocok & validasi One-Time Nonce di Firestore (Anti-Replay Attack)
    if (requestDetails.nonce !== nonce) {
      return res.status(403).json({
        isAuthorized: false,
        message: "Nonce pada token tidak cocok dengan nonce yang dikirim.",
      });
    }

    const nonceRef = db.collection(COLLECTION_NONCE_STORE).doc(nonce);
    const nonceSnap = await nonceRef.get();
    if (!nonceSnap.exists) {
      return res.status(403).json({
        isAuthorized: false,
        message: "Nonce tidak terdaftar atau tidak diterbitkan oleh server resmi.",
      });
    }

    const nonceData = nonceSnap.data();
    const now = Date.now();
    if (nonceData.used === true || now > Number(nonceData.expiresAt || 0)) {
      return res.status(403).json({
        isAuthorized: false,
        message: "Nonce sudah pernah digunakan (Replay Attack terdeteksi) atau telah kedaluwarsa.",
      });
    }
    if (nonceData.nip !== nip) {
      return res.status(403).json({
        isAuthorized: false,
        message: "Nonce tidak sesuai dengan NIP petugas yang melakukan permintaan.",
      });
    }

    // Tandai nonce sebagai sudah terpakai (One-Time Token)
    await nonceRef.update({ used: true, usedAt: now });

    // Validasi 3: Pastikan timestampMillis tidak lebih dari 5 menit
    const tokenTimestamp = Number(requestDetails.timestampMillis || 0);
    if (!tokenTimestamp || Math.abs(now - tokenTimestamp) > MAX_TOKEN_AGE_MS) {
      return res.status(403).json({
        isAuthorized: false,
        message: "Token Play Integrity telah kedaluwarsa (lebih dari 5 menit).",
      });
    }

    // Validasi 4: Pastikan appRecognitionVerdict bernilai PLAY_RECOGNIZED
    const appVerdict = appIntegrity && appIntegrity.appRecognitionVerdict;
    if (appVerdict !== "PLAY_RECOGNIZED") {
      return res.status(403).json({
        isAuthorized: false,
        message: `Aplikasi tidak dikenali resmi oleh Google Play (verdict: ${appVerdict}).`,
      });
    }

    // Validasi 5: Pastikan deviceRecognitionVerdict mengandung MEETS_DEVICE_INTEGRITY
    const deviceVerdicts = (deviceIntegrity && deviceIntegrity.deviceRecognitionVerdict) || [];
    if (!deviceVerdicts.includes("MEETS_DEVICE_INTEGRITY")) {
      return res.status(403).json({
        isAuthorized: false,
        message: `Perangkat tidak memenuhi syarat MEETS_DEVICE_INTEGRITY (${deviceVerdicts.join(", ")}).`,
      });
    }

    // 3. Penyimpanan Sesi ke Firestore di koleksi 'device_security_sessions'
    const verdictString = deviceVerdicts.join(",");
    const sessionData = {
      sessionId: crypto.randomUUID(),
      nip,
      officerNip: nip,
      officerName,
      officerRole,
      deviceId,
      deviceModel,
      osVersion,
      sdkInt,
      verdict: verdictString,
      deviceVerdict: appVerdict,
      classicNonce: nonce,
      integrityToken,
      tokenHash: integrityToken.substring(0, 32),
      isVerified: true,
      accessGranted: true,
      targetModule: "RUANG_KERJA",
      timestamp: now,
    };

    await db.collection(COLLECTION_SECURITY_SESSIONS).doc(nip).set(sessionData, { merge: true });

    return res.status(200).json({
      isAuthorized: true,
      message: "Token Play Integrity terverifikasi valid dan sesi disimpan.",
      session: sessionData,
    });
  } catch (error) {
    console.error("Play Integrity verification failed:", error);
    return res.status(500).json({
      isAuthorized: false,
      message: `Gagal memverifikasi token Play Integrity: ${error.message}`,
    });
  }
});
