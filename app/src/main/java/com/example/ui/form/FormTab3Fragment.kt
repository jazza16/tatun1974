package com.example.ui.form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment

/**
 * Fragment untuk Tab 3: Upaya Penanganan & Kondisi Terkini.
 */
class FormTab3Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    FormTab3Content()
                }
            }
        }
    }
}

@Composable
fun FormTab3Content() {
    var upayaDilakukan by remember { mutableStateOf("TRC BPBD Mukomuko melakukan kaji cepat (rapid assessment), monitoring pesisir Muara Manjunto, dan koordinasi dengan pos ronda desa.") }
    var kondisiMutakhir by remember { mutableStateOf("Situasi kondusif, air pasang telah surut, warga kembali beraktivitas melaut dengan waspada.") }
    var sumberInformasi by remember { mutableStateOf("BMKG Stasiun Geofisika Kepahiang & Laporan Camat Kota Mukomuko") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Tab 3: Upaya Penanganan & Kondisi Terkini",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A)
                )

                OutlinedTextField(
                    value = upayaDilakukan,
                    onValueChange = { upayaDilakukan = it },
                    label = { Text("Upaya Penanganan BPBD / TNI / Polri / Warga") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                OutlinedTextField(
                    value = kondisiMutakhir,
                    onValueChange = { kondisiMutakhir = it },
                    label = { Text("Kondisi Mutakhir Lapangan") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = sumberInformasi,
                    onValueChange = { sumberInformasi = it },
                    label = { Text("Sumber Informasi Awal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}
