package ru.radiationx.anilibria.screen.player.quality

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import ru.radiationx.data.datasource.holders.PreferencesHolder
import ru.radiationx.data.entity.common.PlayerQuality
import javax.inject.Inject

class PlayerQualityPreference @Inject constructor(
    context: Context,
    preferencesHolder: PreferencesHolder,
) {
    // A stored manual choice takes precedence; automatic selection does not persist a value.
    val quality = preferencesHolder.playerQuality.withDefault(getScreenQuality(context))
}

@Suppress("DEPRECATION")
private fun getScreenQuality(context: Context): PlayerQuality {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        ?: return PlayerQuality.SD
    val size = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // TV UI can render at 1080p or 720p even when the video output has a higher resolution.
        display.mode.let { Point(it.physicalWidth, it.physicalHeight) }
    } else {
        Point().also { display.getRealSize(it) }
    }
    return screenPlayerQuality(size.x, size.y)
}

internal fun screenPlayerQuality(width: Int, height: Int): PlayerQuality {
    val longSide = maxOf(width, height)
    val shortSide = minOf(width, height)
    return when {
        longSide >= 1920 && shortSide >= 1080 -> PlayerQuality.FULLHD
        longSide >= 1280 && shortSide >= 720 -> PlayerQuality.HD
        else -> PlayerQuality.SD
    }
}
