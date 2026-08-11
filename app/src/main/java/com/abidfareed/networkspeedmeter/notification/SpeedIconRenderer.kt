package com.abidfareed.networkspeedmeter.notification

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Renders the current speed reading into a small bitmap that is used as the notification's
 * small icon. Android strips color from status-bar small icons and keeps only the alpha
 * channel as a silhouette mask (true since Lollipop), so only the *shape* drawn here matters -
 * we draw fully opaque white glyphs on a transparent background and let the OS do the rest.
 *
 * This is the mechanism that makes readable text appear as a status-bar icon without root:
 * there is no other public API surface for it (see TECHNICAL_FEASIBILITY.md).
 */
object SpeedIconRenderer {

    /** High-resolution square canvas; the OS downsamples for the actual status-bar slot. */
    private const val CANVAS_SIZE = 144

    /**
     * @param primary main line, e.g. "2.4M" (value + abbreviated unit, no "/s" - no room for it)
     * @param secondary optional second line, e.g. an upload value when both are shown
     * @param arrowDown true to prefix the primary line with a down-arrow glyph
     * @param arrowUp true to prefix the secondary line with an up-arrow glyph
     */
    fun render(
        primary: String,
        secondary: String? = null,
        arrowDown: Boolean = true,
        arrowUp: Boolean = true,
        sizeScale: Float = 1.0f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = -1 // opaque white; only alpha coverage is used by the OS
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val line1 = (if (arrowDown) "↓" else "") + primary
        val line2 = secondary?.let { (if (arrowUp) "↑" else "") + it }

        if (line2 == null) {
            paint.textSize = fitTextSize(paint, line1, CANVAS_SIZE * 0.92f, CANVAS_SIZE * 0.62f * sizeScale)
            val y = CANVAS_SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(line1, CANVAS_SIZE / 2f, y, paint)
        } else {
            paint.textSize = fitTextSize(paint, if (line1.length >= line2.length) line1 else line2, CANVAS_SIZE * 0.92f, CANVAS_SIZE * 0.42f * sizeScale)
            val lineHeight = paint.descent() - paint.ascent()
            val totalHeight = lineHeight * 2
            var y = CANVAS_SIZE / 2f - totalHeight / 2f - paint.ascent()
            canvas.drawText(line1, CANVAS_SIZE / 2f, y, paint)
            y += lineHeight
            canvas.drawText(line2, CANVAS_SIZE / 2f, y, paint)
        }

        return bitmap
    }

    private fun fitTextSize(paint: Paint, text: String, maxWidth: Float, startSize: Float): Float {
        var size = startSize
        paint.textSize = size
        while (paint.measureText(text) > maxWidth && size > 12f) {
            size -= 2f
            paint.textSize = size
        }
        return size
    }
}
