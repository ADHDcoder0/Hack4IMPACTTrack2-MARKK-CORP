package com.example.scrapsetu.view.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.example.scrapsetu.view.screens.auth.LoginScreen
import com.example.scrapsetu.view.screens.auth.ProfileScreen
import com.example.scrapsetu.view.screens.auth.RegisterScreen
import com.example.scrapsetu.view.screens.buyer.BuyerDashboardScreen
import com.example.scrapsetu.view.screens.buyer.BuyerRequestsScreen
import com.example.scrapsetu.view.screens.buyer.MatchStatusScreen
import com.example.scrapsetu.view.screens.chat.ChatScreen
import com.example.scrapsetu.view.screens.supplier.SupplierDashboardScreen
import com.example.scrapsetu.view.screens.supplier.SupplierMatchScreen
import com.example.scrapsetu.vm.AuthViewModel


sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object SupplierDashboard : Screen("supplier_dashboard")
    object BuyerDashboard : Screen("buyer_dashboard")
    object BuyerRequests : Screen("buyer_requests")
    object MatchStatus : Screen("match_status")
    object SupplierMatches : Screen("supplier_matches")
    object Chat : Screen("chat/{matchId}/{otherUserId}/{otherUserName}") {
        fun createRoute(matchId: String, otherUserId: String, otherUserName: String): String {
            val encodedName = URLEncoder.encode(otherUserName, StandardCharsets.UTF_8.toString())
            return "chat/$matchId/$otherUserId/$encodedName"
        }
    }
    object Profile : Screen("profile/{role}") {
        fun createRoute(role: String): String = "profile/$role"
    }
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
            val authViewModel: AuthViewModel = hiltViewModel()
            SupplierDashboardScreen(
                onSignOut = {
                    authViewModel.signOut {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onViewMatches = {
                    navController.navigateTopLevel(Screen.SupplierMatches.route)
                },
                onOpenMarketplace = {
                    navController.navigateTopLevel(Screen.SupplierDashboard.route)
                },
                onOpenProfile = {
                    navController.navigate(Screen.Profile.createRoute("supplier"))
                }
            )
        }
        composable(Screen.BuyerDashboard.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            BuyerDashboardScreen(
                onSignOut = {
                    authViewModel.signOut {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onViewMatches = {
                    navController.navigateTopLevel(Screen.MatchStatus.route)
                },
                onOpenDashboard = {
                    navController.navigateTopLevel(Screen.BuyerDashboard.route)
                },
                onOpenProfile = {
                    navController.navigate(Screen.Profile.createRoute("buyer"))
                },
                onOpenRequests = {
                    navController.navigate(Screen.BuyerRequests.route)
                }
            )
        }
        composable(Screen.BuyerRequests.route) {
            BuyerRequestsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MatchStatus.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            MatchStatusScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { matchId, otherUserId, otherUserName ->
                    navController.navigate(
                        Screen.Chat.createRoute(matchId, otherUserId, otherUserName)
                    )
                },
                onNavigateToDashboard = {
                    navController.navigateTopLevel(Screen.BuyerDashboard.route)
                },
                onOpenProfile = {
                    navController.navigate(Screen.Profile.createRoute("buyer"))
                },
                onSignOut = {
                    authViewModel.signOut {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.SupplierMatches.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            SupplierMatchScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { matchId, otherUserId, otherUserName ->
                    navController.navigate(
                        Screen.Chat.createRoute(matchId, otherUserId, otherUserName)
                    )
                },
                onNavigateToMarketplace = {
                    navController.navigateTopLevel(Screen.SupplierDashboard.route)
                },
                onOpenProfile = {
                    navController.navigate(Screen.Profile.createRoute("supplier"))
                },
                onSignOut = {
                    authViewModel.signOut {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val authViewModel: AuthViewModel = hiltViewModel()
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    authViewModel.signOut {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                roleHint = backStackEntry.arguments?.getString("role").orEmpty()
            )
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val authViewModel: AuthViewModel = hiltViewModel()
            val currentUser = authViewModel.currentUser.collectAsState().value
            androidx.compose.runtime.LaunchedEffect(Unit) {
                authViewModel.loadCurrentUserDetails()
            }

            val matchId = backStackEntry.arguments?.getString("matchId").orEmpty()
            val otherUserName = URLDecoder.decode(
                backStackEntry.arguments?.getString("otherUserName").orEmpty(),
                StandardCharsets.UTF_8.toString()
            )

            if (matchId.isNotBlank() && !currentUser?.id.isNullOrBlank()) {
                ChatScreen(
                    matchId = matchId,
                    currentUserId = currentUser?.id.orEmpty(),
                    otherUserName = otherUserName.ifBlank { "User" },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}