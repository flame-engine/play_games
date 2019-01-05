import 'dart:async';
import 'dart:typed_data';
import 'dart:ui';

import 'package:flutter/services.dart';

const MethodChannel _channel = const MethodChannel('play_games');

class CloudSaveError implements Exception {
  String type;
  String message;
  CloudSaveError(this.type, this.message);
  @override
  String toString() => 'Error $type, message: $message';
}

class CloudSaveConflictError extends CloudSaveError {
  String conflictId;
  Snapshot local, server;
  CloudSaveConflictError(String type, String message, this.conflictId, this.local, this.server) : super(type, message);
}

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

class Snapshot {
  String content;
  Map<String, String> metadata;

  Snapshot.fromMap(Map data) {
    this.content = data['content'];
    this.metadata = (data['metadata'] as Map<dynamic, dynamic> ?? {}).cast<String, String>();
  }
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

  static Future<Snapshot> openSnapshot(String name) async {
    final Map<dynamic, dynamic> map = await _channel.invokeMethod('openSnapshot', { 'snapshotName': name });
    if (map['type'] != null && !map['type'].isEmpty) {
      if (map['type'] == 'SNAPSHOT_CONFLICT') {
        throw new CloudSaveConflictError(map['type'], map['message'], map['conflictId'], Snapshot.fromMap(map['local']), Snapshot.fromMap(map['server']));
      }
      throw new CloudSaveError(map['type'], map['message']);
    }
    return Snapshot.fromMap(map);
  }

  static Future<bool> saveSnapshot(String name, String content, { Map<String, String> metadata = const {} }) async {
    final Map<dynamic, dynamic> result = await _channel.invokeMethod('saveSnapshot', { 'snapshotName': name, 'content': content, 'metadata': metadata });
    if (result['type'] != null && !result['type'].isEmpty) {
      throw new CloudSaveError(result['type'], result['message']);
    }
    return result['status'] as bool;
  }

  static Future<Snapshot> resolveSnapshotConflict(String name, String conflictId, String content, { Map<String, String> metadata = const {} }) async {
    final Map<dynamic, dynamic> map = await _channel.invokeMethod('resolveSnapshotConflict', { 'snapshotName': name, 'conflictId': conflictId, 'content': content, 'metadata': metadata });
    if (map['type'] != null && !map['type'].isEmpty) {
      if (map['type'] == 'SNAPSHOT_CONFLICT') {
        throw new CloudSaveConflictError(map['type'], map['message'], map['conflictId'], Snapshot.fromMap(map['local']), Snapshot.fromMap(map['server']));
      }
      throw new CloudSaveError(map['type'], map['message']);
    }
    return Snapshot.fromMap(map);
  }

  static Future<SigninResult> signIn({ bool requestEmail = true, bool scopeSnapshot = false }) async {
    final Map<dynamic, dynamic> map = await _channel.invokeMethod('signIn', { 'requestEmail': requestEmail, 'scopeSnapshot' : scopeSnapshot });
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
