package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.TopUserHeader
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RetailPulseApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RetailPulseApp(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedWalkIn by viewModel.selectedWalkIn.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // If Walk-In detail is selected, show detail screen
    if (selectedWalkIn != null) {
        WalkInDetailScreen(
            lead = selectedWalkIn!!,
            viewModel = viewModel,
            onNavigateBack = { viewModel.selectWalkIn(null) }
        )
        return
    }

    // If New Walk-In screen is open
    if (currentScreen == "new_walk_in") {
        NewWalkInScreen(
            viewModel = viewModel,
            onNavigateBack = { viewModel.navigateTo("home") }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopUserHeader(
                currentUser = currentUser,
                allUsers = allUsers,
                onSwitchUser = { viewModel.switchUser(it) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                when (currentUser.role) {
                    UserRole.EMPLOYEE -> {
                        NavigationBarItem(
                            selected = currentScreen == "home",
                            onClick = { viewModel.navigateTo("home") },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationBarItem(
                            selected = currentScreen == "attendance",
                            onClick = { viewModel.navigateTo("attendance") },
                            icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Attendance") },
                            label = { Text("Attendance") },
                            modifier = Modifier.testTag("nav_attendance")
                        )
                        NavigationBarItem(
                            selected = currentScreen == "follow_ups",
                            onClick = { viewModel.navigateTo("follow_ups") },
                            icon = { Icon(Icons.Default.PhoneCallback, contentDescription = "Follow-Ups") },
                            label = { Text("Follow-Ups") },
                            modifier = Modifier.testTag("nav_follow_ups")
                        )
                        NavigationBarItem(
                            selected = currentScreen == "search",
                            onClick = { viewModel.navigateTo("search") },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search") },
                            modifier = Modifier.testTag("nav_search")
                        )
                    }
                    UserRole.BRANCH_MANAGER -> {
                        NavigationBarItem(
                            selected = currentScreen == "home",
                            onClick = { viewModel.navigateTo("home") },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Store Console") },
                            label = { Text("Console") },
                            modifier = Modifier.testTag("nav_mgr_console")
                        )
                        NavigationBarItem(
                            selected = currentScreen == "attendance",
                            onClick = { viewModel.navigateTo("attendance") },
                            icon = { Icon(Icons.Default.Badge, contentDescription = "My Attendance") },
                            label = { Text("My Attendance") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == "follow_ups",
                            onClick = { viewModel.navigateTo("follow_ups") },
                            icon = { Icon(Icons.Default.PhoneCallback, contentDescription = "Leads Queue") },
                            label = { Text("Queue") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == "search",
                            onClick = { viewModel.navigateTo("search") },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search") }
                        )
                    }
                    UserRole.OWNER -> {
                        NavigationBarItem(
                            selected = currentScreen == "home",
                            onClick = { viewModel.navigateTo("home") },
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Executive") },
                            label = { Text("Executive") },
                            modifier = Modifier.testTag("nav_owner_exec")
                        )
                        NavigationBarItem(
                            selected = currentScreen == "follow_ups",
                            onClick = { viewModel.navigateTo("follow_ups") },
                            icon = { Icon(Icons.Default.Insights, contentDescription = "Leads Queue") },
                            label = { Text("Leads Queue") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == "search",
                            onClick = { viewModel.navigateTo("search") },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Global Search") }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentUser.role == UserRole.EMPLOYEE || currentUser.role == UserRole.BRANCH_MANAGER) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo("new_walk_in") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_new_walkin")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "New Walk-In")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "home" -> {
                    when (currentUser.role) {
                        UserRole.EMPLOYEE -> {
                            EmployeeHomeScreen(
                                viewModel = viewModel,
                                onNavigateToAttendance = { viewModel.navigateTo("attendance") },
                                onNavigateToNewWalkIn = { viewModel.navigateTo("new_walk_in") },
                                onSelectWalkIn = { viewModel.selectWalkIn(it) }
                            )
                        }
                        UserRole.BRANCH_MANAGER -> {
                            BranchManagerDashboardScreen(viewModel = viewModel)
                        }
                        UserRole.OWNER -> {
                            OwnerDashboardScreen(viewModel = viewModel)
                        }
                    }
                }
                "attendance" -> {
                    AttendanceScreen(viewModel = viewModel)
                }
                "follow_ups" -> {
                    FollowUpDashboardScreen(
                        viewModel = viewModel,
                        onSelectWalkIn = { viewModel.selectWalkIn(it) }
                    )
                }
                "search" -> {
                    SearchScreen(
                        viewModel = viewModel,
                        onSelectWalkIn = { viewModel.selectWalkIn(it) }
                    )
                }
                else -> {
                    EmployeeHomeScreen(
                        viewModel = viewModel,
                        onNavigateToAttendance = { viewModel.navigateTo("attendance") },
                        onNavigateToNewWalkIn = { viewModel.navigateTo("new_walk_in") },
                        onSelectWalkIn = { viewModel.selectWalkIn(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
