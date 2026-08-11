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
 *
 * Layout: the number is the dominant element (as large and bold as the icon slot allows) with
 * the unit stacked underneath in smaller text, matching how legible reference apps do it -
 * cramming a value and unit onto one line is what makes text unreadable at status-bar scale.
 */
object SpeedIconRenderer {

    /** High-resolution square canvas; the OS downsamples for the actual status-bar slot. */
    private const val CANVAS_SIZE = 160

    /**
     * Single-metric layout: big bold value on top, unit stacked below.
     * @param arrow optional direction glyph ("↓"/"↑") drawn small, above the value.
     */
    fun render(value: String, unit: String, arrow: String? = null, sizeScale: Float = 1.0f): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = basePaint()

        val valueSize = fitTextSize(paint, value, CANVAS_SIZE * 0.94f, CANVAS_SIZE * 0.62f * sizeScale)
        val unitSize = valueSize * 0.42f
        val arrowSize = valueSize * 0.34f

        paint.textSize = unitSize
        val unitHeight = paint.descent() - paint.ascent()
        paint.textSize = valueSize
        val valueHeight = paint.descent() - paint.ascent()
        val arrowHeight = if (arrow != null) arrowSize * 0.9f else 0f

        val totalHeight = arrowHeight + valueHeight + unitHeight
        var y = CANVAS_SIZE / 2f - totalHeight / 2f

        if (arrow != null) {
            paint.textSize = arrowSize
            y -= paint.ascent()
            canvas.drawText(arrow, CANVAS_SIZE / 2f, y, paint)
            y += arrowHeight
        }

        paint.textSize = valueSize
        y -= paint.ascent()
        canvas.drawText(value, CANVAS_SIZE / 2f, y, paint)
        y += paint.descent()

        paint.textSize = unitSize
        y -= paint.ascent()
        canvas.drawText(unit, CANVAS_SIZE / 2f, y, paint)

        return bitmap
    }

    /** Two-metric layout for combined download+upload display. */
    fun renderCombined(
        downValue: String,
        downUnit: String,
        upValue: String,
        upUnit: String,
        sizeScale: Float = 1.0f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = basePaint()

        val line1 = "↓$downValue$downUnit"
        val line2 = "↑$upValue$upUnit"
        val widest = if (line1.length >= line2.length) line1 else line2
        paint.textSize = fitTextSize(paint, widest, CANVAS_SIZE * 0.94f, CANVAS_SIZE * 0.44f * sizeScale)

        val lineHeight = paint.descent() - paint.ascent()
        val totalHeight = lineHeight * 2
        var y = CANVAS_SIZE / 2f - totalHeight / 2f - paint.ascent()
        canvas.drawText(line1, CANVAS_SIZE / 2f, y, paint)
        y += lineHeight
        canvas.drawText(line2, CANVAS_SIZE / 2f, y, paint)

        return bitmap
    }

    /** Minimal idle indicator used when "show when zero" is disabled. */
    fun renderIdle(): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = basePaint()
        paint.textSize = CANVAS_SIZE * 0.5f
        val y = CANVAS_SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText("•", CANVAS_SIZE / 2f, y, paint)
        return bitmap
    }

    private fun basePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = -1 // opaque white; only alpha coverage is used by the OS
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private fun fitTextSize(paint: Paint, text: String, maxWidth: Float, startSize: Float): Float {
        var size = startSize
        paint.textSize = size
        while (paint.measureText(text) > maxWidth && size > 14f) {
            size -= 2f
            paint.textSize = size
        }
        return size
    }
}
