package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.presentation.components.InputField
import com.aktarjabed.inbusiness.presentation.components.MetricCard
import com.aktarjabed.inbusiness.presentation.viewmodel.CalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val data by viewModel.businessData.collectAsState()
    val metrics by viewModel.financialMetrics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Business Calculator") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Costs & Expenses", style = MaterialTheme.typography.titleMedium)

                    InputField(
                        value = data.rawMaterialsCost.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(rawMaterialsCost = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Raw Materials (₹)"
                    )
                    InputField(
                        value = data.supplierCosts.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(supplierCosts = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Supplier Costs (₹)"
                    )
                    InputField(
                        value = data.inputGst.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(inputGst = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Input GST (₹)"
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Revenue", style = MaterialTheme.typography.titleMedium)

                    InputField(
                        value = data.unitPrice.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(unitPrice = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Unit Price (₹)"
                    )
                    InputField(
                        value = data.quantity.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(quantity = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Quantity"
                    )
                    InputField(
                        value = data.outputGst.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(outputGst = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Output GST (₹)"
                    )
                }
            }

            // Results Section
            Text("Financial Metrics", style = MaterialTheme.typography.titleLarge)

            MetricCard("Net Profit", "₹${metrics.netProfit}")
            MetricCard("EBITDA", "₹${metrics.ebitda}")
            MetricCard("Gross Margin", "${metrics.grossMargin}%")
            MetricCard("Net Margin", "${metrics.netMargin}%")
            MetricCard("ROI", "${metrics.roi}%")
            MetricCard("Break Even Point", "${metrics.breakEvenPoint} units")
            MetricCard("GST Payable", "₹${metrics.gstPayable}")
            MetricCard("Cash Flow", "₹${metrics.cashFlow}")
        }
    }
}
