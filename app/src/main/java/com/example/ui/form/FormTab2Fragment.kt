package com.example.ui.form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.ui.components.formDoneKeyboardOptions
import com.example.ui.components.formNextKeyboardOptions
import com.example.ui.components.formScrollPadding
import com.example.ui.components.rememberFormKeyboardActions

/**
 * Fragment untuk Tab 2: Dampak & Korban Bencana.
 */
class FormTab2Fragment : Fragment() {
    private val sharedViewModel: LaporanViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    FormTab2Content(sharedViewModel = sharedViewModel)
                }
            }
        }
    }
}

@Composable
fun FormTab2Content(sharedViewModel: LaporanViewModel? = null) {
    val keyboardActions = rememberFormKeyboardActions()
    var cakupanDampak by remember {
        mutableStateOf(
            sharedViewModel?.laporanSementara?.cakupanDampak?.ifBlank {
                "3 Desa pesisir terdampak getaran dan genangan pasang air laut"
            } ?: "3 Desa pesisir terdampak getaran dan genangan pasang air laut"
        )
    }
    var korbanLuka by remember { mutableIntStateOf(sharedViewModel?.laporanSementara?.korbanLuka ?: 0) }
    var korbanMeninggal by remember { mutableIntStateOf(sharedViewModel?.laporanSementara?.korbanMeninggal ?: 0) }
    var kerusakan by remember {
        mutableStateOf(
            sharedViewModel?.laporanSementara?.kerusakan?.ifBlank {
                "Nihil korban jiwa. Retak rambut pada 1 pos ronda."
            } ?: "Nihil korban jiwa. Retak rambut pada 1 pos ronda."
        )
    }
    var penyebab by remember {
        mutableStateOf(
            sharedViewModel?.laporanSementara?.penyebab?.ifBlank {
                "Aktivitas subduksi lempeng tektonik Samudra Hindia"
            } ?: "Aktivitas subduksi lempeng tektonik Samudra Hindia"
        )
    }

    androidx.compose.runtime.LaunchedEffect(cakupanDampak, korbanLuka, korbanMeninggal, kerusakan, penyebab) {
        sharedViewModel?.let { vm ->
            vm.laporanSementara = vm.laporanSementara.copy(
                cakupanDampak = cakupanDampak,
                korbanLuka = korbanLuka,
                korbanMeninggal = korbanMeninggal,
                kerusakan = kerusakan,
                penyebab = penyebab
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Tab 2: Cakupan Dampak & Korban Jiwa",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )

                OutlinedTextField(
                    value = cakupanDampak,
                    onValueChange = {
                        cakupanDampak = it
                        sharedViewModel?.let { vm ->
                            vm.laporanSementara = vm.laporanSementara.copy(cakupanDampak = it)
                        }
                    },
                    label = { Text("Cakupan Wilayah Terdampak & KK") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Korban Meninggal", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (korbanMeninggal > 0) {
                                        korbanMeninggal -= 1
                                        sharedViewModel?.let { vm ->
                                            vm.laporanSementara = vm.laporanSementara.copy(korbanMeninggal = korbanMeninggal)
                                        }
                                    }
                                }) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$korbanMeninggal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                IconButton(onClick = {
                                    korbanMeninggal += 1
                                    sharedViewModel?.let { vm ->
                                        vm.laporanSementara = vm.laporanSementara.copy(korbanMeninggal = korbanMeninggal)
                                    }
                                }) {
                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Korban Luka-Luka", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (korbanLuka > 0) {
                                        korbanLuka -= 1
                                        sharedViewModel?.let { vm ->
                                            vm.laporanSementara = vm.laporanSementara.copy(korbanLuka = korbanLuka)
                                        }
                                    }
                                }) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$korbanLuka", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                IconButton(onClick = {
                                    korbanLuka += 1
                                    sharedViewModel?.let { vm ->
                                        vm.laporanSementara = vm.laporanSementara.copy(korbanLuka = korbanLuka)
                                    }
                                }) {
                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = kerusakan,
                    onValueChange = {
                        kerusakan = it
                        sharedViewModel?.let { vm ->
                            vm.laporanSementara = vm.laporanSementara.copy(kerusakan = it)
                        }
                    },
                    label = { Text("Kerusakan Fasilitas / Rumah / Infrastruktur") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = penyebab,
                    onValueChange = {
                        penyebab = it
                        sharedViewModel?.let { vm ->
                            vm.laporanSementara = vm.laporanSementara.copy(penyebab = it)
                        }
                    },
                    label = { Text("Penyebab Terjadinya Bencana") },
                    keyboardOptions = formDoneKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 2
                )
            }
        }
    }
}
