package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `verify DeviceSecuritySession serialization and deserialization`() {
    val session = com.example.security.DeviceSecuritySession(
      sessionId = "test-session-123",
      officerNip = "199807202025061002",
      officerName = "YUDHA SOFRIANTO,S.A.P",
      officerRole = "Operator Pusdalops",
      integrityToken = "sample-token-classic-mode",
      tokenHash = "sample-token-cla",
      classicNonce = "nonce-test-classic-12345",
      verdict = "MEETS_STRONG_INTEGRITY",
      deviceVerdict = "HARDWARE_BACKED_EVALUATION",
      deviceId = "android-device-test-id",
      deviceModel = "Google Pixel 8",
      osVersion = "Android 14 (API 34)",
      sdkInt = 34,
      isVerified = true,
      accessGranted = true,
      targetModule = "RUANG_KERJA",
      timestamp = 1700000000000L
    )

    val map = session.toMap()
    assertEquals("test-session-123", map["sessionId"])
    assertEquals("199807202025061002", map["officerNip"])
    assertEquals("MEETS_STRONG_INTEGRITY", map["verdict"])
    assertEquals("RUANG_KERJA", map["targetModule"])

    val restored = com.example.security.DeviceSecuritySession.fromMap(map)
    assertEquals(session.sessionId, restored.sessionId)
    assertEquals(session.officerNip, restored.officerNip)
    assertEquals(session.officerName, restored.officerName)
    assertEquals(session.integrityToken, restored.integrityToken)
    assertEquals(session.tokenHash, restored.tokenHash)
    assertEquals(session.verdict, restored.verdict)
    assertEquals(session.deviceVerdict, restored.deviceVerdict)
    assertEquals(session.accessGranted, restored.accessGranted)
  }
}
