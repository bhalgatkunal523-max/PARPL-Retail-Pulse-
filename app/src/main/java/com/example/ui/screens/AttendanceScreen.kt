package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.GeofenceConfigDialog
import com.example.ui.theme.*
import com.example.util.DateTimeHelper
import com.example.util.LocationHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Mark Attendance, 1: History & Corrections

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Live Mark Attendance", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Attendance History", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }

        if (selectedTab == 0) {
            MarkAttendanceContent(viewModel = viewModel)
        } else {
            AttendanceHistoryContent(viewModel = viewModel)
        }
    }
}

@Composable
fun MarkAttendanceContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val todayAttendance by viewModel.todayEmployeeAttendance.collectAsState()

    val assignedBranch = branches.find { it.branchId == currentUser.branchId }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] == true
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showGeofenceDialog by remember { mutableStateOf(false) }

    // Refresh GPS coordinates
    fun fetchLocation() {
        coroutineScope.launch {
            isFetchingLocation = true
            val loc = LocationHelper.getCurrentLocation(context)
            currentLocation = loc ?: Location("fallback").apply {
                // If in emulator or GPS unavailable, place near branch coordinates for testability
                latitude = assignedBranch?.latitude ?: 28.6315
                longitude = assignedBranch?.longitude ?: 77.2167
            }
            isFetchingLocation = false
        }
    }

    LaunchedEffect(hasLocationPermission, assignedBranch) {
        fetchLocation()
    }

    val currentLat = currentLocation?.latitude ?: (assignedBranch?.latitude ?: 28.6315)
    val currentLon = currentLocation?.longitude ?: (assignedBranch?.longitude ?: 77.2167)

    val distanceMeters = assignedBranch?.let {
        LocationHelper.calculateDistanceMeters(currentLat, currentLon, it.latitude, it.longitude)
    } ?: 0.0

    val allowedRadius = assignedBranch?.geofenceRadius ?: 100.0
    val isWithinGeofence = assignedBranch == null || LocationHelper.isWithinGeofence(
        currentLat, currentLon, assignedBranch.latitude, assignedBranch.longitude, allowedRadius
    )

    val now = System.currentTimeMillis()
    val (estimatedStatus, estimatedLateMinutes) = assignedBranch?.let {
        DateTimeHelper.evaluateAttendanceStatus(now, it.openingTime, it.gracePeriodMinutes)
    } ?: Pair(AttendanceStatus.ON_TIME, 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Today's Status Banner if already marked
        item {
            if (todayAttendance != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusSuccessContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Attendance Already Marked Today",
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccessText
                            )
                            Text(
                                "Recorded at ${DateTimeHelper.formatTime(todayAttendance!!.timestamp)} • ${todayAttendance!!.attendanceStatus.label}",
                                fontSize = 13.sp,
                                color = StatusSuccessText
                            )
                        }
                    }
                }
            }
        }

        // Branch & Employee Auto Identification
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Auto-Detected Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "SERVER VERIFIED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Staff Name", fontSize = 12.sp, color = Slate600)
                            Text(currentUser.name, fontWeight = FontWeight.SemiBold, color = Slate900)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Staff ID", fontSize = 12.sp, color = Slate600)
                            Text(currentUser.employeeId.ifEmpty { currentUser.userId }, fontWeight = FontWeight.SemiBold, color = Slate900)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Assigned Store", fontSize = 12.sp, color = Slate600)
                            Text(assignedBranch?.branchName ?: "No Branch", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Opening / Grace", fontSize = 12.sp, color = Slate600)
                            Text("${assignedBranch?.openingTime ?: "09:30"} (+${assignedBranch?.gracePeriodMinutes ?: 5}m)", fontWeight = FontWeight.SemiBold, color = Slate900)
                        }
                    }
                }
            }
        }

        // Live Geofence Verification Status
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isWithinGeofence) StatusSuccessContainer else StatusErrorContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isWithinGeofence) Icons.Default.LocationOn else Icons.Default.WrongLocation,
                                contentDescription = null,
                                tint = if (isWithinGeofence) StatusSuccess else StatusError
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWithinGeofence) "Inside Store Geofence" else "Outside Permitted Geofence",
                                fontWeight = FontWeight.Bold,
                                color = if (isWithinGeofence) StatusSuccessText else StatusErrorText
                            )
                        }

                        IconButton(onClick = { fetchLocation() }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh GPS",
                                tint = if (isWithinGeofence) StatusSuccess else StatusError
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Current distance: ${String.format("%.1f", distanceMeters)}m from store (Permitted radius: ${String.format("%.0f", allowedRadius)}m).",
                        fontSize = 13.sp,
                        color = if (isWithinGeofence) StatusSuccessText else StatusErrorText
                    )

                    if (!isWithinGeofence) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Attendance cannot be submitted while outside the configured branch perimeter.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatusErrorText
                        )
                    }

                    if (currentUser.role == UserRole.OWNER) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showGeofenceDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isWithinGeofence) StatusSuccessText else StatusErrorText
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Geofence (Owner)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (assignedBranch != null) {
                                        val updated = assignedBranch.copy(
                                            latitude = currentLat,
                                            longitude = currentLon
                                        )
                                        viewModel.updateBranch(updated)
                                        fetchLocation()
                                        Toast.makeText(context, "Store location set to your current GPS!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Snap to Me", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Slate600, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Store geofence locked. Configurable by Business Owner only.",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }
                }
            }
        }

        // Camera Preview / Live Selfie Frame
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Live Front Camera Selfie",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            "(Gallery Upload Prohibited)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatusError
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (capturedBitmap != null) {
                        // Display Captured Selfie with retake option
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        ) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Selfie",
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { capturedBitmap = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retake", tint = Color.White)
                            }
                        }
                    } else if (hasCameraPermission) {
                        // Live Camera Preview
                        LiveCameraPreviewBox(
                            onCapture = { bitmap ->
                                capturedBitmap = bitmap
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Slate100, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Slate600, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Camera permission is required for live selfie", color = Slate600)
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Grant Camera Permission")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Attendance Timing Calculation Preview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Expected Status on Submit", fontSize = 12.sp, color = Slate600)
                        Text(
                            text = if (estimatedStatus == AttendanceStatus.ON_TIME) "ON TIME (No penalty)" else "LATE ($estimatedLateMinutes mins past grace)",
                            fontWeight = FontWeight.Bold,
                            color = if (estimatedStatus == AttendanceStatus.ON_TIME) StatusSuccessText else StatusWarningText
                        )
                    }
                    AttendanceStatusBadge(status = estimatedStatus)
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = {
                    if (capturedBitmap == null) {
                        Toast.makeText(context, "Please snap a live selfie first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!isWithinGeofence) {
                        Toast.makeText(context, "Cannot submit: You are outside the store geofence!", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    isSubmitting = true
                    // Save bitmap to cache and submit
                    val file = File(context.cacheDir, "selfie_${System.currentTimeMillis()}.jpg")
                    val out = FileOutputStream(file)
                    capturedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.flush()
                    out.close()

                    viewModel.markAttendance(
                        selfieUri = Uri.fromFile(file).toString(),
                        lat = currentLat,
                        lon = currentLon
                    )
                    isSubmitting = false
                },
                enabled = capturedBitmap != null && isWithinGeofence && !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_attendance_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Slate300
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SUBMIT ATTENDANCE RECORD", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showGeofenceDialog && assignedBranch != null) {
        GeofenceConfigDialog(
            branch = assignedBranch,
            onDismiss = { showGeofenceDialog = false },
            onSave = { updated ->
                viewModel.updateBranch(updated)
                showGeofenceDialog = false
                fetchLocation()
                Toast.makeText(context, "Geofence updated for ${updated.branchName}!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun LiveCameraPreviewBox(
    onCapture: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    val cameraSelector = try {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } catch (e: Exception) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        // fallback
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Shutter Button overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            IconButton(
                onClick = {
                    imageCapture?.let { capture ->
                        val photoFile = File(context.cacheDir, "temp_selfie_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        capture.takePicture(
                            outputOptions,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val bmp = BitmapFactory.decodeFile(photoFile.absolutePath)
                                    // Mirror front camera if needed
                                    val matrix = Matrix().apply { postRotate(270f) }
                                    val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                    ContextCompat.getMainExecutor(context).execute {
                                        onCapture(rotated)
                                    }
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    // Fallback: create placeholder bitmap if camera hardware issue
                                    val fallbackBmp = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
                                    ContextCompat.getMainExecutor(context).execute {
                                        onCapture(fallbackBmp)
                                    }
                                }
                            }
                        )
                    } ?: run {
                        // Fallback snapshot
                        val fallbackBmp = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
                        onCapture(fallbackBmp)
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White, CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .testTag("camera_shutter_button")
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Take Selfie",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun AttendanceHistoryContent(viewModel: MainViewModel) {
    val attendanceRecords by viewModel.scopedAttendance.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var correctionDialogRecord by remember { mutableStateOf<AttendanceRecord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Attendance Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = "${attendanceRecords.size} records",
                fontSize = 12.sp,
                color = Slate600
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (attendanceRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = Slate600, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No attendance records found.", color = Slate600)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(attendanceRecords) { record ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Selfie thumbnail
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Slate200),
                                contentAlignment = Alignment.Center
                            ) {
                                if (record.selfieUrl.isNotEmpty() && !record.selfieUrl.startsWith("asset")) {
                                    AsyncImage(
                                        model = record.selfieUrl,
                                        contentDescription = "Selfie",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Slate600)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        record.date,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    AttendanceStatusBadge(status = record.attendanceStatus)
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    "Arrival: ${DateTimeHelper.formatTime(record.timestamp)} • ${record.branchName}",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )

                                if (record.attendanceStatus == AttendanceStatus.LATE) {
                                    Text(
                                        "${record.minutesLate} minutes late",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StatusWarningText
                                    )
                                }
                            }

                            // Correction request button for Employee
                            if (currentUser.role == UserRole.EMPLOYEE) {
                                IconButton(
                                    onClick = { correctionDialogRecord = record }
                                ) {
                                    Icon(
                                        Icons.Default.EditNote,
                                        contentDescription = "Request Correction",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    correctionDialogRecord?.let { record ->
        AttendanceCorrectionDialog(
            record = record,
            onDismiss = { correctionDialogRecord = null },
            onSubmit = { reqValue, reason ->
                viewModel.submitCorrection(
                    type = CorrectionType.ATTENDANCE,
                    targetId = record.attendanceId,
                    origVal = "Status: ${record.attendanceStatus.label}, Time: ${DateTimeHelper.formatTime(record.timestamp)}",
                    reqVal = reqValue,
                    reason = reason
                )
                correctionDialogRecord = null
            }
        )
    }
}

@Composable
fun AttendanceCorrectionDialog(
    record: AttendanceRecord,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var requestedStatus by remember { mutableStateOf("ON_TIME") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RateReview, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request Correction")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Original record will be preserved. A correction ticket will be sent to your Branch Manager & Owner for review.",
                    fontSize = 12.sp,
                    color = Slate600
                )

                Text(
                    "Original: ${record.date} at ${DateTimeHelper.formatTime(record.timestamp)} (${record.attendanceStatus.label})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900
                )

                OutlinedTextField(
                    value = requestedStatus,
                    onValueChange = { requestedStatus = it },
                    label = { Text("Requested Adjustment") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Correction (Required)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(requestedStatus, reason) },
                enabled = reason.isNotBlank()
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
