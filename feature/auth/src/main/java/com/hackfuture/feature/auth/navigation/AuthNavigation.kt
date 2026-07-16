package com.hackfuture.feature.auth.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hackfuture.feature.auth.ui.LoginScreen
import com.hackfuture.feature.auth.ui.RegisterScreen

object AuthRoutes {
    const val GRAPH = "auth_graph"
    const val LOGIN = "login"
    const val REGISTER = "register"

    fun navOptions(): NavOptions = NavOptions.Builder()
        .setPopUpTo(GRAPH, inclusive = true)
        .build()
}

@Composable
fun AuthNavGraph(
    navController: NavHostController,
    onNavigateToHome: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = AuthRoutes.LOGIN,
        route = AuthRoutes.GRAPH,
    ) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(
                onNavigateToHome = onNavigateToHome,
            )
        }
        composable(AuthRoutes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
    }
}
