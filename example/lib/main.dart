import 'package:flutter/material.dart';
import 'package:barcode_scan/barcode_scan.dart';


void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';


  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: const Text('Plugin example app'),
        ),
        body: new Center(
          child: new Text('Running on: $_platformVersion\n'),
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () async {
            var res = await BarcodeScanner.scan();
            print("[qrcode] $res");
          },
        ),
      ),
    );
  }
}
