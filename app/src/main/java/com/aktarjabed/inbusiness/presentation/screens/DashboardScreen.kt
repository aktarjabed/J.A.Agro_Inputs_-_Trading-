package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.presentation.components.MetricCard
import com.aktarjabed.inbusiness.presentation.viewmodel.CalculatorViewModel

@Composable
fun DashboardScreen(
    viewModel: CalculatorViewModel = hiltViewModel(),
    onNavigateToCalculator: () -> Unit,
    onNavigateToInvoice: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {}
) {
    val metrics by viewModel.financialMetrics.collectAsState()
    val scenarios by viewModel.savedScenarios.collectAsState()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Revenue",
                    value = "₹${"%.2f".format(metrics.revenue)}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "EBITDA",
                    value = "₹${"%.2f".format(metrics.ebitda)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Cash Flow",
                    value = "₹${"%.2f".format(metrics.cashFlow)}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "ROI",
                    value = "${"%.1f".format(metrics.roi)}%",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Text("Saved Scenarios", style = MaterialTheme.typography.titleLarge)
        }

        if (scenarios.isEmpty()) {
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        "No scenarios saved yet.\nTap + to create one.",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(scenarios) { scenario ->
            Card(
                onClick = { viewModel.loadScenario(scenario) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(scenario.scenarioName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Revenue: ₹${"%.2f".format(scenario.unitPrice * scenario.quantity)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNavigateToCalculator,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Calculator")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNavigateToInvoice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Invoice")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNavigateToInventory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Inventory")
            }
        }
    }
}