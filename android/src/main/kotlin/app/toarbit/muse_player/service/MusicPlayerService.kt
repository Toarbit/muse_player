package app.toarbit.muse_player.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.media.MediaBrowserServiceCompat
import app.toarbit.muse_player.player.MusicMetadata
import app.toarbit.muse_player.player.MusicPlayerSessionImpl
import app.toarbit.muse_player.player.PlayMode
import app.toarbit.muse_player.player.PlayQueue
import app.toarbit.muse_player.utils.LoggerLevel
import app.toarbit.muse_player.utils.log

/**
 * Created by omit on 2020/4/24 for muse_player.
 */

class MusicPlayerService : MediaBrowserServiceCompat(), LifecycleOwner {
    companion object {
        const val ACTION_MUSIC_PLAYER_SERVICE = "app.toarbit.muse_player.service.MusicPlayerService"
    }

    private val lifecycle = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycle

    private val playerSession by lazy { MusicPlayerSessionImpl(this) }

    private val mediaSession by lazy {
        val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)
        if (sessionIntent == null) {
            log(level = LoggerLevel.ERROR) { "application do not have launcher intent ??" }
        }
        return@lazy MediaSessionCompat(this, "MusicService").also { mediaSession ->
            sessionIntent?.let {
                mediaSession.setSessionActivity(PendingIntent.getActivity(this, 0, it, 0))
            }
            mediaSession.isActive = true
        }
    }

    override fun onCreate() {
        super.onCreate()
        lifecycle.markState(Lifecycle.State.CREATED)
        sessionToken = mediaSession.sessionToken
        mediaSession.setCallback(MediaSessionCallbackAdapter(playerSession))
        playerSession.addCallback(MusicSessionCallbackAdapter(mediaSession))
        val notificationAdapter = NotificationAdapter(this, playerSession, mediaSession)
        playerSession.addCallback(notificationAdapter)
        lifecycle.addObserver(notificationAdapter)

        loadPlaylist()
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == ACTION_MUSIC_PLAYER_SERVICE) {
            return playerSession.asBinder()
        }
        return super.onBind(intent)
    }
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot("ROOT", null)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        playerSession.stop()
        savePlaylist()
    }

    override fun onDestroy() {
        savePlaylist()
        lifecycle.markState(Lifecycle.State.DESTROYED)
        mediaSession.isActive = false
        mediaSession.release()
        playerSession.destroy()
        super.onDestroy()
    }


    /**
     * 加载 Playlist
     * 注意由于要对 初始化的默认 playlist 执行 attach()，由此如果成功加载，已经完成该操作，返回 null
     * 如果没有加载，则返回 初始化的默认 playlist 以确保进行 attach() 操作
     */
    private fun loadPlaylist() {
        val sp = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        val token: String? = sp.getString("flutter.quiet_player_token", "")
        if (token.isNullOrEmpty() || (!playerSession.playQueue.isEmpty && playerSession.playQueue.queueId == token))
            return

        val queue: List<MusicMetadata> = sp.getString("flutter.quiet_player_playlist", "")?.fromCompact() ?: return
        val title: String = sp.getString("flutter.quiet_player_playlist_title", "") ?: ""
        playerSession.playQueue = PlayQueue(token, title, queue, null, null)
        val mediaId = sp.getString("flutter.quiet_player_playing", "") ?: queue[0].mediaId
        playerSession.moveToMediaId(mediaId)
        val playMode = sp.getInt("flutter.quiet_player_play_mode", PlayMode.Sequence.rawValue)
        playerSession.playMode = playMode
        log { "load playlist ${playerSession.playQueue.queueId} with size ${playerSession.playQueue.getQueue().size}" }
    }
    private fun savePlaylist() {
        val sp = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        val token = sp.getString("flutter.quiet_player_token", "")
        sp.edit()
                .apply {
                    playerSession.current?.mediaId?.let { putString("flutter.quiet_player_playing", it) }
                    if (token == null || token != playerSession.playQueue.queueId) {
                        putString("flutter.quiet_player_playlist", playerSession.playQueue.getQueue().toCompact())
                        putString("flutter.quiet_player_token", playerSession.playQueue.queueId)
                        putString("flutter.quiet_player_playlist_title", playerSession.playQueue.queueTitle)
                        putInt("flutter.quiet_player_play_mode", playerSession.playMode)
                        log { "save playlist ${playerSession.playQueue.queueId} with size ${playerSession.playQueue.getQueue().size}" }
                    } else log { "skip save playlist" }
                }.apply()
    }


    private fun String.fromCompact(): List<MusicMetadata> {
        if (isEmpty()) return emptyList()
        val sections = split('\n')
        if (sections.isEmpty()) return emptyList()
        return sections.mapNotNull {
            MusicMetadata.fromCompact(it)
        }.toList()
    }
    private fun List<MusicMetadata>.toCompact(): String {
        if (isEmpty()) return ""
        val builder = StringBuilder()
        forEach { it.toCompact(builder) }
        return builder.toString()
    }
}