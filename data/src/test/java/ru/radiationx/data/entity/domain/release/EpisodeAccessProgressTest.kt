package ru.radiationx.data.entity.domain.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.radiationx.data.entity.domain.types.EpisodeId
import ru.radiationx.data.entity.domain.types.ReleaseId

class EpisodeAccessProgressTest {
    private val id = EpisodeId("1", ReleaseId(1))

    @Test fun `saved progress is independent of completion`() {
        val access = EpisodeAccess.createDefault(id).withPlaybackProgress(90_000L, 123L, false)
        assertTrue(access.hasProgress)
        assertFalse(access.isViewed)
        assertEquals(123L, access.lastAccessRaw)
    }

    @Test fun `rewinding a viewed episode keeps the completion mark`() {
        val access = EpisodeAccess(id, 1_400_000L, true, 1L).withPlaybackProgress(1000L, 2L, false)
        assertTrue(access.isViewed)
        assertEquals(1000L, access.seek)
    }

    @Test fun `manually viewed episode has no playback progress`() {
        val access = EpisodeAccess.createDefault(id).copy(isViewed = true)
        assertFalse(access.hasProgress)
        assertTrue(access.withPlaybackProgress(1000L, 2L, false).isViewed)
    }

    @Test fun `restarting a viewed episode preserves its mark`() {
        val access = EpisodeAccess(id, 1_400_000L, true, 1L).withPlaybackProgress(0L, 2L, false)
        assertTrue(access.isViewed)
        assertFalse(access.hasProgress)
    }

    @Test fun `manual reset clears progress and completion`() {
        val reset = EpisodeAccess.createDefault(id)
        assertFalse(reset.hasProgress)
        assertFalse(reset.isViewed)
        assertEquals(0L, reset.lastAccessRaw)
    }
}
