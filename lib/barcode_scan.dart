import 'dart:io';
import 'dart:async';
import 'package:flutter/services.dart';

class BarcodeScanner {
  static const CameraAccessDenied = 'PERMISSION_NOT_GRANTED';
  static const AndroidStorageDenied = 'ANDROID_STORAGE_PERMISSION_NOT_GRANTED';
  static const EventChannel _eventChannel;
  static const MethodChannel _channel =
      const MethodChannel('com.apptreesoftware.barcode_scan');
  static const List<Function> _storageDenied = [];

  static addPermissionDeniedHandler(Function fn) {
    _storageDenied.add(fn);
  }

  static removePermissionDeniedHandler(Function fn) {
    _storageDenied.remove(fn);
  }

  static Future<String> scan() async {
    if (Platform.isAndroid) {
      if (_eventChannel == null) {
        _eventChannel = const EventChannel("com.apptreesoftware.barcode_scan/event")
      }
      _eventChannel.receiveBroadcastStream().listen((v) {
        if (v == AndroidStorageDenied) {
          _storageDenied.forEach((fn) => fn(v));
        }
      });
    }
    var res = await _channel.invokeMethod('scan');
    return res;
  }
}
