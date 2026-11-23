package com.kostku.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.remote.InboxItem
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.EmptyState
import com.kostku.app.ui.components.FloorTabs
import com.kostku.app.ui.components.TopBarWithBack
import com.kostku.app.ui.theme.Danger
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Warning
import com.kostku.app.ui.theme.WarningBg
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt

/** Jenis notifikasi admin dari server. */
private val InboxItem.isTransferProof: Boolean get() = type.startsWith("transfer")
private val InboxItem.isNewRepair: Boolean get() = type.startsWith("repair")

/**
 * Kotak masuk pengelola: bukti transfer yang masuk dan laporan perbaikan baru dari penghuni.
 * Mengetuk notifikasi membuka layar verifikasi transfer / daftar perbaikan.
 */
@Composable
fun AdminNotificationScreen(vm: AdminViewModel, nav: NavHostController, onOpen: (InboxItem) -> Unit) {
    val inbox by vm.inbox.collectAsStateWithLifecycle()
    val unread by vm.unreadCount.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { vm.loadInbox() }

    val shown = inbox.filter {
        when (filter) {
            1 -> it.isTransferProof
            2 -> it.isNewRepair
            else -> true
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Notifikasi", { nav.popBackStack() }) {
            if (unread > 0) {
                TextButton(onClick = { vm.markAllNotificationsRead() }) {
                    Text("Tandai semua dibaca", color = Green700, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    Text(
                        if (unread > 0) "$unread notifikasi belum dibaca" else "Semua notifikasi sudah dibaca",
                        style = MaterialTheme.typography.bodyMedium, color = Ink600
                    )
                    Spacer(Modifier.height(12.dp))
                    FloorTabs(listOf("Semua", "Transfer", "Perbaikan"), filter, { filter = it })
                    Spacer(Modifier.height(4.dp))
                }
            }
            if (shown.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Outlined.NotificationsNone,
                        "Belum ada notifikasi",
                        "Bukti transfer dan laporan perbaikan dari penghuni akan muncul di sini."
                    )
                }
            }
            items(shown, key = { it.id }) { n ->
                AdminInboxCard(n) {
                    vm.markNotificationRead(n.id)
                    onOpen(n)
                }
            }
        }
    }
}

@Composable
private fun AdminInboxCard(item: InboxItem, onClick: () -> Unit) {
    val (icon, tint, bg) = when {
        item.isTransferProof -> Triple(Icons.Outlined.Payments, Green700, Green50)
        item.isNewRepair -> Triple(Icons.Outlined.Build, Warning, WarningBg)
        else -> Triple(Icons.Outlined.Notifications, Green700, Green50)
    }
    AppCard(Modifier.fillMaxWidth(), onClick = onClick, containerColor = if (item.read) White else Color(0xFFF4FBF6)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(40.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (item.read) FontWeight.SemiBold else FontWeight.Bold),
                        color = Ink900,
                        modifier = Modifier.weight(1f)
                    )
                    if (!item.read) Box(Modifier.size(8.dp).background(Green700, CircleShape))
                }
                Spacer(Modifier.height(2.dp))
                Text(item.body, style = MaterialTheme.typography.bodySmall, color = Ink600)
                Spacer(Modifier.height(6.dp))
                Text(timeAgo(item.sentAt), style = MaterialTheme.typography.labelSmall, color = Ink400)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Ink400, modifier = Modifier.padding(start = 4.dp).size(18.dp))
        }
    }
}

/** "baru saja", "5 menit lalu", "3 jam lalu", "kemarin", lalu tanggal. */
internal fun timeAgo(millis: Long, now: Long = System.currentTimeMillis()): String {
    if (millis <= 0L) return "-"
    val minutes = (now - millis) / 60_000
    return when {
        minutes < 1 -> "Baru saja"
        minutes < 60 -> "$minutes menit lalu"
        minutes < 24 * 60 -> "${minutes / 60} jam lalu"
        minutes < 48 * 60 -> "Kemarin"
        else -> Fmt.date(millis)
    }
}

/** Ikon lonceng dengan jumlah notifikasi belum dibaca (dipakai di banner Beranda admin). */
@Composable
fun NotificationBell(unread: Int, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Box {
            Icon(
                if (unread > 0) Icons.Outlined.Notifications else Icons.Outlined.NotificationsNone,
                contentDescription = if (unread > 0) "Notifikasi, $unread belum dibaca" else "Notifikasi",
                tint = tint
            )
            if (unread > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(18.dp)
                        .background(Danger, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (unread > 9) "9+" else unread.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = White
                    )
                }
            }
        }
    }
}
