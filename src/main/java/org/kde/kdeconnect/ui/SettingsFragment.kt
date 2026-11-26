/*
 * SPDX-FileCopyrightText: 2023 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.Spanned
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kde.kdeconnect.KdeConnect
import org.kde.kdeconnect.BackgroundService
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import org.kde.kdeconnect.helpers.CreateFileParams
import org.kde.kdeconnect.helpers.CreateFileResultContract
import org.kde.kdeconnect.helpers.DeviceHelper
import org.kde.kdeconnect.helpers.DeviceHelper.getDeviceName
import org.kde.kdeconnect.helpers.NotificationHelper
import org.kde.kdeconnect.ui.ThemeUtil.applyTheme
import org.kde.kdeconnect_tp.BuildConfig
import org.kde.kdeconnect_tp.R

class SettingsFragment : MaterialSettingsFragment() {
    private lateinit var pluginSettings: PreferenceCategory
    private var listedDevices: List<Pair<String, String>>? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        screen.addPreference(Preference(context).apply {
            layoutResource = R.layout.nearo_preference_heading
            setTitle(R.string.settings)
            isSelectable = false
        })

        fun section(title: Int, vararg entries: Pair<Preference, Int>) {
            val group = PreferenceCategory(context).apply {
                setTitle(title)
                isIconSpaceReserved = false
            }
            screen.addPreference(group)
            for ((preference, icon) in entries) {
                preference.setIcon(icon)
                group.addPreference(preference)
            }
        }

        section(R.string.settings_this_device,
            deviceNamePref(context) to R.drawable.ic_device_phone_32dp,
            themePref(context) to R.drawable.ic_settings_24dp,
            persistentNotificationPref(context) to R.drawable.ic_baseline_info_24
        )
        section(R.string.settings_connections,
            trustedNetworkPref(context) to R.drawable.ic_wifi,
            devicesByIpPref(context) to R.drawable.ic_device_desktop_32dp,
            bluetoothSupportPref(context) to R.drawable.ic_phonelink_36dp
        )
        pluginSettings = PreferenceCategory(context).apply { setTitle(R.string.device_menu_plugins) }
        screen.addPreference(pluginSettings)
        listedDevices = null
        updatePluginSettings()

        val about = Preference(context).apply {
            setTitle(R.string.about_nearo)
            setOnPreferenceClickListener {
                (requireActivity() as MainActivity).openAbout()
                true
            }
        }
        section(R.string.settings_support,
            exportLogsPref(context) to R.drawable.ic_baseline_code_24,
            about to R.drawable.ic_baseline_info_24
        )

        preferenceScreen = screen
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.clipToPadding = false
        val initialPadding = listView.paddingBottom
        viewLifecycleOwner.lifecycleScope.launch {
            (requireActivity() as MainActivity).navigationClearance.collect { clearance ->
                listView.updatePadding(bottom = initialPadding + clearance)
            }
        }
    }

    override fun onDestroyView() {
        bluetoothPermissionsDialog?.dismiss()
        bluetoothPermissionsDialog = null
        super.onDestroyView()
    }

    private fun deviceNamePref(context: Context) = EditTextPreference(context).apply {
        key = DeviceHelper.KEY_DEVICE_NAME_PREFERENCE
        isSelectable = true
        setOnBindEditTextListener { editText: EditText ->
            editText.setSingleLine()
            editText.filters = arrayOf(
                InputFilter { source: CharSequence, start: Int, end: Int, _: Spanned?, _: Int, _: Int ->
                    DeviceHelper.filterDeviceNameInput(source, start, end)
                },
                LengthFilter(DeviceHelper.MAX_DEVICE_NAME_LENGTH),
            )
        }
        val deviceName = getDeviceName(context)
        setTitle(R.string.settings_rename)
        summary = deviceName
        setDialogTitle(R.string.device_rename_title)
        text = deviceName
        setPositiveButtonText(R.string.device_rename_confirm)
        setNegativeButtonText(R.string.cancel)
        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, new: Any? ->
            val name = new as String?
            if (name.isNullOrBlank()) {
                if (view != null) {
                    Snackbar.make(requireView(), R.string.invalid_device_name, Snackbar.LENGTH_LONG).show()
                }
                false
            }
            else {
                summary = new
                true
            }
        }
    }

    private fun themePref(context: Context) = ListPreference(context).apply {
        key = KEY_APP_THEME
        setTitle(R.string.theme_dialog_title)
        setDialogTitle(R.string.theme_dialog_title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setEntries(R.array.theme_list_v28) else setEntries(R.array.theme_list)
        setEntryValues(R.array.theme_list_values)
        setDefaultValue(ThemeUtil.DEFAULT_MODE)
        summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, new: Any? ->
            if (new is String) {
                applyTheme(new)
            }
            true
        }
    }

    private fun persistentNotificationPref(context: Context): Preference =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Preference(context).apply {
            setTitle(R.string.setting_persistent_notification_oreo)
            setSummary(R.string.setting_persistent_notification_description)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent()
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS")
                intent.putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                context.startActivity(intent)
                true
            }
        }
        // Persistent notification toggle for Android Versions below Oreo
        else SwitchPreference(context).apply {
            isPersistent = false
            isChecked = NotificationHelper.isPersistentNotificationEnabled(context)
            setTitle(R.string.setting_persistent_notification)
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, new: Any ->
                val isChecked = new as Boolean
                NotificationHelper.setPersistentNotificationEnabled(context, isChecked)
                BackgroundService.instance?.changePersistentNotificationVisibility(isChecked)
                NotificationHelper.setPersistentNotificationEnabled(context, isChecked)
                true
            }
        }

    val activityLauncherWithDevicesByIpRefresh = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateDevicesByIpSummary()
    }

    private fun trustedNetworkPref(context: Context) = Preference(context).apply {
        isPersistent = false
        setTitle(R.string.trusted_networks)
        setSummary(R.string.trusted_networks_desc)
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activityLauncherWithNetworkRefresh.launch(Intent(context, TrustedNetworksActivity::class.java))
            true
        }
    }

    private lateinit var devicesByIpPref : Preference

    val activityLauncherWithNetworkRefresh = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        context?.let { BackgroundService.ForceRefreshConnections(it) }
    }

    /** Opens activity to configure device by IP when clicked */
    private fun devicesByIpPref(context: Context) = Preference(context).apply {
        devicesByIpPref = this
        isPersistent = false
        setTitle(R.string.custom_device_list)
        updateDevicesByIpSummary()
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activityLauncherWithDevicesByIpRefresh.launch(Intent(context, CustomDevicesActivity::class.java))
            true
        }
    }

    private fun updateDevicesByIpSummary() {
        devicesByIpPref.setSummary(getString(
            R.string.custom_devices_settings_summary,
            CustomDevicesActivity.getCustomDeviceList(requireContext()).size
        ))
    }

    private var bluetoothPermissionsDialog: AlertDialog? = null

    private val bluetoothPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            results[Manifest.permission.BLUETOOTH_CONNECT] == true &&
            results[Manifest.permission.BLUETOOTH_SCAN] == true) {
            findPreference<SwitchPreference>(KEY_BLUETOOTH_ENABLED)?.isChecked = true // Save the preference before BluetoothLinkProvider reads it
            BackgroundService.ForceRefreshConnections(requireContext()) // Restarts the BluetoothLinkProvider
        }
    }

    private fun bluetoothSupportPref(context: Context) = SwitchPreference(context).apply {
        setDefaultValue(false)
        key = KEY_BLUETOOTH_ENABLED
        setTitle(R.string.enable_bluetooth)
        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && newValue as Boolean) {
                val permissions = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                val permissionsGranted = permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                if (!permissionsGranted) {
                    bluetoothPermissionsDialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.location_permission_needed_title)
                        .setMessage(R.string.bluetooth_permission_needed_desc)
                        .setPositiveButton(R.string.ok) { _, _ -> bluetoothPermissionsLauncher.launch(permissions) }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                    return@OnPreferenceChangeListener false
                }
            }
            isChecked = newValue as Boolean // Save the preference before BluetoothLinkProvider reads it
            BackgroundService.ForceRefreshConnections(context) // Restarts the BluetoothLinkProvider
            true
        }
    }

    private fun exportLogsPref(context: Context) = Preference(context).apply {
        isPersistent = false
        setTitle(R.string.settings_export_logs)
        setSummary(R.string.settings_export_logs_text)
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
            exportLogs.launch(CreateFileParams("text/plain", "nearo-log.txt"))
            true
        }
    }

    private val exportLogs: ActivityResultLauncher<CreateFileParams> = registerForActivityResult(
        CreateFileResultContract()
    ) { uri: Uri? ->
            val output = uri?.let { context?.contentResolver?.openOutputStream(it) }
                ?: return@registerForActivityResult

            CoroutineScope(Dispatchers.IO).launch {
                val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d"))
                process.inputStream.reader(Charsets.UTF_8).use { reader ->
                    output.writer(Charsets.UTF_8).use { writer ->
                        writer.write("Nearo ${BuildConfig.VERSION_NAME}\n")
                        writer.write("Android ${Build.VERSION.RELEASE} (${Build.MANUFACTURER} ${Build.MODEL})\n")
                        reader.copyTo(writer)
                    }
                }
            }
        }

    override fun onStart() {
        super.onStart()
        KdeConnect.getInstance().addDeviceListChangedCallback("SettingsFragment") {
            activity?.runOnUiThread { if (isAdded) updatePluginSettings() }
        }
        updatePluginSettings()
    }

    override fun onStop() {
        KdeConnect.getInstance().removeDeviceListChangedCallback("SettingsFragment")
        super.onStop()
    }

    private fun updatePluginSettings() {
        val devices = KdeConnect.getInstance().devices.values.filter { it.isPaired }.sortedBy { it.name }
        val names = devices.map { it.deviceId to it.name }
        if (names == listedDevices) return
        listedDevices = names
        pluginSettings.removeAll()
        val context = preferenceManager.context
        if (devices.isEmpty()) {
            pluginSettings.addPreference(Preference(context).apply {
                setTitle(R.string.pair_new_device)
                setIcon(R.drawable.ic_phonelink_36dp)
                setOnPreferenceClickListener {
                    (requireActivity() as MainActivity).onDeviceSelected(null)
                    true
                }
            })
        }
        for (device in devices) {
            pluginSettings.addPreference(Preference(context).apply {
                title = device.name
                setIcon(device.iconDrawable)
                setOnPreferenceClickListener {
                    startActivity(PluginSettingsActivity.createIntent(context, device.deviceId))
                    true
                }
            })
        }
    }

    companion object {
        const val KEY_BLUETOOTH_ENABLED: String = "bluetooth_enabled"
        const val KEY_APP_THEME: String = "theme_pref"
    }
}
