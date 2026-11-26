/*
 * SPDX-FileCopyrightText: 2023 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import org.kde.kdeconnect.BackgroundService
import org.kde.kdeconnect.KdeConnect
import org.kde.kdeconnect.extensions.getSafeDrawInsets
import org.kde.kdeconnect.extensions.setOnApplyWindowInsetsListenerCompat
import org.kde.kdeconnect.helpers.DeviceHelper
import org.kde.kdeconnect.ui.about.AboutFragment
import org.kde.kdeconnect_tp.R
import org.kde.kdeconnect_tp.databinding.ActivityMainBinding

private const val STATE_SELECTED_DEVICE = "selected_device" //Saved persistently in preferences

class MainActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val upfrontPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN && results[Manifest.permission.ACCESS_LOCAL_NETWORK] == true) {
            BackgroundService.ForceRefreshConnections(this)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && results[Manifest.permission.POST_NOTIFICATIONS] == true) {
            KdeConnect.getInstance().reloadAllDevicePlugins()
        }
        if (results.containsValue(true)) {
            // If PairingFragment is active, update its permissions header
            (currentFragment as? PairingFragment)?.updatePermissionsHeader()
        }
    }

    private var currentDeviceId: String? = null
    private val preferences: SharedPreferences by lazy { getSharedPreferences("stored_menu_selection", MODE_PRIVATE) }
    private val returnToDevices = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = onDeviceSelected(currentDeviceId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        DeviceHelper.initializeDeviceId(this)

        setContentView(binding.root)
        binding.root.setOnApplyWindowInsetsListenerCompat { view, insets ->
            val safe = insets.getSafeDrawInsets()
            view.updatePadding(left = safe.left, top = safe.top, right = safe.right, bottom = safe.bottom)
            insets.inset(safe.left, safe.top, safe.right, safe.bottom)
        }
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
        DeviceHelper.getDeviceName(this)
        onBackPressedDispatcher.addCallback(this, returnToDevices)
        binding.destinations.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.destination_devices -> onDeviceSelected(currentDeviceId)
                R.id.destination_settings -> showDestination(item.itemId, SettingsFragment())
                R.id.destination_about -> showDestination(item.itemId, AboutFragment.newInstance())
            }
            true
        }
        binding.destinations.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.destination_devices && currentDeviceId != null) onDeviceSelected(null)
        }

        if (savedInstanceState != null) {
            currentDeviceId = savedInstanceState.getString(STATE_SELECTED_DEVICE)
            // Older versions stored a drawer item index. The restored fragment is authoritative.
            selectDestination(when (currentFragment) {
                is SettingsFragment -> R.id.destination_settings
                is AboutFragment -> R.id.destination_about
                else -> R.id.destination_devices
            })
        } else if (!showScreenFromIntent(intent)) {
            onDeviceSelected(preferences.getString(STATE_SELECTED_DEVICE, null))
        }

        val missingPermissions = mutableListOf<String>()

        // Upfront requests for permissions without explanation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCAL_NETWORK) != PERMISSION_GRANTED
            && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_LOCAL_NETWORK))
        {
            missingPermissions.add(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED
            && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS))
        {
            missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Check savedInstanceState to avoid requesting permissions again if the activity is recreated eg on rotation
        if (missingPermissions.isNotEmpty() && savedInstanceState == null) {
            upfrontPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    // MainActivity is singleTop, so intents sent while it's on top of the stack (eg: from notifications) arrive here
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showScreenFromIntent(intent)
    }

    /**
     * Shows the screen that [intent] asks for, and applies its pairing action if it has one.
     * Returns false if the intent doesn't ask for any screen.
     * Must be called only once for each intent delivery.
     */
    private fun showScreenFromIntent(intent: Intent): Boolean {
        val intentAlreadyHandled = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (intentAlreadyHandled) {
            return false
        }
        if (intent.hasExtra(FLAG_FORCE_OVERVIEW)) {
            Log.i(TAG, "Requested to start main overview")
            onDeviceSelected(null)
            return true
        }
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return false
        Log.i(TAG, "Loading selected device from parameter")
        val pairStatus = intent.getStringExtra(PAIR_REQUEST_STATUS)
        if (pairStatus == null) {
            onDeviceSelected(deviceId)
        } else {
            Log.i(TAG, "Pair status is $pairStatus")
            onDeviceSelected(onPairResultFromNotification(deviceId, pairStatus))
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun onPairResultFromNotification(deviceId: String, pairStatus: String): String? {
        if (pairStatus != PAIRING_PENDING) {
            val device = KdeConnect.getInstance().getDevice(deviceId)
            if (device == null) {
                Log.w(TAG, "Reject pairing - device no longer exists: $deviceId")
                return null
            }
            when (pairStatus) {
                PAIRING_ACCEPTED -> device.acceptPairing()
                PAIRING_REJECTED -> device.cancelPairing()
            }
        }
        return if (pairStatus == PAIRING_ACCEPTED || pairStatus == PAIRING_PENDING) deviceId else null
    }

    override fun onStart() {
        super.onStart()
        BackgroundService.ForceRefreshConnections(applicationContext)
    }

    fun onDeviceSelected(deviceId: String?, fromDeviceList: Boolean = false) {
        currentDeviceId = deviceId
        preferences.edit { putString(STATE_SELECTED_DEVICE, deviceId) }
        val fragment = deviceId?.let { DeviceFragment.newInstance(it, fromDeviceList) } ?: PairingFragment()
        showDestination(R.id.destination_devices, fragment)
    }

    private fun selectDestination(id: Int) {
        binding.destinations.menu.findItem(id).isChecked = true
        returnToDevices.isEnabled = id != R.id.destination_devices
    }

    private val currentFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.container)

    private fun showDestination(id: Int, fragment: Fragment) {
        selectDestination(id)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_DEVICE, currentDeviceId)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (DeviceHelper.KEY_DEVICE_NAME_PREFERENCE == key) {
            BackgroundService.ForceRefreshConnections(this) //Re-send our identity packet
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        const val EXTRA_DEVICE_ID = "deviceId"
        const val PAIR_REQUEST_STATUS = "pair_req_status"
        const val PAIRING_ACCEPTED = "accepted"
        const val PAIRING_REJECTED = "rejected"
        const val PAIRING_PENDING = "pending"
        const val FLAG_FORCE_OVERVIEW = "forceOverview"
    }
}
