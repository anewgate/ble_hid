//
//  Generated file. Do not edit.
//

// clang-format off

#include "generated_plugin_registrant.h"

#include <ble_hid/ble_hid_plugin.h>

void fl_register_plugins(FlPluginRegistry* registry) {
  g_autoptr(FlPluginRegistrar) ble_hid_registrar =
      fl_plugin_registry_get_registrar_for_plugin(registry, "BleHidPlugin");
  ble_hid_plugin_register_with_registrar(ble_hid_registrar);
}
