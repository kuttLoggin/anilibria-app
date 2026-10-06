package ru.radiationx.anilibria.screen.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.radiationx.data.entity.domain.release.PlayerSkips

class EpisodeWatchPolicyTest {
    private val duration = 24 * 60 * 1000L
    private val ending = PlayerSkips.Skip(22 * 60 * 1000L, 23 * 60 * 1000L)

    @Test fun `without ending five minute boundary is inclusive`() {
        assertFalse(EpisodeWatchPolicy.isViewed(duration - 300_001L, duration, null))
        assertTrue(EpisodeWatchPolicy.isViewed(duration - 300_000L, duration, null))
    }

    @Test fun `ending takes priority over the last five minutes`() {
        assertFalse(EpisodeWatchPolicy.isViewed(20 * 60 * 1000L, duration, ending))
        assertFalse(EpisodeWatchPolicy.isViewed(ending.start - 1L, duration, ending))
        assertTrue(EpisodeWatchPolicy.isViewed(ending.start, duration, ending))
        assertTrue(EpisodeWatchPolicy.isViewed(ending.end, duration, ending))
    }

    @Test fun `seek beyond ending does not pretend ending was watched`() {
        assertFalse(EpisodeWatchPolicy.isViewed(ending.end + 1L, duration, ending))
    }

    @Test fun `natural completion always marks viewed`() {
        assertTrue(EpisodeWatchPolicy.isViewed(0L, 0L, ending, ended = true))
        assertTrue(EpisodeWatchPolicy.isViewed(duration, duration, ending))
    }

    @Test fun `unknown duration does not activate fallback`() {
        assertFalse(EpisodeWatchPolicy.isViewed(1000L, 0L, null))
        assertFalse(EpisodeWatchPolicy.isViewed(1000L, Long.MIN_VALUE, null))
        assertTrue(EpisodeWatchPolicy.isViewed(ending.start, 0L, ending))
    }

    @Test fun `invalid ending uses fallback`() {
        listOf(PlayerSkips.Skip(0L, 0L), PlayerSkips.Skip(100L, 50L),
            PlayerSkips.Skip(-1L, 1000L), PlayerSkips.Skip(duration, duration + 1L))
            .forEach { assertTrue(EpisodeWatchPolicy.isViewed(duration - 300_000L, duration, it)) }
    }

    @Test fun `zero and unset positions do not count as playback`() {
        assertFalse(EpisodeWatchPolicy.isViewed(0L, 120_000L, null))
        assertFalse(EpisodeWatchPolicy.isViewed(-1L, duration, ending))
        assertTrue(EpisodeWatchPolicy.isViewed(1L, 120_000L, null))
    }
}
