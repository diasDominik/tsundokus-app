package uk.tsundokus.androidapp.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.ByteArrayOutputStream

/** Draws a simple cover for a demo volume: the series colour, its title and the volume number. */
internal object DemoCovers {
    private const val WIDTH = 400
    private const val HEIGHT = 600
    private const val MARGIN = 32f

    fun jpeg(order: DemoLibrary.Order): ByteArray {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val base = order.series.color

        canvas.drawRect(
            0f,
            0f,
            WIDTH.toFloat(),
            HEIGHT.toFloat(),
            Paint().apply {
                shader =
                    LinearGradient(
                        0f,
                        0f,
                        0f,
                        HEIGHT.toFloat(),
                        lighten(base),
                        darken(base),
                        Shader.TileMode.CLAMP,
                    )
            },
        )
        // A large soft circle, so the covers aren't flat.
        canvas.drawCircle(
            WIDTH * 0.72f,
            HEIGHT * 0.45f,
            WIDTH * 0.42f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(48, 255, 255, 255) },
        )

        val titlePaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 46f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
        val width = (WIDTH - 2 * MARGIN).toInt()
        val title =
            StaticLayout.Builder
                .obtain(order.series.title, 0, order.series.title.length, titlePaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
        canvas.save()
        canvas.translate(MARGIN, MARGIN * 1.5f)
        title.draw(canvas)
        canvas.restore()

        canvas.drawText(
            order.volume.toString(),
            WIDTH - MARGIN,
            HEIGHT - MARGIN * 2.2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 255, 255, 255)
                textSize = 150f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            },
        )
        canvas.drawText(
            order.series.author.uppercase(),
            MARGIN,
            HEIGHT - MARGIN,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(200, 255, 255, 255)
                textSize = 22f
                letterSpacing = 0.12f
            },
        )

        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    private fun lighten(color: Int): Int = blend(color, Color.WHITE, 0.18f)

    private fun darken(color: Int): Int = blend(color, Color.BLACK, 0.35f)

    private fun blend(
        from: Int,
        to: Int,
        amount: Float,
    ): Int =
        Color.rgb(
            (Color.red(from) + (Color.red(to) - Color.red(from)) * amount).toInt(),
            (Color.green(from) + (Color.green(to) - Color.green(from)) * amount).toInt(),
            (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount).toInt(),
        )
}
