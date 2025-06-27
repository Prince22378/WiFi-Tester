package com.example.wifiapp1.data

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    entities = [Location::class, Scan::class],
    version  = 3
)
@TypeConverters(Converters::class)
abstract class WifiDatabase : RoomDatabase() {
    abstract fun wifiDao(): WifiDao
}

@Entity
data class Location(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity
data class Scan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val locationId: Int,
    val bssid: String,
    val ssid: String,
    val timestamp: Long,
    val rssiMatrix: String
)

class Converters {
    @TypeConverter
    fun fromRssiMatrix(rssiMatrix: List<Int>): String =
        Json.encodeToString(rssiMatrix)

    @TypeConverter
    fun toRssiMatrix(rssiMatrixString: String): List<Int> =
        Json.decodeFromString(rssiMatrixString)
}
