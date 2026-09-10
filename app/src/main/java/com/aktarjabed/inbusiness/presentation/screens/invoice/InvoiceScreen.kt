package com.aktarjabed.inbusiness.presentation.screens.invoice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import com.aktarjabed.inbusiness.presentation.components.LoadingScreen
import com.aktarjabed.inbusiness.presentation.components.QuotaBlockedDialog
import com.aktarjabed.inbusiness.presentation.components.QuotaWarningBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    viewModel: InvoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var customerName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var taxRate by remember { mutableStateOf("18.0") }

    // Check quota when screen loads
    LaunchedEffect(Unit) {
        viewModel.checkQuotaAndPrepare()
    }

    // Handle success
    LaunchedEffect(uiState) {
        if (uiState is InvoiceUiState.Success) {
            // Navigate back after 1.5 seconds
            kotlinx.coroutines.delay(1500)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Invoice") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is InvoiceUiState.Initial,
                is InvoiceUiState.Loading -> {
                    LoadingScreen(message = "Checking quota...")
                }

                is InvoiceUiState.CreateAllowed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Warning banner if quota is low
                        if (state.remainingToday <= 5) {
                            QuotaWarningBanner(
                                remaining = state.remainingToday,
                                onUpgrade = onNavigateToUpgrade
                            )
                        }

                        Text(
                            text = "Invoice: \${state.invoiceNumber}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = totalAmount,
                            onValueChange = { totalAmount = it },
                            label = { Text("Total Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = taxRate,
                            onValueChange = { taxRate = it },
                            label = { Text("Tax Rate (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val amount = totalAmount.toDoubleOrNull()
                                val tax = taxRate.toDoubleOrNull() ?: 0.0
                                if (amount != null && customerName.isNotBlank()) {
                                    viewModel.createInvoice(customerName, amount, tax)
                                }
                            },
                            enabled = customerName.isNotBlank() && totalAmount.toDoubleOrNull() != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Invoice")
                        }

                        // Quota info
                        Text(
                            text = "Remaining today: \${state.remainingToday} invoices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is InvoiceUiState.QuotaBlocked -> {
                    QuotaBlockedDialog(
                        verdict = state.verdict,
                        onUpgrade = onNavigateToUpgrade,
                        onDismiss = onNavigateBack
                    )
                }

                is InvoiceUiState.Success -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Invoice Created Successfully!",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                is InvoiceUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error: \${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.checkQuotaAndPrepare() }) {
                            Text("Try again")
                        }
                    }
                }
            }
        }
    }
}
