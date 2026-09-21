package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DateTimeHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchManagerDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val kpi by viewModel.dashboardKpi.collectAsState()
    val branchAttendance by viewModel.scopedAttendance.collectAsState()
    val branchWalkIns by viewModel.scopedWalkIns.collectAsState()
    val empPerf by viewModel.employeePerformanceList.collectAsState()
    val corrections by viewModel.scopedCorrectionRequests.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val branches by viewModel.branches.collectAsState()

    val assignedBranch = branches.find { it.branchId == currentUser.branchId }
    var selectedTab by remember { mutableIntStateOf(0) }
    var reviewingCorrection by remember { mutableStateOf<CorrectionRequest?>(null) }

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        // Locked Branch Header
        Surface(
            color = Color.White,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                currentUser.branchName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Text(
                            "Branch Manager Console • Locked to Assigned Store",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                    }
                    RoleBadge(role = UserRole.BRANCH_MANAGER)
                }

                Spacer(modifier = Modifier.height(10.dp))

                assignedBranch?.let { branch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandBlueContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Radar,
                                contentDescription = null,
                                tint = BrandBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Store Geofence: ${branch.geofenceRadius.toInt()}m perimeter",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandBlueOnContainer
                            )
                        }
                        Text(
                            "Configured by Owner",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                DateFilterChips(
                    selectedFilter = selectedDateFilter,
                    onSelectFilter = { viewModel.setDateFilter(it) }
                )
            }
        }

        // Branch Manager Navigation Tabs (Attendance & Walk-Ins Focused Only)
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = BrandBluePrimary,
            edgePadding = 16.dp
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Store KPIs", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Staff Attendance (${branchAttendance.size})", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Customer Leads (${branchWalkIns.size})", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Corrections (${corrections.count { it.status == CorrectionStatus.PENDING }})", fontWeight = FontWeight.Bold) })
        }

        when (selectedTab) {
            0 -> BranchKpisTab(kpi = kpi, empPerf = empPerf)
            1 -> BranchAttendanceListTab(records = branchAttendance)
            2 -> BranchWalkInsListTab(leads = branchWalkIns)
            3 -> CorrectionsReviewTab(corrections = corrections, onReview = { reviewingCorrection = it })
        }
    }

    if (reviewingCorrection != null) {
        CorrectionReviewDialog(
            request = reviewingCorrection!!,
            onDismiss = { reviewingCorrection = null },
            onDecide = { status, notes ->
                viewModel.reviewCorrection(reviewingCorrection!!.requestId, status, notes)
                reviewingCorrection = null
            }
        )
    }
}

@Composable
fun BranchKpisTab(
    kpi: KpiMetrics,
    empPerf: List<EmployeePerformance>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Store Operations Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Today's Attendance",
                    value = "${String.format("%.0f", kpi.attendancePercentage)}%",
                    subtitle = "${kpi.presentToday} present • ${kpi.lateToday} late",
                    icon = Icons.Default.EventAvailable,
                    color = BrandBluePrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Customer Walk-Ins",
                    value = "${kpi.totalWalkIns}",
                    subtitle = "${kpi.closed} closed • ${kpi.interested} interested",
                    icon = Icons.Default.Groups,
                    color = BrandCyanSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Pending Follow-Ups",
                    value = "${kpi.pendingFollowUps}",
                    subtitle = "${kpi.followUpsDueToday} due today • ${kpi.overdueFollowUps} overdue",
                    icon = Icons.Default.Schedule,
                    color = if (kpi.overdueFollowUps > 0) BrightRed else BrandAmberTertiary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Punctuality",
                    value = "${kpi.presentToday - kpi.lateToday}",
                    subtitle = "On-time arrivals today",
                    icon = Icons.Default.VerifiedUser,
                    color = StatusSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Branch Staff Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
        }

        items(empPerf) { emp ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(emp.employeeName, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                        Text("Walk-Ins: ${emp.totalWalkIns} logged • ${emp.closedWalkIns} closed", fontSize = 12.sp, color = Slate700)
                        Text("Punctuality: ${emp.daysPresent} days present • ${emp.lateArrivals} late", fontSize = 12.sp, color = if (emp.lateArrivals > 0) BrightRed else StatusSuccess)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = BrandBlueContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "${emp.pendingFollowUps} Follow-ups",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlueOnContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BranchAttendanceListTab(records: List<AttendanceRecord>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (records.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No attendance records recorded for this filter.", color = Slate600)
                }
            }
        }
        items(records) { rec ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rec.selfieUrl.isNotEmpty() && !rec.selfieUrl.startsWith("asset")) {
                            AsyncImage(
                                model = rec.selfieUrl,
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rec.employeeName, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                            AttendanceStatusBadge(status = rec.attendanceStatus)
                        }
                        Text("${rec.date} at ${DateTimeHelper.formatTime(rec.timestamp)}", fontSize = 12.sp, color = Slate700)
                        if (rec.attendanceStatus == AttendanceStatus.LATE) {
                            Text("${rec.minutesLate} minutes late", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrightRed)
                        } else {
                            Text("On-Time check-in", fontSize = 11.sp, color = StatusSuccess, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BranchWalkInsListTab(leads: List<WalkInLead>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (leads.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No customer walk-in leads recorded for this filter.", color = Slate600)
                }
            }
        }
        items(leads) { lead ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lead.customerName, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                        LeadStatusBadge(status = lead.status)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Product: ${lead.productCategory} • ${lead.productModel}", fontSize = 13.sp, color = Slate800)
                    Text("Logged by: ${lead.employeeName} • Phone: ${lead.customerMobile}", fontSize = 12.sp, color = BrandBluePrimary, fontWeight = FontWeight.Medium)
                    if (lead.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Notes: ${lead.notes}", fontSize = 11.sp, color = Slate600)
                    }
                }
            }
        }
    }
}
