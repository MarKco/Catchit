package com.ilsecondodasinistra.catchit.shortcuts

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ilsecondodasinistra.catchit.MainActivity
import com.ilsecondodasinistra.catchit.data.DestinationInfo
import com.ilsecondodasinistra.catchit.ui.destinationIcon
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Long-press on the launcher icon: one shortcut per destination of the bottom bar, in the bar's order and only the
 * visible ones, straight to that destination. Dynamic (not static XML) because the bar is edited in the settings.
 */
@Singleton
class ShortcutPublisher @Inject constructor(@ApplicationContext private val context: Context) {
    fun publish(destinations: List<DestinationInfo>) {
        val limit = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        val shortcuts = destinations.take(limit).mapIndexed { rank, d ->
            ShortcutInfoCompat.Builder(context, "destination-${d.id}")
                .setShortLabel(d.label)
                .setLongLabel("${d.origin} ⇄ ${d.target}")
                .setIcon(icon(destinationIcon(d.id)))
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(MainActivity.ACTION_OPEN_DESTINATION)
                        .putExtra(MainActivity.EXTRA_DESTINATION, d.id),
                )
                .setRank(rank)
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    /** The bar's glyph in white on a coloured square, as an adaptive icon (the launcher masks it). */
    private fun icon(vector: ImageVector): IconCompat {
        val size = (ICON_DP * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND)
        // The adaptive icon's safe zone is the middle two thirds: the glyph takes half of the whole.
        val glyph = size * 0.5f
        val scale = glyph / vector.viewportWidth
        canvas.translate((size - glyph) / 2, (size - glyph) / 2)
        canvas.scale(scale, scale)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GLYPH }
        fun draw(group: VectorGroup) {
            for (node in group) when (node) {
                is VectorGroup -> draw(node)
                is VectorPath -> canvas.drawPath(node.pathData.toPath().asAndroidPath(), paint)
            }
        }
        draw(vector.root)
        return IconCompat.createWithAdaptiveBitmap(bitmap)
    }

    private companion object {
        const val ICON_DP = 108
        const val BACKGROUND = 0xFF3949AB.toInt()
        const val GLYPH = 0xFFFFFFFF.toInt()
    }
}
