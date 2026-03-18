package com.example.scrapsetu.view.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.scrapsetu.view.screens.auth.LoginScreen
import com.example.scrapsetu.view.screens.auth.RegisterScreen
import com.example.scrapsetu.view.screens.buyer.BuyerDashboardScreen
import com.example.scrapsetu.view.screens.buyer.MatchStatusScreen
import com.example.scrapsetu.view.screens.supplier.SupplierDashboardScreen
import com.example.scrapsetu.view.screens.supplier.SupplierMatchScreen


sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object SupplierDashboard : Screen("supplier_dashboard")
    object BuyerDashboard : Screen("buyer_dashboard")
    object MatchStatus : Screen("match_status")
    object SupplierMatches : Screen("supplier_matches")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onBuyerLogin = {
                    navController.navigate(Screen.BuyerDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSupplierLogin = {
                    navController.navigate(Screen.SupplierDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.SupplierDashboard.route) {
            SupplierDashboardScreen(
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onViewMatches = {
                    navController.navigate(Screen.SupplierMatches.route)
                }
            )
        }
        composable(Screen.BuyerDashboard.route) {
            BuyerDashboardScreen(
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onViewMatches = {
                    navController.navigate(Screen.MatchStatus.route)
                }
            )
        }
        composable(Screen.MatchStatus.route) {
            MatchStatusScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SupplierMatches.route) {
            SupplierMatchScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}