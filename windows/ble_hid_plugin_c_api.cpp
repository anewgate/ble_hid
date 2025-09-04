#include "include/ble_hid/ble_hid_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "ble_hid_plugin.h"

void BleHidPluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  ble_hid::BleHidPlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
