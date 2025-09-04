
import 'ble_hid_platform_interface.dart';

class BleHid {
  Future<String?> getPlatformVersion() {
    return BleHidPlatform.instance.getPlatformVersion();
  }
}
