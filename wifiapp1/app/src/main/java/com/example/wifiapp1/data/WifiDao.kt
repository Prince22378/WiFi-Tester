package com.example.wifiapp1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiDao {
    @Insert suspend fun insertLocation(location: Location): Long
    @Insert suspend fun insertScan(scan: Scan): Long

    @Query("SELECT * FROM Location")
    fun getAllLocationsAsFlow(): Flow<List<Location>>

    @Query("SELECT * FROM Location")
    suspend fun getAllLocations(): List<Location>

    @Query("SELECT * FROM Scan")
    fun getAllScansAsFlow(): Flow<List<Scan>>

    @Query("SELECT bssid FROM Scan WHERE locationId = :locationId LIMIT 1")
    suspend fun getBssidForLocation(locationId: Int): String?

    @Query("SELECT rssiMatrix FROM Scan WHERE locationId = :locationId AND bssid = :bssid LIMIT 1")
    suspend fun getRssiMatrixForBssidInLocation(bssid: String, locationId: Int): String?

    @Query("SELECT ssid FROM Scan WHERE bssid = :bssid AND locationId = :locationId LIMIT 1")
    suspend fun getSsidForBssid(bssid: String, locationId: Int): String?

    /** Zero out every stored rssiMatrix (100-element JSON of zeros) */
    @Query("UPDATE Scan SET rssiMatrix = :zeroMatrix")
    suspend fun zeroAllMatrices(zeroMatrix: String)

    @Query("DELETE FROM Location WHERE id = :id")
    suspend fun deleteLocation(id: Int)

    @Query("DELETE FROM Scan WHERE locationId = :locationId")
    suspend fun deleteScansForLocation(locationId: Int)

    /** Deletes any scans whose locationId is not in the given list */
    @Query("DELETE FROM Scan WHERE locationId NOT IN (:locationIds)")
    suspend fun deleteScansNotIn(locationIds: List<Int>)

    /** Overwrites rssiMatrix with zeros for the given locations */
    @Query("UPDATE Scan SET rssiMatrix = :zeroMatrix WHERE locationId IN (:locationIds)")
    suspend fun zeroMatricesForLocations(locationIds: List<Int>, zeroMatrix: String)
}
