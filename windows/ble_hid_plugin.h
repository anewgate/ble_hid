#ifndef FLUTTER_PLUGIN_BLE_HID_PLUGIN_H_
#define FLUTTER_PLUGIN_BLE_HID_PLUGIN_H_

#include <flutter/method_channel.h>
#include <flutter/plugin_registrar_windows.h>

#include <memory>

namespace ble_hid {

class BleHidPlugin : public flutter::Plugin {
 public:
  static void RegisterWithRegistrar(flutter::PluginRegistrarWindows *registrar);

  BleHidPlugin();

  virtual ~BleHidPlugin();

  // Disallow copy and assign.
  BleHidPlugin(const BleHidPlugin&) = delete;
  BleHidPlugin& operator=(const BleHidPlugin&) = delete;

  // Called when a method is called on this plugin's channel from Dart.
  void HandleMethodCall(
      const flutter::MethodCall<flutter::EncodableValue> &method_call,
      std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result);
};

}  // namespace ble_hid

#endif  // FLUTTER_PLUGIN_BLE_HID_PLUGIN_H_
