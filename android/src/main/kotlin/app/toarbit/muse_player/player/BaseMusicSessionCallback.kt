package app.toarbit.muse_player.player

import app.toarbit.muse_player.MusicSessionCallback

/**
 * Created by omit on 2020/4/24 for muse_player.
 */

abstract class BaseMusicSessionCallback : MusicSessionCallback.Stub() {
    override fun onPlaybackStateChanged(state: PlaybackState) {
    }

    override fun onMetadataChanged(metadata: MusicMetadata?) {
    }

    override fun onPlayQueueChanged(queue: PlayQueue) {
    }

    override fun onPlayModeChanged(playMode: Int) {
    }
}