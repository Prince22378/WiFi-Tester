package com.example.wifiapp1.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.wifiapp1.AppScaffold
import com.example.wifiapp1.data.Location
import com.example.wifiapp1.data.WifiDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationListScreen(
    navController: NavController,
    wifiDao: WifiDao,
    snackbarHostState: SnackbarHostState
) {
    val locations by wifiDao.getAllLocationsAsFlow().collectAsState(initial = emptyList())
    var newLocationName by remember { mutableStateOf("") }
    var errorMessage     by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Keep track of which locations the user has tapped
    val selectedIds = remember { mutableStateListOf<Int>() }

    AppScaffold(
        navController     = navController,
        title             = "Locations",
        fabIcon           = null,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // ─── Input Row ─────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = newLocationName,
                    onValueChange = {
                        newLocationName = it
                        errorMessage = null
                    },
                    label = { Text("New Location") },
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.width(12.dp))
                ExtendedFloatingActionButton(
                    onClick = {
                        if (newLocationName.isBlank()) {
                            errorMessage = "Cannot be empty"
                            scope.launch { snackbarHostState.showSnackbar("Enter a name first") }
                        } else {
                            scope.launch {
                                wifiDao.insertLocation(Location(name = newLocationName))
                                newLocationName = ""
                                snackbarHostState.showSnackbar("Location added")
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("Add") },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // ─── Scrollable List ────────────────────────────
            Box(Modifier.weight(1f)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(locations) { location ->
                        val isSelected = selectedIds.contains(location.id)
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedIds.remove(location.id)
                                    else selectedIds.add(location.id)
                                },
                            elevation = CardDefaults.elevatedCardElevation(4.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = location.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        scope.launch {
                                            wifiDao.deleteScansForLocation(location.id)
                                            wifiDao.deleteLocation(location.id)
                                            selectedIds.remove(location.id)
                                            snackbarHostState.showSnackbar("Location deleted")
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            navController.navigate("scanning/${location.id}")
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor   = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(Icons.Default.Wifi, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Scan")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── Bottom Actions ─────────────────────────────
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        // Pass the selected IDs into the route
                        val arg = selectedIds.joinToString(",")
                        navController.navigate("comparison/$arg")
                    },
                    enabled = selectedIds.size >= 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Compare Selected (${selectedIds.size})")
                }
                Button(
                    onClick = { navController.navigate("dataView") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("View Stored Data")
                }
            }
        }
    }
}
