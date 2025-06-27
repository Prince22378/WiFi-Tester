package com.example.wifiapp1.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.wifiapp1.AppScaffold
import com.example.wifiapp1.data.WifiDao
import com.example.wifiapp1.data.WifiScanManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.max

@Composable
fun ComparisonScreen(
    navController     : NavController,
    wifiDao           : WifiDao,
    snackbarHostState : SnackbarHostState,
    selectedIdsArg    : String
) {
    // parse the comma‐separated selected IDs
    val selectedIds = selectedIdsArg
        .split(",")
        .mapNotNull { it.toIntOrNull() }
        .distinct()

    // load latest 100‐sample matrix for each AP in each selected location
    val storedData by produceState<Map<Int, Map<String, List<Int>>>>(
        initialValue = emptyMap(), selectedIdsArg
    ) {
        value = withContext(Dispatchers.IO) {
            selectedIds.associateWith { locId ->
                wifiDao.getAllScansAsFlow()
                    .first()
                    .filter { it.locationId == locId }
                    .groupBy { it.bssid }
                    .mapValues { scans ->
                        val latest = scans.value.maxByOrNull { it.timestamp }!!
                        Json.decodeFromString(latest.rssiMatrix)
                    }
            }
        }
    }

    // find APs that appear in *all* selected locations
    val commonBssids = remember(storedData) {
        if (selectedIds.size < 2) emptyList()
        else storedData.values
            .flatMap { it.keys }
            .groupingBy { it }
            .eachCount()
            .filter { it.value == selectedIds.size }
            .keys
            .toList()
            .sorted()
    }

    AppScaffold(
        navController      = navController,
        title              = "Compare Locations",
        showBackButton     = true,
        snackbarHostState  = snackbarHostState
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (selectedIds.size < 2 || commonBssids.isEmpty()) {
                Text(
                    "Select at least 2 locations sharing ≥2 APs.",
                    style = MaterialTheme.typography.bodyLarge
                )
                FilledTonalButton(
                    onClick  = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
                return@Column
            }

            // ─── One card per common AP ───────────────────────────
            commonBssids.forEach { bssid ->
                Card(
                    Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("AP: $bssid", style = MaterialTheme.typography.headlineSmall)

                        // plot area: left = fixed Y axis; right = horizontally scrollable curves
                        Row(Modifier.height(200.dp).fillMaxWidth()) {
                            // Y‐axis labels (-100…0 every 10 dB)
                            Column(
                                Modifier
                                    .width(40.dp)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                (0..10).forEach { i ->
                                    Text(text = "${-100 + i*10}", fontSize = 10.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "RSSI\ndBm",
                                    fontSize  = 9.sp,
                                    textAlign = TextAlign.Center,
                                    modifier  = Modifier
                                        .graphicsLayer { rotationZ = -90f }
                                        .offset(x = (-4).dp)
                                )
                            }

                            // scrollable canvas
                            val hScroll = rememberScrollState()
                            val sample  = storedData[selectedIds.first()]?.get(bssid) ?: emptyList()
                            val chartW  = max(300f, sample.size * 8f).dp
                            val strokePx     = with(LocalDensity.current){2.dp.toPx()}
                            val cornerEffect = PathEffect.cornerPathEffect(50f)

                            Box(
                                Modifier
                                    .horizontalScroll(hScroll)
                                    .width(chartW)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val xStep = if (sample.size>1) w/(sample.size-1) else w

                                    // vertical grid lines
                                    for (i in 0 until sample.size step max(1, sample.size/10)) {
                                        val x = i * xStep
                                        drawLine(Color.LightGray, Offset(x,0f), Offset(x,h), 1f)
                                    }
                                    // horizontal grid lines
                                    for (j in 0..10) {
                                        val y = j * (h/10f)
                                        drawLine(Color.LightGray, Offset(0f,y), Offset(w,y), 1f)
                                    }

                                    // axes
                                    drawLine(Color.Black, Offset(0f,h), Offset(w,h), 2f)
                                    drawLine(Color.Black, Offset(0f,0f), Offset(0f,h), 2f)
                                    // arrowheads
                                    drawPath(Path().apply {
                                        moveTo(w,h); lineTo(w-10,h-5); lineTo(w-10,h+5); close()
                                    }, Color.Black)
                                    drawPath(Path().apply {
                                        moveTo(0f,0f); lineTo(-5f,10f); lineTo(5f,10f); close()
                                    }, Color.Black)

                                    // map RSSI → y (−100→0 to y=0; 0→100 to y=h)
                                    fun rssiToY(r: Int) = (r + 100) / 100f * h

                                    // X‐axis tick labels
                                    for (i in 0 until sample.size step max(1, sample.size/10)) {
                                        val x = i * xStep
                                        drawContext.canvas.nativeCanvas.apply {
                                            drawText(
                                                "${i+1}",
                                                x,
                                                h + 24f,
                                                android.graphics.Paint().apply {
                                                    textSize = 10.sp.toPx()
                                                    textAlign = android.graphics.Paint.Align.CENTER
                                                    color = android.graphics.Color.BLACK
                                                }
                                            )
                                        }
                                    }

                                    // draw each location’s curve
                                    selectedIds.forEachIndexed { idx, locId ->
                                        val readings = storedData[locId]?.get(bssid) ?: return@forEachIndexed
                                        val path = Path().apply {
                                            readings.forEachIndexed { i, rssi ->
                                                val x = i * xStep
                                                val y = rssiToY(rssi)
                                                if (i==0) moveTo(x,y) else lineTo(x,y)
                                            }
                                        }
                                        drawPath(
                                            path  = path,
                                            color = generateDynamicColor(idx),
                                            style = Stroke(width=strokePx, pathEffect=cornerEffect)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            "Measurement Index (1–100)",
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 8.dp)
                        )
                    }
                }
            }

            // ─── Max/Min RSSI per Location ─────────────────────────
            Card(
                Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Max & Min RSSI per Location",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    // filter out padding values (0 and –99) before computing stats
                    val rssiStats = selectedIds.mapNotNull { locId ->
                        val all = storedData[locId]?.values?.flatten() ?: return@mapNotNull null
                        val valid = all.filter { it != 0 && it != -99 }
                        if (valid.isEmpty()) return@mapNotNull null

                        val maxR = valid.maxOrNull()!!
                        val minR = valid.minOrNull()!!
                        locId to (maxR to minR)
                    }

                    rssiStats.forEach { (loc, stats) ->
                        Text(
                            "Location $loc: Max = ${stats.first} dBm, Min = ${stats.second} dBm",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    rssiStats.maxByOrNull { it.second.first }?.let { (bestLoc, bestStats) ->
                        Text(
                            "✅ Best network likely at Location $bestLoc (Max = ${bestStats.first} dBm)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// generate a distinct color per index
fun generateDynamicColor(idx: Int): Color {
    val hue = (idx * 45f) % 360f
    return Color.hsv(hue, 0.8f, 0.8f)
}
