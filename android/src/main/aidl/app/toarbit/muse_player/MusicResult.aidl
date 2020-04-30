// MusicResult.aidl
package app.toarbit.muse_player;

import app.toarbit.muse_player.player.MusicMetadata;

interface MusicResult {
    void onResult(in MusicMetadata metadata);
}
