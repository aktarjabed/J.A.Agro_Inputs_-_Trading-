package com.aktarjabed.inbusiness.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aktarjabed.inbusiness.presentation.screens.CalculatorScreen
import com.aktarjabed.inbusiness.presentation.screens.DashboardScreen
import com.aktarjabed.inbusiness.presentation.screens.invoice.InvoiceScreen
import com.aktarjabed.inbusiness.presentation.screens.ProductEntryScreen
import com.aktarjabed.inbusiness.presentation.viewmodel.ProductViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun InBusinessNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToCalculator = { navController.navigate("calculator") },
                onNavigateToInvoice = { navController.navigate("invoice") }
            )
        }
        composable("calculator") {
            CalculatorScreen()
        }
        composable("invoice") {
            InvoiceScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUpgrade = { /* TODO: Navigate to upgrade screen */ }
            )
        }
        composable("addProduct") {
            val productViewModel: ProductViewModel = hiltViewModel()
            val categories by productViewModel.existingCategories.collectAsState()
            val unitTypes by productViewModel.existingUnitTypes.collectAsState()

            ProductEntryScreen(
                existingCategories = categories,
                existingUnitTypes = unitTypes,
                onNavigateBack = { navController.popBackStack() },
                onSaveProduct = { name, brand, category, unitType, price, stock, batch, isWholesale ->
                    productViewModel.saveProduct(
                        name = name,
                        brand = brand,
                        category = category,
                        unitType = unitType,
                        price = price,
                        stock = stock,
                        batch = batch.ifBlank { null },
                        isWholesale = isWholesale
                    )
                    navController.popBackStack()
                }
            )
        }
    }
}