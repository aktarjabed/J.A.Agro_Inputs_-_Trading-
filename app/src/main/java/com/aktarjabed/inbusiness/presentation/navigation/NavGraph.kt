package com.aktarjabed.inbusiness.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aktarjabed.inbusiness.presentation.screens.CalculatorScreen
import com.aktarjabed.inbusiness.presentation.screens.DashboardScreen
import com.aktarjabed.inbusiness.presentation.screens.inventory.InventoryListScreen
import com.aktarjabed.inbusiness.presentation.screens.inventory.ProductEntryScreen
import com.aktarjabed.inbusiness.presentation.screens.invoice.InvoiceScreen

@Composable
fun InBusinessNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToCalculator = { navController.navigate("calculator") },
                onNavigateToInvoice = { navController.navigate("invoice") },
                onNavigateToInventory = { navController.navigate("inventory") }
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
        composable("inventory") {
            InventoryListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddProduct = { navController.navigate("addProduct") },
                onNavigateToEditProduct = { productId -> navController.navigate("editProduct/$productId") }
            )
        }
        composable("addProduct") {
            ProductEntryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "editProduct/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId")
            ProductEntryScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}