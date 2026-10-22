package com.ilsecondodasinistra.catchit.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import kotlin.math.max

/**
 * The markers are drawn as bitmaps and handed to the map as style images: the map renders text only with a font server,
 * and a bitmap gives full control of the look anyway. Sizes are in dp; the bitmaps carry the screen density so the map
 * shows them at that size.
 */
internal class MapMarkers(context: Context) {
    private val metrics = context.resources.displayMetrics
    private val d = metrics.density
    private fun dp(value: Float) = value * d

    private val fontScale = context.resources.configuration.fontScale.coerceAtMost(1.3f)
    private fun text(sizeSp: Float, color: Int, bold: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = sizeSp * d * fontScale
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeJoin = Paint.Join.ROUND
    }

    private fun bitmap(width: Float, height: Float, draw: Canvas.() -> Unit): Bitmap =
        Bitmap.createBitmap(metrics, max(1, width.toInt()), max(1, height.toInt()), Bitmap.Config.ARGB_8888).also {
            it.density = metrics.densityDpi
            Canvas(it).draw()
        }

    private fun onColor(color: Int) = if (ColorUtils.calculateLuminance(color) > 0.4) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

    /**
     * The line's badge with, above it, the time to the stop. The badge sits at the centre of the bitmap (the image is
     * transparent below it) so the vehicle's coordinate is the middle of the badge.
     */
    fun vehicle(line: String, color: Int, eta: String?, highlighted: Boolean): Bitmap {
        val badgeHeight = dp(if (highlighted) 34f else 26f)
        val badgePaint = text(if (highlighted) 16f else 13f, onColor(color))
        val badgeWidth = max(badgeHeight + dp(4f), badgePaint.measureText(line) + dp(18f))
        val bubblePaint = text(12f, 0xFF1B1B1F.toInt())
        val bubbleHeight = dp(22f)
        val bubbleWidth = eta?.let { bubblePaint.measureText(it) + dp(16f) } ?: 0f
        val tail = dp(5f)
        val gap = dp(3f)
        val pad = dp(4f)
        val width = max(badgeWidth, bubbleWidth) + 2 * pad
        val half = (if (eta != null) bubbleHeight + tail + gap else 0f) + badgeHeight / 2 + pad
        return bitmap(width, 2 * half) {
            val cx = width / 2
            val cy = half
            val badge = RectF(cx - badgeWidth / 2, cy - badgeHeight / 2, cx + badgeWidth / 2, cy + badgeHeight / 2)
            if (highlighted) drawRoundRect(RectF(badge).apply { inset(-dp(3f), -dp(3f)) }, badgeHeight, badgeHeight, fill(0xFF1B1B1F.toInt()))
            drawRoundRect(RectF(badge).apply { inset(-dp(1.5f), -dp(1.5f)) }, badgeHeight, badgeHeight, fill(0xFFFFFFFF.toInt()))
            drawRoundRect(badge, badgeHeight / 2, badgeHeight / 2, fill(color))
            drawText(line, cx - badgePaint.measureText(line) / 2, cy - (badgePaint.ascent() + badgePaint.descent()) / 2, badgePaint)
            if (eta != null) {
                val bottom = badge.top - gap - tail
                val bubble = RectF(cx - bubbleWidth / 2, bottom - bubbleHeight, cx + bubbleWidth / 2, bottom)
                val tailPath = Path().apply {
                    moveTo(cx - tail, bottom - dp(1f))
                    lineTo(cx, bottom + tail)
                    lineTo(cx + tail, bottom - dp(1f))
                    close()
                }
                drawRoundRect(bubble, dp(8f), dp(8f), fill(0xFFFFFFFF.toInt()))
                drawPath(tailPath, fill(0xFFFFFFFF.toInt()))
                drawRoundRect(bubble, dp(8f), dp(8f), stroke(0x33000000, dp(1f)))
                drawText(eta, cx - bubblePaint.measureText(eta) / 2, bubble.centerY() - (bubblePaint.ascent() + bubblePaint.descent()) / 2, bubblePaint)
            }
        }
    }

    /** A vehicle we can't tell the line of: a yellow pill (grey vanishes on the map) that says only what it is, with a dark ring. */
    fun unknown(kind: String): Bitmap {
        val paint = text(12f, 0xFF1B1B1F.toInt())
        val height = dp(24f)
        val width = paint.measureText(kind) + dp(20f)
        val pad = dp(3f)
        return bitmap(width + 2 * pad, height + 2 * pad) {
            val pill = RectF(pad, pad, pad + width, pad + height)
            drawRoundRect(RectF(pill).apply { inset(-dp(1.5f), -dp(1.5f)) }, height, height, fill(0xFFFFFFFF.toInt()))
            drawRoundRect(pill, height / 2, height / 2, fill(0xFFFFD600.toInt()))
            drawRoundRect(pill, height / 2, height / 2, stroke(0xFF3E2723.toInt(), dp(1.5f)))
            drawText(kind, pill.centerX() - paint.measureText(kind) / 2, pill.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
        }
    }

    /**
     * A pointer that peeks out of the badge in the heading direction: the tip points up (north), the map rotates it by
     * the bearing; it is centred, so the badge drawn over it hides all but the tip.
     */
    fun arrow(color: Int, highlighted: Boolean): Bitmap {
        val size = dp(if (highlighted) 76f else 64f)
        val c = size / 2
        val tip = dp(if (highlighted) 36f else 30f)
        val baseHalf = dp(if (highlighted) 11f else 9f)
        return bitmap(size, size) {
            val triangle = Path().apply {
                moveTo(c, c - tip)
                lineTo(c + baseHalf, c - dp(6f))
                lineTo(c - baseHalf, c - dp(6f))
                close()
            }
            drawPath(triangle, stroke(0xFFFFFFFF.toInt(), dp(4f)))
            drawPath(triangle, fill(color))
        }
    }

    /** A pole the leg's passages leave from: a dot with its name below. The dot is at the centre of the bitmap. */
    fun boardingStop(name: String): Bitmap = stopLabel(name, 0xFF1B1B1F.toInt(), 0xFFFFFFFF.toInt(), 0xFF1B1B1F.toInt())

    /** The stop where the highlighted run gets off, in the colour of the line. */
    fun alightStop(name: String, color: Int): Bitmap = stopLabel(name, color, color, onColor(color))

    private fun stopLabel(name: String, ring: Int, pillFill: Int, pillText: Int): Bitmap {
        val paint = text(11f, pillText)
        val pillHeight = dp(20f)
        val pillWidth = paint.measureText(name) + dp(14f)
        val dot = dp(7f)
        val gap = dp(4f)
        val pad = dp(3f)
        val width = max(pillWidth, 2 * dot) + 2 * pad
        val half = dot + gap + pillHeight + pad
        return bitmap(width, 2 * half) {
            val cx = width / 2
            drawCircle(cx, half, dot, fill(0xFFFFFFFF.toInt()))
            drawCircle(cx, half, dot - dp(2f), stroke(ring, dp(3f)))
            val top = half + dot + gap
            val pill = RectF(cx - pillWidth / 2, top, cx + pillWidth / 2, top + pillHeight)
            drawRoundRect(pill, dp(10f), dp(10f), fill(pillFill))
            drawRoundRect(pill, dp(10f), dp(10f), stroke(0x33000000, dp(1f)))
            drawText(name, cx - paint.measureText(name) / 2, pill.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
        }
    }
}
