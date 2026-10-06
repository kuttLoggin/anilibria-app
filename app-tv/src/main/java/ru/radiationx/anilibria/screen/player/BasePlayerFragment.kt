package ru.radiationx.anilibria.screen.player

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.PlaybackSeekUi
import androidx.leanback.widget.PlaybackSeekDataProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.ui.leanback.LeanbackPlayerAdapter
import ru.radiationx.anilibria.BuildConfig
import ru.radiationx.anilibria.R
import ru.radiationx.anilibria.ui.presenter.cust.CustomListRowPresenter
import ru.radiationx.data.datasource.holders.PreferencesHolder
import ru.radiationx.data.player.PlayerDataSourceProvider
import ru.radiationx.quill.get
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

open class BasePlayerFragment : VideoSupportFragment() {

    @UnstableApi
    protected var playerGlue: VideoPlayerGlue? = null
        private set

    protected var player: ExoPlayer? = null
        private set

    protected var skipsPart: PlayerSkipsPart? = null
        private set

    protected var isPlaybackSeeking = false
        private set

    private var isPlayPausePressed = false
    private var upNavigationFocus: View? = null

    @SuppressLint("RestrictedApi")
    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isPlaybackSeeking = false
        view.setBackgroundColor(Color.BLACK)

        isControlsOverlayAutoHideEnabled = true
        isShowOrHideControlsOverlayOnUserInteraction = true

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initializePlayer()
        initializeRows()

        // Attach after the glue host, which installs its own key listener during initialization.
        setOnKeyInterceptListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (!isPlayPausePressed && event.repeatCount == 0) {
                            if (playerGlue?.isPlaying == true) {
                                playerGlue?.pause()
                            } else {
                                playerGlue?.play()
                            }
                        }
                        isPlayPausePressed = true
                    }
                    KeyEvent.ACTION_UP -> isPlayPausePressed = false
                }
                true
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            val focusedView = view.findFocus()
                            upNavigationFocus = focusedView?.takeIf {
                                isControlsOverlayVisible &&
                                    isShowOrHideControlsOverlayOnUserInteraction &&
                                    // Up is intentionally consumed while the seek bar is seeking.
                                    it.id != androidx.leanback.R.id.playback_progress
                            }
                        }
                        false
                    }
                    KeyEvent.ACTION_UP -> {
                        val previousFocus = upNavigationFocus
                        upNavigationFocus = null
                        if (previousFocus != null && previousFocus === view.findFocus() &&
                            isControlsOverlayVisible && isShowOrHideControlsOverlayOnUserInteraction
                        ) {
                            // Leanback shows the overlay on key-down events, so hide on release.
                            hideControlsOverlay(true)
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            } else {
                playerGlue?.onKey(view, keyCode, event) == true
            }
        }

        skipsPart = PlayerSkipsPart(
            parent = view as FrameLayout,
            skipButtonText = getString(R.string.player_skip),
            coroutineScope = viewLifecycleOwner.lifecycleScope,
            playerSkipsTimer = get<PreferencesHolder>().playerSkipsTimer,
            onSeek = { position -> player?.seekTo(position) },
            onPlayPause = { player?.let { it.playWhenReady = !it.playWhenReady } },
            onSkipShow = {
                isShowOrHideControlsOverlayOnUserInteraction = false
                isControlsOverlayAutoHideEnabled = false
            },
            onSkipHide = {
                isShowOrHideControlsOverlayOnUserInteraction = true
                isControlsOverlayAutoHideEnabled = skipsPart?.isSeeking != true && player?.isPlaying == true
                if (skipsPart?.isSeeking == true) showControlsOverlay(false)
            }
        )

        playerGlue?.isControlsOverlayAutoHideEnabled = false
        playerGlue?.playbackListener = object : VideoPlayerGlue.PlaybackListener {
            override fun onUpdateProgress() {
                updateSkipPlaybackState()
            }
        }

    }

    override fun onVideoSizeChanged(videoWidth: Int, videoHeight: Int) {
        if (videoWidth == 0 || videoHeight == 0) {
            return
        }
        super.onVideoSizeChanged(videoWidth, videoHeight)
    }

    override fun onPause() {
        isPlayPausePressed = false
        upNavigationFocus = null
        super.onPause()
        playerGlue?.pause()
    }

    @OptIn(UnstableApi::class)
    override fun onDestroyView() {
        super.onDestroyView()
        upNavigationFocus = null
        skipsPart?.dispose()
        skipsPart = null
        playerGlue?.playbackListener = null
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        releasePlayer()
        playerGlue = null
    }

    protected open fun onCompletePlaying() {}
    protected open fun onPreparePlaying() {}

    @UnstableApi
    private fun initializeRows() {
        val playerGlue = this.playerGlue ?: return
        val controlsRow = playerGlue.controlsRow ?: return

        val rowsPresenter = ClassPresenterSelector().apply {
            addClassPresenter(ListRow::class.java, CustomListRowPresenter())
            addClassPresenter(controlsRow.javaClass, playerGlue.playbackRowPresenter)
        }
        val rowsAdapter = ArrayObjectAdapter(rowsPresenter).apply {
            add(controlsRow)
        }

        adapter = rowsAdapter
    }

    @UnstableApi
    private fun initializePlayer() {
        check(player == null) { "Player already initialized" }

        val dataSourceProvider = get<PlayerDataSourceProvider>()
        val dataSourceType = dataSourceProvider.get()
        val targetBufferBytes = PlayerBufferPolicy.targetBufferBytes(Runtime.getRuntime().maxMemory())
        val loadControl = DefaultLoadControl.Builder()
            .setTargetBufferBytes(targetBufferBytes)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()
        val playbackPaused = AtomicBoolean(true)
        val readGate = BufferReadGate(
            limitBytes = targetBufferBytes * 2,
            allocatedBytes = { loadControl.allocator.totalBytesAllocated },
            isPlaybackPaused = { playbackPaused.get() },
        )
        val upstreamFactory = DefaultDataSource.Factory(requireContext(), dataSourceType.factory)
        val dataSourceFactory = DataSource.Factory {
            HlsBufferingDataSource(upstreamFactory.createDataSource(), readGate)
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(requireContext()).apply {
            setDataSourceFactory(dataSourceFactory)
            setLoadErrorHandlingPolicy(object : DefaultLoadErrorHandlingPolicy() {
                override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long =
                    if (loadErrorInfo.exception is BufferCapacityException) C.TIME_UNSET
                    else super.getRetryDelayMsFor(loadErrorInfo)
            })
        }
        player = ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                if (BuildConfig.DEBUG) {
                    addAnalyticsListener(object : AnalyticsListener {
                        override fun onLoadCompleted(
                            eventTime: AnalyticsListener.EventTime,
                            loadEventInfo: LoadEventInfo,
                            mediaLoadData: MediaLoadData,
                        ) {
                            val runtime = Runtime.getRuntime()
                            Timber.tag("TvPlayerBuffer").d(
                                "positionMs=%d bufferedMs=%d allocatedBytes=%d heapUsedBytes=%d " +
                                    "heapMaxBytes=%d targetBufferBytes=%d gateWaitCount=%d " +
                                    "gatePeakAllocatedBytes=%d loadedBytes=%d video=%dx%d",
                                currentPosition,
                                totalBufferedDuration,
                                loadControl.allocator.totalBytesAllocated,
                                runtime.totalMemory() - runtime.freeMemory(),
                                runtime.maxMemory(),
                                targetBufferBytes,
                                readGate.waitCount.get(),
                                readGate.peakAllocatedBytes.get(),
                                loadEventInfo.bytesLoaded,
                                videoSize.width,
                                videoSize.height,
                            )
                        }
                    })
                }
                addListener(object : Player.Listener {
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        playbackPaused.set(!playWhenReady)
                    }

                    override fun onRenderedFirstFrame() {
                        skipsPart?.onFrameRendered(this@BasePlayerFragment.player?.currentPosition ?: 0)
                        updateSkipPlaybackState()
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateSkipPlaybackState()
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int,
                    ) {
                        if (reason == Player.DISCONTINUITY_REASON_SEEK ||
                            reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
                        ) {
                            skipsPart?.onFramePending(newPosition.positionMs, oldPosition.positionMs != newPosition.positionMs || oldPosition.mediaItemIndex != newPosition.mediaItemIndex)
                            updateSkipPlaybackState()
                        }
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        when (playbackState) {
                            Player.STATE_ENDED -> onCompletePlaying()
                            Player.STATE_READY -> onPreparePlaying()
                            Player.STATE_BUFFERING, Player.STATE_IDLE -> {}
                        }
                        updateSkipPlaybackState()
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        Toast.makeText(
                            requireContext(),
                            "Ошибка при воспроизведении: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            }

        val playerAdapter = LeanbackPlayerAdapter(requireContext(), player!!, 500)

        playerGlue = VideoPlayerGlue(
            context = requireContext(),
            fragment = this,
            playerAdapter = playerAdapter
        ).apply {
            host = object : VideoSupportFragmentGlueHost(this@BasePlayerFragment) {
                override fun setPlaybackSeekUiClient(client: PlaybackSeekUi.Client?) {
                    super.setPlaybackSeekUiClient(client?.let { delegate ->
                        object : PlaybackSeekUi.Client() {
                            override fun isSeekEnabled(): Boolean = delegate.isSeekEnabled

                            override fun getPlaybackSeekDataProvider(): PlaybackSeekDataProvider? =
                                delegate.playbackSeekDataProvider

                            override fun onSeekStarted() {
                                isPlaybackSeeking = true
                                skipsPart?.onSeekStarted()
                                delegate.onSeekStarted()
                            }

                            override fun onSeekPositionChanged(pos: Long) {
                                delegate.onSeekPositionChanged(pos)
                            }

                            override fun onSeekFinished(cancelled: Boolean) {
                                delegate.onSeekFinished(cancelled)
                                isPlaybackSeeking = false
                                skipsPart?.onSeekFinished(
                                    this@BasePlayerFragment.player?.currentPosition ?: 0,
                                    cancelled,
                                )
                            }
                        }
                    })
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun updateSkipPlaybackState() {
        val player = player ?: return
        skipsPart?.onPlaybackChanged(player.playbackState == Player.STATE_READY, player.isPlaying, player.currentPosition)
        val autoHide = isShowOrHideControlsOverlayOnUserInteraction &&
            skipsPart?.isSeeking != true && player.isPlaying
        if (isControlsOverlayAutoHideEnabled != autoHide) isControlsOverlayAutoHideEnabled = autoHide
        if (!player.playWhenReady && isShowOrHideControlsOverlayOnUserInteraction) showControlsOverlay(false)
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    protected fun preparePlayer(url: String) {
        skipsPart?.onVideoLoading()
        player?.setMediaItem(MediaItem.fromUri(url.toUri()), false)
        player?.prepare()
    }

}
