package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.api.FirebaseStoragePhotoUploader
import com.example.data.local.AppDatabase
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pemantau Koneksi Jaringan Real-Time (Setara `connectivity_plus` di Android Native via [ConnectivityManager.NetworkCallback]):
 * - Memantau perubahan sinyal internet perangkat secara langsung.
 * - Begitu sinyal internet kembali tersedia ([ConnectivityManager.NetworkCallback.onAvailable]),
 *   otomatis membaca laporan warga & path foto lokal yang tersimpan saat offline, mengunggah foto
 *   ke Firebase Storage (dibungkus `try-catch` aman), dan menyimpan dokumen laporan beserta `fotoUrl`
 *   ke koleksi `citizen_disaster_reports` di Cloud Firestore.
 */
object OfflineReportConnectivityMonitor {
    private const val TAG = "OfflineConnMonitor"
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRegistered = AtomicBoolean(false)
    private val isSyncInProgress = AtomicBoolean(false)

    private val _isOnlineFlow = MutableStateFlow(true)
    val isOnlineFlow: StateFlow<Boolean> = _isOnlineFlow.asStateFlow()

    /**
     * Memeriksa status internet saat ini dan memperbarui [isOnlineFlow].
     */
    fun refreshConnectivityState(context: Context?): Boolean {
        val online = FirebaseStoragePhotoUploader.isNetworkOnline(context)
        _isOnlineFlow.value = online
        return online
    }

    /**
     * Mendaftarkan [ConnectivityManager.NetworkCallback] sekali seumur hidup aplikasi
     * untuk mendeteksi pemulihan sinyal dan mengirim laporan + foto lokal yang tertunda.
     */
    fun ensureRegistered(context: Context?) {
        val appCtx = context?.applicationContext ?: return
        refreshConnectivityState(appCtx)
        if (!isRegistered.compareAndSet(false, true)) return

        try {
            val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnlineFlow.value = true
                        Log.i(TAG, "Koneksi internet kembali tersedia — memicu pengiriman otomatis laporan & foto lokal warga...")
                        triggerPendingOfflineReportsUpload(appCtx)
                    }

                    override fun onLost(network: Network) {
                        val stillOnline = FirebaseStoragePhotoUploader.isNetworkOnline(appCtx)
                        _isOnlineFlow.value = stillOnline
                        if (!stillOnline) {
                            Log.i(TAG, "Koneksi internet terputus — mengaktifkan mode Offline Persistence (Zero Friction).")
                            CitizenReportSyncWorker.enqueueWhenConnected(appCtx)
                        }
                    }
                }
            )
            Log.i(TAG, "OfflineReportConnectivityMonitor terdaftar dan aktif memantau sinyal.")
        } catch (e: Throwable) {
            isRegistered.set(false)
            Log.w(TAG, "Gagal mendaftarkan NetworkCallback: ${e.message}")
        }
    }

    /**
     * Mengirim seluruh laporan warga dan file foto lokal yang tertunda segera saat koneksi kembali.
     */
    fun triggerPendingOfflineReportsUpload(context: Context?) {
        val appCtx = context?.applicationContext ?: return
        if (!FirebaseStoragePhotoUploader.isNetworkOnline(appCtx)) return
        if (!isSyncInProgress.compareAndSet(false, true)) return

        monitorScope.launch {
            try {
                val db = AppDatabase.getDatabase(appCtx)
                val dao = db.weatherDao()
                val pending = dao.getUnsyncedCitizenReports()
                if (pending.isNotEmpty()) {
                    Log.i(TAG, "Menemukan ${pending.size} laporan warga offline di perangkat. Mengunggah foto lokal & dokumen ke Firestore...")
                    val repo = WeatherRepository(dao)
                    val synced = repo.syncCitizenDisasterReportsOnline()
                    Log.i(TAG, "Sinkronisasi otomatis saat sinyal kembali selesai ($synced laporan terkirim).")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Pengiriman otomatis laporan offline tertunda, menjadwalkan ulang via WorkManager: ${e.message}")
                CitizenReportSyncWorker.enqueueWhenConnected(appCtx)
            } finally {
                isSyncInProgress.set(false)
            }
        }
    }
}
