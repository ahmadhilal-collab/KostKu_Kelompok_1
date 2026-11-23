package com.kostku.app.ui.screens.admin

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kostku.app.data.local.Session
import com.kostku.app.push.PushNavigation
import com.kostku.app.push.PushNotifications
import com.kostku.app.ui.navigation.AppViewModel
import com.kostku.app.ui.components.BottomTab
import com.kostku.app.ui.components.KostKuBottomBar
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.SpotlightTarget
import com.kostku.app.ui.components.navigateTab
import com.kostku.app.ui.screens.common.ProfileInfoScreen
import com.kostku.app.ui.screens.common.ReceiptScreen
import com.kostku.app.ui.theme.AppBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AdminRoutes {
    const val HOME = "a_home"
    const val ROOMS = "a_rooms"
    const val REPAIRS = "a_repairs"
    const val PROFILE = "a_profile"
    const val ROOM = "a_room/{id}"
    const val TENANTS = "a_tenants"
    const val TENANT_ADD = "a_tenant_add?roomId={roomId}"
    const val TENANT = "a_tenant/{id}"
    const val ANNOUNCE = "a_announce"
    const val ANNOUNCE_NEW = "a_announce_new"
    const val CATALOG = "a_catalog"
    const val RECEIPTS = "a_receipts"
    const val RECEIPT = "a_receipt/{id}"
    const val RECAP_MONTH = "a_recap_month"
    const val RECAP_ROOM = "a_recap_room"
    const val ROOM_HISTORY = "a_room_history/{id}"
    const val TRANSFER = "a_transfer/{id}"
    const val PAYMENT_SETTINGS = "a_payment_settings"
    const val TENANT_EDIT = "a_tenant_edit/{id}"
    const val NOTIFICATIONS = "a_notifications"

    fun room(id: Long) = "a_room/$id"
    fun tenant(id: Long) = "a_tenant/$id"
    fun tenantAdd(roomId: Long = -1L) = "a_tenant_add?roomId=$roomId"
    fun receipt(id: Long) = "a_receipt/$id"
    fun roomHistory(id: Long) = "a_room_history/$id"
    fun transfer(id: Long) = "a_transfer/$id"
    fun tenantEdit(id: Long) = "a_tenant_edit/$id"
}

private val adminTabs = listOf(
    BottomTab(AdminRoutes.HOME, "Beranda", Icons.Outlined.Home, Icons.Filled.Home),
    BottomTab(AdminRoutes.ROOMS, "Kelola Kamar", Icons.Outlined.MeetingRoom, Icons.Filled.MeetingRoom),
    BottomTab(
        AdminRoutes.REPAIRS, "Perbaikan", Icons.Outlined.Build, Icons.Filled.Build,
        tourTarget = SpotlightTarget.PERBAIKAN_FASILITAS
    ),
    BottomTab(AdminRoutes.PROFILE, "Profil & Info", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
)

@Composable
fun AdminShell(session: Session, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val vm: AdminViewModel = viewModel(factory = AdminViewModel.factory(session.name))
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
    // Android 13+: minta izin notifikasi supaya HP admin bisa menerima push bukti transfer & laporan.
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) notify("Notifikasi dimatikan. Aktifkan di Pengaturan HP agar tidak melewatkan bukti transfer & laporan.")
    }
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val tourDone by appViewModel.isAdminTourCompleted.collectAsStateWithLifecycle()
    LaunchedEffect(tourDone) {
        if (tourDone == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PushNotifications.canPost(context)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Buka layar yang dirujuk notifikasi admin (dari kotak masuk maupun dari notifikasi HP). */
    fun openNotification(type: String, refId: Long?) {
        when {
            type.startsWith("transfer") && refId != null -> nav.navigate(AdminRoutes.transfer(refId))
            type.startsWith("repair") -> {
                vm.focusReport(refId)
                nav.navigateTab(AdminRoutes.REPAIRS, AdminRoutes.HOME)
            }
            else -> nav.navigate(AdminRoutes.NOTIFICATIONS)
        }
    }

    // Notifikasi HP diketuk → muat data terbaru lalu buka layar terkait.
    val pushTarget by PushNavigation.target.collectAsStateWithLifecycle()
    LaunchedEffect(pushTarget) {
        val target = pushTarget ?: return@LaunchedEffect
        PushNavigation.consume()
        if (target.scope != "admin") return@LaunchedEffect
        vm.refresh()
        target.notificationId?.let { vm.markNotificationRead(it) }
        openNotification(target.type, target.refId)
    }

    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = adminTabs.any { it.route == route }

    CompositionLocalProvider(LocalNotifier provides notify) {
        Scaffold(
            containerColor = AppBackground,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (showBar) {
                    KostKuBottomBar(adminTabs, route) { nav.navigateTab(it, AdminRoutes.HOME) }
                }
            }
        ) { inner ->
            NavHost(
                navController = nav,
                startDestination = AdminRoutes.HOME,
                modifier = Modifier.padding(inner).consumeWindowInsets(inner),
                enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 24 } },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(260)) },
                popExitTransition = { fadeOut(tween(160)) + androidx.compose.animation.slideOutVertically(tween(200)) { it / 24 } }
            ) {
                composable(AdminRoutes.HOME) { AdminHomeScreen(vm, nav) }
                composable(AdminRoutes.ROOMS) { DenahScreen(vm, nav, showBack = false) }
                composable(AdminRoutes.REPAIRS) { AdminRepairScreen(vm) }
                composable(AdminRoutes.PROFILE) {
                    ProfileInfoScreen(
                        session = session,
                        roleLabel = "Admin / Pengelola",
                        onLogout = onLogout,
                        onReplayTour = { nav.navigateTab(AdminRoutes.HOME, AdminRoutes.HOME) }
                    )
                }
                composable(
                    AdminRoutes.ROOM,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { RoomDetailScreen(it.arguments?.getLong("id") ?: -1L, vm, nav) }
                composable(AdminRoutes.TENANTS) { TenantListScreen(vm, nav) }
                composable(
                    AdminRoutes.TENANT_ADD,
                    arguments = listOf(navArgument("roomId") { type = NavType.LongType; defaultValue = -1L })
                ) { TenantAddScreen(vm, nav, it.arguments?.getLong("roomId") ?: -1L) }
                composable(
                    AdminRoutes.TENANT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { TenantDetailScreen(it.arguments?.getLong("id") ?: -1L, vm, nav) }
                composable(AdminRoutes.ANNOUNCE) { AnnouncementListScreen(vm, nav) }
                composable(AdminRoutes.ANNOUNCE_NEW) { AnnouncementCreateScreen(vm, nav) }
                composable(AdminRoutes.CATALOG) { CatalogScreen(vm, nav) }
                composable(AdminRoutes.RECEIPTS) { PaymentHubScreen(vm, nav) }
                composable(AdminRoutes.RECAP_MONTH) { MonthlyRecapScreen(vm, nav) }
                composable(AdminRoutes.RECAP_ROOM) { RoomRecapScreen(vm, nav) }
                composable(
                    AdminRoutes.ROOM_HISTORY,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { RoomPaymentHistoryScreen(it.arguments?.getLong("id") ?: -1L, vm, nav) }
                composable(
                    AdminRoutes.TRANSFER,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { TransferVerifyScreen(it.arguments?.getLong("id") ?: -1L, vm, nav) }
                composable(AdminRoutes.PAYMENT_SETTINGS) { PaymentSettingsScreen(vm, nav) }
                composable(AdminRoutes.NOTIFICATIONS) {
                    AdminNotificationScreen(vm, nav) { n -> openNotification(n.type, n.refId) }
                }
                composable(
                    AdminRoutes.TENANT_EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { TenantEditScreen(it.arguments?.getLong("id") ?: -1L, vm, nav) }
                composable(
                    AdminRoutes.RECEIPT,
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
