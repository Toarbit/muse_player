// MusicSessionCallback.aidl
package app.toarbit.muse_player;
import app.toarbit.muse_player.player.PlaybackState;
import app.toarbit.muse_player.player.MusicMetadata;
import app.toarbit.muse_player.player.PlayQueue;

interface MusicSessionCallback {

    void onPlaybackStateChanged(in PlaybackState state);

    void onMetadataChanged(in MusicMetadata metadata);

    void onPlayQueueChanged(in PlayQueue queue);

    void onPlayModeChanged(int playMode);

}