package com.kostku.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kostku.app.data.local.UserRole
import com.kostku.app.ui.screens.auth.AdminLoginScreen
import com.kostku.app.ui.screens.auth.AdminRegisterScreen
import com.kostku.app.ui.screens.auth.OnboardingScreen
import com.kostku.app.ui.screens.auth.RoleSelectScreen
import com.kostku.app.ui.screens.auth.SplashScreen
import com.kostku.app.ui.screens.auth.TenantLoginScreen
import com.kostku.app.ui.screens.admin.AdminShell
import com.kostku.app.ui.screens.tenant.TenantShell
import com.kostku.app.ui.theme.White

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val ROLE = "role"
    const val ADMIN_LOGIN = "admin_login"
    const val ADMIN_REGISTER = "admin_register"
    const val TENANT_LOGIN = "tenant_login"
    const val ADMIN_HOME = "admin_home"
    const val TENANT_HOME = "tenant_home"
}

private fun NavHostController.goHomeClearingAll(role: UserRole) {
    val target = if (role == UserRole.ADMIN) Routes.ADMIN_HOME else Routes.TENANT_HOME
    navigate(target) { popUpTo(graph.id) { inclusive = true } }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val sessionState by appViewModel.session.collectAsStateWithLifecycle()

    // Jika sesi berakhir (logout) saat berada di beranda, kembali ke pilihan peran.
    LaunchedEffect(sessionState) {
        val route = navController.currentDestination?.route
        if (sessionState is SessionState.LoggedOut &&
            (route == Routes.ADMIN_HOME || route == Routes.TENANT_HOME)
        ) {
            navController.navigate(Routes.ROLE) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = { slideInHorizontally(tween(380)) { it / 4 } + fadeIn(tween(380)) },
        exitTransition = { slideOutHorizontally(tween(380)) { -it / 6 } + fadeOut(tween(260)) },
        popEnterTransition = { slideInHorizontally(tween(380)) { -it / 6 } + fadeIn(tween(380)) },
        popExitTransition = { slideOutHorizontally(tween(380)) { it / 4 } + fadeOut(tween(260)) }
    ) {
        composable(
            Routes.SPLASH,
            exitTransition = { fadeOut(tween(350)) }
        ) {
            SplashScreen(
                onFinished = {
                    val state = sessionState
                    if (state is SessionState.LoggedIn) {
                        navController.goHomeClearingAll(state.session.role)
                    } else {
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onStart = { navController.navigate(Routes.ROLE) })
        }
        composable(Routes.ROLE) {
            RoleSelectScreen(
                onAdmin = { navController.navigate(Routes.ADMIN_LOGIN) },
                onTenant = { navController.navigate(Routes.TENANT_LOGIN) },
                onBack = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else null
            )
        }
        composable(Routes.ADMIN_LOGIN) {
            AdminLoginScreen(
                onBack = { navController.popBackStack() },
                onRegister = { navController.navigate(Routes.ADMIN_REGISTER) },
                onSuccess = { navController.goHomeClearingAll(UserRole.ADMIN) }
            )
        }
        composable(Routes.ADMIN_REGISTER) {
            AdminRegisterScreen(
                onBack = { navController.popBackStack() },
                onGoLogin = { navController.popBackStack() },
                onSuccess = { navController.goHomeClearingAll(UserRole.ADMIN) }
            )
        }
        composable(Routes.TENANT_LOGIN) {
            TenantLoginScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.goHomeClearingAll(UserRole.TENANT) }
            )
        }
        composable(Routes.ADMIN_HOME) {
            val state = sessionState
            if (state is SessionState.LoggedIn) {
                AdminShell(
                    session = state.session,
                    onLogout = { appViewModel.logout() }
                )
            } else {
                Box(Modifier.fillMaxSize().background(White))
            }
        }
        composable(Routes.TENANT_HOME) {
            val state = sessionState
            if (state is SessionState.LoggedIn) {
                TenantShell(
                    session = state.session,
                    onLogout = { appViewModel.logout() }
                )
            } else {
                Box(Modifier.fillMaxSize().background(White))
            }
        }
    }
}
