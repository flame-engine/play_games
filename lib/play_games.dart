import 'dart:async';
import 'dart:typed_data';
import 'dart:ui';

import 'package:flutter/services.dart';

const MethodChannel _channel = const MethodChannel('play_games');

enum SigninResultType {
  SUCCESS,
  ERROR_SIGNIN,
  ERROR_FETCH_PLAYER_PROFILE,
  ERROR_NOT_SIGNED_IN,
  ERROR_IOS
}

SigninResultType _typeFromStr(String value) {
  return SigninResultType.values
      .firstWhere((e) => e.toString().split('.')[1] == value);
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
  String hiResImageUri;
  String iconImageUri;

  Future<Image> get hiResImage async =>
      await _fetchToMemory(await _channel.invokeMethod('getHiResImage'));
  Future<Image> get iconImage async =>
      await _fetchToMemory(await _channel.invokeMethod('getIconImage'));
}

Future<Image> _fetchToMemory(Map<dynamic, dynamic> result) {
  Uint8List bytes = result['bytes'];
  if (bytes == null) {
    print('was null, mate');
    return Future.value(null);
  }
  Completer<Image> completer = new Completer();
  decodeImageFromList(bytes, (image) => completer.complete(image));
  return completer.future;
}

class PlayGames {
  static Future<bool> unlockAchievementById(String id) async {
    return await _channel.invokeMethod('unlockAchievementById', {'id': id});
  }

  static Future<bool> unlockAchievementByName(String name) async {
    return await _channel
        .invokeMethod('unlockAchievementByName', {'name': name});
  }

  static Future<bool> incrementAchievementById(String id,
      [int amount = 1]) async {
    return await _channel
        .invokeMethod('incrementAchievementById', {'id': id, 'amount': amount});
  }

  static Future<bool> incrementAchievementByName(String name,
      [int amount = 1]) async {
    return await _channel.invokeMethod(
        'incrementAchievementByName', {'name': name, 'amount': amount});
  }

  // TODO better way to set gravity
  static Future<bool> setPopupOptions(
      {bool show = true, int gravity = 49}) async {
    return await _channel
        .invokeMethod('setPopupOptions', {'show': show, 'gravity': gravity});
  }

  static Future<bool> showAchievements() async {
    final Map<dynamic, dynamic> map =
        await _channel.invokeMethod('showAchievements');
    return map['closed'];
  }

  static Future<SigninResult> signIn() async {
    final Map<dynamic, dynamic> map = await _channel.invokeMethod('signIn');
    SigninResultType type = _typeFromStr(map['type']);
    SigninResult result = new SigninResult()..type = type;
    if (type == SigninResultType.SUCCESS) {
      result.account = new Account()
        ..id = map['id']
        ..displayName = map['displayName']
        ..email = map['email']
        ..hiResImageUri = map['hiResImageUri']
        ..iconImageUri = map['iconImageUri'];
    } else {
      result.message = map['message'];
    }
    return result;
  }
}
