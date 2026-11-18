package com.kostku.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.BrandHeader
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.ScreenContainer
import com.kostku.app.ui.components.SecondaryButton
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.White
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val highlight: String
)

private val onboardingSlides = listOf(
    OnboardingSlide(
        title = "Kelola Kost Jadi Lebih Mudah",
        description = "Semua kebutuhan pengelolaan kost kini dapat dilakukan melalui satu aplikasi yang praktis dan terintegrasi.",
        icon = Icons.Outlined.Apartment,
        highlight = "Manajemen Kost Terpadu"
    ),
    OnboardingSlide(
        title = "Pantau Kamar dalam Satu Genggaman",
        description = "Lihat data kamar, informasi penghuni, status ketersediaan, dan fasilitas kost dengan lebih mudah.",
        icon = Icons.Outlined.MeetingRoom,
        highlight = "Denah & Status Kamar"
    ),
    OnboardingSlide(
        title = "Pembayaran Lebih Teratur",
        description = "Pantau tagihan bulanan, status pembayaran, riwayat transaksi, dan pengingat jatuh tempo.",
        icon = Icons.Outlined.Payments,
        highlight = "Pencatatan & Pengingat WA"
    ),
    OnboardingSlide(
        title = "Kenyamanan Penghuni Lebih Terjaga",
        description = "Laporkan kerusakan fasilitas kost dan pantau perkembangan perbaikannya dengan mudah.",
        icon = Icons.Outlined.Build,
        highlight = "Laporan Fasilitas Lengkap Foto"
    )
)

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    val pagerState = rememberPagerState { onboardingSlides.size }
    val scope = rememberCoroutineScope()

    ScreenContainer {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().entrance(0),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BrandHeader()
            if (pagerState.currentPage < onboardingSlides.size - 1) {
                TextButton(onClick = onStart) {
                    Text("Lewati", color = Ink600, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Multi-Slide Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val slide = onboardingSlides[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large Visual Box
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Green50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line),
                    modifier = Modifier.size(140.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = slide.title,
                            tint = Green700,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                ) {
                    Text(
                        text = slide.highlight,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Green700,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Ink900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink600,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        // Indicator Dots
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            onboardingSlides.indices.forEach { index ->
                val active = pagerState.currentPage == index
                val width by androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (active) 28.dp else 10.dp,
                    animationSpec = tween(220),
                    label = "indicatorWidth"
                )
                val color by animateColorAsState(
                    targetValue = if (active) Green700 else Line,
                    animationSpec = tween(220),
                    label = "indicatorColor"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(10.dp)
                        .width(width)
                        .background(color, CircleShape)
                )
            }
        }

        // Bottom Navigation Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                SecondaryButton(
                    text = "Kembali",
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (pagerState.currentPage < onboardingSlides.size - 1) {
                PrimaryButton(
                    text = "Selanjutnya",
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.weight(if (pagerState.currentPage > 0) 1f else 2f)
                )
            } else {
                PrimaryButton(
                    text = "Mulai Sekarang",
                    onClick = onStart,
                    modifier = Modifier.weight(if (pagerState.currentPage > 0) 1f else 2f)
                )
            }
        }
    }
}
