package app.toarbit.muse_player.player

import android.content.Context
import android.os.SystemClock
import app.toarbit.muse_player.MusicPlayerServicePlugin
import app.toarbit.muse_player.MusicPlayerSession
import app.toarbit.muse_player.MusicResult
import app.toarbit.muse_player.MusicSessionCallback
import app.toarbit.muse_player.ext.durationOrZero
import app.toarbit.muse_player.ext.mapPlaybackState
import app.toarbit.muse_player.ext.toMediaSource
import app.toarbit.muse_player.service.ShimMusicSessionCallback
import app.toarbit.muse_player.utils.log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Created by omit on 2020/4/24 for muse_player.
 */

class MusicPlayerSessionImpl constructor(private val context: Context) : MusicPlayerSession.Stub(),
        CoroutineScope by MainScope() {

    companion object {
        private val audioAttribute = AudioAttributes.Builder()
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

    }

    // Wrap a SimpleExoPlayer with a decorator to handle audio focus for us.
    private val player: ExoPlayer by lazy {
        SimpleExoPlayer.Builder(context).build().apply {
            setWakeMode(C.WAKE_MODE_NETWORK)
            setHandleAudioBecomingNoisy(true)
            setAudioAttributes(audioAttribute, true)
            addListener(ExoPlayerEventListener())
        }
    }

    @Suppress("JoinDeclarationAndAssignment")
    internal val servicePlugin: MusicPlayerServicePlugin

    private val shimSessionCallback = ShimMusicSessionCallback()

    private var playMode: PlayMode = PlayMode.Sequence

    private var playQueue: PlayQueue = PlayQueue.Empty

    private var metadata: MusicMetadata? = null

    private fun performPlay(metadata: MusicMetadata?) {
        this.metadata = metadata
        if (metadata == null) {
            player.stop(true)
            return
        }
        player.prepare(metadata.toMediaSource(context, servicePlugin))
        player.playWhenReady = true
        invalidatePlaybackState()
        invalidateMetadata()
    }


    override fun skipToNext() {
        skipTo { getNext(current) }
    }

    override fun skipToPrevious() {
        skipTo { getPrevious(current) }
    }

    private fun skipTo(call: suspend (PlayQueue) -> MusicMetadata?) {
        player.stop(true)
        launch {
            val next = runCatching { call(playQueue) }.getOrNull()
            performPlay(next)
        }
    }


    override fun play() {
        if (player.playbackError != null) {
            player.stop(true)
            performPlay(metadata)
        } else {
            player.playWhenReady = true
        }
    }

    override fun pause() {
        player.playWhenReady = false
    }

    override fun getPlayQueue(): PlayQueue {
        return playQueue
    }

    private suspend fun getPrevious(anchor: MusicMetadata?): MusicMetadata? {
        log { "getPrevious $anchor" }
        return playQueue.getPrevious(anchor, playMode)
                ?: servicePlugin.onNoMoreMusic(SkipType.Previous, playQueue, playMode)
    }

    private suspend fun getNext(anchor: MusicMetadata?): MusicMetadata? {
        return playQueue.getNext(anchor, playMode)
                ?: servicePlugin.onNoMoreMusic(SkipType.Next, playQueue, playMode)
    }

    override fun getPrevious(anchor: MusicMetadata?, result: MusicResult) {
        launch { result.onResult(getPrevious(anchor)) }
    }

    override fun getNext(anchor: MusicMetadata?, result: MusicResult) {
        launch { result.onResult(getNext(anchor)) }
    }

    override fun setPlayQueue(queue: PlayQueue) {
        playQueue.onQueueChanged = null
        queue.onQueueChanged = ::invalidatePlayQueue
        playQueue = queue
        invalidatePlayQueue()
    }

    override fun seekTo(pos: Long) {
        player.seekTo(pos)
    }

    override fun removeCallback(callback: MusicSessionCallback) {
        shimSessionCallback.removeCallback(callback)
    }

    override fun getPlaybackState(): PlaybackState {
        return playbackStateBackup
    }

    override fun stop() {
        player.playWhenReady = false
    }

    override fun addCallback(callback: MusicSessionCallback) {
        shimSessionCallback.addCallback(callback)
    }

    override fun playFromMediaId(mediaId: String) {
        skipTo { it.getByMediaId(mediaId) }
    }
    private fun moveTo(metadata: MusicMetadata?) {
        this.metadata = metadata
        if (metadata == null) {
            player.stop()
            return
        }
        player.prepare(metadata.toMediaSource(context, servicePlugin))
        invalidateMetadata()
    }
    fun moveToMediaId(mediaId: String) {
        launch {
            val next = runCatching { playQueue.getByMediaId(mediaId) }.getOrNull()
            moveTo(next)
        }
    }

    override fun getPlayMode(): Int {
        return playMode.rawValue
    }

    override fun getCurrent(): MusicMetadata? {
        return metadata
    }

    override fun setPlayMode(playMode: Int) {
        this.playMode = PlayMode.valueOf(playMode)
        shimSessionCallback.onPlayModeChanged(playMode)
    }

    override fun addMetadata(metadata: MusicMetadata, anchorMediaId: String?) {
        playQueue.add(anchorMediaId, metadata)
        invalidatePlayQueue()
    }

    override fun removeMetadata(mediaId: String) {
        playQueue.remove(mediaId)
        invalidatePlayQueue()
    }

    override fun setPlaybackSpeed(speed: Double) {
        player.setPlaybackParameters(PlaybackParameters(speed.toFloat()))
        invalidatePlaybackState()
    }

    /**
     * insert a list to current playing queue
     *
     * TODO: available for ui channel
     */
    fun insertMetadataList(list: List<MusicMetadata>, index: Int) {
        playQueue.insert(index, list)
        invalidatePlayQueue()
    }

    private var playbackStateBackup: PlaybackState = PlaybackState(
            state = State.None,
            position = 0,
            bufferedPosition = 0,
            speed = 1F,
            error = null,
            updateTime = System.currentTimeMillis(),
            duration = player.durationOrZero()
    )

    private fun invalidatePlaybackState() {
        val playerError = player.playbackError
        val state = playerError?.let { State.Error } ?: player.mapPlaybackState()
        val playbackState = PlaybackState(
                state = state,
                position = player.currentPosition,
                bufferedPosition = player.bufferedPosition,
                speed = player.playbackParameters.speed,
                error = playerError?.let { PlayerError(it.type, it.message ?: "") },
                updateTime = SystemClock.uptimeMillis(),
                duration = player.durationOrZero(),
        )
        this.playbackStateBackup = playbackState
        shimSessionCallback.onPlaybackStateChanged(playbackState)
    }

    private fun invalidateMetadata() {
        metadata = metadata?.copyWith(duration = player.durationOrZero())
        shimSessionCallback.onMetadataChanged(metadata)
    }

    private fun invalidatePlayQueue() {
        shimSessionCallback.onPlayQueueChanged(playQueue)
    }

    override fun destroy() {
        cancel()
    }


    private inner class ExoPlayerEventListener : Player.EventListener {

        override fun onLoadingChanged(isLoading: Boolean) {
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            invalidatePlaybackState()
        }

        override fun onPositionDiscontinuity(reason: Int) {
            invalidatePlaybackState()
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackStateInt: Int) {
            invalidatePlaybackState()
            // auto play next
            if (playbackStateInt == Player.STATE_ENDED) {
                if (playMode == PlayMode.Single) {
                    player.seekTo(0)
                    player.playWhenReady = true
                } else {
                    skipToNext()
                }
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            invalidatePlaybackState()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            invalidateMetadata()
        }

    }


    init {
        servicePlugin = MusicPlayerServicePlugin.startServiceIsolate(context, this)
    }

    private enum class SkipType {
        Next, Previous,
    }

    // return the next music for playing
    private suspend fun MusicPlayerServicePlugin.onNoMoreMusic(
            skip: SkipType,
            playQueue: PlayQueue,
            playMode: PlayMode
    ): MusicMetadata? {
        return when (skip) {
            SkipType.Next -> onPlayNextNoMoreMusic(playQueue, playMode)
            SkipType.Previous -> onPlayPreviousNoMoreMusic(playQueue, playMode)
        }
    }

}
