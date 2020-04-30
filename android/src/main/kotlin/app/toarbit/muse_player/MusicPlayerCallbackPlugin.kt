package app.toarbit.muse_player

import app.toarbit.muse_player.player.MusicMetadata
import app.toarbit.muse_player.player.PlayQueue
import app.toarbit.muse_player.player.PlaybackState
import io.flutter.plugin.common.MethodChannel

/**
 * Created by omit on 2020/4/30 for muse_player.
 */

class MusicPlayerCallbackPlugin constructor(
        private val methodChannel: MethodChannel
) : MusicSessionCallback.Stub() {

    override fun onPlaybackStateChanged(state: PlaybackState) {
        methodChannel.invokeMethod("onPlaybackStateChanged", state.toMap())
    }

    override fun onMetadataChanged(metadata: MusicMetadata?) {
        methodChannel.invokeMethod("onMetadataChanged", metadata?.obj)
    }

    override fun onPlayQueueChanged(queue: PlayQueue) {
        methodChannel.invokeMethod("onPlayQueueChanged", queue.toDartMapObject())
    }

    override fun onPlayModeChanged(playMode: Int) {
        methodChannel.invokeMethod("onPlayModeChanged", playMode)
    }

}