package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.example.ui.components.LeadStatusBadge
import com.example.ui.theme.*
import com.example.util.DateTimeHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewWalkInScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var customerName by remember { mutableStateOf("") }
    var customerMobile by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Smartphones") }
    var productModel by remember { mutableStateOf("") }
    var approximateBudget by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val categories = listOf(
        "Smartphones", "Smart TVs", "Laptops", "Audio & Soundbars",
        "Home Appliances", "Wearables & Smartwatches", "Gaming Consoles", "Accessories"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Walk-In Lead", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Auto Attached Metadata Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Auto-Attaching Metadata (Server Verified):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Staff: ${currentUser.name} (${currentUser.employeeId.ifEmpty { currentUser.userId }})", fontSize = 12.sp)
                            Text("Branch: ${currentUser.branchName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Customer Name & Mobile (Required)
            item {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("walkin_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = customerMobile,
                    onValueChange = { customerMobile = it },
                    label = { Text("Customer Mobile Number *") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("walkin_mobile_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Category Selection Chips
            item {
                Text("Product Category of Interest", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = cat == selectedCategory,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Product Model
            item {
                OutlinedTextField(
                    value = productModel,
                    onValueChange = { productModel = it },
                    label = { Text("Specific Model (e.g. Sony Bravia 55' OLED)") },
                    leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Budget
            item {
                OutlinedTextField(
                    value = approximateBudget,
                    onValueChange = { approximateBudget = it },
                    label = { Text("Approximate Budget (₹)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Customer Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Customer Requirements / Exchange Info") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Save Lead Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (customerName.isBlank() || customerMobile.isBlank()) {
                            Toast.makeText(context, "Customer Name and Mobile are required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val budget = approximateBudget.toDoubleOrNull() ?: 0.0
                        viewModel.createWalkIn(
                            name = customerName,
                            mobile = customerMobile,
                            category = selectedCategory,
                            model = productModel,
                            budget = budget,
                            notes = notes
                        )
                        onNavigateBack()
                    },
                    enabled = customerName.isNotBlank() && customerMobile.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_walkin_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE WALK-IN LEAD (< 30s Entry)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkInDetailScreen(
    lead: WalkInLead,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val followUps by viewModel.scopedFollowUps.collectAsState()
    val leadFollowUps = followUps.filter { it.walkInId == lead.walkInId }

    var showFollowUpDialog by remember { mutableStateOf(false) }
    var showCloseSaleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lead.customerName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    LeadStatusBadge(status = lead.status, modifier = Modifier.padding(end = 16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (lead.status != LeadStatus.CLOSED && lead.status != LeadStatus.LOST) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showFollowUpDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Follow-Up", fontSize = 13.sp)
                        }

                        Button(
                            onClick = { showCloseSaleDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Close Sale", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Header Card with Phone Quick-Triggers
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    lead.customerName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    lead.customerMobile,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate600
                                )
                            }

                            // Quick Contact Actions: Call & WhatsApp
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.customerMobile}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(BrandBlueContainer, CircleShape)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = BrandBluePrimary)
                                }

                                IconButton(
                                    onClick = {
                                        val cleanPhone = lead.customerMobile.replace("+", "").replace(" ", "")
                                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(StatusSuccessContainer, CircleShape)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = StatusSuccess)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Interested In", fontSize = 12.sp, color = Slate600)
                                Text("${lead.productCategory} • ${lead.productModel.ifEmpty { "General Inquiry" }}", fontWeight = FontWeight.SemiBold)
                            }
                            if (lead.approximateBudget > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Budget", fontSize = 12.sp, color = Slate600)
                                    Text("₹${String.format("%,.0f", lead.approximateBudget)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        if (lead.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Slate100,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    lead.notes,
                                    fontSize = 12.sp,
                                    color = Slate700,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Created on ${DateTimeHelper.formatDateTime(lead.createdAt)} by ${lead.employeeName} (${lead.branchName})",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }
            }

            // Lead Pipeline Status Stepper
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Pipeline Status Stepper",
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val stages = listOf(
                            LeadStatus.NEW,
                            LeadStatus.CONTACTED,
                            LeadStatus.FOLLOW_UP_REQUIRED,
                            LeadStatus.INTERESTED,
                            LeadStatus.CLOSED
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            stages.forEachIndexed { index, stage ->
                                val isPassedOrCurrent = stages.indexOf(lead.status) >= index
                                val isCurrent = lead.status == stage

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCurrent -> MaterialTheme.colorScheme.primary
                                                    isPassedOrCurrent -> StatusSuccess
                                                    else -> Slate200
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPassedOrCurrent && !isCurrent) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text(
                                                "${index + 1}",
                                                color = if (isCurrent) Color.White else Slate600,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        stage.name.take(4),
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Slate600
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Follow-up History Timeline
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Follow-up Timeline (${leadFollowUps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
            }

            if (leadFollowUps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No follow-ups recorded yet. Tap '+ Follow-Up' below.", color = Slate600)
                    }
                }
            } else {
                items(leadFollowUps) { fu ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        when (fu.contactMethod) {
                                            ContactMethod.PHONE_CALL -> Icons.Default.Call
                                            ContactMethod.WHATSAPP -> Icons.Default.Chat
                                            ContactMethod.IN_STORE_VISIT -> Icons.Default.Storefront
                                            ContactMethod.OTHER -> Icons.Default.Mail
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(fu.contactMethod.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text(DateTimeHelper.formatDateTime(fu.timestamp), fontSize = 11.sp, color = Slate600)
                            }

                            if (fu.outcome.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(fu.outcome, fontSize = 13.sp, color = Slate900)
                            }

                            if (fu.nextFollowUpDate.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Next Scheduled: ${fu.nextFollowUpDate}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBluePrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Follow-Up Dialog
    if (showFollowUpDialog) {
        AddFollowUpDialog(
            onDismiss = { showFollowUpDialog = false },
            onSubmit = { method, outcome, notes, nextDate, nextStatus ->
                viewModel.addFollowUp(
                    walkInId = lead.walkInId,
                    method = method,
                    outcome = outcome,
                    notes = notes,
                    nextDate = nextDate,
                    statusAfter = nextStatus
                )
                showFollowUpDialog = false
            }
        )
    }

    // Close Sale Dialog
    if (showCloseSaleDialog) {
        CloseSaleDialog(
            lead = lead,
            viewModel = viewModel,
            onDismiss = { showCloseSaleDialog = false },
            onSaleClosed = {
                showCloseSaleDialog = false
                onNavigateBack()
            }
        )
    }
}

@Composable
fun AddFollowUpDialog(
    onDismiss: () -> Unit,
    onSubmit: (ContactMethod, String, String, String, LeadStatus) -> Unit
) {
    var method by remember { mutableStateOf(ContactMethod.PHONE_CALL) }
    var outcome by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var nextDate by remember { mutableStateOf(DateTimeHelper.todayDateString()) }
    var nextStatus by remember { mutableStateOf(LeadStatus.FOLLOW_UP_REQUIRED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Customer Follow-Up")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Contact Method", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ContactMethod.values().forEach { m ->
                        FilterChip(
                            selected = m == method,
                            onClick = { method = m },
                            label = { Text(m.label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = outcome,
                    onValueChange = { outcome = it },
                    label = { Text("Call / Discussion Outcome *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nextDate,
                    onValueChange = { nextDate = it },
                    label = { Text("Next Follow-up Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Update Customer Pipeline Status", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(LeadStatus.FOLLOW_UP_REQUIRED, LeadStatus.INTERESTED, LeadStatus.LOST).forEach { st ->
                        FilterChip(
                            selected = st == nextStatus,
                            onClick = { nextStatus = st },
                            label = { Text(st.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(method, outcome, notes, nextDate, nextStatus) },
                enabled = outcome.isNotBlank()
            ) {
                Text("Save Follow-Up")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CloseSaleDialog(
    lead: WalkInLead,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSaleClosed: () -> Unit
) {
    var invoiceNumber by remember { mutableStateOf("") }
    var billAmountStr by remember { mutableStateOf(if (lead.approximateBudget > 0) lead.approximateBudget.toInt().toString() else "") }
    var saleDate by remember { mutableStateOf(DateTimeHelper.todayDateString()) }
    var productCategory by remember { mutableStateOf(lead.productCategory.ifEmpty { "Smartphones" }) }
    var productModel by remember { mutableStateOf(lead.productModel) }
    var quantityStr by remember { mutableStateOf("1") }
    var discountStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val isDuplicate = remember(invoiceNumber) {
        if (invoiceNumber.length >= 3) {
            viewModel.checkDuplicateInvoice(invoiceNumber)
        } else false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Close Sale & Issue Bill")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                item {
                    Text(
                        "30-Second Mobile Entry: Converting lead to CLOSED.",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }

                item {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Invoice / Bill Number *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_invoice_input"),
                        isError = isDuplicate
                    )
                    if (isDuplicate) {
                        Text(
                            "Warning: Duplicate invoice number found in this branch!",
                            fontSize = 11.sp,
                            color = StatusError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = billAmountStr,
                        onValueChange = { billAmountStr = it },
                        label = { Text("Final Bill Amount (₹) *") },
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_amount_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = saleDate,
                        onValueChange = { saleDate = it },
                        label = { Text("Sale Date (YYYY-MM-DD) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = productCategory,
                        onValueChange = { productCategory = it },
                        label = { Text("Product Category *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = productModel,
                        onValueChange = { productModel = it },
                        label = { Text("Product Model Sold") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountStr,
                            onValueChange = { discountStr = it },
                            label = { Text("Discount (₹)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val billAmt = billAmountStr.toDoubleOrNull() ?: 0.0
                    val qty = quantityStr.toIntOrNull() ?: 1
                    val disc = discountStr.toDoubleOrNull() ?: 0.0

                    viewModel.closeSale(
                        walkInId = lead.walkInId,
                        invoiceNumber = invoiceNumber,
                        billAmount = billAmt,
                        saleDate = saleDate,
                        productCategory = productCategory,
                        productModel = productModel,
                        quantity = qty,
                        discount = disc,
                        notes = notes
                    )
                    onSaleClosed()
                },
                enabled = invoiceNumber.isNotBlank() && (billAmountStr.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
            ) {
                Text("Confirm Sale & Close Lead")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
