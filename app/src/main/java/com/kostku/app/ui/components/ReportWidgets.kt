package com.kostku.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kostku.app.data.local.ReportStatus
import com.kostku.app.ui.theme.Gold100
import com.kostku.app.ui.theme.Green100
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Warning

fun reportStatusLabel(status: ReportStatus): String = when (status) {
    ReportStatus.SENT -> "Dikirim"
    ReportStatus.IN_PROGRESS -> "Diproses"
    ReportStatus.DONE -> "Selesai"
}

@Composable
fun ReportStatusChip(status: ReportStatus, modifier: Modifier = Modifier) {
    when (status) {
        ReportStatus.SENT -> StatusChip("DIKIRIM", Gold100, Warning, modifier)
        ReportStatus.IN_PROGRESS -> StatusChip("DIPROSES", Green50, Green700, modifier)
        ReportStatus.DONE -> StatusChip("SELESAI", Green100, Green700, modifier)
    }
}
