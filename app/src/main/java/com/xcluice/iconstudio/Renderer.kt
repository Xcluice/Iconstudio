package com.xcluice.iconstudio

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

object IconRenderer {

    // ---------- colour helpers ----------
    private fun mix(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
    )

    private fun withAlpha(c: Int, a: Int): Int = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    private fun hsv(c: Int): FloatArray {
        val h = FloatArray(3)
        Color.colorToHSV(c, h)
        return h
    }

    private fun fromHsv(h: Float, s: Float, v: Float): Int =
        Color.HSVToColor(floatArrayOf(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))

    // ---------- shapes ----------
    fun squircle(s: Float, n: Double = 4.4): Path {
        val p = Path()
        val r = s / 2f
        val steps = 120
        for (i in 0..steps) {
            val t = i * 2.0 * Math.PI / steps
            val ct = cos(t)
            val st = sin(t)
            val x = r + r * sign(ct) * abs(ct).pow(2.0 / n)
            val y = r + r * sign(st) * abs(st).pow(2.0 / n)
            if (i == 0) p.moveTo(x.toFloat(), y.toFloat()) else p.lineTo(x.toFloat(), y.toFloat())
        }
        p.close()
        return p
    }

    private fun circle(s: Float) = Path().apply { addCircle(s / 2f, s / 2f, s / 2f, Path.Direction.CW) }

    private fun teardrop(s: Float): Path {
        val r = s * 0.5f
        val sm = s * 0.12f
        return Path().apply {
            addRoundRect(RectF(0f, 0f, s, s), floatArrayOf(r, r, r, r, sm, sm, r, r), Path.Direction.CW)
        }
    }

    private fun softSquare(s: Float): Path {
        val r = s * 0.24f
        return Path().apply { addRoundRect(RectF(0f, 0f, s, s), r, r, Path.Direction.CW) }
    }

    private fun masked(size: Int, shape: Path, draw: (Canvas) -> Unit): Bitmap {
        val layer = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val lc = Canvas(layer)
        draw(lc)
        val m = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.BLACK
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        lc.drawPath(shape, m)
        return layer
    }

    private fun silhouette(src: Bitmap, color: Int): Bitmap {
        val o = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN) }
        Canvas(o).drawBitmap(src, 0f, 0f, p)
        return o
    }

    private fun blurred(src: Bitmap, color: Int, radius: Float): Bitmap {
        val sil = silhouette(src, color)
        val o = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(radius.coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        Canvas(o).drawBitmap(sil, 0f, 0f, p)
        return o
    }

    // ---------- One UI glyph tile ----------
    fun glyphTile(c: Canvas, kind: GlyphKind, s: Float, k: Float, accent: Int?) {
        val top = accent?.let { mix(it, Color.WHITE, 0.22f) } ?: kind.c1
        val bot = accent?.let { mix(it, Color.BLACK, 0.18f) } ?: kind.c2
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, top, bot, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, s, s, p)
        val gb = glyphBitmap(kind, s.toInt(), k)
        val g = if (accent != null) silhouette(gb, Color.WHITE) else gb
        c.drawBitmap(g, 0f, 0f, null)
    }

    // ---------- main entry ----------
    fun render(src: IconSource, style: IconStyle, tw: Tweak, size: Int): Bitmap {
        val s = size.toFloat()
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val accent = tw.accent ?: src.accent()
        when (style) {
            IconStyle.ONE_UI -> {
                val kind = src.glyph
                c.drawBitmap(masked(size, squircle(s)) { lc ->
                    if (tw.oneUiGlyphs && kind != null) glyphTile(lc, kind, s, tw.glyphScale, tw.accent)
                    else src.drawComposite(lc, s)
                }, 0f, 0f, null)
            }
            IconStyle.CIRCLE -> c.drawBitmap(masked(size, circle(s)) { src.drawComposite(it, s) }, 0f, 0f, null)
            IconStyle.TEARDROP -> c.drawBitmap(masked(size, teardrop(s)) { src.drawComposite(it, s) }, 0f, 0f, null)
            IconStyle.SOFT_SQUARE -> c.drawBitmap(masked(size, softSquare(s)) { src.drawComposite(it, s) }, 0f, 0f, null)
            IconStyle.FROST_GLASS -> frost(c, src, tw, accent, s, size)
            IconStyle.GRADIENT_POP -> pop(c, src, tw, accent, s, size)
            IconStyle.MONO_TINT -> mono(c, src, tw, accent, s, size)
            IconStyle.NEON_RING -> neon(c, src, tw, accent, s, size)
        }
        return out
    }

    private fun frost(c: Canvas, src: IconSource, tw: Tweak, accent: Int, s: Float, size: Int) {
        val inset = s * 0.04f
        val rr = s * 0.30f
        val rect = RectF(inset, inset, s - inset, s - inset)
        val path = Path().apply { addRoundRect(rect, rr, rr, Path.Direction.CW) }
        val layer = masked(size, path) { lc ->
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, s, s,
                    intArrayOf(withAlpha(mix(accent, Color.WHITE, 0.35f), 205), withAlpha(accent, 105)),
                    null, Shader.TileMode.CLAMP
                )
            }
            lc.drawRect(0f, 0f, s, s, bg)
            val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, s * 0.55f,
                    Color.argb(120, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP
                )
            }
            lc.drawOval(RectF(-s * 0.25f, -s * 0.6f, s * 1.25f, s * 0.5f), gloss)
            val fg = src.foreground(size, tw.glyphScale)
            lc.drawBitmap(blurred(fg, Color.argb(95, 0, 0, 0), s * 0.025f), 0f, s * 0.022f, null)
            lc.drawBitmap(fg, 0f, 0f, null)
        }
        c.drawBitmap(layer, 0f, 0f, null)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.016f
            shader = LinearGradient(
                0f, 0f, s, s,
                intArrayOf(Color.argb(235, 255, 255, 255), Color.argb(45, 255, 255, 255), Color.argb(140, 255, 255, 255)),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
            )
        }
        val br = RectF(rect).apply { inset(s * 0.008f, s * 0.008f) }
        c.drawRoundRect(br, rr, rr, border)
    }

    private fun pop(c: Canvas, src: IconSource, tw: Tweak, accent: Int, s: Float, size: Int) {
        val h = hsv(accent)
        val a = fromHsv(h[0] - 18f, h[1].coerceAtLeast(0.55f), (h[2] * 1.1f).coerceAtLeast(0.8f))
        val b = fromHsv(h[0] + 22f, h[1].coerceAtLeast(0.6f), (h[2] * 0.78f).coerceAtLeast(0.55f))
        c.drawBitmap(masked(size, squircle(s)) { lc ->
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, s, s, a, b, Shader.TileMode.CLAMP)
            }
            lc.drawRect(0f, 0f, s, s, bg)
            val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, s * 0.6f, Color.argb(70, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP)
            }
            lc.drawRect(0f, 0f, s, s * 0.6f, gloss)
            val fg = src.foreground(size, tw.glyphScale)
            lc.drawBitmap(blurred(fg, Color.argb(70, 0, 0, 0), s * 0.02f), 0f, s * 0.02f, null)
            lc.drawBitmap(silhouette(fg, Color.WHITE), 0f, 0f, null)
        }, 0f, 0f, null)
    }

    private fun mono(c: Canvas, src: IconSource, tw: Tweak, accent: Int, s: Float, size: Int) {
        val h = hsv(accent)
        val top = fromHsv(h[0], 0.20f, 0.98f)
        val bot = fromHsv(h[0], 0.34f, 0.88f)
        val ink = fromHsv(h[0], 0.80f, 0.34f)
        c.drawBitmap(masked(size, squircle(s)) { lc ->
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, s, s, top, bot, Shader.TileMode.CLAMP)
            }
            lc.drawRect(0f, 0f, s, s, bg)
            lc.drawBitmap(silhouette(src.foreground(size, tw.glyphScale), ink), 0f, 0f, null)
        }, 0f, 0f, null)
    }

    private fun neon(c: Canvas, src: IconSource, tw: Tweak, accent: Int, s: Float, size: Int) {
        val h = hsv(accent)
        val second = fromHsv(h[0] + 40f, h[1].coerceAtLeast(0.6f), 1f)
        val bright = mix(accent, Color.WHITE, 0.25f)
        c.drawBitmap(masked(size, squircle(s)) { lc ->
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, s, 0xFF121A2B.toInt(), 0xFF070B14.toInt(), Shader.TileMode.CLAMP)
            }
            lc.drawRect(0f, 0f, s, s, bg)
            val fg = src.foreground(size, tw.glyphScale * 0.92f)
            lc.drawBitmap(blurred(fg, withAlpha(accent, 210), s * 0.045f), 0f, 0f, null)
            lc.drawBitmap(silhouette(fg, bright), 0f, 0f, null)
            val ring = squircle(s * 0.86f).apply { offset(s * 0.07f, s * 0.07f) }
            val sweep = SweepGradient(s / 2f, s / 2f, intArrayOf(accent, second, accent), null)
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = s * 0.05f
                shader = sweep
                alpha = 170
                maskFilter = BlurMaskFilter(s * 0.03f, BlurMaskFilter.Blur.NORMAL)
            }
            lc.drawPath(ring, glow)
            val crisp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = s * 0.028f
                shader = sweep
            }
            lc.drawPath(ring, crisp)
        }, 0f, 0f, null)
    }
}
