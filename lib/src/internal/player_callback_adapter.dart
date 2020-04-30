import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:muse_player/src/channel_ui.dart';
import 'package:muse_player/src/player/music_metadata.dart';
import 'package:muse_player/src/player/music_player_callback.dart';
import 'package:muse_player/src/player/play_mode.dart';
import 'package:muse_player/src/player/play_queue.dart';
import 'package:muse_player/src/player/playback_state.dart';

import 'ext_copy.dart';
import 'serialization.dart';

/// 处理与 [MusicPlayerValue] 数据变动相关的事件
/// 通过 [MusicPlayerValueCopy] 来合并变更后的数据
mixin ChannelPlayerCallbackAdapter on ValueNotifier<MusicPlayerValue> implements MusicPlayerCallback {
  bool handleRemoteCall(MethodCall call) {
    switch (call.method) {
      case 'onPlaybackStateChanged':
        onPlaybackStateChanged(createPlaybackState(call.arguments));
        break;
      case 'onMetadataChanged':
        onMetadataChange(createMusicMetadata(call.arguments));
        break;
      case 'onPlayQueueChanged':
        onPlayQueueChanged(createPlayQueue(call.arguments));
        break;
      case 'onPlayModeChanged':
        onPlayModeChanged(PlayMode(call.arguments as int));
        break;
      default:
        return false;
    }
    return true;
  }

  @override
  void onMetadataChange(MusicMetadata metadata) {
    value = value.copy(metadata: metadata);
  }

  @override
  void onPlayModeChanged(PlayMode playMode) {
    value = value.copy(playMode: playMode);
  }

  @override
  void onPlayQueueChanged(PlayQueue queue) {
    value = value.copy(queue: queue);
  }

  @override
  void onPlaybackStateChanged(PlaybackState state) {
    value = value.copy(state: state);
  }
}
