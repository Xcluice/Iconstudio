package com.xcluice.iconstudio

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

enum class IconStyle(val title: String, val blurb: String) {
    ONE_UI("One UI", "Soft squircles with the signature glyph set"),
    CIRCLE("Circle", "Clean round icons, Pixel-style"),
    TEARDROP("Teardrop", "Playful drop with one sharp corner"),
    SOFT_SQUARE("Soft Square", "Rounded square, crisp and classic"),
    FROST_GLASS("Frost Glass", "Translucent glass tiles with a glossy edge"),
    GRADIENT_POP("Gradient Pop", "Bold gradient tiles, white glyphs"),
    MONO_TINT("Mono Tint", "Tonal pastel tiles, themed-icon look"),
    NEON_RING("Neon Ring", "Dark tiles with a glowing ring")
}

enum class GlyphKind(val title: String, val c1: Int, val c2: Int, val fg: Int = 0xFFFFFFFF.toInt()) {
    PHONE("Phone", 0xFF3DDC84.toInt(), 0xFF15A85A.toInt()),
    MESSAGES("Messages", 0xFF4C9BFF.toInt(), 0xFF2457E6.toInt()),
    CHAT("Chat", 0xFF34D27B.toInt(), 0xFF12A150.toInt()),
    CAMERA("Camera", 0xFF4A5362.toInt(), 0xFF1E242E.toInt()),
    GALLERY("Gallery", 0xFFFFC857.toInt(), 0xFFFF8F3A.toInt()),
    SETTINGS("Settings", 0xFF8E9BAE.toInt(), 0xFF5C6B80.toInt()),
    CLOCK("Clock", 0xFF2B3A55.toInt(), 0xFF111A2C.toInt()),
    CALCULATOR("Calculator", 0xFFFFA53D.toInt(), 0xFFEE7410.toInt()),
    CONTACTS("Contacts", 0xFF3FC1C9.toInt(), 0xFF1C8C99.toInt()),
    CALENDAR("Calendar", 0xFFFFFFFF.toInt(), 0xFFE6ECF4.toInt(), 0xFFFF5A4D.toInt()),
    FILES("Files", 0xFF4CC9A7.toInt(), 0xFF1E9E85.toInt()),
    BROWSER("Browser", 0xFF52C7FF.toInt(), 0xFF2D8CFF.toInt()),
    MUSIC("Music", 0xFFFF7B54.toInt(), 0xFFEB4B3C.toInt()),
    STORE("Store", 0xFF1FD1A5.toInt(), 0xFF0A9B7D.toInt()),
    MAIL("Mail", 0xFF4FC3F7.toInt(), 0xFF1E88E5.toInt()),
    MAPS("Maps", 0xFF6BD16B.toInt(), 0xFF2E9E4F.toInt()),
    VIDEO("Video", 0xFFFF5A5F.toInt(), 0xFFD92D3A.toInt()),
    NOTES("Notes", 0xFFFFD54A.toInt(), 0xFFF2B000.toInt()),
    WEATHER("Weather", 0xFF4FB5FF.toInt(), 0xFF2A7DE1.toInt())
}

val SampleKinds = GlyphKind.entries

data class Tweak(
    val accent: Int? = null,
    val glyphScale: Float = 0.72f,
    val oneUiGlyphs: Boolean = true
)

private fun has(s: String, vararg words: String): Boolean = words.any { s.contains(it) }

fun detectGlyph(pkg: String, label: String): GlyphKind? {
    val p = pkg.lowercase()
    val l = label.lowercase()
    val both = "$p $l"
    return when {
        has(p, "dialer", "incallui") || l == "phone" || l == "call" -> GlyphKind.PHONE
        has(p, "whatsapp", "telegram", "signal", "messenger") || has(l, "whatsapp", "telegram", "signal", "messenger") -> GlyphKind.CHAT
        has(p, ".mms", "messaging", "messages") || l == "messages" || l == "message" || l == "sms" -> GlyphKind.MESSAGES
        has(both, "camera") -> GlyphKind.CAMERA
        has(p, "gallery", "photos") || l == "gallery" || l == "photos" -> GlyphKind.GALLERY
        p.endsWith(".settings") || l == "settings" -> GlyphKind.SETTINGS
        has(both, "clock") -> GlyphKind.CLOCK
        has(both, "calculator") -> GlyphKind.CALCULATOR
        has(both, "contacts") -> GlyphKind.CONTACTS
        has(both, "calendar") -> GlyphKind.CALENDAR
        has(p, "filemanager", "documentsui", ".files") || l == "files" || l == "file manager" -> GlyphKind.FILES
        has(p, "chrome", "browser", "firefox", "opera") || l == "browser" -> GlyphKind.BROWSER
        has(both, "music") -> GlyphKind.MUSIC
        p == "com.android.vending" || l == "play store" -> GlyphKind.STORE
        has(both, "gmail") || l == "mail" || l == "email" -> GlyphKind.MAIL
        has(p, "maps") || l == "maps" -> GlyphKind.MAPS
        has(p, "youtube", "video") || l == "videos" -> GlyphKind.VIDEO
        has(p, "notes", "notepad") || l == "notes" -> GlyphKind.NOTES
        has(both, "weather") -> GlyphKind.WEATHER
        else -> null
    }
}

val AccentPalette = intArrayOf(
    0xFF12B5A5.toInt(), 0xFF2F80ED.toInt(), 0xFFFF7A59.toInt(), 0xFFFFB02E.toInt(),
    0xFF2EC27E.toInt(), 0xFF22B8E8.toInt(), 0xFFEF4B5C.toInt()
)

abstract class IconSource {
    abstract val key: String
    abstract val label: String
    abstract val glyph: GlyphKind?
    abstract fun drawComposite(c: Canvas, s: Float)
    abstract fun foreground(s: Int, k: Float): Bitmap
    abstract fun accent(): Int
}

class GlyphSource(val kind: GlyphKind) : IconSource() {
    override val key: String = "g:${kind.name}"
    override val label: String = kind.title
    override val glyph: GlyphKind? = kind
    override fun drawComposite(c: Canvas, s: Float) = IconRenderer.glyphTile(c, kind, s, 0.72f, null)
    override fun foreground(s: Int, k: Float): Bitmap = glyphBitmap(kind, s, k)
    override fun accent(): Int = kind.c1
}

object Samples {
    private val map = GlyphKind.entries.associateWith { GlyphSource(it) }
    fun of(k: GlyphKind): GlyphSource = map.getValue(k)
}

class AppEntry(
    val pkg: String,
    override val label: String,
    val component: ComponentName,
    private val icon: Drawable
) : IconSource() {
    val id: String = component.flattenToShortString()
    override val key: String get() = id
    override val glyph: GlyphKind? by lazy { detectGlyph(pkg, label) }
    private var accentCache: Int? = null

    private fun fresh(): Drawable = icon.constantState?.newDrawable()?.mutate() ?: icon

    override fun drawComposite(c: Canvas, s: Float) {
        val d = fresh()
        if (d is AdaptiveIconDrawable) {
            val side = s * 1.5f
            val o = (side - s) / 2f
            val l = (-o).toInt()
            val r = (s + o).toInt()
            d.background?.let { it.setBounds(l, l, r, r); it.draw(c) }
            d.foreground?.let { it.setBounds(l, l, r, r); it.draw(c) }
        } else {
            d.setBounds(0, 0, s.toInt(), s.toInt())
            d.draw(c)
        }
    }

    override fun foreground(s: Int, k: Float): Bitmap {
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val d = fresh()
        val sf = s.toFloat()
        if (d is AdaptiveIconDrawable) {
            val side = sf * 1.5f * k
            val a = ((sf - side) / 2f).toInt()
            val b = ((sf + side) / 2f).toInt()
            d.foreground?.let { it.setBounds(a, a, b, b); it.draw(c) }
        } else {
            val side = sf * k * 0.92f
            val a = ((sf - side) / 2f).toInt()
            val b = ((sf + side) / 2f).toInt()
            d.setBounds(a, a, b, b)
            d.draw(c)
        }
        return bmp
    }

    override fun accent(): Int {
        accentCache?.let { return it }
        val b = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        drawComposite(Canvas(b), 32f)
        val col = dominantColor(b) ?: AccentPalette[Math.abs(pkg.hashCode()) % AccentPalette.size]
        accentCache = col
        return col
    }
}

fun dominantColor(b: Bitmap): Int? {
    val px = IntArray(b.width * b.height)
    b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
    var r = 0.0
    var g = 0.0
    var bl = 0.0
    var wsum = 0.0
    val hsv = FloatArray(3)
    for (p in px) {
        if (Color.alpha(p) < 100) continue
        Color.colorToHSV(p, hsv)
        val w = (hsv[1] * hsv[2]).toDouble() + 0.03
        r += Color.red(p) * w
        g += Color.green(p) * w
        bl += Color.blue(p) * w
        wsum += w
    }
    if (wsum <= 0.0) return null
    val col = Color.rgb((r / wsum).toInt(), (g / wsum).toInt(), (bl / wsum).toInt())
    Color.colorToHSV(col, hsv)
    if (hsv[1] < 0.2f) return null
    hsv[1] = hsv[1].coerceAtLeast(0.55f)
    hsv[2] = hsv[2].coerceIn(0.65f, 0.95f)
    return Color.HSVToColor(hsv)
}
