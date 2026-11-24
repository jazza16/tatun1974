package com.example.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DisclaimerConsentManagerTest {

    private lateinit var context: Context
    private lateinit var manager: DisclaimerConsentManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = DisclaimerConsentManager(context)
        manager.resetConsentForTesting()
    }

    @Test
    fun `fresh install requires disclaimer consent before starting app`() {
        assertFalse(manager.hasAcceptedCurrentDisclaimer())
        assertEquals("", manager.getAcceptedVersion())
        assertEquals(0L, manager.getAcceptedTimestamp())
        assertNull(manager.getFormattedAcceptedDate())
    }

    @Test
    fun `recording disclaimer acceptance persists version and timestamp across manager instances`() {
        manager.recordConsentAccepted()

        val reloaded = DisclaimerConsentManager(context)
        assertTrue(reloaded.hasAcceptedCurrentDisclaimer())
        assertEquals(DisclaimerConsentManager.CURRENT_DISCLAIMER_VERSION, reloaded.getAcceptedVersion())
        assertTrue(reloaded.getAcceptedTimestamp() > 0L)
        assertNotNull(reloaded.getFormattedAcceptedDate())
    }

    @Test
    fun `older accepted disclaimer version triggers re-consent when version increments`() {
        manager.recordConsentAccepted(version = "2025.0")

        val reloaded = DisclaimerConsentManager(context)
        assertFalse(
            "Outdated consent version must require user to review and accept updated disclaimer",
            reloaded.hasAcceptedCurrentDisclaimer()
        )
        assertEquals("2025.0", reloaded.getAcceptedVersion())
    }
}
