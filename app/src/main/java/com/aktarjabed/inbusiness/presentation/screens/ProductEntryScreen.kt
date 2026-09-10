package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aktarjabed.inbusiness.presentation.components.SearchableDropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEntryScreen(
    existingCategories: List<String>,
    existingUnitTypes: List<String>,
    onNavigateBack: () -> Unit,
    onSaveProduct: (name: String, brand: String, category: String, unitType: String, price: Double, stock: Double, batch: String, isWholesale: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf("") }
    var pricePerUnit by remember { mutableStateOf("") }
    var availableStock by remember { mutableStateOf("") }
    var batchNumber by remember { mutableStateOf("") }
    var isWholesaleOnly by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Product") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text("Product Details", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name (e.g., Urea 46% N)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Brand (e.g., IFFCO)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Categorization", style = MaterialTheme.typography.titleMedium)

            SearchableDropdownField(
                value = category,
                onValueChange = { category = it },
                suggestions = existingCategories,
                label = "Category (e.g., Fertilizer, Spice)",
                modifier = Modifier.fillMaxWidth()
            )

            SearchableDropdownField(
                value = unitType,
                onValueChange = { unitType = it },
                suggestions = existingUnitTypes,
                label = "Unit Type (e.g., 50kg Bag, Gram)",
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Inventory & Pricing", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = pricePerUnit,
                    onValueChange = { pricePerUnit = it },
                    label = { Text("Price (₹)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = availableStock,
                    onValueChange = { availableStock = it },
                    label = { Text("Initial Stock") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = batchNumber,
                onValueChange = { batchNumber = it },
                label = { Text("Batch Number (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isWholesaleOnly,
                    onCheckedChange = { isWholesaleOnly = it }
                )
                Text("Wholesale / Sub-distributor Only Item")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val price = pricePerUnit.toDoubleOrNull() ?: 0.0
                    val stock = availableStock.toDoubleOrNull() ?: 0.0

                    onSaveProduct(name, brand, category, unitType, price, stock, batchNumber, isWholesaleOnly)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank() && category.isNotBlank() && unitType.isNotBlank()
            ) {
                Text("Save Product")
            }
        }
    }
}
