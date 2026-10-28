@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.xcluice.iconstudio

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.round

class StudioState {
    var tab by mutableIntStateOf(0)
    var style by mutableStateOf(IconStyle.ONE_UI)
    var accent by mutableStateOf<Int?>(null)
    var scale by mutableFloatStateOf(0.72f)
    var glyphs by mutableStateOf(true)
    var apps by mutableStateOf<List<AppEntry>>(emptyList())
    var loading by mutableStateOf(true)
    val selected = mutableStateListOf<String>()
    val tweak: Tweak get() = Tweak(accent, round(scale * 50f) / 50f, glyphs)
}

// ---------------------------------------------------------------- root
@Composable
fun StudioRoot(st: StudioState) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        History.load(ctx)
        st.apps = withContext(Dispatchers.IO) { loadApps(ctx) }
        st.loading = false
    }
    BackHandler(enabled = st.tab != 0) { st.tab = 0 }
    Box(Modifier.fillMaxSize()) {
        Aurora()
        AnimatedContent(
            targetState = st.tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (fadeIn(tween(280)) + slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 8 * dir }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { -it / 10 * dir })
            },
            label = "tabs"
        ) { tab ->
            when (tab) {
                0 -> DiscoverScreen(st)
                1 -> StylesScreen(st)
                2 -> AppsScreen(st)
                else -> MineScreen(st)
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
        ) {
            ApplyBar(st)
            Spacer(Modifier.height(8.dp))
            NavBar(st.tab, { st.tab = it }, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

// ---------------------------------------------------------------- discover
private class Banner(
    val title: String,
    val sub: String,
    val c1: Color,
    val c2: Color,
    val style: IconStyle,
    val kinds: List<GlyphKind>
)

private val Banners = listOf(
    Banner("One UI icon set", "Signature glyphs on soft squircles", Teal, Blue, IconStyle.ONE_UI,
        listOf(GlyphKind.CAMERA, GlyphKind.GALLERY, GlyphKind.MUSIC)),
    Banner("Frost Glass", "Translucent tiles that float over any wallpaper", Blue, Sky, IconStyle.FROST_GLASS,
        listOf(GlyphKind.MAIL, GlyphKind.WEATHER, GlyphKind.MAPS)),
    Banner("Gradient Pop", "Bold colour with zero compromise", Coral, Amber, IconStyle.GRADIENT_POP,
        listOf(GlyphKind.PHONE, GlyphKind.CALENDAR, GlyphKind.NOTES)),
    Banner("Neon Ring", "Dark tiles with glowing rings", Color(0xFF0B3A4A), Teal, IconStyle.NEON_RING,
        listOf(GlyphKind.CLOCK, GlyphKind.VIDEO, GlyphKind.SETTINGS))
)

@Composable
private fun BannerCard(b: Banner, onTry: () -> Unit, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "float")
    val dy by t.animateFloat(
        -5f, 5f,
        infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(b.c1, b.c2)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.10f), radius = size.height * 0.9f, center = Offset(size.width * 0.95f, size.height * 0.1f))
            drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.5f, center = Offset(size.width * 0.55f, size.height * 1.05f))
        }
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.58f)
                .padding(start = 22.dp)
        ) {
            Text(b.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(b.sub, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            GlassButton("Try it", onClick = onTry)
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(width = 124.dp, height = 132.dp)
        ) {
            StyledIcon(Samples.of(b.kinds[0]), b.style, Tweak(), 62.dp, Modifier.align(Alignment.TopStart).offset(y = dy.dp))
            StyledIcon(Samples.of(b.kinds[1]), b.style, Tweak(), 62.dp, Modifier.align(Alignment.CenterEnd).offset(y = (-dy).dp))
            StyledIcon(Samples.of(b.kinds[2]), b.style, Tweak(), 62.dp, Modifier.align(Alignment.BottomStart).offset(y = dy.dp))
        }
    }
}

@Composable
fun DiscoverScreen(st: StudioState) {
    val p = LocalPal.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 190.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 18.dp).staggerIn(0)) {
                Text(
                    "Icon Studio",
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(Teal, Blue)),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text("Themes for your home screen", color = p.sub, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        item {
            val pager = rememberPagerState(pageCount = { Banners.size })
            LaunchedEffect(pager) {
                while (true) {
                    delay(4800)
                    pager.animateScrollToPage((pager.currentPage + 1) % Banners.size)
                }
            }
            Column(Modifier.staggerIn(1)) {
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                    val sc = lerp(0.92f, 1f, 1f - off)
                    BannerCard(
                        Banners[page],
                        onTry = { st.style = Banners[page].style; st.tab = 1 },
                        modifier = Modifier.graphicsLayer { scaleX = sc; scaleY = sc; alpha = lerp(0.65f, 1f, 1f - off) }
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
                    Banners.indices.forEach { i ->
                        val w by animateDpAsState(if (i == pager.currentPage) 22.dp else 7.dp, spring(0.7f), label = "dot")
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .height(7.dp)
                                .width(w)
                                .clip(CircleShape)
                                .background(if (i == pager.currentPage) Teal else p.sub.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
        item { SectionTitle("One UI icon set", "${SampleKinds.size} signature glyphs, tap to use") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(SampleKinds) { i, k ->
                    Column(
                        Modifier.tap { st.style = IconStyle.ONE_UI; st.tab = 2 },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StyledIcon(Samples.of(k), IconStyle.ONE_UI, Tweak(), 62.dp)
                        Text(k.title, color = p.text, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                    }
                }
            }
        }
        item { SectionTitle("Icon styles", "Eight looks, all customisable") }
        items(IconStyle.entries.chunked(2)) { row ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { s ->
                    Column(
                        Modifier
                            .weight(1f)
                            .glass(RoundedCornerShape(26.dp))
                            .tap { st.style = s; st.tab = 1 }
                            .padding(14.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(GlyphKind.PHONE, GlyphKind.CAMERA, GlyphKind.MUSIC).forEach {
                                StyledIcon(Samples.of(it), s, Tweak(), 40.dp)
                            }
                        }
                        Text(s.title, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .glass(RoundedCornerShape(26.dp))
                    .padding(18.dp)
            ) {
                Text("How it works", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Pick a style, choose your apps and tap Apply. Your launcher adds each restyled icon to the home screen. Remove the old icons afterwards.",
                    color = p.sub, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- styles
private val Swatches: List<Int?> = listOf(
    null,
    0xFF12C2B0.toInt(), 0xFF2F80ED.toInt(), 0xFF22D3EE.toInt(), 0xFF2EC27E.toInt(),
    0xFFFFB02E.toInt(), 0xFFFF7A59.toInt(), 0xFFEF4B5C.toInt(), 0xFF64748B.toInt()
)

@Composable
private fun Swatch(color: Int?, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPal.current
    val sc by animateFloatAsState(if (selected) 1.12f else 1f, spring(0.55f), label = "sw")
    Box(
        Modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (color == null) Brush.sweepGradient(listOf(Teal, Blue, Sky, Teal, Amber, Coral, Teal))
                else SolidColor(Color(color))
            )
            .border(if (selected) 3.dp else 1.dp, if (selected) p.text else Color.White.copy(alpha = 0.5f), CircleShape)
            .tap(0.9f, onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color == null) Text("A", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun GlassSwitch(on: Boolean, onToggle: () -> Unit) {
    val x by animateDpAsState(if (on) 22.dp else 2.dp, spring(0.7f), label = "sx")
    val a by animateFloatAsState(if (on) 1f else 0f, tween(220), label = "sa")
    val p = LocalPal.current
    Box(
        Modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Teal.copy(alpha = a), Blue.copy(alpha = a))))
            .border(1.dp, p.sub.copy(alpha = 0.3f * (1f - a)), CircleShape)
            .tap(0.94f, onToggle)
    ) {
        Box(
            Modifier
                .offset(x = x, y = 3.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (on) Color.White else p.sub.copy(alpha = 0.55f))
        )
    }
}

@Composable
fun StylesScreen(st: StudioState) {
    val p = LocalPal.current
    val tw = st.tweak
    val previewKinds = listOf(GlyphKind.PHONE, GlyphKind.MESSAGES, GlyphKind.CAMERA, GlyphKind.GALLERY, GlyphKind.SETTINGS)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 190.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 14.dp)) {
                Text("Styles", color = p.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("Tune it live, then pick your apps", color = p.sub, fontSize = 14.sp)
            }
        }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .glass(RoundedCornerShape(30.dp))
                    .padding(16.dp)
                    .staggerIn(1)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF0EA5A0), Color(0xFF2F6FE0), Color(0xFFF59E0B))))
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        previewKinds.forEach { StyledIcon(Samples.of(it), st.style, tw, 52.dp) }
                    }
                }
                Text(st.style.title, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
                Text(st.style.blurb, color = p.sub, fontSize = 13.sp)

                Text("Accent", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp)) {
                    items(Swatches) { c -> Swatch(c, st.accent == c) { st.accent = c } }
                }

                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Glyph size", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${(st.scale * 100).toInt()}%", color = p.sub, fontSize = 13.sp)
                }
                GlassSlider(st.scale, 0.5f..0.95f) { st.scale = it }

                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("One UI glyph set", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Swap system apps for drawn glyphs", color = p.sub, fontSize = 12.sp)
                    }
                    GlassSwitch(st.glyphs) { st.glyphs = !st.glyphs }
                }
                GradientButton("Choose apps", Modifier.padding(top = 16.dp).fillMaxWidth()) { st.tab = 2 }
            }
        }
        item { SectionTitle("All styles") }
        itemsIndexed(IconStyle.entries) { i, s ->
            val sel = s == st.style
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
                    .staggerIn(i)
                    .glass(RoundedCornerShape(26.dp))
                    .then(
                        if (sel) Modifier.border(2.dp, Brush.linearGradient(listOf(Teal, Blue)), RoundedCornerShape(26.dp))
                        else Modifier
                    )
                    .tap { st.style = s }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(s.title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(s.blurb, color = p.sub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(GlyphKind.PHONE, GlyphKind.MAIL, GlyphKind.CAMERA, GlyphKind.MUSIC).forEach {
                            StyledIcon(Samples.of(it), s, tw, 42.dp)
                        }
                    }
                }
                AnimatedVisibility(visible = sel, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Teal, Blue))), contentAlignment = Alignment.Center) {
                        CheckMark(Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckMark(color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val st = Stroke(width = w * 0.17f, cap = StrokeCap.Round)
        drawLine(color, Offset(0.1f * w, 0.55f * h), Offset(0.4f * w, 0.85f * h), st.width, StrokeCap.Round)
        drawLine(color, Offset(0.4f * w, 0.85f * h), Offset(0.92f * w, 0.18f * h), st.width, StrokeCap.Round)
    }
}

// ---------------------------------------------------------------- apps
@Composable
private fun AppCell(app: AppEntry, st: StudioState) {
    val p = LocalPal.current
    val sel = app.id in st.selected
    val sc by animateFloatAsState(if (sel) 0.88f else 1f, spring(0.5f, 400f), label = "sel")
    Column(
        Modifier.tap(0.94f) { if (sel) st.selected.remove(app.id) else st.selected.add(app.id) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier
                    .graphicsLayer { scaleX = sc; scaleY = sc }
                    .padding(3.dp)
            ) {
                StyledIcon(app, st.style, st.tweak, 58.dp)
            }
            androidx.compose.animation.AnimatedVisibility(visible = sel, enter = scaleIn(spring(0.5f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Teal, Blue))).border(2.dp, p.bg, CircleShape),
                    contentAlignment = Alignment.Center
                ) { CheckMark(Color.White) }
            }
        }
        Text(
            app.label, color = p.text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
        )
    }
}

@Composable
fun AppsScreen(st: StudioState) {
    val p = LocalPal.current
    var q by remember { mutableStateOf("") }
    val filtered = remember(st.apps, q) {
        if (q.isBlank()) st.apps else st.apps.filter { it.label.contains(q, ignoreCase = true) }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 6.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Apps", color = p.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${filtered.size} apps  ·  ${st.selected.size} selected", color = p.sub, fontSize = 13.sp)
                    }
                    val allSel = filtered.isNotEmpty() && filtered.all { it.id in st.selected }
                    GlassButton(if (allSel) "Clear" else "Select all") {
                        if (allSel) filtered.forEach { st.selected.remove(it.id) }
                        else filtered.forEach { if (it.id !in st.selected) st.selected.add(it.id) }
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(IconStyle.entries) { s -> StyleChip(s.title, s == st.style) { st.style = s } }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .padding(bottom = 14.dp)
                        .glass(RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 13.dp)
                ) {
                    if (q.isEmpty()) Text("Search apps", color = p.sub, fontSize = 15.sp)
                    BasicTextField(
                        value = q,
                        onValueChange = { q = it },
                        singleLine = true,
                        textStyle = TextStyle(color = p.text, fontSize = 15.sp),
                        cursorBrush = SolidColor(Teal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (st.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Loading your apps…", color = p.sub, fontSize = 14.sp, modifier = Modifier.padding(20.dp))
            }
        }
        items(filtered, key = { it.id }) { app -> AppCell(app, st) }
    }
}

// ---------------------------------------------------------------- apply bar
@Composable
fun ApplyBar(st: StudioState) {
    val p = LocalPal.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = PinQueue.active || (st.tab == 2 && st.selected.isNotEmpty()),
        enter = slideInVertically(spring(0.8f)) { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .glass(RoundedCornerShape(30.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (PinQueue.active) {
                Column(Modifier.weight(1f)) {
                    Text("Pinning ${PinQueue.index + 1} of ${PinQueue.items.size}", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(PinQueue.items.getOrNull(PinQueue.index)?.label ?: "", color = p.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                GlassButton("Skip") { PinQueue.skip(ctx) }
                GradientButton("Pin") { PinQueue.pinCurrent(ctx) }
                GlassButton("✕") { PinQueue.stop() }
            } else {
                Column(Modifier.weight(1f)) {
                    Text("${st.selected.size} selected", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(st.style.title, color = p.sub, fontSize = 12.sp, maxLines = 1)
                }
                GlassButton("Export") {
                    val list = st.apps.filter { it.id in st.selected }
                    val tw = st.tweak
                    val style = st.style
                    scope.launch(Dispatchers.Default) {
                        var n = 0
                        list.forEach { a ->
                            val b = IconRenderer.render(a, style, tw, 512)
                            if (Pinner.export(ctx, b, "${a.label}_${style.name.lowercase()}")) n++
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                ctx,
                                if (n > 0) "Saved $n icons to Pictures/IconStudio" else "Export needs Android 10 or newer",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                GradientButton("Apply") {
                    val list = st.apps.filter { it.id in st.selected }
                    if (list.isNotEmpty()) {
                        PinQueue.start(ctx, list, st.style, st.tweak)
                        st.selected.clear()
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- mine
@Composable
fun MineScreen(st: StudioState) {
    val p = LocalPal.current
    val ctx = LocalContext.current
    val fmt = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 190.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 14.dp)) {
                Text("Mine", color = p.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("Your applied icons and tips", color = p.sub, fontSize = 14.sp)
            }
        }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .glass(RoundedCornerShape(28.dp))
                    .padding(18.dp)
                    .staggerIn(1)
            ) {
                Text("Making it stick", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                val steps = listOf(
                    "1  Pick a style and your apps, then tap Apply.",
                    "2  Your launcher asks to add each icon. Tap Add, then come back for the next one.",
                    "3  The original icons stay put. Long-press them on the home screen and remove them.",
                    "4  Nothing showing up? In Settings, Apps, Icon Studio, Permissions, allow shortcut creation."
                )
                steps.forEach { Text(it, color = p.sub, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .glass(RoundedCornerShape(28.dp))
                    .padding(18.dp)
                    .staggerIn(2)
            ) {
                Text("Want every icon replaced?", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Use Export to save high-resolution PNGs, then apply them through any launcher that supports custom icons.",
                    color = p.sub, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SectionTitle("Applied", "${History.items.size} icons so far") }
                if (History.items.isNotEmpty()) GlassButton("Clear") { History.clear(ctx) }
            }
        }
        if (History.items.isEmpty()) {
            item {
                Text(
                    "Nothing yet. Icons you pin will show up here.",
                    color = p.sub, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp)
                )
            }
        }
        itemsIndexed(History.items) { i, h ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp)
                    .glass(RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(h.label, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(h.style, color = p.sub, fontSize = 12.sp)
                }
                Text(fmt.format(Date(h.time)), color = p.sub, fontSize = 12.sp)
            }
        }
    }
}
