package com.example.menstruation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.menstruation.ui.home.HomeScreen
import com.example.menstruation.ui.stats.StatsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Stats : Screen("stats")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}