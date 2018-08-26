import 'dart:async';

import 'package:flutter/services.dart';

class PlayGames {
  static const MethodChannel _channel =
      const MethodChannel('play_games');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
