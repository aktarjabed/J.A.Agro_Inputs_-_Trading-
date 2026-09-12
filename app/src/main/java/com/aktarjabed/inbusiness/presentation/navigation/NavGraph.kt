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
import com.aktarjabed.inbusiness.presentation.screens.invoice_preview.InvoicePreviewScreen
import com.aktarjabed.inbusiness.presentation.screens.SplashScreen
import com.aktarjabed.inbusiness.presentation.screens.SetupScreen

@Composable
fun InBusinessNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onNavigateToDashboard = {
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToSetup = {
                    navController.navigate("setup") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("setup") {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate("dashboard") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }
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
                onNavigateToUpgrade = { /* TODO: Navigate to upgrade screen */ },
                onNavigateToPreview = { invoiceId ->
                    navController.popBackStack()
                    navController.navigate("invoice-preview/$invoiceId")
                }
            )
        }
        composable(
            route = "invoice-preview/{invoiceId}",
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
        ) {
            InvoicePreviewScreen(
                onNavigateBack = { navController.popBackStack() }
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