// MusicPlayerSession.aidl
package app.toarbit.muse_player;

import app.toarbit.muse_player.MusicSessionCallback;
import app.toarbit.muse_player.MusicResult;
import app.toarbit.muse_player.player.PlaybackState;
import app.toarbit.muse_player.player.MusicMetadata;
import app.toarbit.muse_player.player.PlayQueue;

interface MusicPlayerSession {


    void addCallback(in MusicSessionCallback callback);

    void removeCallback(in MusicSessionCallback callback);

    void destroy();

    // Transport Controls

    /**
     * Request that the player start its playback at its current position.
     */
    void play();


    void playFromMediaId(String mediaId);


    void pause();


    void stop();

    /**
     * Moves to a new location in the media stream.
     *
     * @param pos Position to move to, in milliseconds.
     */
    void seekTo(long pos);


    void skipToNext();

    void skipToPrevious();

    void setPlayMode(int playMode);

    void addMetadata(in MusicMetadata metadata, in String anchorMediaId);

    void removeMetadata(in String mediaId);

    /**
     * Update current play queue
     */
    void setPlayQueue(in PlayQueue queue);

    void getNext(in MusicMetadata anchor, in MusicResult result);

    void getPrevious(in MusicMetadata anchor, in MusicResult result);

    MusicMetadata getCurrent();

    PlayQueue getPlayQueue();

    PlaybackState getPlaybackState();

    int getPlayMode();

    void setPlaybackSpeed(double speed);
}
