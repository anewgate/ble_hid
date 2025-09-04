package com.harftware.ble_hid

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.*
import android.view.KeyEvent.*
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlin.math.abs

/** BleHidPlugin
 *  - HID(Gamepad/Joystick/DPAD/일부 Mouse) 축/키 입력을 수집해 Flutter로 스트리밍 전달
 */
class BleHidPlugin :
    FlutterPlugin,
    MethodChannel.MethodCallHandler,
    ActivityAware {
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var eventSink: EventChannel.EventSink? = null
    private var appContext: Context? = null
    private var activity: Activity? = null
    private var gamepadView: GamepadInputView? = null
    private var originalWindowCallback: Window.Callback? = null
    private var enabled: Boolean = true

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        methodChannel = MethodChannel(binding.binaryMessenger, "ble_hid")
        methodChannel.setMethodCallHandler(this)

        eventChannel = EventChannel(binding.binaryMessenger, "ble_hid/events")
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(args: Any?, sink: EventChannel.EventSink?) {
                eventSink = sink
            }

            override fun onCancel(args: Any?) {
                eventSink = null
            }
        })
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        appContext = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        attachInputViewAndInterceptors()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        detachInputViewAndInterceptors()
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        attachInputViewAndInterceptors()
    }

    override fun onDetachedFromActivity() {
        detachInputViewAndInterceptors()
        activity = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getPlatformVersion" -> result.success("Android ${android.os.Build.VERSION.RELEASE}")
            "enable" -> {
                enabled = true
                requestFocusInternal()
                result.success(null)
            }

            "disable" -> {
                enabled = false
                result.success(null)
            }

            "requestFocus" -> {
                requestFocusInternal()
                result.success(null)
            }

            "setDeadZone" -> {
                val dz = (call.argument<Number>("deadZone") ?: 0.10).toFloat()
                gamepadView?.deadZone = dz
                result.success(null)
            }

            else -> result.notImplemented()
        }
    }

    private fun attachInputViewAndInterceptors() {
        val act = activity ?: return
        if (gamepadView == null) {
            gamepadView = GamepadInputView(act).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                onAxisChanged = { s, t, h ->
                    if (enabled) {
                        eventSink?.success(
                            mapOf(
                                "type" to "axis",
                                "lx" to s.lx, "ly" to s.ly,
                                "rx" to s.rx, "ry" to s.ry,
                                "lt" to t.lt, "rt" to t.rt,
                                "hatX" to h.x, "hatY" to h.y
                            )
                        )
                    }
                }
                onKeyEvent = { actionDown, keyCode, keyName ->
                    if (enabled) {
                        eventSink?.success(
                            mapOf(
                                "type" to "key",
                                "action" to if (actionDown) "down" else "up",
                                "keyCode" to keyCode,
                                "keyName" to keyName
                            )
                        )
                    }
                }
            }
            (act.window.decorView as? ViewGroup)?.addView(
                gamepadView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            requestFocusInternal()
        }

        if (originalWindowCallback == null) {
            val w = act.window
            originalWindowCallback = w.callback
            w.callback = object : Window.Callback by (originalWindowCallback ?: w.callback) {
                override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
                    if (gamepadView?.onGenericMotionEvent(ev) == true) return true
                    return originalWindowCallback?.dispatchGenericMotionEvent(ev) ?: false
                }

                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (gamepadView?.dispatchKeyEvent(event) == true) return true
                    return originalWindowCallback?.dispatchKeyEvent(event) ?: false
                }
            }
        }
    }

    private fun detachInputViewAndInterceptors() {
        val act = activity ?: return
        (act.window.decorView as? ViewGroup)?.let { root ->
            gamepadView?.let { v -> root.removeView(v) }
        }
        gamepadView = null
        originalWindowCallback?.let { orig ->
            act.window.callback = orig
        }
        originalWindowCallback = null
    }

    private fun requestFocusInternal() {
        gamepadView?.post {
            gamepadView?.requestFocus()
        }
    }

    data class Sticks(
        val lx: Float = 0f,
        val ly: Float = 0f,
        val rx: Float = 0f,
        val ry: Float = 0f
    )

    data class Triggers(val lt: Float = 0f, val rt: Float = 0f)
    data class Hat(val x: Int = 0, val y: Int = 0)

    private class GamepadInputView(context: Context) : View(context) {

        var onAxisChanged: ((Sticks, Triggers, Hat) -> Unit)? = null
        var onKeyEvent: ((Boolean, Int, String) -> Unit)? = null // (down?, keyCode, keyName)

        var deadZone: Float = 0.10f

        private var sticks = Sticks()
        private var triggers = Triggers()
        private var hat = Hat()

        override fun onGenericMotionEvent(event: MotionEvent): Boolean {
            val isGameLike =
                event.isFromSource(InputDevice.SOURCE_JOYSTICK) ||
                        event.isFromSource(InputDevice.SOURCE_GAMEPAD) ||
                        event.isFromSource(InputDevice.SOURCE_DPAD) ||
                        event.isFromSource(InputDevice.SOURCE_MOUSE) ||
                        ((event.source and InputDevice.SOURCE_CLASS_JOYSTICK) == InputDevice.SOURCE_CLASS_JOYSTICK)

            if (!isGameLike) return super.onGenericMotionEvent(event)

            fun axis(e: MotionEvent, ax: Int): Float {
                val v = e.getAxisValue(ax)
                return if (abs(v) < deadZone) 0f else v
            }

            val lx = axis(event, MotionEvent.AXIS_X)
            val ly = -axis(event, MotionEvent.AXIS_Y)
            val rx = axis(event, MotionEvent.AXIS_RX)
            val ry = -axis(event, MotionEvent.AXIS_RY)

            val ltRaw = when {
                event.getAxisValue(MotionEvent.AXIS_LTRIGGER) != 0f -> event.getAxisValue(
                    MotionEvent.AXIS_LTRIGGER
                )

                event.getAxisValue(MotionEvent.AXIS_Z) != 0f -> event.getAxisValue(MotionEvent.AXIS_Z)
                event.getAxisValue(MotionEvent.AXIS_BRAKE) != 0f -> -event.getAxisValue(MotionEvent.AXIS_BRAKE)
                else -> 0f
            }
            val rtRaw = when {
                event.getAxisValue(MotionEvent.AXIS_RTRIGGER) != 0f -> event.getAxisValue(
                    MotionEvent.AXIS_RTRIGGER
                )

                event.getAxisValue(MotionEvent.AXIS_RZ) != 0f -> event.getAxisValue(MotionEvent.AXIS_RZ)
                event.getAxisValue(MotionEvent.AXIS_GAS) != 0f -> event.getAxisValue(MotionEvent.AXIS_GAS)
                else -> 0f
            }
            val lt = if (ltRaw < deadZone) 0f else ltRaw.coerceIn(0f, 1f)
            val rt = if (rtRaw < deadZone) 0f else rtRaw.coerceIn(0f, 1f)
            val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X).toInt().coerceIn(-1, 1)
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y).toInt().coerceIn(-1, 1)
            val lx2 = if (lx == 0f && hatX != 0) hatX.toFloat() else lx
            val ly2 = if (ly == 0f && hatY != 0) (-hatY).toFloat() else ly
            sticks = Sticks(lx2, ly2, rx, ry)
            triggers = Triggers(lt, rt)
            hat = Hat(hatX, hatY)
            onAxisChanged?.invoke(sticks, triggers, hat)
            return true
        }

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
            if (isGamepadEvent(event)) {
                onKeyEvent?.invoke(true, keyCode, KeyEvent.keyCodeToString(keyCode))
                applyDpadFromKey(keyCode, pressed = true)
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
            if (isGamepadEvent(event)) {
                onKeyEvent?.invoke(false, keyCode, KeyEvent.keyCodeToString(keyCode))
                applyDpadFromKey(keyCode, pressed = false)
                return true
            }
            return super.onKeyUp(keyCode, event)
        }

        private fun isGamepadEvent(event: KeyEvent): Boolean {
            val src = event.source
            return (src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
                    (src and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        }

        private fun applyDpadFromKey(keyCode: Int, pressed: Boolean) {
            when (keyCode) {
                KEYCODE_DPAD_LEFT -> hat = hat.copy(x = if (pressed) -1 else 0)
                KEYCODE_DPAD_RIGHT -> hat = hat.copy(x = if (pressed) 1 else 0)
                KEYCODE_DPAD_UP -> hat = hat.copy(y = if (pressed) -1 else 0)
                KEYCODE_DPAD_DOWN -> hat = hat.copy(y = if (pressed) 1 else 0)
                else -> return
            }
            onAxisChanged?.invoke(sticks, triggers, hat)
        }
    }
}
