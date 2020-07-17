package app.toarbit.muse_player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import app.toarbit.muse_player.player.MusicMetadata
import app.toarbit.muse_player.player.PlayQueue
import app.toarbit.muse_player.service.MusicPlayerService
import app.toarbit.muse_player.utils.SleepTimer
import app.toarbit.muse_player.utils.getNext
import app.toarbit.muse_player.utils.getPrevious

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


private const val UI_PLUGIN_NAME = "app.toarbit.muse_player/player.ui"

/** MusePlayerPlugin */
class MusePlayerPlugin: FlutterPlugin {

    private lateinit var playerUiChannel: MusePlayerUiChannel

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(binding.binaryMessenger, UI_PLUGIN_NAME)
        playerUiChannel = MusePlayerUiChannel(channel, binding.applicationContext)
        channel.setMethodCallHandler(playerUiChannel)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        playerUiChannel.channel.setMethodCallHandler(null)
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        @JvmStatic
        fun registerWith(registrar: PluginRegistry.Registrar) {
            val channel = MethodChannel(registrar.messenger(), UI_PLUGIN_NAME)
            channel.setMethodCallHandler(MusePlayerUiChannel(channel, registrar.context()))
        }
    }
}

/**
 * 代理操作 [MusicPlayerService]
 */
private class MusePlayerUiChannel(
        val channel: MethodChannel,
        context: Context
) : MethodCallHandler {

    private val remotePlayer = context.startMusicService()

    private val uiPlaybackPlugin = MusicPlayerCallbackPlugin(channel)

    init {
        remotePlayer.doWhenSessionReady { it.addCallback(uiPlaybackPlugin) }
    }

    override fun onMethodCall(call: MethodCall, result: Result) =
            remotePlayer.doWhenSessionReady { session ->
                val r: Any? = when (call.method) {
                    "init" -> {
                        uiPlaybackPlugin.onMetadataChanged(session.current)
                        uiPlaybackPlugin.onPlayModeChanged(session.playMode)
                        uiPlaybackPlugin.onPlayQueueChanged(session.playQueue)
                        uiPlaybackPlugin.onPlaybackStateChanged(session.playbackState)
                    }
                    "play" -> session.play()
                    "pause" -> session.pause()
                    "playFromMediaId" -> session.playFromMediaId(call.arguments())
                    "skipToNext" -> session.skipToNext()
                    "skipToPrevious" -> session.skipToPrevious()
                    "seekTo" -> session.seekTo(call.arguments<Number>().toLong())
                    "setPlayMode" -> session.playMode = call.arguments()
                    "setPlayQueue" -> session.playQueue = PlayQueue(call.arguments<Map<String, Any>>())
                    "getNext" -> session.getNext(MusicMetadata.fromMap(call.arguments()))?.obj
                    "getPrevious" -> session.getPrevious(MusicMetadata.fromMap(call.arguments()))?.obj
                    "insertToNext" -> session.addMetadata(
                            MusicMetadata.fromMap(call.arguments()),
                            session.current?.mediaId
                    )
                    "getSleepTimer" -> SleepTimer.getMillisUntilFinish()
                    "setSleepTimer" -> SleepTimer.toggleTimer(call.arguments())
                    else -> null
                }

                when (r) {
                    Unit -> result.success(null)
                    null -> result.notImplemented()
                    else -> result.success(r)
                }
            }

}


private fun Context.startMusicService(): RemotePlayer {
    val intent = Intent(this, MusicPlayerService::class.java)
    intent.action = MusicPlayerService.ACTION_MUSIC_PLAYER_SERVICE
    startService(intent)
    val player = RemotePlayer()
    if (!bindService(intent, player, Context.BIND_AUTO_CREATE)) {
        if (BuildConfig.DEBUG) {
            throw IllegalStateException("can not connect to MusicService")
        }
    }
    return player
}


internal class RemotePlayer : ServiceConnection {

    private var playerSession: MusicPlayerSession? = null

    private val pendingExecution = mutableListOf<suspend (MusicPlayerSession) -> Unit>()

    override fun onServiceDisconnected(name: ComponentName?) {
        playerSession?.destroy()
        playerSession = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        playerSession = MusicPlayerSession.Stub.asInterface(service)
        ArrayList(pendingExecution).forEach(::doWhenSessionReady)
        pendingExecution.clear()
    }

    fun doWhenSessionReady(call: suspend (MusicPlayerSession) -> Unit) {
        val session = playerSession
        if (session == null) {
            pendingExecution.add(call)
        } else {
            GlobalScope.launch(Dispatchers.Main) { call(session) }
        }
    }
}