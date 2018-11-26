import 'dart:io';
import 'dart:async';
import 'package:flutter/services.dart';

class BarcodeScanner {
  static const CameraAccessDenied = 'PERMISSION_NOT_GRANTED';
  static const MethodChannel _channel =
  const MethodChannel('com.apptreesoftware.barcode_scan');

  static Future<String> scan({ String storageDenied}) async {
    Map params = {};
    if (storageDenied != null && storageDenied.isNotEmpty) {
      params['storageDenied'] = storageDenied;
    }
    var res = await _channel.invokeMethod('scan', params);
    return res;
  }
}
