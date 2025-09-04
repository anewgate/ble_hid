import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'ble_hid_platform_interface.dart';

/// An implementation of [BleHidPlatform] that uses method channels.
class MethodChannelBleHid extends BleHidPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('ble_hid');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
