package com.example.wifiapp1.ViewModel


//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.launch
//
//class ComparisonViewModel(private val wifiDao: WifiDao) : ViewModel() {
//    val comparisonData = MutableStateFlow<List<ComparisonData>>(emptyList())
//
//    init {
//        viewModelScope.launch {
//            val locations = wifiDao.getAllLocations()
//            Log.d("ComparisonViewModel", "Found ${locations.size} locations")
//            if (locations.size >= 3) {
//                val bssids = locations.mapNotNull { location ->
//                    wifiDao.getBssidForLocation(location.id)
//                }.toSet()
//                val commonBssid = bssids.firstOrNull() // Assume one common BSSID for simplicity
//                if (commonBssid != null) {
//                    val firstLocationId = locations.first().id
//                    val ssid = wifiDao.getSsidForBssid(commonBssid, firstLocationId) ?: "Unknown"
//                    val locationStats = locations.map { location ->
//                        val rssiList = wifiDao.getRssiForBssidInLocation(commonBssid, location.id)
//                        val minRssi = rssiList.minOrNull() ?: 0
//                        val maxRssi = rssiList.maxOrNull() ?: 0
//                        Log.d("ComparisonViewModel", "Location ${location.name}: RSSI range $minRssi to $maxRssi for BSSID $commonBssid")
//                        LocationStat(location.name, minRssi, maxRssi, rssiList)
//                    }
//                    comparisonData.value = listOf(ComparisonData(commonBssid, ssid, locationStats))
//                } else {
//                    Log.w("ComparisonViewModel", "No common BSSID found")
//                }
//            } else {
//                Log.w("ComparisonViewModel", "Need at least 3 locations for comparison")
//            }
//        }
//    }
//}
//
//data class ComparisonData(
//    val apBssid: String,
//    val apSsid: String,
//    val locationStats: List<LocationStat>
//)
//
//data class LocationStat(
//    val locationName: String,
//    val minRssi: Int,
//    val maxRssi: Int,
//    val rssiList: List<Int>
//)

////////////////////////////////////                     //////////////////////////////////////////

//upr wale version ko uncomment krke we can run the basic version of the app.
// NOTE - keep in mind to have only one part uncommented

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiapp1.data.WifiDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ComparisonViewModel(private val wifiDao: WifiDao) : ViewModel() {
    val comparisonData = MutableStateFlow<List<ComparisonData>>(emptyList())

    init {
        viewModelScope.launch {
            val locations = wifiDao.getAllLocations()
            Log.d("ComparisonViewModel", "Found ${locations.size} locations")
            if (locations.size >= 3) {
                val bssids = locations.mapNotNull { location ->
                    wifiDao.getBssidForLocation(location.id)
                }.toSet()
                val commonBssid = bssids.firstOrNull()
                if (commonBssid != null) {
                    val firstLocationId = locations.first().id
                    val ssid = wifiDao.getSsidForBssid(commonBssid, firstLocationId) ?: "Unknown"
                    val locationStats = locations.map { location ->
                        val rssiMatrixString = wifiDao.getRssiMatrixForBssidInLocation(commonBssid, location.id)
                            ?: return@map null
                        val rssiMatrix = Json.decodeFromString<List<Int>>(rssiMatrixString)
                        val validRssi = rssiMatrix.filter { it != 0 }
                        val minRssi = validRssi.minOrNull() ?: 0
                        val maxRssi = validRssi.maxOrNull() ?: 0
                        Log.d("ComparisonViewModel", "Location ${location.name}: RSSI range $minRssi to $maxRssi for BSSID $commonBssid")
                        LocationStat(location.name, minRssi, maxRssi, rssiMatrix)
                    }.filterNotNull()
                    //
                    val allRssi = locationStats.flatMap { it.rssiMatrix.filter { rssi -> rssi != 0 } }
                    val overallMinRssi = allRssi.minOrNull() ?: 0
                    val overallMaxRssi = allRssi.maxOrNull() ?: 0
                    Log.d("ComparisonViewModel", "Overall RSSI range for BSSID $commonBssid: $overallMinRssi to $overallMaxRssi")
                    comparisonData.value = listOf(
                        ComparisonData(
                            apBssid = commonBssid,
                            apSsid = ssid,
                            locationStats = locationStats,
                            minRssi = overallMinRssi,
                            maxRssi = overallMaxRssi
                        )
                    )
                } else {
                    Log.w("ComparisonViewModel", "No common BSSID found")
                }
            } else {
                Log.w("ComparisonViewModel", "Need at least 3 locations for comparison")
            }
        }
    }
}

data class ComparisonData(
    val apBssid: String,
    val apSsid: String,
    val locationStats: List<LocationStat>,
    val minRssi: Int,
    val maxRssi: Int
)

data class LocationStat(
    val locationName: String,
    val minRssi: Int,
    val maxRssi: Int,
    val rssiMatrix: List<Int>
)