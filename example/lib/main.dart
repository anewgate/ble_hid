import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class BleHid {
  static const _method = MethodChannel('ble_hid');
  static const _events = EventChannel('ble_hid/events');

  static Stream<Map<dynamic, dynamic>> listen() =>
      _events.receiveBroadcastStream().map((e) => Map<dynamic, dynamic>.from(e));

  static Future<void> enable() => _method.invokeMethod('enable');
  static Future<void> disable() => _method.invokeMethod('disable');
  static Future<void> requestFocus() => _method.invokeMethod('requestFocus');
  static Future<void> setDeadZone(double dz) =>
      _method.invokeMethod('setDeadZone', {'deadZone': dz});
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _bleHidPlugin = BleHid();
  late final StreamSubscription sub;
  String hidInput = '';

  @override
  void initState() {
    super.initState();
    printInputs();
  }

  void printInputs(){
    BleHid.enable();
    sub = BleHid.listen().listen((e) {
      if (e['type'] == 'axis') {
        // e['lx'], e['ly'], e['rx'], e['ry'], e['lt'], e['rt'], e['hatX'], e['hatY']
        debugPrint('-----${e.toString()}');
        setState(() {
          hidInput = e.toString();
        });
      } else if (e['type'] == 'key') {
        // e['action'] == 'down'|'up', e['keyCode'], e['keyName']
        debugPrint('-----${e.toString()}');
        setState(() {
          hidInput = e.toString();
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('BLE HID Plugin example app'),
        ),
        body: Center(
          child: Text('HID input:$hidInput\n'),
        ),
      ),
    );
  }
}
