package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WalkInLead
import com.example.ui.MainViewModel
import com.example.ui.components.LeadStatusBadge
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onSelectWalkIn: (WalkInLead) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.scopedWalkIns.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("Search by name, mobile, model, invoice...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Scoped to: ${currentUser.role.displayName} (${currentUser.branchName.ifEmpty { "All 13 Branches" }}) • ${searchResults.size} results",
            fontSize = 12.sp,
            color = Slate600
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchQuery.isBlank()) "Type in search box to find customers or invoices." else "No records match '$searchQuery'.",
                    color = Slate600
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(searchResults) { lead ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
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
                                Text(lead.customerName, fontWeight = FontWeight.Bold, color = Slate900)
                                LeadStatusBadge(status = lead.status)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(lead.customerMobile, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text("${lead.productCategory} • ${lead.productModel}", fontSize = 12.sp, color = Slate600)
                            Text("Staff: ${lead.employeeName} (${lead.branchName})", fontSize = 11.sp, color = Slate600)
                        }
                    }
                }
            }
        }
    }
}
