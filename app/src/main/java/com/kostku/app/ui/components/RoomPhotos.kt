package com.kostku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kostku.app.data.local.RoomPhotoEntity
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.White
import java.io.File

/** Galeri foto kamar yang bisa digeser. Jika belum ada foto, tampil placeholder. */
@Composable
fun RoomPhotoPager(
    photos: List<RoomPhotoEntity>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null
) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Green50)) {
        if (photos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.Bed, null, tint = Ink400, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(6.dp))
                Text("Belum ada foto kamar", style = MaterialTheme.typography.bodySmall, color = Ink600)
            }
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = File(photos[page].uri),
                    contentDescription = photos[page].caption.ifBlank { "Foto kamar" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "${pagerState.currentPage + 1} / ${photos.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = White
                )
            }
        }
        if (badge != null) Box(Modifier.align(Alignment.TopStart).padding(12.dp)) { badge() }
    }
}

/** Garis putus-putus untuk area unggah foto. */
fun Modifier.dashedBorder(color: Color, cornerRadius: androidx.compose.ui.unit.Dp = 16.dp): Modifier =
    this.drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)))
        )
    }
