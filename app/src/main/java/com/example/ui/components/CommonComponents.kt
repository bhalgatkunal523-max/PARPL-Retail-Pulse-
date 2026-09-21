package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun RoleBadge(role: UserRole, modifier: Modifier = Modifier) {
    val (bg, text, label) = when (role) {
        UserRole.OWNER -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), "OWNER")
        UserRole.BRANCH_MANAGER -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "BRANCH MANAGER")
        UserRole.EMPLOYEE -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "RETAIL SALES")
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AttendanceStatusBadge(status: AttendanceStatus, modifier: Modifier = Modifier) {
    val (bg, text, icon) = when (status) {
        AttendanceStatus.ON_TIME -> Triple(StatusSuccessContainer, StatusSuccessText, Icons.Default.CheckCircle)
        AttendanceStatus.LATE -> Triple(StatusWarningContainer, StatusWarningText, Icons.Default.Schedule)
        AttendanceStatus.REJECTED -> Triple(StatusErrorContainer, StatusErrorText, Icons.Default.Cancel)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = text, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                color = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun LeadStatusBadge(status: LeadStatus, modifier: Modifier = Modifier) {
    val (bg, text) = when (status) {
        LeadStatus.NEW -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
        LeadStatus.CONTACTED -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        LeadStatus.FOLLOW_UP_REQUIRED -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
        LeadStatus.INTERESTED -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        LeadStatus.CLOSED -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
        LeadStatus.LOST -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            color = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchSelectorDropdown(
    branches: List<Branch>,
    selectedBranchId: String,
    onSelectBranch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val currentLabel = if (selectedBranchId.isEmpty()) {
        "All Branches (13 Stores)"
    } else {
        branches.find { it.branchId == selectedBranchId }?.let { "${it.branchName} • ${it.address.take(20)}..." } ?: "Select Branch"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Active Branch Filter") },
            leadingIcon = {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("branch_selector_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ALL BRANCHES (Consolidated)", fontWeight = FontWeight.Bold)
                    }
                },
                onClick = {
                    onSelectBranch("")
                    expanded = false
                }
            )
            HorizontalDivider()
            branches.forEach { branch ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(branch.branchName, fontWeight = FontWeight.SemiBold)
                            Text(
                                branch.address,
                                fontSize = 12.sp,
                                color = Slate600,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    onClick = {
                        onSelectBranch(branch.branchId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DateFilterChips(
    selectedFilter: DateFilterType,
    onSelectFilter: (DateFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateFilterType.values().forEach { filter ->
            val isSelected = filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = { onSelectFilter(filter) },
                label = { Text(filter.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}
