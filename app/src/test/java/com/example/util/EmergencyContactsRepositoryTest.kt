package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.EmergencyContactsApi
import com.example.data.repository.EmergencyContactsRepository
import com.example.data.repository.EmergencyContactsSource
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmergencyContactsRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("bpbd_public_emergency_contacts_cache", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        EmergencyContactsRepository.resetForTesting()
    }

    @After
    fun tearDown() {
        EmergencyContactsRepository.resetForTesting()
    }

    @Test
    fun defaultPublicContacts_containsAllFiveMandatoryAgenciesAndZeroAsnPersonnel() {
        val defaults = EmergencyContactsRepository.getDefaultPublicContacts()
        val numbers = defaults.map { it.phoneNumber }
        val namesJoined = defaults.joinToString(" | ") { "${it.agencyName} ${it.category} ${it.subtitle}" }

        assertTrue(numbers.contains("117"))
        assertTrue(numbers.contains("110"))
        assertTrue(numbers.contains("113"))
        assertTrue(numbers.contains("119"))
        assertTrue(numbers.contains("115"))
        assertTrue(namesJoined.contains("BPBD", ignoreCase = true))
        assertTrue(namesJoined.contains("Kepolisian", ignoreCase = true))
        assertTrue(namesJoined.contains("Damkar", ignoreCase = true))
        assertTrue(namesJoined.contains("RSUD", ignoreCase = true))
        assertTrue(namesJoined.contains("Basarnas", ignoreCase = true))

        // Pastikan tidak ada data personil ASN atau NIP
        assertFalse(namesJoined.contains("Ruri Irwandi", ignoreCase = true))
        assertFalse(namesJoined.contains("Hitatur Arazak", ignoreCase = true))
        assertFalse(namesJoined.contains("NIP", ignoreCase = true))
        assertFalse(namesJoined.contains("Personil BPBD", ignoreCase = true))
    }

    @Test
    fun fetchPublicContacts_loadsFromRemoteApiFiltersPersonnelAndCachesOffline() = runBlocking {
        val serverPayload = """
            {
              "contacts": [
                {
                  "id": "bpbd_117",
                  "agencyName": "BPBD / Pusdalops Server",
                  "category": "Bencana",
                  "phoneNumber": "117",
                  "displayPhone": "117",
                  "subtitle": "Updated from Admin Web",
                  "iconKey": "bpbd",
                  "accentHex": "#EA580C",
                  "isTollFree": true,
                  "priority": 1
                },
                {
                  "id": "leaked_asn",
                  "agencyName": "Ruri Irwandi (NIP. 197801012005011001)",
                  "category": "Personil BPBD",
                  "phoneNumber": "081234567890",
                  "displayPhone": "0812-3456-7890",
                  "subtitle": "Kepala Pelaksana"
                },
                {
                  "id": "pln_123",
                  "agencyName": "PLN Gangguan Listrik",
                  "category": "Teknis",
                  "phoneNumber": "123",
                  "displayPhone": "123",
                  "subtitle": "Layanan darurat listrik 24 jam",
                  "iconKey": "power",
                  "accentHex": "#0284C7",
                  "priority": 2
                }
              ]
            }
        """.trimIndent()

        EmergencyContactsRepository.overrideApiForTesting = object : EmergencyContactsApi {
            override suspend fun getEmergencyContacts(): Response<ResponseBody> {
                return Response.success(serverPayload.toResponseBody("application/json".toMediaType()))
            }
        }

        val remoteResult = EmergencyContactsRepository.fetchPublicContacts(context)
        assertEquals(EmergencyContactsSource.REMOTE_API, remoteResult.source)
        assertEquals(2, remoteResult.contacts.size)
        assertEquals("BPBD / Pusdalops Server", remoteResult.contacts[0].agencyName)
        assertEquals("PLN Gangguan Listrik", remoteResult.contacts[1].agencyName)

        // Simulasikan mode offline -> pastikan memuat cache lokal dari server
        EmergencyContactsRepository.overrideApiForTesting = object : EmergencyContactsApi {
            override suspend fun getEmergencyContacts(): Response<ResponseBody> {
                throw RuntimeException("Offline")
            }
        }
        val offlineResult = EmergencyContactsRepository.fetchPublicContacts(context)
        assertEquals(EmergencyContactsSource.LOCAL_CACHE, offlineResult.source)
        assertEquals(2, offlineResult.contacts.size)
        assertEquals("117", offlineResult.contacts[0].phoneNumber)
    }
}
