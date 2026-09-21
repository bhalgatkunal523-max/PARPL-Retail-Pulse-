package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.LeadStatusBadge
import com.example.ui.theme.*
import com.example.util.DateTimeHelper

@Composable
fun EmployeeHomeScreen(
    viewModel: MainViewModel,
    onNavigateToAttendance: () -> Unit,
    onNavigateToNewWalkIn: () -> Unit,
    onSelectWalkIn: (WalkInLead) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val todayAttendance by viewModel.todayEmployeeAttendance.collectAsState()
    val walkIns by viewModel.scopedWalkIns.collectAsState()
    val followUps by viewModel.scopedFollowUps.collectAsState()

    val pendingFollowUpCount = walkIns.count { it.status == LeadStatus.FOLLOW_UP_REQUIRED || it.status == LeadStatus.INTERESTED }
    val dueTodayLeads = walkIns.filter { it.status == LeadStatus.FOLLOW_UP_REQUIRED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Store Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Welcome, ${currentUser.name.split(" ").first()}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    currentUser.branchName,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                currentUser.employeeId.ifEmpty { "STAFF" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2 Primary Prominent Action Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // MARK ATTENDANCE CARD
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToAttendance() }
                        .testTag("home_mark_attendance_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (todayAttendance != null) StatusSuccessContainer else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (todayAttendance != null) StatusSuccess.copy(alpha = 0.2f) else BrandBlueContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (todayAttendance != null) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (todayAttendance != null) StatusSuccess else BrandBluePrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Attendance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate900
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (todayAttendance != null) {
                            Text(
                                "Marked at ${DateTimeHelper.formatTime(todayAttendance!!.timestamp)}",
                                fontSize = 11.sp,
                                color = StatusSuccessText,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                "Live Selfie & GPS",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }
                }

                // NEW WALK-IN CARD
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToNewWalkIn() }
                        .testTag("home_new_walkin_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BrandAmberContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = BrandAmberTertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "New Walk-In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate900
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "< 30s Quick Lead",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }
            }
        }

        // Action Center: Pending Follow-ups & Due Today
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${dueTodayLeads.size}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBluePrimary
                        )
                        Text("Due Today", fontSize = 12.sp, color = Slate600)
                    }
                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Slate200))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$pendingFollowUpCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandAmberTertiary
                        )
                        Text("Pending Leads", fontSize = 12.sp, color = Slate600)
                    }
                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Slate200))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val closedCount = walkIns.count { it.status == LeadStatus.CLOSED }
                        Text(
                            "$closedCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                        Text("Closed Leads", fontSize = 12.sp, color = Slate600)
                    }
                }
            }
        }

        // Due Today Follow-Ups Section
        if (dueTodayLeads.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Follow-Ups Due Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Surface(
                        color = BrandBlueContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "${dueTodayLeads.size} ACTION REQUIRED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBluePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            items(dueTodayLeads) { lead ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectWalkIn(lead) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lead.customerName, fontWeight = FontWeight.Bold, color = Slate900)
                            Text("${lead.productCategory} • ${lead.productModel}", fontSize = 12.sp, color = Slate600)
                            Text(lead.customerMobile, fontSize = 12.sp, color = BrandBluePrimary, fontWeight = FontWeight.Medium)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.customerMobile}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BrandBlueContainer, CircleShape)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call", tint = BrandBluePrimary, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { onSelectWalkIn(lead) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Open", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Recent Walk-Ins List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Walk-In Customers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    "${walkIns.size} total",
                    fontSize = 12.sp,
                    color = Slate600
                )
            }
        }

        if (walkIns.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No customer walk-ins yet. Tap '+ New Walk-In' above.", color = Slate600)
                }
            }
        } else {
            items(walkIns.take(10)) { lead ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectWalkIn(lead) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(lead.customerName, fontWeight = FontWeight.Bold, color = Slate900)
                                Spacer(modifier = Modifier.width(8.dp))
                                LeadStatusBadge(status = lead.status)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${lead.productCategory} • ${lead.productModel.ifEmpty { "General Interest" }}", fontSize = 12.sp, color = Slate600)
                            Text(lead.customerMobile, fontSize = 11.sp, color = Slate600)
                        }

                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate300)
                    }
                }
            }
        }
    }
}
