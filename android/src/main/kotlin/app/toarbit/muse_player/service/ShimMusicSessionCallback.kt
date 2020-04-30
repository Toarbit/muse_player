package app.toarbit.muse_player.service

import android.os.IBinder
import app.toarbit.muse_player.MusicSessionCallback
import app.toarbit.muse_player.player.MusicMetadata
import app.toarbit.muse_player.player.PlayQueue
import app.toarbit.muse_player.player.PlaybackState

/**
 * Created by omit on 2020/4/24 for muse_player.
 */

internal class ShimMusicSessionCallback : MusicSessionCallback {

    private val callbacks = mutableListOf<MusicSessionCallback>()


    fun addCallback(callback: MusicSessionCallback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: MusicSessionCallback) {
        callbacks.remove(callback)
    }

    override fun onPlaybackStateChanged(state: PlaybackState) {
        callbacks.forEach { it.onPlaybackStateChanged(state) }
    }

    override fun onPlayQueueChanged(queue: PlayQueue) {
        callbacks.forEach { it.onPlayQueueChanged(queue) }
    }

    override fun onMetadataChanged(metadata: MusicMetadata?) {
        callbacks.forEach { it.onMetadataChanged(metadata) }
    }

    override fun onPlayModeChanged(playMode: Int) {
        callbacks.forEach { it.onPlayModeChanged(playMode) }
    }

    override fun asBinder(): IBinder {
        throw IllegalAccessError()
    }
}