package app.toarbit.muse_player

import android.os.Handler
import android.os.Looper
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

    private val handler = Handler(Looper.getMainLooper())

    private fun ui(action: () -> Unit) {
        handler.post(action)
    }

    override fun onPlaybackStateChanged(state: PlaybackState) = ui  {
        methodChannel.invokeMethod("onPlaybackStateChanged", state.toMap())
    }

    override fun onMetadataChanged(metadata: MusicMetadata?) = ui {
        methodChannel.invokeMethod("onMetadataChanged", metadata?.obj)
    }

    override fun onPlayQueueChanged(queue: PlayQueue) = ui {
        methodChannel.invokeMethod("onPlayQueueChanged", queue.toDartMapObject())
    }

    override fun onPlayModeChanged(playMode: Int) = ui {
        methodChannel.invokeMethod("onPlayModeChanged", playMode)
    }

}