package ru.radiationx.anilibria.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.radiationx.data.entity.domain.release.EpisodeAccess
import ru.radiationx.data.entity.domain.types.EpisodeId
import ru.radiationx.data.entity.domain.types.ReleaseId

class ReleasePlaybackStateTest {
    private val episodes = (1..3).map { EpisodeId(it.toString(), ReleaseId(1)) }
    private fun access(index: Int, viewed: Boolean = true, seek: Long = 0L, date: Long = 1L) =
        EpisodeAccess(episodes[index], seek, viewed, date)

    @Test fun `all viewed episodes restart from first without requiring a saved position`() {
        val state = ReleasePlaybackState.resolve(episodes, episodes.indices.map { access(it) })
        assertTrue(state.allEpisodesViewed)
        assertFalse(state.hasProgress)
        assertEquals(episodes.first(), state.continueEpisodeId)
    }

    @Test fun `all viewed episodes restart instead of resuming most recent episode`() {
        val state = ReleasePlaybackState.resolve(episodes, listOf(access(0), access(1), access(2, seek = 100L, date = 5L)))
        assertTrue(state.allEpisodesViewed)
        assertEquals(episodes.first(), state.continueEpisodeId)
    }

    @Test fun `one unfinished episode restores continue and uses most recent progress`() {
        val state = ReleasePlaybackState.resolve(episodes, listOf(access(0), access(1), access(2, false, 100L, 5L)))
        assertFalse(state.allEpisodesViewed)
        assertTrue(state.hasProgress)
        assertEquals(episodes.last(), state.continueEpisodeId)
    }

    @Test fun `newly available episode removes all viewed state`() {
        assertFalse(ReleasePlaybackState.resolve(episodes, listOf(access(0), access(1))).allEpisodesViewed)
    }

    @Test fun `stale or foreign marks cannot satisfy missing available episodes`() {
        val foreign = EpisodeAccess(EpisodeId("3", ReleaseId(2)), 100L, true, 10L)
        val state = ReleasePlaybackState.resolve(episodes, listOf(access(0), access(1), foreign))
        assertFalse(state.allEpisodesViewed)
        assertFalse(state.hasProgress)
        assertNull(state.continueEpisodeId)
    }

    @Test fun `empty playlist never offers restart`() {
        val state = ReleasePlaybackState.resolve(emptyList(), listOf(access(0)))
        assertFalse(state.allEpisodesViewed)
        assertNull(state.continueEpisodeId)
    }
}
