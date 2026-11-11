package com.kostku.app.ui.screens.tenant

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kostku.app.data.local.Session
import com.kostku.app.ui.components.BottomTab
import com.kostku.app.ui.components.KostKuBottomBar
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.navigateTab
import com.kostku.app.ui.screens.common.ProfileInfoScreen
import com.kostku.app.ui.screens.common.ReceiptScreen
import com.kostku.app.ui.theme.White
import kotlinx.coroutines.launch

object TenantRoutes {
    const val HOME = "t_home"
    const val REPAIR = "t_repair"
    const val PROFILE = "t_profile"
    const val NOTICE = "t_notice"
    const val PAYMENTS = "t_payments"
    const val RECEIPT = "t_receipt/{id}"
    fun receipt(id: Long) = "t_receipt/$id"
}

private val tenantTabs = listOf(
    BottomTab(TenantRoutes.HOME, "Beranda", Icons.Outlined.Home, Icons.Filled.Home),
    BottomTab(TenantRoutes.REPAIR, "Perbaikan", Icons.Outlined.Build, Icons.Filled.Build),
    BottomTab(TenantRoutes.PROFILE, "Profil & Info", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
)

@Composable
fun TenantShell(session: Session, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val vm: TenantViewModel = viewModel(factory = TenantViewModel.factory(session.userId))
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify = remember {
        val f: (String) -> Unit = { msg ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(msg)
            }
        }
        f
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tenantTabs.any { it.route == route }

    CompositionLocalProvider(LocalNotifier provides notify) {
        Scaffold(
            containerColor = White,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (showBar) KostKuBottomBar(tenantTabs, route) { nav.navigateTab(it, TenantRoutes.HOME) }
            }
        ) { inner ->
            NavHost(
                navController = nav,
                startDestination = TenantRoutes.HOME,
                modifier = Modifier.padding(inner).consumeWindowInsets(inner),
                enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 24 } },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(260)) },
                popExitTransition = { fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 24 } }
            ) {
                composable(TenantRoutes.HOME) { TenantHomeScreen(vm, session, nav) }
                composable(TenantRoutes.REPAIR) { TenantRepairScreen(vm, session) }
                composable(TenantRoutes.PROFILE) {
                    ProfileInfoScreen(session = session, roleLabel = "Penghuni", onLogout = onLogout)
                }
                composable(TenantRoutes.NOTICE) { NoticeScreen(vm, session, nav) }
                composable(TenantRoutes.PAYMENTS) { PaymentsScreen(vm, session, nav) }
                composable(
                    TenantRoutes.RECEIPT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) {
                    ReceiptScreen(
                        paymentId = it.arguments?.getLong("id") ?: -1L,
                        load = vm::loadReceipt,
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}
