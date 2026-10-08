package ru.radiationx.anilibria.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import ru.radiationx.anilibria.R

class PosterCardView(context: Context) : WatchedPosterCardView(context) {

    var showFavoriteBadge: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    var franchiseOrdinal: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    var isCurrentRelease: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val orderBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; alpha = 190 }
    private val currentBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dark_colorAccent)
    }
    private val orderText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val badgeBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 128
    }
    private val star = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_poster_favorite))

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val image = mainImageView ?: return
        franchiseOrdinal?.let { ordinal ->
            val label = "#$ordinal"
            val left = image.left + 8f * density
            val top = image.top + 8f * density
            val padding = 8f * density
            val badgeWidth = orderText.measureText(label) + padding * 2
            canvas.drawRoundRect(left, top, left + badgeWidth, top + 30f * density,
                6f * density, 6f * density, orderBackground)
            canvas.drawText(label, left + padding, top + 21f * density, orderText)
        }
        if (isCurrentRelease) {
            val bottom = image.bottom.toFloat() - 3f * density
            val top = bottom - 28f * density
            canvas.drawRect(image.left.toFloat(), top, image.right.toFloat(), bottom, currentBackground)
            val label = "Открыт"
            canvas.drawText(label, image.left + (image.width - orderText.measureText(label)) / 2,
                top + 20f * density, orderText)
        }
        if (!showFavoriteBadge) return
        val radius = 16f * density
        val centerX = image.right - 8f * density - radius
        val centerY = image.top + 8f * density + radius
        canvas.drawCircle(centerX, centerY, radius, badgeBackground)
        val halfStar = 12f * density
        star.setBounds(
            (centerX - halfStar).toInt(),
            (centerY - halfStar).toInt(),
            (centerX + halfStar).toInt(),
            (centerY + halfStar).toInt(),
        )
        star.draw(canvas)
    }
}
