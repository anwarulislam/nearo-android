/*
 * SPDX-FileCopyrightText: 2014 Ahmed I. Khalil <ahmedibrahimkhali@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.mousepad

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.kde.kdeconnect.KdeConnect
import org.kde.kdeconnect.base.BaseActivity
import org.kde.kdeconnect.ui.PluginSettingsActivity
import org.kde.kdeconnect.ui.compose.components.addDeviceDisconnectedOverlay
import org.kde.kdeconnect.ui.showDeviceStatus
import org.kde.kdeconnect_tp.R
import org.kde.kdeconnect_tp.databinding.ActivityMousepadBinding
import kotlin.math.abs
import kotlin.math.pow

class MousePadActivity :
    BaseActivity<ActivityMousepadBinding>(),
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    MousePadGestureDetector.OnGestureListener,
    SensorEventListener,
    SharedPreferences.OnSharedPreferenceChangeListener,
    OnApplyWindowInsetsListener {

    private lateinit var plugin: MousePadPlugin

    private var mPrevX: Float = 0f
    private var mPrevY: Float = 0f
    private var dragging: Boolean = false
    private var maybeDragging: Boolean = false
    private var accumulatedDragDistance2: Float = 0.0f
    private var mCurrentSensitivity: Float = 1.0f
    private var displayDpiMultiplier: Float = 1.0f
    private var scrollDirection: Int = 1
    private var scrollCoefficient: Double = 1.0
    private var allowGyro: Boolean = false
    private var gyroEnabled: Boolean = false
    private var doubleTapDragEnabled: Boolean = false
    private var gyroscopeSensitivity: Int = 100
    private var isScrolling: Boolean = false
    private var accumulatedDistanceX: Double = 0.0
    private var accumulatedDistanceY: Double = 0.0
    private var pendingShowKeyboard: Boolean = false
    private var keyboardShown: Boolean = false
    private var lastTextFocusToken = ""
    private val modifiers = linkedSetOf<String>()
    private val keyboardDismissals by lazy { getSharedPreferences("remoteKeyboardDismissals", MODE_PRIVATE) }
    private var watchingTextFocus = false

    private lateinit var mDetector: GestureDetector
    private var mSensorManager: SensorManager? = null
    private lateinit var mMousePadGestureDetector: MousePadGestureDetector
    private lateinit var mPointerAccelerationProfile: PointerAccelerationProfile

    private val mouseDelta = PointerAccelerationProfile.MouseDelta() // to be reused on every touch move event

    private lateinit var keyListenerView: KeyListenerView

    private var prefsApplied: Boolean = false

    override val binding: ActivityMousepadBinding by lazy {
        ActivityMousepadBinding.inflate(layoutInflater)
    }

    private enum class ClickType {
        LEFT,
        RIGHT,
        MIDDLE,
        NONE;

        companion object {
            fun fromString(s: String?): ClickType {
                return when (s) {
                    "left" -> LEFT
                    "right" -> RIGHT
                    "middle" -> MIDDLE
                    else -> NONE
                }
            }
        }
    }

    private var singleTapAction: ClickType = ClickType.NONE
    private var doubleTapAction: ClickType = ClickType.NONE
    private var tripleTapAction: ClickType = ClickType.NONE

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        val values = event.values

        var x = -values[2] * 70 * (gyroscopeSensitivity / 100.0f)
        var y = -values[0] * 70 * (gyroscopeSensitivity / 100.0f)

        if (x < 0.25 && x > -0.25) {
            x = 0f
        } else {
            x *= (gyroscopeSensitivity / 100.0f)
        }

        if (y < 0.25 && y > -0.25) {
            y = 0f
        } else {
            y *= (gyroscopeSensitivity / 100.0f)
        }

        plugin.sendMouseDelta(x, y)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        requireNotNull(supportActionBar).setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.mouseClickLeft.setOnClickListener { sendLeftClick() }
        binding.mouseClickMiddle.setOnClickListener { sendMiddleClick() }
        binding.mouseClickRight.setOnClickListener { sendRightClick() }

        plugin = KdeConnect.getInstance().getDevicePlugin<MousePadPlugin>(intent.getStringExtra(EXTRA_DEVICE_ID))
            ?: return finish()
        binding.root.addDeviceDisconnectedOverlay(plugin.device)
        showDeviceStatus(plugin.device)

        window.decorView.isHapticFeedbackEnabled = true

        mDetector = GestureDetector(this, this)
        mMousePadGestureDetector = MousePadGestureDetector(this)
        mDetector.setOnDoubleTapListener(this)
        mSensorManager = getSystemService<SensorManager>()

        binding.scrollRail.sendScroll = { dy -> sendScroll(0.0, scrollDirection * dy) }
        binding.spacePrevious.setOnClickListener { sendSpaceShortcut(KeyEvent.KEYCODE_DPAD_LEFT) }
        binding.spaceOverview.setOnClickListener { sendSpaceShortcut(KeyEvent.KEYCODE_DPAD_UP) }
        binding.spaceNext.setOnClickListener { sendSpaceShortcut(KeyEvent.KEYCODE_DPAD_RIGHT) }

        keyListenerView = binding.keyListener
        keyListenerView.setOnTouchListener { _, event -> handleTrackpadTouch(event) }
        lastTextFocusToken = savedInstanceState?.getString("lastTextFocusToken").orEmpty()
        setupShortcutKeys()
        keyListenerView.setDeviceId(plugin.device.deviceId)
        ViewCompat.setOnApplyWindowInsetsListener(keyListenerView, this)

        plugin.preferences.registerOnSharedPreferenceChangeListener(this)

        applyPrefs()

        // Technically xdpi and ydpi should be handled separately,
        // but since ydpi is usually almost equal to xdpi, only xdpi is used for the multiplier.
        displayDpiMultiplier = STANDARD_DPI / resources.displayMetrics.xdpi

        val decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                var fullscreenType = 0

                fullscreenType = fullscreenType or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                fullscreenType = fullscreenType or View.SYSTEM_UI_FLAG_FULLSCREEN
                fullscreenType = fullscreenType or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

                window.decorView.systemUiVisibility = fullscreenType
            }
        }
    }

    override fun onResume() {
        applyPrefs()

        if (allowGyro && !gyroEnabled) {
            mSensorManager?.let {
                it.registerListener(this, it.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME)
            }
            gyroEnabled = true
        }

        invalidateOptionsMenu()

        super.onResume()
        if (!::plugin.isInitialized) return
        watchingTextFocus = true
        plugin.startTextFocus { token ->
            if (token == lastTextFocusToken) return@startTextFocus
            lastTextFocusToken = token
            if (token.isNotEmpty() && token != keyboardDismissals.getString(plugin.device.deviceId, null)) {
                if (keyboardDismissals.contains(plugin.device.deviceId)) {
                    keyboardDismissals.edit().remove(plugin.device.deviceId).apply()
                }
                if (hasWindowFocus()) showKeyboard() else pendingShowKeyboard = true
            } else {
                pendingShowKeyboard = false
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && pendingShowKeyboard) {
            showKeyboard()
            pendingShowKeyboard = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("lastTextFocusToken", lastTextFocusToken)
    }

    override fun onPause() {
        watchingTextFocus = false
        if (::plugin.isInitialized) plugin.stopTextFocus()
        pendingShowKeyboard = false
        clearShortcutKeys()
        if (gyroEnabled) {
            mSensorManager?.unregisterListener(this)
            gyroEnabled = false
        }
        super.onPause()
    }

    override fun onStop() {
        if (gyroEnabled) {
            mSensorManager?.unregisterListener(this)
            gyroEnabled = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (::plugin.isInitialized) {
            plugin.preferences.unregisterOnSharedPreferenceChangeListener(this)
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!::plugin.isInitialized) {
            return false
        }

        menuInflater.inflate(R.menu.menu_mousepad, menu)

        val mouseButtonsEnabled = plugin.preferences.getBoolean(getString(R.string.mousepad_mouse_buttons_enabled_pref), true)

        menu.findItem(R.id.menu_right_click).isVisible = !mouseButtonsEnabled
        menu.findItem(R.id.menu_middle_click).isVisible = !mouseButtonsEnabled
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_right_click -> {
                sendRightClick()
                true
            }
            R.id.menu_middle_click -> {
                sendMiddleClick()
                true
            }
            R.id.menu_open_mousepad_settings -> {
                val intent = PluginSettingsActivity.createIntent(this, plugin.device.deviceId, MousePadPlugin::class.java.simpleName)
                startActivity(intent)
                true
            }
            R.id.menu_show_keyboard -> {
                if (plugin.isKeyboardEnabled) {
                    toggleKeyboard()
                } else {
                    Toast.makeText(this, R.string.mousepad_keyboard_input_not_supported, Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menu_open_compose_send -> {
                if (plugin.isKeyboardEnabled) {
                    showCompose()
                } else {
                    Toast.makeText(this, R.string.mousepad_keyboard_input_not_supported, Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleTrackpadTouch(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) isScrolling = false
        if (mMousePadGestureDetector.onTouchEvent(event)) {
            return true
        }
        if (mDetector.onTouchEvent(event)) {
            return true
        }

        val actionType = event.action

        if (isScrolling) {
            if (actionType == MotionEvent.ACTION_UP || actionType == MotionEvent.ACTION_CANCEL) {
                isScrolling = false
            } else {
                return false
            }
        }


        when (actionType) {
            MotionEvent.ACTION_DOWN -> {
                mPrevX = event.x
                mPrevY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val mCurrentX = event.x
                val mCurrentY = event.y

                val deltaX = (mCurrentX - mPrevX) * displayDpiMultiplier * mCurrentSensitivity
                val deltaY = (mCurrentY - mPrevY) * displayDpiMultiplier * mCurrentSensitivity

                if (maybeDragging) {
                    accumulatedDragDistance2 += deltaX * deltaX + deltaY * deltaY
                    if (accumulatedDragDistance2 >= MIN_DRAGGING_DISTANCE_SQUARED) {
                        maybeDragging = false
                        dragging = true
                        accumulatedDragDistance2 = 0.0f
                        plugin.sendSingleHold()
                    }
                } else {
                    // Run the mouse delta through the pointer acceleration profile
                    mPointerAccelerationProfile.touchMoved(deltaX, deltaY, event.eventTime)
                    mPointerAccelerationProfile.commitAcceleratedMouseDelta(mouseDelta)

                    plugin.sendMouseDelta(mouseDelta.x, mouseDelta.y)
                }

                mPrevX = mCurrentX
                mPrevY = mCurrentY
            }
            MotionEvent.ACTION_UP -> {
                if (doubleTapDragEnabled && maybeDragging) {
                    maybeDragging = false
                    accumulatedDragDistance2 = 0.0f
                    plugin.sendDoubleClick()
                }
            }
        }
        return true
    }

    override fun onDown(e: MotionEvent): Boolean = false

    override fun onShowPress(e: MotionEvent) {
        // From GestureDetector, left empty
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean = false

    override fun onGenericMotionEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_SCROLL) {
            val distanceY = e.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val distanceX = e.getAxisValue(MotionEvent.AXIS_HSCROLL)

            accumulatedDistanceX += distanceX
            accumulatedDistanceY += distanceY

            if (abs(accumulatedDistanceX) > MIN_DISTANCE_TO_SEND_SCROLL || abs(accumulatedDistanceY) > MIN_DISTANCE_TO_SEND_SCROLL) {
                sendScroll(accumulatedDistanceX, accumulatedDistanceY)
                accumulatedDistanceX = 0.0
                accumulatedDistanceY = 0.0
            }
        }

        return super.onGenericMotionEvent(e)
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        // If only one thumb is used then cancel the scroll gesture
        if (e2.pointerCount <= 1) {
            return false
        }

        isScrolling = true

        var filteredDistanceX = distanceX
        var filteredDistanceY = distanceY

        val absX = abs(filteredDistanceX)
        val absY = abs(filteredDistanceY)

        // Suppress minor perpendicular motion near either axis while preserving
        // deliberate diagonal scrolling.
        if (absX < absY * DIRECTION_LOCK_RATIO) {
            filteredDistanceX = 0f
        } else if (absY < absX * DIRECTION_LOCK_RATIO) {
            filteredDistanceY = 0f
        }

        accumulatedDistanceX += filteredDistanceX * scrollCoefficient
        accumulatedDistanceY += filteredDistanceY * scrollCoefficient

        if (abs(accumulatedDistanceX) > MIN_DISTANCE_TO_SEND_SCROLL || abs(accumulatedDistanceY) > MIN_DISTANCE_TO_SEND_SCROLL) {
            sendScroll(-scrollDirection * accumulatedDistanceX, scrollDirection * accumulatedDistanceY)

            accumulatedDistanceX = 0.0
            accumulatedDistanceY = 0.0
        }

        return true
    }

    override fun onLongPress(e: MotionEvent) {
        if (!doubleTapDragEnabled && !dragging) {
            window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            plugin.sendSingleHold()
            dragging = true
        }
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        when (singleTapAction) {
            ClickType.LEFT -> sendLeftClick()
            ClickType.RIGHT -> sendRightClick()
            ClickType.MIDDLE -> sendMiddleClick()
            ClickType.NONE -> {}
        }
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (!dragging) {
            if (doubleTapDragEnabled) {
                maybeDragging = true
            } else {
                plugin.sendDoubleClick()
            }
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP && maybeDragging) {
            // Make sure we pass the event on to the general motion event handler which takes
            // care of ending an eventual drag.
            return false
        }
        return true
    }

    override fun onTripleFingerTap(ev: MotionEvent): Boolean {
        when (tripleTapAction) {
            ClickType.LEFT -> sendLeftClick()
            ClickType.RIGHT -> sendRightClick()
            ClickType.MIDDLE -> sendMiddleClick()
            ClickType.NONE -> {}
        }
        return true
    }

    override fun onDoubleFingerTap(ev: MotionEvent): Boolean {
        when (doubleTapAction) {
            ClickType.LEFT -> sendLeftClick()
            ClickType.RIGHT -> sendRightClick()
            ClickType.MIDDLE -> sendMiddleClick()
            ClickType.NONE -> {}
        }
        return true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        prefsApplied = false
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        if (keyboardShown && !imeVisible && watchingTextFocus && hasWindowFocus()) rememberDismissal()
        keyboardShown = imeVisible
        binding.shortcutKeys.visibility = if (keyboardShown) View.VISIBLE else View.GONE
        if (::plugin.isInitialized) updateMouseControls()
        if (!keyboardShown) clearShortcutKeys()
        return ViewCompat.onApplyWindowInsets(v, insets)
    }

    private fun sendLeftClick() {
        if (dragging) {
            plugin.sendSingleRelease()
            dragging = false
        } else {
            plugin.sendLeftClick()
        }
    }

    private fun sendMiddleClick() {
        plugin.sendMiddleClick()
    }

    private fun sendRightClick() {
        plugin.sendRightClick()
    }

    private fun sendScroll(x: Double, y: Double) {
        plugin.sendScroll(x, y)
    }

    private fun sendSpaceShortcut(keyCode: Int) {
        if (plugin.isKeyboardEnabled) {
            plugin.sendArrow(keyCode, control = true)
        } else {
            Toast.makeText(this, R.string.mousepad_keyboard_input_not_supported, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupShortcutKeys() {
        val keys = mapOf(binding.commandKey to "super", binding.controlKey to "ctrl",
            binding.optionKey to "alt", binding.shiftKey to "shift")
        keys.forEach { (button, modifier) ->
            button.isCheckable = true
            button.setOnClickListener {
                if (button.isChecked) modifiers.add(modifier) else modifiers.remove(modifier)
            }
        }
        keyListenerView.takeModifiers = {
            modifiers.toSet().also { clearShortcutKeys() }
        }
        binding.escapeKey.setOnClickListener { keyListenerView.sendEscape() }
    }

    private fun clearShortcutKeys() {
        modifiers.clear()
        binding.commandKey.isChecked = false
        binding.controlKey.isChecked = false
        binding.optionKey.isChecked = false
        binding.shiftKey.isChecked = false
    }

    private fun rememberDismissal() {
        if (lastTextFocusToken.isNotEmpty()) {
            keyboardDismissals.edit().putString(plugin.device.deviceId, lastTextFocusToken).apply()
        }
    }

    private fun toggleKeyboard() {
        if (keyboardShown) {
            rememberDismissal()
            pendingShowKeyboard = false
            hideKeyboard()
        } else {
            keyboardDismissals.edit().remove(plugin.device.deviceId).apply()
            showKeyboard()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService<InputMethodManager>()
        if (::keyListenerView.isInitialized) {
            imm?.hideSoftInputFromWindow(keyListenerView.windowToken, 0)
        }
    }

    private fun showKeyboard() {
        val imm = getSystemService<InputMethodManager>()
        if (::keyListenerView.isInitialized) {
            keyListenerView.requestFocus()
            imm?.showSoftInput(keyListenerView, 0)
        }
    }

    private fun showCompose() {
        val intent = Intent(this, ComposeSendActivity::class.java)
        intent.putExtra(ComposeSendActivity.EXTRA_DEVICE_ID, plugin.device.deviceId)
        startActivity(intent)
    }

    private fun applyPrefs() {
        if (prefsApplied) return

        val p = plugin.preferences

        val reverseScroll = p.getBoolean(getString(R.string.mousepad_scroll_direction), false)
        scrollDirection = if (reverseScroll) -1 else 1

        var scrollSensitivity = p.getInt(getString(R.string.mousepad_scroll_sensitivity), 100)
        if (scrollSensitivity == 0) scrollSensitivity = 1
        scrollCoefficient = (scrollSensitivity / 100f).toDouble().pow(1.5)

        binding.scrollRail.sensitivity = scrollCoefficient

        allowGyro = isGyroSensorAvailable() && p.getBoolean(getString(R.string.gyro_mouse_enabled), false)
        if (allowGyro) {
            gyroscopeSensitivity = p.getInt(getString(R.string.gyro_mouse_sensitivity), 100)
        }

        val singleTapSetting = p.getString(
            getString(R.string.mousepad_single_tap_key),
            getString(R.string.mousepad_default_single),
        )
        val doubleTapSetting = p.getString(
            getString(R.string.mousepad_double_tap_key),
            getString(R.string.mousepad_default_double),
        )
        val tripleTapSetting = p.getString(
            getString(R.string.mousepad_triple_tap_key),
            getString(R.string.mousepad_default_triple),
        )
        val sensitivitySetting = p.getString(
            getString(R.string.mousepad_sensitivity_key),
            getString(R.string.mousepad_default_sensitivity),
        )

        val accelerationProfileName = p.getString(
            getString(R.string.mousepad_acceleration_profile_key),
            getString(R.string.mousepad_default_acceleration_profile),
        ) ?: getString(R.string.mousepad_default_acceleration_profile)

        mPointerAccelerationProfile = PointerAccelerationProfileFactory.getProfileWithName(accelerationProfileName)

        singleTapAction = ClickType.fromString(singleTapSetting)
        doubleTapAction = ClickType.fromString(doubleTapSetting)
        tripleTapAction = ClickType.fromString(tripleTapSetting)

        when (sensitivitySetting) {
            "slowest" -> mCurrentSensitivity = 0.2f
            "aboveSlowest" -> mCurrentSensitivity = 0.5f
            "default" -> mCurrentSensitivity = 1.0f
            "aboveDefault" -> mCurrentSensitivity = 1.5f
            "fastest" -> mCurrentSensitivity = 2.0f
            else -> {
                mCurrentSensitivity = 1.0f
                return
            }
        }
        if (p.getBoolean(getString(R.string.pref_mousepad_show_keyboard), false) &&
            !keyboardDismissals.contains(plugin.device.deviceId)) {
            if (hasWindowFocus()) {
                showKeyboard()
            } else {
                // Defer until the window gains focus, see pendingShowKeyboard.
                pendingShowKeyboard = true
            }
        } else {
            hideKeyboard()
        }

        updateMouseControls()

        doubleTapDragEnabled = p.getBoolean(getString(R.string.mousepad_doubletap_drag_enabled_pref), true)
        keyListenerView.contentDescription = getString(
            if (doubleTapDragEnabled) R.string.mousepad_info_double_tap
            else R.string.mousepad_info
        )

        prefsApplied = true
    }

    private fun updateMouseControls() {
        val compact = keyboardShown && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        binding.spacesHeading.visibility = if (compact) View.GONE else View.VISIBLE
        binding.spacesButtons.visibility = binding.spacesHeading.visibility
        val showMouse = plugin.preferences.getBoolean(getString(R.string.mousepad_mouse_buttons_enabled_pref), true)
        binding.mouseButtons.visibility = if (showMouse && !compact) View.VISIBLE else View.GONE
    }

    private fun isGyroSensorAvailable(): Boolean {
        return mSensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    override fun onSupportNavigateUp(): Boolean {
        hideKeyboard()
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_DEVICE_ID = "deviceId"

        private const val MIN_DISTANCE_TO_SEND_SCROLL = 2.5f // touch gesture scroll
        private const val DIRECTION_LOCK_RATIO = 0.5f
        private const val STANDARD_DPI = 240.0f // = hdpi
        private const val MIN_DRAGGING_DISTANCE_SQUARED = 25.0f // distance squared to move after a double tap to start dragging
    }
}
