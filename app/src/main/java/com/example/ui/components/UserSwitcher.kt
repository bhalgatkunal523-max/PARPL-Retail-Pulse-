package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun TopUserHeader(
    currentUser: UserProfile,
    allUsers: List<UserProfile>,
    onSwitchUser: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Quick access default accounts for 1-tap switching
    val ownerUser = allUsers.find { it.role == UserRole.OWNER }
        ?: UserProfile("usr_owner_01", "Vikram Mehra", "vikram@retailpulse.com", "OWNER-001", UserRole.OWNER, "", "All Branches")
    val managerUser = allUsers.find { it.role == UserRole.BRANCH_MANAGER }
        ?: UserProfile("usr_mgr_01", "Rajesh Sharma", "rajesh@retailpulse.com", "MGR-101", UserRole.BRANCH_MANAGER, "br_01", "Branch 01")
    val employeeUser = allUsers.find { it.role == UserRole.EMPLOYEE }
        ?: UserProfile("usr_emp_101", "Amit Verma", "amit@retailpulse.com", "EMP-101", UserRole.EMPLOYEE, "br_01", "Branch 01")

    Surface(
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            // Top identity row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // User Avatar & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDialog = true }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when (currentUser.role) {
                                    UserRole.OWNER -> BrandBlueContainer
                                    UserRole.BRANCH_MANAGER -> Color(0xFFE0F2FE)
                                    UserRole.EMPLOYEE -> Color(0xFFDCFCE7)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (currentUser.role) {
                                UserRole.OWNER -> Icons.Default.AdminPanelSettings
                                UserRole.BRANCH_MANAGER -> Icons.Default.ManageAccounts
                                UserRole.EMPLOYEE -> Icons.Default.Badge
                            },
                            contentDescription = "User Avatar",
                            tint = when (currentUser.role) {
                                UserRole.OWNER -> BrandBluePrimary
                                UserRole.BRANCH_MANAGER -> Color(0xFF0369A1)
                                UserRole.EMPLOYEE -> Color(0xFF15803D)
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = currentUser.name,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 15.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoleBadge(role = currentUser.role)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentUser.branchName.isNotEmpty()) currentUser.branchName else "All Stores",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700
                            )
                        }
                    }
                }

                // Switch Role button that opens dialog
                OutlinedButton(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandBluePrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("switch_role_button")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("All Users", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Direct 1-Tap Role Switch Bar: Instantly switch between Owner, Manager, Staff
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate100, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // OWNER Pill
                val isOwner = currentUser.role == UserRole.OWNER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isOwner) BrandBluePrimary else Color.Transparent)
                        .clickable {
                            if (!isOwner) {
                                onSwitchUser(ownerUser)
                                Toast.makeText(context, "Switched to Owner (Vikram Mehra)", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = if (isOwner) Color.White else Slate700,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Owner",
                            fontSize = 11.sp,
                            fontWeight = if (isOwner) FontWeight.Bold else FontWeight.Medium,
                            color = if (isOwner) Color.White else Slate700
                        )
                    }
                }

                // MANAGER Pill
                val isManager = currentUser.role == UserRole.BRANCH_MANAGER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isManager) BrandBluePrimary else Color.Transparent)
                        .clickable {
                            if (!isManager) {
                                onSwitchUser(managerUser)
                                Toast.makeText(context, "Switched to Store Manager (Rajesh Sharma)", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ManageAccounts,
                            contentDescription = null,
                            tint = if (isManager) Color.White else Slate700,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Manager",
                            fontSize = 11.sp,
                            fontWeight = if (isManager) FontWeight.Bold else FontWeight.Medium,
                            color = if (isManager) Color.White else Slate700
                        )
                    }
                }

                // STAFF Pill
                val isEmployee = currentUser.role == UserRole.EMPLOYEE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEmployee) BrandBluePrimary else Color.Transparent)
                        .clickable {
                            if (!isEmployee) {
                                onSwitchUser(employeeUser)
                                Toast.makeText(context, "Switched to Sales Staff (Amit Verma)", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            tint = if (isEmployee) Color.White else Slate700,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Staff",
                            fontSize = 11.sp,
                            fontWeight = if (isEmployee) FontWeight.Bold else FontWeight.Medium,
                            color = if (isEmployee) Color.White else Slate700
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        UserSwitcherDialog(
            currentUser = currentUser,
            allUsers = allUsers,
            onDismiss = { showDialog = false },
            onSelectUser = { user ->
                onSwitchUser(user)
                Toast.makeText(context, "Switched to ${user.name} (${user.role.displayName})", Toast.LENGTH_SHORT).show()
                showDialog = false
            }
        )
    }
}

@Composable
fun UserSwitcherDialog(
    currentUser: UserProfile,
    allUsers: List<UserProfile>,
    onDismiss: () -> Unit,
    onSelectUser: (UserProfile) -> Unit
) {
    val safeUsers = if (allUsers.isNotEmpty()) allUsers else listOf(
        UserProfile(
            userId = "usr_owner_01",
            name = "Vikram Mehra",
            employeeId = "OWNER-001",
            role = UserRole.OWNER,
            branchId = "",
            branchName = "All Branches"
        ),
        UserProfile(
            userId = "usr_mgr_01",
            name = "Rajesh Sharma",
            employeeId = "MGR-101",
            role = UserRole.BRANCH_MANAGER,
            branchId = "br_01",
            branchName = "Branch 01"
        ),
        UserProfile(
            userId = "usr_mgr_02",
            name = "Priya Patel",
            employeeId = "MGR-201",
            role = UserRole.BRANCH_MANAGER,
            branchId = "br_02",
            branchName = "Branch 02"
        ),
        UserProfile(
            userId = "usr_emp_101",
            name = "Amit Verma",
            employeeId = "EMP-101",
            role = UserRole.EMPLOYEE,
            branchId = "br_01",
            branchName = "Branch 01"
        ),
        UserProfile(
            userId = "usr_emp_102",
            name = "Rahul Gupta",
            employeeId = "EMP-102",
            role = UserRole.EMPLOYEE,
            branchId = "br_01",
            branchName = "Branch 01"
        ),
        UserProfile(
            userId = "usr_emp_201",
            name = "Sneha Rao",
            employeeId = "EMP-201",
            role = UserRole.EMPLOYEE,
            branchId = "br_02",
            branchName = "Branch 02"
        ),
        UserProfile(
            userId = "usr_emp_301",
            name = "Karan Singh",
            employeeId = "EMP-301",
            role = UserRole.EMPLOYEE,
            branchId = "br_03",
            branchName = "Branch 03"
        )
    )

    var roleFilter by remember { mutableStateOf<UserRole?>(null) }

    val filteredUsers = remember(safeUsers, roleFilter) {
        if (roleFilter == null) safeUsers else safeUsers.filter { it.role == roleFilter }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ManageAccounts,
                    contentDescription = null,
                    tint = BrandBluePrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Select User & Role", fontWeight = FontWeight.Bold)
                    Text(
                        "Test app under Owner, Manager, or Employee",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Role Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    item {
                        FilterChip(
                            selected = roleFilter == null,
                            onClick = { roleFilter = null },
                            label = { Text("All (${safeUsers.size})", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = roleFilter == UserRole.OWNER,
                            onClick = { roleFilter = UserRole.OWNER },
                            label = { Text("Owner", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = roleFilter == UserRole.BRANCH_MANAGER,
                            onClick = { roleFilter = UserRole.BRANCH_MANAGER },
                            label = { Text("Managers", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = roleFilter == UserRole.EMPLOYEE,
                            onClick = { roleFilter = UserRole.EMPLOYEE },
                            label = { Text("Sales Staff", fontSize = 11.sp) }
                        )
                    }
                }

                // List of users
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    items(filteredUsers) { user ->
                        val isSelected = user.userId == currentUser.userId

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BrandBlueContainer else PureWhite
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, BrandBluePrimary) else androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectUser(user) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (user.role) {
                                                    UserRole.OWNER -> BrandBlueContainer
                                                    UserRole.BRANCH_MANAGER -> Color(0xFFE0F2FE)
                                                    UserRole.EMPLOYEE -> Color(0xFFDCFCE7)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (user.role) {
                                                UserRole.OWNER -> Icons.Default.AdminPanelSettings
                                                UserRole.BRANCH_MANAGER -> Icons.Default.ManageAccounts
                                                UserRole.EMPLOYEE -> Icons.Default.Badge
                                            },
                                            contentDescription = null,
                                            tint = when (user.role) {
                                                UserRole.OWNER -> BrandBluePrimary
                                                UserRole.BRANCH_MANAGER -> Color(0xFF0369A1)
                                                UserRole.EMPLOYEE -> Color(0xFF15803D)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                fontSize = 14.sp
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Active",
                                                    tint = StatusSuccess,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (user.role == UserRole.OWNER) "Full Access (All 13 Branches)" else "Store: ${user.branchName}",
                                            fontSize = 12.sp,
                                            color = Slate700
                                        )

                                        Text(
                                            text = "ID: ${user.employeeId.ifEmpty { user.userId }}",
                                            fontSize = 11.sp,
                                            color = BrandBluePrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    RoleBadge(role = user.role)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
