import 'package:flutter_test/flutter_test.dart';
import 'package:ble_hid/ble_hid.dart';
import 'package:ble_hid/ble_hid_platform_interface.dart';
import 'package:ble_hid/ble_hid_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockBleHidPlatform
    with MockPlatformInterfaceMixin
    implements BleHidPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final BleHidPlatform initialPlatform = BleHidPlatform.instance;

  test('$MethodChannelBleHid is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelBleHid>());
  });

  test('getPlatformVersion', () async {
    BleHid bleHidPlugin = BleHid();
    MockBleHidPlatform fakePlatform = MockBleHidPlatform();
    BleHidPlatform.instance = fakePlatform;

    expect(await bleHidPlugin.getPlatformVersion(), '42');
  });
}
