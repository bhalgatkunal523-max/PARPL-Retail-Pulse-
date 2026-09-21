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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LeadStatus
import com.example.model.WalkInLead
import com.example.ui.MainViewModel
import com.example.ui.components.LeadStatusBadge
import com.example.ui.theme.*

@Composable
fun FollowUpDashboardScreen(
    viewModel: MainViewModel,
    onSelectWalkIn: (WalkInLead) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val walkIns by viewModel.scopedWalkIns.collectAsState()
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Due Today, 1: Overdue, 2: All Pending, 3: Completed

    val now = System.currentTimeMillis()

    val filteredLeads = remember(walkIns, selectedFilter) {
        when (selectedFilter) {
            0 -> walkIns.filter { it.status == LeadStatus.FOLLOW_UP_REQUIRED }
            1 -> walkIns.filter {
                it.nextFollowUpTimestamp != null &&
                        it.nextFollowUpTimestamp!! < now &&
                        it.status != LeadStatus.CLOSED &&
                        it.status != LeadStatus.LOST
            }
            2 -> walkIns.filter { it.status == LeadStatus.FOLLOW_UP_REQUIRED || it.status == LeadStatus.INTERESTED }
            3 -> walkIns.filter { it.status == LeadStatus.CLOSED }
            else -> walkIns
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedFilter,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(selected = selectedFilter == 0, onClick = { selectedFilter = 0 }, text = { Text("Due Today") })
            Tab(selected = selectedFilter == 1, onClick = { selectedFilter = 1 }, text = { Text("Overdue") })
            Tab(selected = selectedFilter == 2, onClick = { selectedFilter = 2 }, text = { Text("Pending") })
            Tab(selected = selectedFilter == 3, onClick = { selectedFilter = 3 }, text = { Text("Closed") })
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filteredLeads.size} Leads in Queue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
            }

            if (filteredLeads.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No leads in this queue.", color = Slate600)
                    }
                }
            } else {
                items(filteredLeads) { lead ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectWalkIn(lead) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(lead.customerName, fontWeight = FontWeight.Bold, color = Slate900)
                                    Text(lead.customerMobile, fontSize = 12.sp, color = BrandBluePrimary)
                                }
                                LeadStatusBadge(status = lead.status)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${lead.productCategory} • ${lead.productModel}", fontSize = 12.sp, color = Slate600)

                            if (lead.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Notes: ${lead.notes}", fontSize = 11.sp, color = Slate700)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Staff: ${lead.employeeName} (${lead.branchName})",
                                    fontSize = 11.sp,
                                    color = Slate600
                                )

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
            }
        }
    }
}
