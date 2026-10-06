package ru.radiationx.anilibria.common

import ru.radiationx.data.entity.domain.release.EpisodeAccess
import ru.radiationx.data.entity.domain.types.EpisodeId

internal data class ReleasePlaybackState(
    val hasProgress: Boolean,
    val allEpisodesViewed: Boolean,
    val continueEpisodeId: EpisodeId?,
) {
    companion object {
        // Episode ids are supplied in playback order, starting with the first episode.
        fun resolve(episodeIds: List<EpisodeId>, accesses: List<EpisodeAccess>): ReleasePlaybackState {
            val byId = accesses.associateBy { it.id }
            val allViewed = episodeIds.isNotEmpty() && episodeIds.all { byId[it]?.isViewed == true }
            val latestProgress = accesses.filter { it.hasProgress && it.id in episodeIds }
                .maxByOrNull { it.lastAccessRaw }
            return ReleasePlaybackState(
                hasProgress = latestProgress != null,
                allEpisodesViewed = allViewed,
                continueEpisodeId = if (allViewed) episodeIds.first() else latestProgress?.id,
            )
        }
    }
}
