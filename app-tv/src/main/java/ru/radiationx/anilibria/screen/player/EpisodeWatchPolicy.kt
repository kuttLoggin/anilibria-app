package ru.radiationx.anilibria.screen.player

import ru.radiationx.data.entity.domain.release.PlayerSkips

internal object EpisodeWatchPolicy {
    private const val FIVE_MINUTES_MS = 5 * 60 * 1000L

    fun isViewed(
        position: Long,
        duration: Long,
        ending: PlayerSkips.Skip?,
        ended: Boolean = false,
    ): Boolean {
        if (ended) return true
        if (position <= 0L) return false
        if (duration > 0L && position >= duration) return true
        val validEnding = ending?.takeIf {
            it.start >= 0L && it.end > it.start &&
                (duration <= 0L || it.end <= duration)
        }
        if (validEnding != null) return position in validEnding.start..validEnding.end
        return duration > 0L && duration - position <= FIVE_MINUTES_MS
    }
}
