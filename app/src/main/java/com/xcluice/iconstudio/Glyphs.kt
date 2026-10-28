package com.xcluice.iconstudio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.PathParser

private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    style = Paint.Style.FILL
}

private fun strokePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    style = Paint.Style.STROKE
    strokeWidth = width
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
}

private fun clearFill() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
}

private fun clearStroke(width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = width
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
}

/** Glyph on a transparent square bitmap. Glyphs are designed on a 100x100 grid at scale k = 0.72. */
fun glyphBitmap(kind: GlyphKind, size: Int, k: Float): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val sc = size / 100f * (k / 0.72f)
    c.translate(size / 2f, size / 2f)
    c.scale(sc, sc)
    c.translate(-50f, -50f)
    drawGlyph(c, kind)
    return bmp
}

private fun rr(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, p: Paint) =
    c.drawRoundRect(RectF(l, t, r, b), rad, rad, p)

private fun drawGlyph(c: Canvas, kind: GlyphKind) {
    val w = fillPaint(kind.fg)
    val x = clearFill()
    when (kind) {
        GlyphKind.PHONE -> {
            val path = PathParser.createPathFromPathData(
                "M6.62,10.79c1.44,2.83 3.76,5.14 6.59,6.59l2.2,-2.2c0.27,-0.27 0.67,-0.36 1.02,-0.24 1.12,0.37 2.33,0.57 3.57,0.57 0.55,0 1,0.45 1,1V20c0,0.55 -0.45,1 -1,1 -9.39,0 -17,-7.61 -17,-17 0,-0.55 0.45,-1 1,-1h3.5c0.55,0 1,0.45 1,1 0,1.25 0.2,2.45 0.57,3.57 0.11,0.35 0.03,0.74 -0.25,1.02l-2.2,2.2z"
            )
            val m = Matrix()
            m.setScale(3.2f, 3.2f)
            m.postTranslate(11.6f, 11.6f)
            path.transform(m)
            c.drawPath(path, w)
        }
        GlyphKind.MESSAGES -> {
            rr(c, 20f, 22f, 80f, 64f, 13f, w)
            val tail = Path().apply {
                moveTo(32f, 58f); lineTo(32f, 80f); lineTo(52f, 62f); close()
            }
            c.drawPath(tail, w)
            c.drawCircle(37f, 43f, 4.5f, x)
            c.drawCircle(50f, 43f, 4.5f, x)
            c.drawCircle(63f, 43f, 4.5f, x)
        }
        GlyphKind.CHAT -> {
            c.drawCircle(50f, 48f, 31f, w)
            val tail = Path().apply {
                moveTo(22f, 82f); lineTo(30f, 60f); lineTo(44f, 74f); close()
            }
            c.drawPath(tail, w)
            rr(c, 36f, 41f, 64f, 46f, 2.5f, x)
            rr(c, 36f, 52f, 56f, 57f, 2.5f, x)
        }
        GlyphKind.CAMERA -> {
            rr(c, 36f, 24f, 64f, 40f, 5f, w)
            rr(c, 18f, 32f, 82f, 76f, 11f, w)
            c.drawCircle(50f, 54f, 17f, x)
            c.drawCircle(50f, 54f, 11.5f, w)
            c.drawCircle(50f, 54f, 5f, x)
            c.drawCircle(72f, 42f, 3f, x)
        }
        GlyphKind.GALLERY -> {
            rr(c, 18f, 22f, 82f, 78f, 11f, w)
            c.drawCircle(37f, 40f, 6.5f, x)
            val mt = Path().apply {
                moveTo(24f, 72f); lineTo(44f, 50f); lineTo(56f, 62f); lineTo(66f, 52f); lineTo(78f, 72f); close()
            }
            c.drawPath(mt, x)
        }
        GlyphKind.SETTINGS -> {
            c.drawCircle(50f, 50f, 23f, w)
            for (i in 0 until 8) {
                c.save()
                c.rotate(i * 45f, 50f, 50f)
                rr(c, 43.5f, 17f, 56.5f, 33f, 3.5f, w)
                c.restore()
            }
            c.drawCircle(50f, 50f, 9.5f, x)
        }
        GlyphKind.CLOCK -> {
            c.drawCircle(50f, 50f, 33f, w)
            c.drawCircle(50f, 50f, 27f, x)
            val hands = strokePaint(kind.fg, 5.5f)
            c.drawLine(50f, 50f, 50f, 31f, hands)
            c.drawLine(50f, 50f, 63f, 57f, hands)
            c.drawCircle(50f, 50f, 3.5f, w)
        }
        GlyphKind.CALCULATOR -> {
            rr(c, 24f, 16f, 76f, 84f, 11f, w)
            rr(c, 31f, 23f, 69f, 39f, 3.5f, x)
            val xs = floatArrayOf(31f, 45f, 59f)
            val ys = floatArrayOf(47f, 59f, 71f)
            for (yy in ys) for (xx in xs) rr(c, xx, yy, xx + 10f, yy + 8f, 2.5f, x)
        }
        GlyphKind.CONTACTS -> {
            c.drawCircle(50f, 36f, 14f, w)
            val body = Path().apply {
                addRoundRect(RectF(25f, 56f, 75f, 84f), floatArrayOf(25f, 25f, 25f, 25f, 7f, 7f, 7f, 7f), Path.Direction.CW)
            }
            c.drawPath(body, w)
        }
        GlyphKind.CALENDAR -> {
            rr(c, 18f, 22f, 82f, 82f, 12f, w)
            rr(c, 24f, 38f, 76f, 76f, 7f, x)
            rr(c, 32f, 15f, 39f, 29f, 3.5f, w)
            rr(c, 61f, 15f, 68f, 29f, 3.5f, w)
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = kind.fg
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            c.drawText("28", 50f, 67f, tp)
        }
        GlyphKind.FILES -> {
            rr(c, 18f, 26f, 47f, 46f, 7f, w)
            rr(c, 18f, 36f, 82f, 78f, 10f, w)
            rr(c, 26f, 52f, 74f, 56f, 2f, x)
        }
        GlyphKind.BROWSER -> {
            c.drawCircle(50f, 50f, 33f, w)
            c.drawCircle(50f, 50f, 28.5f, x)
            val line = strokePaint(kind.fg, 4.2f)
            c.drawOval(RectF(38f, 22f, 62f, 78f), line)
            c.drawLine(22f, 50f, 78f, 50f, line)
            c.drawLine(27f, 34f, 73f, 34f, line)
            c.drawLine(27f, 66f, 73f, 66f, line)
        }
        GlyphKind.MUSIC -> {
            c.drawCircle(36f, 71f, 9.5f, w)
            c.drawCircle(66f, 65f, 9.5f, w)
            rr(c, 41f, 28f, 47f, 71f, 2f, w)
            rr(c, 71f, 22f, 77f, 65f, 2f, w)
            val beam = Path().apply {
                moveTo(41f, 28f); lineTo(77f, 20f); lineTo(77f, 33f); lineTo(41f, 41f); close()
            }
            c.drawPath(beam, w)
        }
        GlyphKind.STORE -> {
            c.drawArc(RectF(37f, 18f, 63f, 52f), 180f, 180f, false, strokePaint(kind.fg, 5f))
            rr(c, 22f, 34f, 78f, 82f, 10f, w)
            val tri = Path().apply {
                moveTo(42f, 47f); lineTo(42f, 71f); lineTo(63f, 59f); close()
            }
            c.drawPath(tri, x)
        }
        GlyphKind.MAIL -> {
            rr(c, 17f, 27f, 83f, 73f, 9f, w)
            val flap = Path().apply {
                moveTo(23f, 35f); lineTo(50f, 56f); lineTo(77f, 35f)
            }
            c.drawPath(flap, clearStroke(4.5f))
        }
        GlyphKind.MAPS -> {
            c.drawCircle(50f, 40f, 23f, w)
            val tip = Path().apply {
                moveTo(29f, 51f); lineTo(71f, 51f); lineTo(50f, 85f); close()
            }
            c.drawPath(tip, w)
            c.drawCircle(50f, 40f, 9f, x)
        }
        GlyphKind.VIDEO -> {
            rr(c, 17f, 27f, 83f, 73f, 11f, w)
            val tri = Path().apply {
                moveTo(43f, 39f); lineTo(43f, 61f); lineTo(63f, 50f); close()
            }
            c.drawPath(tri, x)
        }
        GlyphKind.NOTES -> {
            rr(c, 26f, 16f, 74f, 84f, 9f, w)
            rr(c, 34f, 33f, 66f, 38f, 2.5f, x)
            rr(c, 34f, 45f, 66f, 50f, 2.5f, x)
            rr(c, 34f, 57f, 54f, 62f, 2.5f, x)
        }
        GlyphKind.WEATHER -> {
            val sun = strokePaint(kind.fg, 4f)
            c.drawCircle(37f, 37f, 11f, w)
            for (i in 0 until 8) {
                c.save()
                c.rotate(i * 45f, 37f, 37f)
                c.drawLine(37f, 17f, 37f, 21.5f, sun)
                c.restore()
            }
            c.drawCircle(46f, 65f, 14f, x)
            c.drawCircle(46f, 65f, 11f, w)
            c.drawCircle(62f, 61f, 16f, x)
            c.drawCircle(62f, 61f, 13f, w)
            rr(c, 30f, 62f, 80f, 78f, 8f, w)
        }
    }
}
