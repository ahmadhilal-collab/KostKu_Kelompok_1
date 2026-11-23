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
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kostku.app.push.PushNavigation
import com.kostku.app.push.PushNotifications
import com.kostku.app.ui.navigation.AppViewModel
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
import com.kostku.app.ui.theme.AppBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TenantRoutes {
    const val HOME = "t_home"
    const val REPAIR = "t_repair"
    const val PROFILE = "t_profile"
    const val NOTICE = "t_notice"
    const val PAYMENTS = "t_payments"
    const val RECEIPT = "t_receipt/{id}"
    fun receipt(id: Long) = "t_receipt/$id"
    const val PAY = "t_pay/{id}?cash={cash}"
    fun pay(id: Long, cash: Boolean = false) = "t_pay/$id?cash=$cash"
    const val PROFILE_EDIT = "t_profile_edit"
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
    // Nama di sesi ikut berubah setelah penghuni/pengelola memperbarui data diri.
    val profile by vm.profile.collectAsStateWithLifecycle()
    val live = profile?.name?.takeIf { it.isNotBlank() }?.let { session.copy(name = it) } ?: session
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
    // Pesan dari server (berhasil / gagal / tidak ada internet) → snackbar
    LaunchedEffect(vm) { vm.messages.collect { notify(it) } }
    // Ambil data terbaru dari server saat aplikasi tampil, lalu tiap 30 detik selama tetap tampil.
    // Berhenti otomatis saat aplikasi di latar belakang (hemat baterai & kuota).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(vm, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                vm.refresh()
                delay(30_000)
            }
        }
    }
    // Android 13+: minta izin notifikasi SETELAH panduan selesai (supaya tidak menumpuk dengan panduan).
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val tourDone by appViewModel.isTenantTourCompleted.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) notify("Notifikasi dimatikan. Aktifkan di Pengaturan HP agar tidak melewatkan tagihan.")
    }
    LaunchedEffect(tourDone) {
        if (tourDone == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PushNotifications.canPost(context)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Notifikasi HP diketuk → buka layar yang sesuai (tagihan, perbaikan, pengumuman, profil).
    val pushTarget by PushNavigation.target.collectAsStateWithLifecycle()
    LaunchedEffect(pushTarget) {
        val target = pushTarget ?: return@LaunchedEffect
        PushNavigation.consume()
        if (target.scope != "tenant") return@LaunchedEffect
        vm.refresh()
        val t = target.type
        when {
            t.startsWith("payment") || t.startsWith("due:") -> nav.navigate(TenantRoutes.PAYMENTS) { launchSingleTop = true }
            t.startsWith("repair") -> nav.navigateTab(TenantRoutes.REPAIR, TenantRoutes.HOME)
            t.startsWith("announcement") -> nav.navigate(TenantRoutes.NOTICE) { launchSingleTop = true }
            t.startsWith("profile") -> nav.navigateTab(TenantRoutes.PROFILE, TenantRoutes.HOME)
        }
    }

    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tenantTabs.any { it.route == route }

    CompositionLocalProvider(LocalNotifier provides notify) {
        Scaffold(
            containerColor = AppBackground,
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
                composable(TenantRoutes.HOME) { TenantHomeScreen(vm, live, nav) }
                composable(TenantRoutes.REPAIR) { TenantRepairScreen(vm, live) }
                composable(TenantRoutes.PROFILE) {
                    ProfileInfoScreen(
                        session = live,
                        roleLabel = "Penghuni",
                        onLogout = onLogout,
                        onReplayTour = { nav.navigateTab(TenantRoutes.HOME, TenantRoutes.HOME) },
                        onEditProfile = { nav.navigate(TenantRoutes.PROFILE_EDIT) }
                    )
                }
                composable(TenantRoutes.NOTICE) { NoticeScreen(vm, live, nav) }
                composable(TenantRoutes.PAYMENTS) { PaymentsScreen(vm, live, nav) }
                composable(
                    TenantRoutes.PAY,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("cash") { type = NavType.BoolType; defaultValue = false }
                    )
                ) {
                    PayScreen(
                        paymentId = it.arguments?.getLong("id") ?: -1L,
                        startWithCash = it.arguments?.getBoolean("cash") ?: false,
                        vm = vm, session = live, nav = nav
                    )
                }
                composable(TenantRoutes.PROFILE_EDIT) { ProfileEditScreen(vm, nav) }
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
