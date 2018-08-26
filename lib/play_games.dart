import 'dart:async';

import 'package:flutter/services.dart';

enum SigninResultType {
  SUCCESS,
  ERROR,
  IOS
}

class SigninResult {
  SigninResultType type;
  Account account;
  String messages;
}

class Account {
  String email;
}

class PlayGames {
  static const MethodChannel _channel =
      const MethodChannel('play_games');

  static Future<SigninResult> signIn() async {
    final String result = await _channel.invokeMethod('signIn');
    print('----------------');
    print(result);
    print('----------------');
    return null;
  }
}
