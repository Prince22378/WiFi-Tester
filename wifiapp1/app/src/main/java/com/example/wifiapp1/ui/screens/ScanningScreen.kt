package com.example.wifiapp1.ui.screens

import android.net.wifi.WifiManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.wifiapp1.AppScaffold
import com.example.wifiapp1.data.WifiDao
import com.example.wifiapp1.data.WifiScanManager

@Composable
fun ScanningScreen(
    navController: NavController,
    wifiDao: WifiDao,
    locationId: Int,
    snackbarHostState: SnackbarHostState
) {
    val context     = LocalContext.current
    val wifiManager = context.getSystemService(WifiManager::class.java)

    // collect this location's flows
    val scanCount by WifiScanManager
        .getScanCountFlow(locationId)
        .collectAsState(initial = 0)

    val apMap by WifiScanManager
        .getApMapFlow(locationId)
        .collectAsState(initial = emptyMap())

    val progress by animateFloatAsState(targetValue = scanCount / 100f)

    // start scanning once only
    LaunchedEffect(locationId) {
        WifiScanManager.startScanning(wifiManager, wifiDao, locationId)
    }

    AppScaffold(
        navController    = navController,
        title            = "Scan Wi-Fi (Loc $locationId)",
        showBackButton   = true,
        snackbarHostState= snackbarHostState
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(progress, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("Measurements: $scanCount / 100", fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apMap.entries.toList()) { (bssid, readings) ->
                    Card(elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("AP: $bssid",
                                style = MaterialTheme.typography.bodyLarge)
                            readings.chunked(10).forEach { row ->
                                Row(Modifier.fillMaxWidth()) {
                                    row.forEach { rssi ->
                                        Text(
                                            text    = rssi.toString(),
                                            fontSize= 12.sp,
                                            modifier= Modifier
                                                .weight(1f)
                                                .padding(1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // just pop back—scan keeps running
                Button(onClick = { navController.popBackStack() }) {
                    Text("Run in Background")
                }
                // explicitly cancel and pop
                Button(onClick = {
                    WifiScanManager.cancelScanning(locationId)
                    navController.popBackStack()
                }) {
                    Text("Cancel Scanning")
                }
                if (scanCount >= 100) {
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Done")
                    }
                }
            }
        }
    }
}