import 'dart:async';
import 'dart:typed_data';
import 'dart:ui';

import 'package:flutter/services.dart';

const MethodChannel _channel = const MethodChannel('play_games');

enum SigninResultType {
  SUCCESS,
  ERROR_SIGNIN,
  ERROR_FETCH_PLAYER_PROFILE,
  ERROR_IOS
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
  String hiResImageUri;
  String iconImageUri;

  Future<Image> get hiResImage async => await _fetchToMemory(await _channel.invokeMethod('getHiResImage'));
  Future<Image> get iconImage async => await _fetchToMemory(await _channel.invokeMethod('getIconImage'));
 
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
