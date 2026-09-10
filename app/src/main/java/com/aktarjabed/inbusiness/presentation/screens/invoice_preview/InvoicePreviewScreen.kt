package com.aktarjabed.inbusiness.presentation.screens.invoice_preview

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import android.content.Intent
import androidx.core.content.FileProvider
import com.aktarjabed.inbusiness.utils.pdf.PdfGenerator
import kotlinx.coroutines.launch
import com.aktarjabed.inbusiness.presentation.components.LoadingScreen
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: InvoicePreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Preview") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState is InvoicePreviewUiState.Success) {
                val state = uiState as InvoicePreviewUiState.Success
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                            val coroutineScope = rememberCoroutineScope()
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val generator = PdfGenerator(context)
                                        val file = generator.generateInvoicePdf(state.business, state.invoice, state.items)
                                        if (file != null) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Invoice"))
                                        }
                                    }
                                },

                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.padding(end = 8.dp))
                            Text("Share PDF")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is InvoicePreviewUiState.Loading -> LoadingScreen(message = "Loading invoice...")
                is InvoicePreviewUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) { Text("Go Back") }
                    }
                }
                is InvoicePreviewUiState.Success -> {
                    InvoiceDetails(invoice = state.invoice, items = state.items)
                }
            }
        }
    }
}

@Composable
fun InvoiceDetails(invoice: Invoice, items: List<InvoiceItem>) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Invoice #${invoice.invoiceNumber}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Date: ${dateFormatter.format(invoice.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Supply Type: ${invoice.supplyType.replace("_", " ")}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Billed To
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Billed To", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Name: ${invoice.customerName}", style = MaterialTheme.typography.bodyLarge)
                if (!invoice.customerGSTIN.isNullOrBlank()) {
                    Text("GSTIN: ${invoice.customerGSTIN}", style = MaterialTheme.typography.bodyMedium)
                }
                if (invoice.buyerAddress.isNotBlank()) {
                    Text("Address: ${invoice.buyerAddress}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Items Table
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Line Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                items.forEachIndexed { index, item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${index + 1}.", modifier = Modifier.width(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.description, fontWeight = FontWeight.Medium)
                            Text("${item.quantity} ${item.unitType} x ₹${item.pricePerUnit} (+ ${item.gstPercentage}% GST)", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(String.format(Locale.US, "₹%.2f", item.totalAmount), fontWeight = FontWeight.Bold)
                    }
                    if (index < items.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        // Totals Summary
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val subtotal = invoice.totalAmount - invoice.taxAmount

                SummaryRow("Subtotal:", subtotal)
                if (invoice.totalCgst > 0) SummaryRow("CGST:", invoice.totalCgst)
                if (invoice.totalSgst > 0) SummaryRow("SGST:", invoice.totalSgst)
                if (invoice.totalIgst > 0) SummaryRow("IGST:", invoice.totalIgst)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Grand Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(String.format(Locale.US, "₹%.2f", invoice.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(String.format(Locale.US, "₹%.2f", amount))
    }
}
