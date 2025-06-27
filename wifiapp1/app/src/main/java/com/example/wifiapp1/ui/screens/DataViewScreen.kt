package com.example.wifiapp1.ui.screens

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.wifiapp1.AppScaffold
import com.example.wifiapp1.data.Scan
import com.example.wifiapp1.data.WifiDao
import com.example.wifiapp1.data.WifiScanManager
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.runtime.collectAsState
@Composable
fun DataViewScreen(
    navController: NavController,
    wifiDao: WifiDao,
    snackbarHostState: SnackbarHostState
) {
    val context     = LocalContext.current
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // build id → name map
    val locations by wifiDao.getAllLocationsAsFlow().collectAsState(initial = emptyList())
    val locNameMap = remember(locations) { locations.associate { it.id to it.name } }

    // flows
    val storedScans by wifiDao.getAllScansAsFlow().collectAsState(initial = emptyList())
    val activeLocs by WifiScanManager.activeLocations.collectAsState(initial = emptySet())

    val scope  = rememberCoroutineScope()

    AppScaffold(
        navController     = navController,
        title             = "Stored & Live Data",
        showBackButton    = true,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                // ─── Replace scrollable Column with LazyColumn ────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // 1) LIVE sections
                        activeLocs.sorted().forEach { locId ->
                            val name = locNameMap[locId] ?: "Location $locId"

                            item {
                                Text(
                                    "🔴 Live @ $name",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }

                            item {
                                val liveCount by WifiScanManager
                                    .getScanCountFlow(locId)
                                    .collectAsState(initial = 0)
                                Text(
                                    "Measurements: $liveCount / 100",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            val liveEntries = WifiScanManager.getApMapFlow(locId)
                                .value
                                .entries
                                .sortedBy { it.key }

                            items(liveEntries) { (bssid, readings) ->
                                val ssid = wifiManager.scanResults
                                    .firstOrNull { it.BSSID == bssid }
                                    ?.SSID ?: "Unknown"

                                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            "AP: $ssid ($bssid)",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        readings.chunked(10).forEach { row ->
                                            Row(Modifier.fillMaxWidth()) {
                                                row.forEach { v ->
                                                    Text(
                                                        text = v.toString(),
                                                        modifier = Modifier.weight(1f),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2) STORED sections
                        val byLoc = storedScans.groupBy { it.locationId }
                        byLoc.keys.sorted().forEach { locId ->
                            val name = locNameMap[locId] ?: "Location $locId"

                            item {
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    "💾 Stored @ $name",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(Modifier.height(4.dp))
                            }

                            val latestPerBssid = byLoc[locId]!!
                                .groupBy { it.bssid }
                                .mapValues { entry ->
                                    entry.value.maxByOrNull { it.timestamp }!!
                                }
                                .values
                                .toList()

                            items(latestPerBssid) { scan ->
                                val matrix: List<Int> =
                                    Json.decodeFromString(scan.rssiMatrix)
                                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            "AP: ${scan.ssid} (${scan.bssid})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        matrix.chunked(10).forEach { row ->
                                            Row(Modifier.fillMaxWidth()) {
                                                row.forEach { v ->
                                                    Text(
                                                        text = v.toString(),
                                                        modifier = Modifier.weight(1f),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── Fixed Bottom Buttons (unchanged) ────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                        .clip(MaterialTheme.shapes.medium),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val locIds = wifiDao.getAllLocations().map { it.id }
                                wifiDao.deleteScansNotIn(locIds)
                                val zeroMatrix = Json.encodeToString(List(100) { 0 })
                                wifiDao.zeroMatricesForLocations(locIds, zeroMatrix)
                                snackbarHostState.showSnackbar("Stored data reset")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Stored Data")
                    }
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                activeLocs.forEach { locId ->
                                    val liveMap = WifiScanManager.getApMapFlow(locId).value
                                    val timestamp = System.currentTimeMillis()
                                    liveMap.forEach { (bssid, readings) ->
                                        val padded = readings + List(maxOf(0, 100 - readings.size)) { 0 }
                                        val ssid = wifiManager.scanResults
                                            .firstOrNull { it.BSSID == bssid }?.SSID ?: "Unknown"
                                        val scan = Scan(
                                            locationId = locId,
                                            bssid = bssid,
                                            ssid = ssid,
                                            timestamp = timestamp,
                                            rssiMatrix = Json.encodeToString(padded)
                                        )
                                        wifiDao.insertScan(scan)
                                    }
                                }
                                snackbarHostState.showSnackbar("Live data stored")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Store Live Data")
                    }
                }
            }
        }
    }
}
