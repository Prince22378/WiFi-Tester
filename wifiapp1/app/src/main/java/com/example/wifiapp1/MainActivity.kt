package com.example.wifiapp1

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.wifiapp1.data.WifiDatabase
import com.example.wifiapp1.ui.screens.*
import com.example.wifiapp1.ui.theme.Wifiapp1Theme

class MainActivity : ComponentActivity() {
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] != true) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))

        setContent {
            Wifiapp1Theme {
                val navController      = rememberNavController()
                val context            = LocalContext.current
                val database           = remember {
                    Room.databaseBuilder(
                        context,
                        WifiDatabase::class.java,
                        "wifi-db"
                    )
                        .fallbackToDestructiveMigration(false)
                        .build()
                }
                val wifiDao            = database.wifiDao()
                val snackbarHostState  = remember { SnackbarHostState() }

                NavHost(
                    navController   = navController,
                    startDestination= "locationList",
                    modifier        = Modifier // ensure you import Modifier
                ) {
                    composable("locationList") {
                        LocationListScreen(
                            navController      = navController,
                            wifiDao            = wifiDao,
                            snackbarHostState  = snackbarHostState
                        )
                    }
                    composable("scanning/{locationId}") { backStackEntry ->
                        val locationId = backStackEntry.arguments
                            ?.getString("locationId")
                            ?.toIntOrNull() ?: 0

                        ScanningScreen(
                            navController      = navController,
                            wifiDao            = wifiDao,
                            locationId         = locationId,
                            snackbarHostState  = snackbarHostState
                        )
                    }
                    composable("dataView") {
                        DataViewScreen(
                            navController      = navController,
                            wifiDao            = wifiDao,
                            snackbarHostState  = snackbarHostState
                        )
                    }

                    // ← Here’s the corrected comparison route:
                    composable("comparison/{selectedIds}") { backStackEntry ->
                        // Pull the comma-separated IDs out of the route:
                        val selectedIdsArg = backStackEntry.arguments
                            ?.getString("selectedIds") ?: ""

                        ComparisonScreen(
                            navController      = navController,
                            wifiDao            = wifiDao,
                            snackbarHostState  = snackbarHostState,
                            selectedIdsArg     = selectedIdsArg
                        )
                    }
                }
            }
        }
    }
}
