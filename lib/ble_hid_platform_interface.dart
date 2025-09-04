import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'ble_hid_method_channel.dart';

abstract class BleHidPlatform extends PlatformInterface {
  /// Constructs a BleHidPlatform.
  BleHidPlatform() : super(token: _token);

  static final Object _token = Object();

  static BleHidPlatform _instance = MethodChannelBleHid();

  /// The default instance of [BleHidPlatform] to use.
  ///
  /// Defaults to [MethodChannelBleHid].
  static BleHidPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [BleHidPlatform] when
  /// they register themselves.
  static set instance(BleHidPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
