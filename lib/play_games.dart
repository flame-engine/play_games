import 'dart:async';

import 'package:flutter/services.dart';

enum SigninResultType {
  SUCCESS,
  ERROR,
  IOS
}

SigninResultType _typeFromStr(String value) {
  return SigninResultType.values.firstWhere((e) => e.toString().split('.')[1] == value);
}

class SigninResult {
  SigninResultType type;
  Account account;
  String message;

  bool get success => type == SigninResultType.SUCCESS;
}

class Account {
  String id;
  String displayName;
  String email;
  String avatar;
}

class PlayGames {
  static const MethodChannel _channel = const MethodChannel('play_games');

  static Future<SigninResult> signIn() async {
    final Map<dynamic, dynamic> map = await _channel.invokeMethod('signIn');
    SigninResultType type = _typeFromStr(map['type']);
    SigninResult result = new SigninResult()..type = type;
    if (type == SigninResultType.SUCCESS) {
      result.account = new Account()
        ..id = map['id']
        ..displayName = map['displayName']
        ..email = map['email']
        ..avatar = map['avatar'];
    } else {
      result.message = map['message'];
    }
    return result;
  }
}
