package app.toarbit.muse_player.utils

import app.toarbit.muse_player.MusicPlayerSession
import app.toarbit.muse_player.MusicResult
import app.toarbit.muse_player.player.MusicMetadata
import kotlinx.coroutines.CompletableDeferred

/**
 * Created by omit on 2020/4/24 for muse_player.
 */

private class FutureMusicResult : MusicResult.Stub() {

    private val future = CompletableDeferred<MusicMetadata?>()

    override fun onResult(metadata: MusicMetadata?) {
        future.complete(metadata)
    }

    suspend fun await(): MusicMetadata? {
        return future.await()
    }

}

suspend fun MusicPlayerSession.getNext(anchor: MusicMetadata?): MusicMetadata? {
    val result = FutureMusicResult()
    getNext(anchor, result)
    return result.await()
}


suspend fun MusicPlayerSession.getPrevious(anchor: MusicMetadata?): MusicMetadata? {
    return FutureMusicResult().apply { getPrevious(anchor, this) }.await()
}