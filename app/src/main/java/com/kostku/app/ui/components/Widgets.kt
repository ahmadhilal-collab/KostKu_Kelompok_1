package com.kostku.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kostku.app.ui.theme.Danger
import com.kostku.app.ui.theme.DangerBg
import com.kostku.app.ui.theme.Gold100
import com.kostku.app.ui.theme.Green100
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.Warning
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Fungsi untuk menampilkan pesan singkat (snackbar) dari layar mana pun di dalam shell Admin/Penghuni. */
val LocalNotifier = staticCompositionLocalOf<(String) -> Unit> { { _ -> } }

@Composable
fun TopBarWithBack(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onBack)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Ink900,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

@Composable
fun PageHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink900)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ink600)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = Ink900, modifier = modifier)
}

@Composable
fun StatusChip(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = content)
    }
}

@Composable
fun PaymentStatusChip(paid: Boolean, modifier: Modifier = Modifier) {
    if (paid) StatusChip("LUNAS", Green100, Green700, modifier)
    else StatusChip("BELUM BAYAR", DangerBg, Danger, modifier)
}

@Composable
fun MaintenanceChip(modifier: Modifier = Modifier) = StatusChip("PERBAIKAN", Gold100, Warning, modifier)

@Composable
fun EmptyChip(text: String = "KOSONG", modifier: Modifier = Modifier) = StatusChip(text, Green50, Ink600, modifier)

@Composable
fun FloorTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            val bg by animateColorAsState(if (isSelected) Green50 else White, tween(180), label = "tabBg")
            val borderColor by animateColorAsState(if (isSelected) Green700 else Line, tween(180), label = "tabBorder")
            Surface(
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = bg,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(vertical = 11.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (isSelected) Green700 else Ink600
                )
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = White,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val inner: @Composable () -> Unit = { Column(Modifier.padding(16.dp), content = content) }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, Line),
            shadowElevation = 2.dp,
            content = inner
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, Line),
            shadowElevation = 2.dp,
            content = inner
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).background(Green50, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Green700, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink900, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = Ink600, textAlign = TextAlign.Center)
    }
}

@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 44.dp) {
    val initials = name.trim().split(" ").filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }
    Box(
        modifier.size(size).background(Green100, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, style = MaterialTheme.typography.labelLarge, color = Green700)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green700,
    unfocusedBorderColor = Line,
    errorBorderColor = Danger,
    focusedContainerColor = White,
    unfocusedContainerColor = White,
    errorContainerColor = White,
    focusedTextColor = Ink900,
    unfocusedTextColor = Ink900
)

@Composable
fun DropdownField(
    label: String,
    value: String,
    placeholder: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = Ink900)
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text(placeholder, color = Ink400, style = MaterialTheme.typography.bodyLarge) },
                trailingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, null, tint = Ink400) },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors()
            )
            Box(Modifier.matchParentSize().clickable { expanded = true })
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(White)
            ) {
                if (options.isEmpty()) {
                    DropdownMenuItem(text = { Text("Tidak ada pilihan") }, onClick = { expanded = false })
                }
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option, style = MaterialTheme.typography.bodyMedium) },
                        onClick = { expanded = false; onSelect(index) }
                    )
                }
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = Danger, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = Ink900)
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedTextField(
                value = Fmt.date(date),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                trailingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = Ink400) },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors()
            )
            Box(Modifier.matchParentSize().clickable { show = true })
        }
    }
    if (show) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    show = false
                }) { Text("Pilih", color = Green700) }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Batal", color = Ink600) } }
        ) { DatePicker(state = state) }
    }
}

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    /** Jika diisi, tab ini bisa disorot oleh panduan interaktif. */
    val tourTarget: SpotlightTarget? = null
)

@Composable
fun KostKuBottomBar(tabs: List<BottomTab>, currentRoute: String?, onSelect: (String) -> Unit) {
    Surface(
        color = White,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Line),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column {
            NavigationBar(
                containerColor = White,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.height(68.dp)
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val scale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1.0f,
                        animationSpec = tween(180),
                        label = "tabIconScale"
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelect(tab.route) },
                        modifier = tab.tourTarget?.let { Modifier.tourTarget(it) } ?: Modifier,
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Green700,
                            selectedTextColor = Green700,
                            indicatorColor = Green50,
                            unselectedIconColor = Ink600,
                            unselectedTextColor = Ink600
                        )
                    )
                }
            }
        }
    }
}
