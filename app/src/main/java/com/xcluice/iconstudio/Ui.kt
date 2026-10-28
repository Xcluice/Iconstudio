package com.xcluice.iconstudio

import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.foundation.isSystemInDarkTheme
import kotlin.math.cos
import kotlin.math.sin

// ---------- palette ----------
val Teal = Color(0xFF12C2B0)
val Blue = Color(0xFF2F80ED)
val Sky = Color(0xFF22D3EE)
val Coral = Color(0xFFFF7A59)
val Amber = Color(0xFFFFC857)

@Immutable
class Pal(val dark: Boolean) {
    val bg: Color = if (dark) Color(0xFF080D18) else Color(0xFFF3F7FC)
    val surface: Color = if (dark) Color(0xFF131B2B) else Color.White
    val text: Color = if (dark) Color(0xFFF2F6FC) else Color(0xFF0E1A2B)
    val sub: Color = if (dark) Color(0xFF93A3BC) else Color(0xFF64748B)
}

val LocalPal = staticCompositionLocalOf { Pal(false) }

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val pal = remember(dark) { Pal(dark) }
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        CompositionLocalProvider(LocalPal provides pal) { content() }
    }
}

// ---------- modifiers ----------
fun Modifier.tap(scaleTo: Float = 0.96f, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) scaleTo else 1f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "tap"
    )
    this
        .graphicsLayer { scaleX = sc; scaleY = sc }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

@Composable
fun Modifier.glass(shape: Shape, strength: Float = 1f): Modifier {
    val p = LocalPal.current
    val a1 = (if (p.dark) 0.16f else 0.66f) * strength
    val a2 = (if (p.dark) 0.06f else 0.32f) * strength
    val b1 = if (p.dark) 0.38f else 0.95f
    return this
        .clip(shape)
        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = a1), Color.White.copy(alpha = a2))))
        .border(
            1.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = b1), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = b1 * 0.5f))),
            shape
        )
}

@Composable
fun Modifier.staggerIn(index: Int): Modifier {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index * 55L).coerceAtMost(520L))
        a.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 44f
    }
}

// ---------- background ----------
@Composable
fun Aurora() {
    val p = LocalPal.current
    val t = rememberInfiniteTransition(label = "aurora")
    val ph by t.animateFloat(
        0f, 6.2832f,
        infiniteRepeatable(tween(28000, easing = LinearEasing)),
        label = "ph"
    )
    Canvas(Modifier.fillMaxSize()) {
        drawRect(p.bg)
        val w = size.width
        val h = size.height
        fun blob(cx: Float, cy: Float, r: Float, col: Color, a: Float) {
            drawCircle(
                Brush.radialGradient(listOf(col.copy(alpha = a), Color.Transparent), center = Offset(cx, cy), radius = r),
                radius = r, center = Offset(cx, cy)
            )
        }
        val k = if (p.dark) 0.32f else 0.40f
        blob(w * (0.12f + 0.14f * sin(ph)), h * (0.10f + 0.06f * cos(ph)), w * 0.95f, Teal, k)
        blob(w * (0.95f - 0.12f * cos(ph)), h * (0.38f + 0.08f * sin(ph)), w * 0.85f, Blue, k * 0.9f)
        blob(w * (0.25f + 0.10f * cos(ph * 0.8f)), h * (0.85f - 0.07f * sin(ph)), w * 0.9f, Coral, k * 0.75f)
        blob(w * (0.80f + 0.06f * sin(ph * 1.3f)), h * 0.98f, w * 0.6f, Amber, k * 0.6f)
    }
}

// ---------- icon rendering in compose ----------
object IconCache {
    private val lru = LruCache<String, ImageBitmap>(320)
    fun get(k: String): ImageBitmap? = lru.get(k)
    fun put(k: String, v: ImageBitmap) { lru.put(k, v) }
}

@Composable
fun StyledIcon(src: IconSource, style: IconStyle, tw: Tweak, size: Dp, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val px = with(density) { size.roundToPx() }.coerceIn(48, 256)
    val key = "${src.key}|${style.name}|${tw.accent}|${tw.glyphScale}|${tw.oneUiGlyphs}|$px"
    val img by produceState<ImageBitmap?>(initialValue = IconCache.get(key), key) {
        if (value == null) {
            val b = withContext(Dispatchers.Default) { IconRenderer.render(src, style, tw, px).asImageBitmap() }
            IconCache.put(key, b)
            value = b
        }
    }
    val a by animateFloatAsState(if (img != null) 1f else 0f, tween(260), label = "ia")
    Box(modifier.size(size)) {
        img?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = a
                    scaleX = 0.88f + 0.12f * a
                    scaleY = 0.88f + 0.12f * a
                }
            )
        }
    }
}

// ---------- small components ----------
@Composable
fun SectionTitle(title: String, sub: String? = null) {
    val p = LocalPal.current
    androidx.compose.foundation.layout.Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 12.dp)) {
        Text(title, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, color = p.sub, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, c1: Color = Teal, c2: Color = Blue, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(c1, c2)))
            .tap(0.94f, onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPal.current
    Box(
        modifier
            .glass(RoundedCornerShape(50))
            .tap(0.94f, onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun StyleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPal.current
    val a by animateFloatAsState(if (selected) 1f else 0f, tween(240), label = "chip")
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(Teal.copy(alpha = a), Blue.copy(alpha = a))))
            .border(1.dp, p.sub.copy(alpha = 0.25f * (1f - a)), RoundedCornerShape(50))
            .tap(0.94f, onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(label, color = if (selected) Color.White else p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun GlassSlider(value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val p = LocalPal.current
    var w by remember { mutableFloatStateOf(1f) }
    val cb by rememberUpdatedState(onChange)
    val frac = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    fun at(x: Float) = range.start + (x / w).coerceIn(0f, 1f) * (range.endInclusive - range.start)
    Box(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .onSizeChanged { w = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) { detectTapGestures { cb(at(it.x)) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> change.consume(); cb(at(change.position.x)) } },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(p.sub.copy(alpha = 0.22f)))
        Box(
            Modifier
                .fillMaxWidth(frac)
                .height(6.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Teal, Blue)))
        )
        val thumbX = with(LocalDensity.current) { (w * frac).toDp() }
        Box(
            Modifier
                .offset(x = if (thumbX - 13.dp < 0.dp) 0.dp else thumbX - 13.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, Teal, CircleShape)
        )
    }
}

// ---------- bottom nav ----------
@Composable
private fun NavGlyph(index: Int, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        when (index) {
            0 -> {
                val path = Path().apply {
                    moveTo(0.5f * w, 0.10f * h)
                    lineTo(0.94f * w, 0.48f * h)
                    lineTo(0.80f * w, 0.48f * h)
                    lineTo(0.80f * w, 0.90f * h)
                    lineTo(0.20f * w, 0.90f * h)
                    lineTo(0.20f * w, 0.48f * h)
                    lineTo(0.06f * w, 0.48f * h)
                    close()
                }
                drawPath(path, color)
            }
            1 -> {
                val path = Path().apply {
                    moveTo(0.5f * w, 0.04f * h)
                    quadraticBezierTo(0.56f * w, 0.44f * h, 0.96f * w, 0.5f * h)
                    quadraticBezierTo(0.56f * w, 0.56f * h, 0.5f * w, 0.96f * h)
                    quadraticBezierTo(0.44f * w, 0.56f * h, 0.04f * w, 0.5f * h)
                    quadraticBezierTo(0.44f * w, 0.44f * h, 0.5f * w, 0.04f * h)
                    close()
                }
                drawPath(path, color)
            }
            2 -> {
                val s = Size(0.38f * w, 0.38f * h)
                val r = CornerRadius(0.11f * w)
                drawRoundRect(color, Offset(0.08f * w, 0.08f * h), s, r)
                drawRoundRect(color, Offset(0.54f * w, 0.08f * h), s, r)
                drawRoundRect(color, Offset(0.08f * w, 0.54f * h), s, r)
                drawRoundRect(color, Offset(0.54f * w, 0.54f * h), s, r)
            }
            else -> {
                drawCircle(color, radius = 0.20f * w, center = Offset(0.5f * w, 0.30f * h))
                drawRoundRect(color, Offset(0.16f * w, 0.58f * h), Size(0.68f * w, 0.34f * h), CornerRadius(0.17f * w))
            }
        }
    }
}

@Composable
fun NavBar(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    val labels = listOf("Discover", "Styles", "Apps", "Mine")
    Row(
        modifier
            .padding(horizontal = 20.dp)
            .glass(RoundedCornerShape(32.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { i, label ->
            val sel = i == tab
            val a by animateFloatAsState(if (sel) 1f else 0f, tween(280), label = "nav")
            Row(
                Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Teal.copy(alpha = a), Blue.copy(alpha = a))))
                    .tap(0.9f) { onTab(i) }
                    .padding(horizontal = 15.dp, vertical = 11.dp)
                    .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 380f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavGlyph(i, if (sel) Color.White else p.sub)
                AnimatedVisibility(
                    visible = sel,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Text(
                        label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, modifier = Modifier.padding(start = 7.dp)
                    )
                }
            }
        }
    }
}
