package com.example.ui.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminPrdChecklistStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AdminPrdChecklistStore.resetDefaults(context)
    }

    @After
    fun tearDown() {
        AdminPrdChecklistStore.resetDefaults(context)
    }

    @Test
    fun defaultChecklistHasAccurateCompletedAndPendingCounts() {
        val map = AdminPrdChecklistStore.loadCompletionMap(context)
        val completed = map.values.count { it }
        val pending = map.values.count { !it }

        assertEquals(17, map.size)
        assertEquals(10, completed)
        assertEquals(7, pending)
        assertTrue(map["prd_trilogi_kebijakan"] == true)
        assertTrue(map["prd_live_usage_guide"] == true)
        assertFalse(map["pending_sos_hold_3s"] == true)
    }

    @Test
    fun checklistTogglePersistsAndResetsCleanly() {
        AdminPrdChecklistStore.saveCompletion(context, "pending_sms_fallback", true)
        val updated = AdminPrdChecklistStore.loadCompletionMap(context)
        assertTrue(updated["pending_sms_fallback"] == true)

        val reset = AdminPrdChecklistStore.resetDefaults(context)
        assertFalse(reset["pending_sms_fallback"] == true)
    }
}
