package com.example.wifiapp1.data

import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WifiScanManager {
    // one MutableStateFlow<Int> per locationId
    private val scanCounts = mutableMapOf<Int, MutableStateFlow<Int>>()
    private fun countFlow(loc: Int) =
        scanCounts.getOrPut(loc) { MutableStateFlow(0) }

    // one MutableStateFlow<Map<BSSID,List<dBm>>> per locationId
    private val apMaps = mutableMapOf<Int, MutableStateFlow<Map<String, List<Int>>>>()
    private fun apFlow(loc: Int) =
        apMaps.getOrPut(loc) { MutableStateFlow(emptyMap()) }

    // active scan jobs by locationId
    private val jobs = mutableMapOf<Int, Job>()
    private val _activeLocations = MutableStateFlow<Set<Int>>(emptySet())
    val activeLocations: StateFlow<Set<Int>> get() = _activeLocations

    fun getScanCountFlow(loc: Int): StateFlow<Int> = countFlow(loc)
    fun getApMapFlow(loc: Int): StateFlow<Map<String, List<Int>>> = apFlow(loc)

    /** Launches up to 100 scans for this locationId; no‐op if already running. */
    fun startScanning(
        wifiManager: WifiManager,
        wifiDao: WifiDao,
        locationId: Int
    ) {
        if (jobs.containsKey(locationId)) return

        // reset state
        countFlow(locationId).value = 0
        apFlow(locationId).value    = emptyMap()

        val tempMap = mutableMapOf<String, MutableList<Int>>()

        val job = CoroutineScope(Dispatchers.IO).launch {
            repeat(100) { cycle ->
                // 1) trigger system scan
                try {
                    wifiManager.startScan()
                } catch (e: SecurityException) {
                    Log.e("WifiScanMgr", "startScan failed", e)
                }

                // 2) wait ~3s for results to arrive
                delay(3000)

                // 3) grab the results snapshot
                val results: List<ScanResult> = wifiManager.scanResults

                // 4) advance count
                countFlow(locationId).value = cycle + 1

                // 5) record each seen AP
                results.forEach { r ->
                    val list = tempMap.getOrPut(r.BSSID) { mutableListOf() }
                    if (list.size < 100) list.add(r.level)
                }
                // 6) pad unseen APs with 0
                tempMap.keys
                    .filter { b -> results.none { it.BSSID == b } }
                    .forEach { bssid ->
                        val l = tempMap[bssid]!!
                        if (l.size < countFlow(locationId).value)
                            l.add(0)
                    }

                // 7) publish a snapshot
                apFlow(locationId).value = tempMap.mapValues { it.value.toList() }
            }

            // persist each AP’s final 100‐element matrix
            tempMap.forEach { (bssid, readings) ->
                val ssid = wifiManager.scanResults
                    .firstOrNull { it.BSSID == bssid }?.SSID ?: "Unknown"
                val scan = Scan(
                    locationId = locationId,
                    bssid      = bssid,
                    ssid       = ssid,
                    timestamp  = System.currentTimeMillis(),
                    rssiMatrix = Json.encodeToString(readings)
                )
                wifiDao.insertScan(scan)
            }

            // cleanup
            jobs.remove(locationId)
            _activeLocations.value = jobs.keys
        }

        jobs[locationId] = job
        _activeLocations.value = jobs.keys
    }

    /** Stops an in-progress scan for this locationId. */
    fun cancelScanning(locationId: Int) {
        jobs[locationId]?.cancel()
        jobs.remove(locationId)
        _activeLocations.value = jobs.keys
    }
}
