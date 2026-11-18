package com.kostku.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kostku.app.ui.theme.DarkGreen
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.White
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.flow.first

enum class SpotlightTarget {
    HEADER,
    DENAH_KAMAR,
    DATA_PENGHUNI,
    KATALOG_KAMAR,
    NOTA_PEMBAYARAN,
    PENGUMUMAN,
    PERBAIKAN_FASILITAS,
    TAGIHAN_PENGHUNI,
    TOMBOL_PERBAIKAN,
    FINAL_OVERVIEW
}

data class TourStepData(
    val title: String,
    val description: String,
    val targetName: String,
    val target: SpotlightTarget,
    val icon: ImageVector = Icons.Outlined.Explore
)

val adminTourSteps = listOf(
    TourStepData(
        title = "Selamat Datang, Pengelola!",
        description = "Ini adalah halaman utama untuk memantau aktivitas, statistik kamar, dan kondisi kost Anda.",
        targetName = "Dashboard Utama",
        target = SpotlightTarget.HEADER,
        icon = Icons.Outlined.Explore
    ),
    TourStepData(
        title = "Pantau Seluruh Kamar",
        description = "Gunakan menu Denah Kamar untuk melihat kamar yang kosong, terisi, maupun memiliki status pembayaran tertentu.",
        targetName = "Denah Kamar",
        target = SpotlightTarget.DENAH_KAMAR,
        icon = Icons.Outlined.Map
    ),
    TourStepData(
        title = "Kelola Data Penghuni",
        description = "Lihat dan kelola informasi identitas, nomor telepon, dan status kamar penghuni kost.",
        targetName = "Data Penghuni",
        target = SpotlightTarget.DATA_PENGHUNI,
        icon = Icons.Outlined.Groups
    ),
    TourStepData(
        title = "Informasi Kamar",
        description = "Kelola informasi kamar, daftar fasilitas All-In, harga sewa, dan ketersediaan kamar.",
        targetName = "Katalog Kamar",
        target = SpotlightTarget.KATALOG_KAMAR,
        icon = Icons.Outlined.Apartment
    ),
    TourStepData(
        title = "Pantau Pembayaran",
        description = "Periksa nota pembayaran tunai, tagihan bulanan, dan histori pembayaran lunas.",
        targetName = "Nota Pembayaran",
        target = SpotlightTarget.NOTA_PEMBAYARAN,
        icon = Icons.Outlined.ReceiptLong
    ),
    TourStepData(
        title = "Sampaikan Informasi",
        description = "Gunakan fitur Pengumuman untuk menyampaikan informasi penting kepada seluruh penghuni kost.",
        targetName = "Pengumuman",
        target = SpotlightTarget.PENGUMUMAN,
        icon = Icons.Outlined.Campaign
    ),
    TourStepData(
        title = "Pantau Laporan Kerusakan",
        description = "Buka tab Perbaikan untuk melihat laporan kerusakan dari penghuni dan memperbarui status pengerjaannya hingga selesai.",
        targetName = "Perbaikan Fasilitas",
        target = SpotlightTarget.PERBAIKAN_FASILITAS,
        icon = Icons.Outlined.Build
    ),
    TourStepData(
        title = "Anda Siap Mengelola Kost!",
        description = "Sekarang Anda sudah mengenal fitur utama KostKu. Silakan mulai mengelola kost dengan lebih mudah.",
        targetName = "Panduan Selesai",
        target = SpotlightTarget.FINAL_OVERVIEW,
        icon = Icons.Outlined.CheckCircle
    )
)

val tenantTourSteps = listOf(
    TourStepData(
        title = "Selamat Datang di KostKu!",
        description = "Pantau kebutuhan, tagihan bulanan, dan informasi kost Anda melalui halaman utama ini.",
        targetName = "Beranda Penghuni",
        target = SpotlightTarget.HEADER,
        icon = Icons.Outlined.Explore
    ),
    TourStepData(
        title = "Cek Tagihan Bulanan",
        description = "Lihat jumlah tagihan All-In, periode pembayaran, status lunas, dan tanggal jatuh tempo.",
        targetName = "Informasi Tagihan",
        target = SpotlightTarget.TAGIHAN_PENGHUNI,
        icon = Icons.Outlined.Payments
    ),
    TourStepData(
        title = "Jangan Lewatkan Informasi",
        description = "Dapatkan kabar terbaru dari pengelola kost, termasuk pengingat pembayaran bulanan.",
        targetName = "Pengumuman & Notifikasi",
        target = SpotlightTarget.PENGUMUMAN,
        icon = Icons.Outlined.Campaign
    ),
    TourStepData(
        title = "Laporkan Kerusakan Fasilitas",
        description = "Jika terdapat fasilitas yang rusak, Anda dapat mengirim laporan disertai foto dan deskripsi kerusakan.",
        targetName = "Laporan Perbaikan",
        target = SpotlightTarget.TOMBOL_PERBAIKAN,
        icon = Icons.Outlined.Build
    ),
    TourStepData(
        title = "Pantau Riwayat Pembayaran",
        description = "Periksa transaksi pembayaran sebelumnya dan lihat nota digital lunas Anda.",
        targetName = "Status & Riwayat Pembayaran",
        target = SpotlightTarget.NOTA_PEMBAYARAN,
        icon = Icons.Outlined.ReceiptLong
    ),
    TourStepData(
        title = "Semua Siap Digunakan!",
        description = "Kini Anda dapat menggunakan KostKu untuk membantu kebutuhan kost sehari-hari.",
        targetName = "Panduan Selesai",
        target = SpotlightTarget.FINAL_OVERVIEW,
        icon = Icons.Outlined.CheckCircle
    )
)

// ─────────────────────────────────────────────────────────────────────────────
//  Infrastruktur panduan: TourHost (dipasang sekali di MainActivity),
//  Modifier.tourTarget() (penanda elemen yang disorot), dan TourController.
// ─────────────────────────────────────────────────────────────────────────────

/** Jarak kartu penjelasan dari tepi aman layar (status bar / navigation bar). */
private val CardEdgePadding = 12.dp

/** Jarak minimum antara elemen yang disorot dan kartu penjelasan. */
private val CardTargetGap = 16.dp

/** Jarak garis sorot dari tepi elemen yang disorot. */
private val SpotlightPadding = 6.dp
private val SpotlightRadius = 20.dp
private val ScrimColor = Color.Black.copy(alpha = 0.75f)

private class TourTargetEntry(val owner: Any, val coordinates: LayoutCoordinates)

internal class TourSession(
    val steps: List<TourStepData>,
    val onFinish: () -> Unit,
    val bringIntoView: suspend (SpotlightTarget) -> Unit
)

/**
 * Penghubung antara layar (yang memiliki elemen target) dan lapisan panduan (yang digambar
 * di atas seluruh aplikasi oleh [TourHost]). Ambil lewat `LocalTourController.current`.
 */
@Stable
class TourController {
    internal var session by mutableStateOf<TourSession?>(null)

    /** true = halaman lain (mis. Profil & Info) meminta panduan diputar ulang di Beranda. */
    var replayRequested by mutableStateOf(false)
        private set

    private val targets = mutableMapOf<SpotlightTarget, TourTargetEntry>()
    internal var overlayCoordinates: LayoutCoordinates? = null

    /** Area layar (koordinat lapisan panduan) yang bebas dari kartu saat kartu berada di bawah. */
    internal var safeZone: Rect? = null

    fun requestReplay() {
        replayRequested = true
    }

    /** Mengembalikan true satu kali jika ada permintaan putar ulang, lalu menghapus permintaannya. */
    fun consumeReplayRequest(): Boolean {
        val requested = replayRequested
        replayRequested = false
        return requested
    }

    /** true jika elemen target sedang ada di layar (sudah ter-compose dan terpasang). */
    fun isOnScreen(target: SpotlightTarget): Boolean =
        targets[target]?.coordinates?.isAttached == true

    internal fun register(target: SpotlightTarget, owner: Any, coordinates: LayoutCoordinates) {
        targets[target] = TourTargetEntry(owner, coordinates)
    }

    internal fun unregister(target: SpotlightTarget, owner: Any) {
        if (targets[target]?.owner === owner) targets.remove(target)
    }

    /**
     * Posisi target dalam koordinat lapisan panduan. Karena dihitung relatif terhadap lapisan
     * itu sendiri (bukan terhadap window), hasilnya selalu pas — tidak terpengaruh tinggi
     * status bar. [clip] = true mengembalikan hanya bagian yang benar-benar terlihat.
     */
    internal fun boundsOf(target: SpotlightTarget, clip: Boolean): Rect? {
        val overlay = overlayCoordinates ?: return null
        val coordinates = targets[target]?.coordinates ?: return null
        if (!overlay.isAttached || !coordinates.isAttached) return null
        val rect = try {
            overlay.localBoundingBoxOf(coordinates, clip)
        } catch (e: IllegalStateException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }
        return if (rect.width > 0f && rect.height > 0f) rect else null
    }

    /**
     * Menggulung [scrollable] (ScrollState, LazyGridState, LazyListState, …) secukupnya agar
     * [target] terlihat utuh di area yang tidak tertutup kartu penjelasan.
     */
    suspend fun scrollIntoView(target: SpotlightTarget, scrollable: ScrollableState) {
        // Tunggu beberapa frame: item lazy yang baru muncul butuh waktu untuk melaporkan posisinya.
        var bounds: Rect? = null
        var frame = 0
        while (bounds == null && frame < 15) {
            withFrameNanos { }
            bounds = boundsOf(target, clip = false)
            frame++
        }
        val full = bounds ?: return
        val zone = safeZone ?: return
        val delta = when {
            // Target terlalu ke atas → gulung mundur sampai tepi atasnya masuk area aman.
            full.top < zone.top -> full.top - zone.top
            // Target terlalu ke bawah (tertutup kartu) → gulung maju, tapi jangan sampai tepi
            // atasnya keluar layar bila target lebih tinggi daripada area aman.
            full.bottom > zone.bottom -> min(full.bottom - zone.bottom, full.top - zone.top)
            else -> 0f
        }
        if (abs(delta) > 1f) {
            scrollable.animateScrollBy(delta, tween(durationMillis = 380, easing = FastOutSlowInEasing))
        }
    }
}

val LocalTourController = staticCompositionLocalOf<TourController?> { null }

/**
 * Pasang SEKALI di akar aplikasi (MainActivity) membungkus AppNavHost().
 * Lapisan panduan digambar di window yang sama dengan aplikasi (bukan Dialog), sehingga
 * koordinat sorotan selalu sama persis dengan posisi elemen, dan sorotan bisa menjangkau
 * seluruh layar termasuk bottom bar.
 */
@Composable
fun TourHost(content: @Composable () -> Unit) {
    val controller = remember { TourController() }
    CompositionLocalProvider(LocalTourController provides controller) {
        // propagateMinConstraints = true: isi aplikasi tetap dipaksa memenuhi layar,
        // sama persis seperti sebelum dibungkus TourHost.
        Box(Modifier.fillMaxSize(), propagateMinConstraints = true) {
            content()
            val session = controller.session
            if (session != null) {
                key(session) { TourOverlay(controller, session) }
            }
        }
    }
}

/** Tandai sebuah elemen sebagai target sorotan panduan. */
fun Modifier.tourTarget(target: SpotlightTarget): Modifier = composed {
    val controller = LocalTourController.current
    val owner = remember { Any() }
    DisposableEffect(controller, target) {
        onDispose { controller?.unregister(target, owner) }
    }
    Modifier.onGloballyPositioned { coordinates ->
        controller?.register(target, owner, coordinates)
    }
}

/**
 * Menampilkan panduan interaktif selama composable ini berada di komposisi.
 *
 * @param onBringIntoView dipanggil setiap pindah langkah supaya layar bisa menggulung
 *   target ke posisi yang terlihat (lihat [TourController.scrollIntoView]).
 */
@Composable
fun InteractiveTourDialog(
    steps: List<TourStepData>,
    onFinish: () -> Unit,
    onBringIntoView: suspend (SpotlightTarget) -> Unit = {}
) {
    val controller = LocalTourController.current
        ?: error("TourHost belum dipasang. Bungkus AppNavHost() dengan TourHost { ... } di MainActivity.")
    val currentOnFinish by rememberUpdatedState(onFinish)
    val currentBringIntoView by rememberUpdatedState(onBringIntoView)

    DisposableEffect(controller, steps) {
        val session = TourSession(
            steps = steps,
            onFinish = { currentOnFinish() },
            bringIntoView = { target -> currentBringIntoView(target) }
        )
        controller.session = session
        onDispose {
            if (controller.session === session) controller.session = null
        }
    }
}

private enum class CardPlacement { TOP, CENTER, BOTTOM }

/**
 * Kartu ditaruh di sisi yang TIDAK menutupi target: di bawah bila muat, kalau tidak di atas.
 * Jika dua-duanya tidak muat (layar sangat pendek), pilih sisi yang lebih lega.
 */
private fun choosePlacement(
    target: Rect?,
    cardHeight: Int,
    areaTop: Float,
    areaBottom: Float,
    gap: Float
): CardPlacement {
    if (target == null) return CardPlacement.CENTER
    val spaceBelow = areaBottom - target.bottom - gap
    val spaceAbove = target.top - areaTop - gap
    return when {
        cardHeight <= spaceBelow -> CardPlacement.BOTTOM
        cardHeight <= spaceAbove -> CardPlacement.TOP
        spaceBelow >= spaceAbove -> CardPlacement.BOTTOM
        else -> CardPlacement.TOP
    }
}

@Composable
private fun TourOverlay(controller: TourController, session: TourSession) {
    val steps = session.steps
    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { session.onFinish() }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentIndex.coerceIn(0, steps.lastIndex)]
    val isLast = currentIndex >= steps.lastIndex
    val hasSpotlight = step.target != SpotlightTarget.FINAL_OVERVIEW

    val density = LocalDensity.current
    val safeInsets = WindowInsets.safeDrawing
    val edgePx = with(density) { CardEdgePadding.toPx() }
    val gapPx = with(density) { CardTargetGap.toPx() }

    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var cardHeightPx by remember { mutableIntStateOf(0) }
    // Bagian target yang sedang terlihat (null = belum/tidak ada di layar).
    var targetRect by remember { mutableStateOf<Rect?>(null) }
    // Posisi sorotan terakhir yang diketahui, supaya sorotan bisa memudar di tempat.
    var lastRect by remember { mutableStateOf<Rect?>(null) }
    var placement by remember { mutableStateOf(CardPlacement.BOTTOM) }

    // Batas atas & bawah area yang boleh ditempati kartu (di dalam status bar / navigation bar).
    val areaTop by rememberUpdatedState(safeInsets.getTop(density) + edgePx)
    val areaBottom by rememberUpdatedState(overlaySize.height - safeInsets.getBottom(density) - edgePx)

    BackHandler { session.onFinish() }

    DisposableEffect(controller) {
        onDispose {
            controller.overlayCoordinates = null
            controller.safeZone = null
        }
    }

    // Beri tahu controller area mana yang bebas dari kartu, dipakai scrollIntoView().
    SideEffect {
        controller.safeZone =
            if (cardHeightPx > 0 && overlaySize.height > 0) {
                Rect(
                    left = 0f,
                    top = areaTop,
                    right = overlaySize.width.toFloat(),
                    bottom = areaBottom - cardHeightPx - gapPx - edgePx
                )
            } else {
                null
            }
    }

    // Ikuti posisi target setiap frame. Dengan begini sorotan tetap pas walau target sedang
    // bergerak (animasi masuk halaman, proses gulung) dan tidak pernah memakai posisi basi.
    LaunchedEffect(step.target) {
        while (true) {
            val rect = if (hasSpotlight) controller.boundsOf(step.target, clip = true) else null
            if (rect != targetRect) targetRect = rect
            if (rect != null && rect != lastRect) lastRect = rect
            withFrameNanos { }
        }
    }

    // Setiap pindah langkah: gulung target ke area yang terlihat, lalu tentukan posisi kartu.
    LaunchedEffect(currentIndex) {
        val target = step.target
        if (target == SpotlightTarget.FINAL_OVERVIEW) {
            placement = CardPlacement.CENTER
            session.bringIntoView(target)
            return@LaunchedEffect
        }
        // Tunggu sampai ukuran kartu & layar diketahui.
        snapshotFlow { cardHeightPx > 0 && overlaySize.height > 0 }.first { it }
        session.bringIntoView(target)
        // Beri waktu target melaporkan posisinya (mis. item yang baru saja digulung masuk).
        var waited = 0
        while (controller.boundsOf(target, clip = true) == null && waited < 20) {
            withFrameNanos { }
            waited++
        }
        withFrameNanos { }
        withFrameNanos { }
        snapshotFlow { choosePlacement(targetRect, cardHeightPx, areaTop, areaBottom, gapPx) }
            .collect { placement = it }
    }

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(durationMillis = 220)) }

    val cardBias by animateFloatAsState(
        targetValue = when (placement) {
            CardPlacement.TOP -> -1f
            CardPlacement.CENTER -> 0f
            CardPlacement.BOTTOM -> 1f
        },
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "tourCardBias"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value }
            .onGloballyPositioned { coordinates ->
                controller.overlayCoordinates = coordinates
                overlaySize = coordinates.size
            }
            // Semua sentuhan berhenti di sini: layar di belakang tidak bisa diklik/digulung.
            .pointerInput(Unit) { detectTapGestures(onTap = { }) }
    ) {
        // 1. Latar gelap + lubang sorotan
        TourScrim(rect = lastRect, visible = hasSpotlight && targetRect != null)

        // 2. Kartu penjelasan: bawah / atas / tengah, selalu di dalam area aman layar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = CardEdgePadding),
            contentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = cardBias)
        ) {
            TourCard(
                steps = steps,
                currentIndex = currentIndex.coerceIn(0, steps.lastIndex),
                onBack = { if (currentIndex > 0) currentIndex-- },
                onNext = { if (isLast) session.onFinish() else currentIndex++ },
                onClose = { session.onFinish() },
                modifier = Modifier.onSizeChanged { cardHeightPx = it.height }
            )
        }
    }
}

/** Latar gelap dengan lubang terang + garis hijau di sekeliling target. */
@Composable
private fun TourScrim(rect: Rect?, visible: Boolean) {
    val spotAlpha by animateFloatAsState(
        targetValue = if (visible && rect != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "tourSpotAlpha"
    )
    if (rect == null) {
        Box(Modifier.fillMaxSize().background(ScrimColor))
        return
    }
    // Sorotan berpindah halus dari satu target ke target berikutnya.
    val animatedRect by animateRectAsState(
        targetValue = rect,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tourSpotRect"
    )
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        drawRect(color = ScrimColor)
        if (spotAlpha > 0.01f) {
            val pad = SpotlightPadding.toPx()
            val edge = 2.dp.toPx()
            val left = (animatedRect.left - pad).coerceAtLeast(edge)
            val top = (animatedRect.top - pad).coerceAtLeast(edge)
            val right = (animatedRect.right + pad).coerceAtMost(size.width - edge)
            val bottom = (animatedRect.bottom + pad).coerceAtMost(size.height - edge)
            if (right > left && bottom > top) {
                val topLeft = Offset(left, top)
                val holeSize = Size(right - left, bottom - top)
                val radius = SpotlightRadius.toPx()
                // Lubang terang pada area target
                drawRoundRect(
                    color = Color.Black.copy(alpha = spotAlpha),
                    topLeft = topLeft,
                    size = holeSize,
                    cornerRadius = CornerRadius(radius, radius),
                    blendMode = BlendMode.DstOut
                )
                // Garis tepi hijau KostKu di sekeliling area sorotan
                drawRoundRect(
                    color = Green700.copy(alpha = spotAlpha),
                    topLeft = topLeft,
                    size = holeSize,
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun TourCard(
    steps: List<TourStepData>,
    currentIndex: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStep = steps[currentIndex]
    val isLast = currentIndex >= steps.lastIndex

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = White,
        border = BorderStroke(1.dp, Line),
        shadowElevation = 14.dp,
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Progress Bar Line & Step Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Green50
                ) {
                    Text(
                        text = "Langkah ${currentIndex + 1} dari ${steps.size}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Green700,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClose) {
                        Text("Lewati", color = Ink600, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Tutup Panduan", tint = Ink600)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Linear Progress Indicator
            val progress = (currentIndex + 1).toFloat() / steps.size.toFloat()
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                label = "tourProgress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Green700,
                trackColor = Line
            )

            Spacer(Modifier.height(16.dp))

            // Isi langkah. Semua langkah ikut diukur (tak terlihat) supaya tinggi kartu
            // selalu sama: tombol tidak naik-turun dan posisi kartu bisa dihitung pasti.
            Box {
                steps.forEach { other ->
                    TourStepContent(
                        step = other,
                        modifier = Modifier
                            .alpha(0f)
                            .clearAndSetSemantics { }
                    )
                }
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(160)) },
                    label = "tourStepContent"
                ) { shown ->
                    TourStepContent(step = shown)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Navigation Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentIndex > 0) {
                    SecondaryButton(
                        text = "Kembali",
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    )
                }

                PrimaryButton(
                    text = if (isLast) "Mulai Gunakan KostKu" else "Lanjut",
                    onClick = onNext,
                    modifier = Modifier.weight(if (currentIndex > 0) 1f else 2f)
                )
            }
        }
    }
}

@Composable
private fun TourStepContent(step: TourStepData, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Green50, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = Green700,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = step.targetName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Green700
                )
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink900
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink600
        )
    }
}

/** Component Tooltip / Bubble Bantuan Kontekstual. */
@Composable
fun ContextualTooltipCard(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Green50,
        border = BorderStroke(1.dp, Green700.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = Green700,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = DarkGreen
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Mengerti", color = Green700, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
