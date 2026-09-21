package com.example.ui.components

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Branch
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusSuccess
import com.example.util.LocationHelper
import kotlinx.coroutines.launch

@Composable
fun GeofenceConfigDialog(
    branch: Branch,
    onDismiss: () -> Unit,
    onSave: (Branch) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var latStr by remember { mutableStateOf(branch.latitude.toString()) }
    var lonStr by remember { mutableStateOf(branch.longitude.toString()) }
    var radiusStr by remember { mutableStateOf(branch.geofenceRadius.toInt().toString()) }
    var openingTime by remember { mutableStateOf(branch.openingTime) }
    var closingTime by remember { mutableStateOf(branch.closingTime) }
    var isDetectingGps by remember { mutableStateOf(false) }
    var gpsStatusMessage by remember { mutableStateOf<String?>(null) }

    fun captureCurrentLocation() {
        coroutineScope.launch {
            isDetectingGps = true
            gpsStatusMessage = "Fetching device GPS coordinates..."
            val loc = LocationHelper.getCurrentLocation(context)
            if (loc != null) {
                latStr = String.format(java.util.Locale.US, "%.6f", loc.latitude)
                lonStr = String.format(java.util.Locale.US, "%.6f", loc.longitude)
                gpsStatusMessage = "Coordinates set to your current device location!"
            } else {
                gpsStatusMessage = "Could not obtain current GPS. Ensure location permissions are granted."
            }
            isDetectingGps = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ShareLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Edit Geofence & Location", style = MaterialTheme.typography.titleLarge)
                    Text(
                        branch.branchName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                item {
                    // Quick calibrate button
                    Button(
                        onClick = { captureCurrentLocation() },
                        enabled = !isDetectingGps,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isDetectingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting GPS...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set Store to My Current Location", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (gpsStatusMessage != null) {
                        Text(
                            text = gpsStatusMessage!!,
                            fontSize = 12.sp,
                            color = if (gpsStatusMessage!!.contains("set to your current", ignoreCase = true)) StatusSuccess else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                item {
                    Text(
                        "Geofence Radius Presets",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presets = listOf(50, 100, 250, 500, 1000, 5000)
                        items(presets.size) { index ->
                            val p = presets[index]
                            val isSelected = radiusStr == p.toString()
                            FilterChip(
                                selected = isSelected,
                                onClick = { radiusStr = p.toString() },
                                label = {
                                    Text(
                                        when (p) {
                                            100 -> "100m (Standard)"
                                            5000 -> "5km (Testing)"
                                            else -> "${p}m"
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = radiusStr,
                        onValueChange = { radiusStr = it },
                        label = { Text("Geofence Radius (meters)") },
                        leadingIcon = { Icon(Icons.Default.Radar, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latStr,
                            onValueChange = { latStr = it },
                            label = { Text("Store Latitude") },
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lonStr,
                            onValueChange = { lonStr = it },
                            label = { Text("Store Longitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text(
                        "Store Operating Hours (for Attendance Lateness)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = openingTime,
                            onValueChange = { openingTime = it },
                            label = { Text("Opening (HH:mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = closingTime,
                            onValueChange = { closingTime = it },
                            label = { Text("Closing (HH:mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latStr.toDoubleOrNull() ?: branch.latitude
                    val lon = lonStr.toDoubleOrNull() ?: branch.longitude
                    val radius = radiusStr.toDoubleOrNull() ?: branch.geofenceRadius
                    val updated = branch.copy(
                        latitude = lat,
                        longitude = lon,
                        geofenceRadius = radius,
                        openingTime = openingTime.trim(),
                        closingTime = closingTime.trim()
                    )
                    onSave(updated)
                }
            ) {
                Text("Save Geofence")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
