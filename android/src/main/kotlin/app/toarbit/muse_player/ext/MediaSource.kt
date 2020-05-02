package app.toarbit.muse_player.ext

import android.content.Context
import android.net.Uri
import app.toarbit.muse_player.MusicPlayerServicePlugin
import app.toarbit.muse_player.player.MusicMetadata
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Created by omit on 2020/4/30 for muse_player.
 */
const val MAX_CACHE_SIZE: Long = 512 * 1024 * 1024

internal fun MusicMetadata.toMediaSource(
        context: Context,
        servicePlugin: MusicPlayerServicePlugin
): ProgressiveMediaSource? {
    var factory: DataSource.Factory = DefaultDataSourceFactory(
            context,
            servicePlugin.config.userAgent
                    ?: Util.getUserAgent(context, context.applicationInfo.name), null
    )
    factory = UrlUpdatingDataSource.Factory(factory, servicePlugin)
    if (servicePlugin.config.enableCache) {
        val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
        val databaseProvider = ExoDatabaseProvider(context)
        factory = CacheDataSourceFactory(SimpleCache(File(context.cacheDir, "Media"), evictor, databaseProvider), factory)
    }
    return ProgressiveMediaSource.Factory(factory)
            .setCustomCacheKey(mediaId)
            .createMediaSource(buildMediaUri(this))
}


private const val SCHEME = "muse"

/**
 * Build a Decorated Uri for this Media.
 * this uri will handle in [UrlUpdatingDataSource]
 */
private fun buildMediaUri(metadata: MusicMetadata): Uri {
    return Uri.Builder()
            .scheme(SCHEME)
            .path("player")
            .appendQueryParameter("id", metadata.mediaId)
            .appendQueryParameter("uri", metadata.mediaUri ?: "")
            .build()
}

/**
 * auto update url when we read data from dataSource
 */
private class UrlUpdatingDataSource(
        private val dataSource: DataSource,
        private val backgroundChannel: MusicPlayerServicePlugin
) : DataSource by dataSource {

    class Factory(
            private val factory: DataSource.Factory,
            private val backgroundChannel: MusicPlayerServicePlugin
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return UrlUpdatingDataSource(
                    factory.createDataSource(),
                    backgroundChannel
            )
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        val former = dataSpec.uri
        return if (former.scheme == SCHEME) {
            val id = former.getQueryParameter("id")!!
            val fallback = former.getQueryParameter("uri")
            val uri = runBlocking {
                val playUrl = backgroundChannel.getPlayUrl(id, fallback)
                if ("asset".equals(playUrl.scheme, true)) {
                    return@runBlocking Uri.parse("asset:///flutter_assets${playUrl.path}")
                } else {
                    playUrl
                }
            }
            val newSpec = dataSpec.withUri(uri)
            dataSource.open(newSpec)
        } else {
            dataSource.open(dataSpec)
        }
    }


    override fun getUri(): Uri? {
        return dataSource.uri
    }


    override fun close() {
        dataSource.close()
    }

}