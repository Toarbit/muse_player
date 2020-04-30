import 'package:flutter/services.dart';
import 'package:muse_player/muse_player.dart';

import '../main.dart';

class ExamplePlayQueueInterceptor extends PlayQueueInterceptor {
  @override
  Future<List<MusicMetadata>> fetchMoreMusic(BackgroundPlayQueue queue, PlayMode playMode) async {
    if (queue.queueId == "fm" && queue.queue.length == 1) {
      return medias.getRange(1, 3).toList();
    }
    throw MissingPluginException();
  }
}
