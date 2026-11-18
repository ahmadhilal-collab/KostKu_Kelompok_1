package com.kostku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.kostku.app.data.local.RoomPhotoEntity
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.White
import com.kostku.app.util.photoModel
import java.io.File

/** Galeri foto kamar yang bisa digeser dan diklik untuk membuka Full Screen Viewer. */
@Composable
fun RoomPhotoPager(
    photos: List<RoomPhotoEntity>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
    onPhotoClick: ((Int) -> Unit)? = null
) {
    var fullScreenIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier.clip(RoundedCornerShape(18.dp)).background(Green50)) {
        if (photos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.Bed, null, tint = Ink400, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(6.dp))
                Text("Belum ada foto kamar", style = MaterialTheme.typography.bodySmall, color = Ink600)
            }
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = photoModel(photos[page].uri),
                    contentDescription = photos[page].caption.ifBlank { "Foto kamar" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (onPhotoClick != null) onPhotoClick(page)
                            else fullScreenIndex = page
                        }
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
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

    // Full Screen Image Viewer Modal
    val showIndex = fullScreenIndex
    if (showIndex != null && photos.isNotEmpty()) {
        FullScreenImageViewer(
            photos = photos,
            initialIndex = showIndex,
            onDismiss = { fullScreenIndex = null }
        )
    }
}

/** Tampilan Full Screen Image Viewer dengan Pinch-to-Zoom, Pan, & Double Tap. */
@Composable
fun FullScreenImageViewer(
    photos: List<RoomPhotoEntity>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Reset zoom ketika berpindah halaman
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = scale == 1f, // Swipe halaman hanya jika tidak dalam mode zoom
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val currentPhoto = photos.getOrNull(page)
                    if (currentPhoto != null) {
                        AsyncImage(
                            model = photoModel(currentPhoto.uri),
                            contentDescription = currentPhoto.caption.ifBlank { "Foto Kamar Fullscreen" },
                            contentScale = ContentScale.Fit, // Tampilkan seluruh gambar tanpa terpotong
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                                        scale = newScale
                                        if (newScale > 1f) {
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1.2f) {
                                                scale = 1f
                                                offsetX = 0f
                                                offsetY = 0f
                                            } else {
                                                scale = 2.5f
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }
            }

            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Foto Kamar",
                        style = MaterialTheme.typography.titleMedium,
                        color = White
                    )
                    photos.getOrNull(pagerState.currentPage)?.caption?.let { cap ->
                        if (cap.isNotBlank()) {
                            Text(
                                text = cap,
                                style = MaterialTheme.typography.bodySmall,
                                color = White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
                // Counter Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${photos.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                if (scale > 1f) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset Zoom",
                            tint = White
                        )
                    }
                }
            }
        }
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
