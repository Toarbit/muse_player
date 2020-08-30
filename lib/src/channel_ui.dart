import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:muse_player/src/player/music_player.dart';

import 'internal/meta.dart';
import 'internal/player_callback_adapter.dart';
import 'internal/serialization.dart';
import 'player/music_metadata.dart';
import 'player/play_mode.dart';
import 'player/play_queue.dart';
import 'player/playback_state.dart';
import 'player/transport_controls.dart';

/// MusicPlayer for UI interaction.
class MusicPlayer extends Player {

  static const _uiChannel = MethodChannel("app.toarbit.muse_player/player.ui");

  static MusicPlayer _player;

  /// 默认初始化为空，并通知 Platform段执行 init
  MusicPlayer._internal() : super() {
    _uiChannel.setMethodCallHandler(_handleRemoteCall);
    _uiChannel.invokeMethod("init");

    _queue.addListener(notifyListeners);
    _playMode.addListener(notifyListeners);
    _playbackState.addListener(notifyListeners);
    _metadata.addListener(notifyListeners);
  }

  factory MusicPlayer() {
    if (_player == null) {
      _player = MusicPlayer._internal();
    }
    return _player;
  }

  void setPlayQueue(@nonNull PlayQueue queue) {
    _uiChannel.invokeMethod("setPlayQueue", queue.toMap());
  }

  Future<MusicMetadata> getNextMusic(@nonNull MusicMetadata metadata) async {
    assert(metadata != null);
    final Map map = await _uiChannel.invokeMethod("getNext", metadata.toMap());
    return createMusicMetadata(map);
  }

  Future<MusicMetadata> getPreviousMusic(@nonNull MusicMetadata metadata) async {
    assert(metadata != null);
    final Map map = await _uiChannel.invokeMethod("getPrevious", metadata.toMap());
    return createMusicMetadata(map);
  }

  @override
  ValueListenable<PlayQueue> get queue => _queue;

  @override
  ValueListenable<PlaybackState> get playbackState => _playbackState;

  @override
  ValueListenable<PlayMode> get playMode => _playMode;

  @override
  ValueListenable<MusicMetadata> get metadata => _metadata;

  final ValueNotifier<PlayQueue> _queue = ValueNotifier(PlayQueue.empty());
  final ValueNotifier<PlaybackState> _playbackState = ValueNotifier(PlaybackState.none());
  final ValueNotifier<PlayMode> _playMode = ValueNotifier(PlayMode.sequence);
  final ValueNotifier<MusicMetadata> _metadata = ValueNotifier(null);

  Future<dynamic> _handleRemoteCall(MethodCall call) async {
    debugPrint("on MethodCall: ${call.method} args = ${call.arguments}");
    switch (call.method) {
      case 'onPlaybackStateChanged':
        _playbackState.value = createPlaybackState(call.arguments);
        break;
      case 'onMetadataChanged':
        _metadata.value = createMusicMetadata(call.arguments);
        break;
      case 'onPlayQueueChanged':
        _queue.value = createPlayQueue(call.arguments);
        break;
      case 'onPlayModeChanged':
        _playMode.value = PlayMode(call.arguments as int);
        break;
      default:
        throw UnimplementedError();
    }
  }

  @nonNull
  TransportControls transportControls = TransportControls(_uiChannel);

  void insertToNext(@nonNull MusicMetadata metadata) {
    assert(metadata != null);
    _uiChannel.invokeMethod("insertToNext", metadata.toMap());
  }

  void playWithQueue(@nonNull PlayQueue playQueue, {MusicMetadata metadata}) {
    assert(playQueue != null);
    setPlayQueue(playQueue);
    if (playQueue.isEmpty) {
      return;
    }
    metadata = metadata ?? playQueue.queue.first;
    transportControls.playFromMediaId(metadata.mediaId);
  }

  void removeMusicItem(MusicMetadata metadata) {}

  Future<int> getSleepTimer() async {
    final int time = (await _uiChannel.invokeMethod<int>("getSleepTimer") / 1000).floor();
    return time;
  }
  Future<bool> setSleepTimer(int time) {
    return _uiChannel.invokeMethod<bool>("setSleepTimer", time);
  }
}

class MusicPlayerValue {
  @nonNull
  final PlayQueue queue;

  @nonNull
  final PlayMode playMode;

  @nonNull
  final PlaybackState playbackState;

  @nullable
  final MusicMetadata metadata;

  MusicPlayerValue({
    this.queue,
    this.playMode,
    this.metadata,
    this.playbackState,
  }) : assert(queue != null);

  static final _empty = MusicPlayerValue(
    queue: PlayQueue.empty(),
    playMode: PlayMode.sequence,
    metadata: null,
    playbackState: PlaybackState.none(),
  );

  factory MusicPlayerValue.none() {
    return _empty;
  }
}
