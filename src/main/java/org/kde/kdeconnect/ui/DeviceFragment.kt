/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.kde.kdeconnect.BackgroundService
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.Device.PluginsChangedListener
import org.kde.kdeconnect.KdeConnect
import org.kde.kdeconnect.PairingHandler
import org.kde.kdeconnect.base.BaseFragment
import org.kde.kdeconnect.helpers.security.SslHelper
import org.kde.kdeconnect.plugins.battery.BatteryPlugin
import org.kde.kdeconnect.plugins.connectivityreport.ConnectivityReportPlugin
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect.ui.compose.screen.device.DeviceErrorScreen
import org.kde.kdeconnect.ui.compose.screen.device.DevicePairingScreen
import org.kde.kdeconnect.ui.compose.screen.device.DeviceStatusCard
import org.kde.kdeconnect.ui.compose.screen.device.DeviceViewModel
import org.kde.kdeconnect.ui.compose.screen.device.PluginsScreen
import org.kde.kdeconnect_tp.R
import org.kde.kdeconnect_tp.databinding.ActivityDeviceBinding

/**
 * Main view. Displays the current device and its plugins
 */
class DeviceFragment : BaseFragment<ActivityDeviceBinding>() {

    companion object {
        private const val ARG_DEVICE_ID = "deviceId"
        private const val ARG_FROM_DEVICE_LIST = "fromDeviceList"
        private const val TAG = "KDE/DeviceFragment"
        fun newInstance(deviceId: String, fromDeviceList: Boolean): DeviceFragment {
            val frag = DeviceFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DEVICE_ID, deviceId)
                    putBoolean(ARG_FROM_DEVICE_LIST, fromDeviceList)
                }
            }
            return frag
        }
    }

    val deviceId: String by lazy {
        arguments?.getString(ARG_DEVICE_ID)
            ?: throw RuntimeException("You must instantiate a new DeviceFragment using DeviceFragment.newInstance()")
    }

    private val device by lazy { KdeConnect.getInstance().getDevice(deviceId) }

    private val viewModel by viewModels<DeviceViewModel>()

    override fun onInflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ActivityDeviceBinding {
        val fromDeviceList = requireArguments().getBoolean(ARG_FROM_DEVICE_LIST, false)
        if (fromDeviceList) {
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Handle back button, so we go to the list of devices in case we came from there
                    (activity as? MainActivity)?.onDeviceSelected(null)
                }
            }
            requireActivity().onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), callback)
        }
        return ActivityDeviceBinding.inflate(inflater, container, false)
    }

    private fun deviceActions(device: Device): List<Pair<String, () -> Unit>> = buildList {
        if (device.isReachable) {
            device.loadedPlugins.values.flatMap { it.getUiMenuEntries() }.forEach { entry ->
                add(entry.name to { entry.onClick(requireActivity()) })
            }
            add(getString(R.string.encryption_info_title) to {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.encryption_info_title)
                    .setMessage(listOf(
                        "${getString(R.string.my_device_fingerprint)}\n${SslHelper.getCertificateHash(SslHelper.certificate)}",
                        "${getString(R.string.remote_device_fingerprint)}\n${SslHelper.getCertificateHash(device.certificate)}",
                        "${getString(R.string.protocol_version)} ${device.protocolVersion}"
                    ).joinToString("\n\n"))
                    .setPositiveButton(R.string.ok, null)
                    .show()
            })
        }
        if (device.isPaired) {
            add(getString(R.string.device_menu_unpair) to {
                device.removePairingCallback(pairingCallback)
                device.removePluginsChangedListener(pluginsChangedListener)
                device.unpair()
                (requireActivity() as MainActivity).onDeviceSelected(null)
            })
        }
        if (device.pairStatus == PairingHandler.PairState.Requested) {
            add(getString(R.string.cancel_pairing) to { device.cancelPairing() })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        device?.apply {
            removePairingCallback(pairingCallback)
            addPairingCallback(pairingCallback)
            removePluginsChangedListener(pluginsChangedListener)
            addPluginsChangedListener(pluginsChangedListener)
        } ?: run { // device is null
            Log.e(TAG, "Trying to display a device fragment but the device is not present")
            (activity as? MainActivity)?.onDeviceSelected(null)
        }
        refreshUI()
    }

    private fun refreshDevicesAction() {
        BackgroundService.ForceRefreshConnections(requireContext())
        viewModel.setRefreshing(refreshing = true)
        binding.deviceView.postDelayed({
            if (isResumed && !isDetached) { // the view might be destroyed by now
                viewModel.setRefreshing(refreshing = false)
            }
        }, 1500)
    }

    override fun onPause() {
        viewModel.setRefreshing(refreshing = false)
        super.onPause()
    }

    private val pluginsChangedListener =
        PluginsChangedListener { activity?.runOnUiThread { refreshUI() } }

    override fun onDestroyView() {
        device?.apply {
            removePluginsChangedListener(pluginsChangedListener)
            removePairingCallback(pairingCallback)
        }
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        with(requireView()) {
            isFocusableInTouchMode = true
            requestFocus()
        }
    }

    @UiThread
    private fun refreshUI() {
        if (!hasBinding) return // in case onDestroyView has already been called
        val device = device ?: return

        //Once in-app, there is no point in keep displaying the notification if any
        device.hidePairingNotification()

        binding.deviceView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                KdeTheme(context) {
                    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                    val pairingErrorMessage by viewModel.pairingErrorMessage.collectAsStateWithLifecycle()

                    val header: @Composable () -> Unit = {
                        DeviceStatusCard(
                            name = device.name,
                            icon = device.iconDrawable,
                            state = stringResource(device.connectionLabel),
                            details = if (device.isPaired && device.isReachable) remoteStatusDetails() else null,
                            onDevices = { (requireActivity() as MainActivity).onDeviceSelected(null) },
                            onSettings = if (device.isPaired || device.isReachable) ({
                                startActivity(PluginSettingsActivity.createIntent(requireContext(), deviceId))
                            }) else null,
                            actions = deviceActions(device)
                        )
                    }
                    when (device.pairStatus) {
                        PairingHandler.PairState.Paired -> {
                            if (device.isReachable) {
                                val pluginsWithButtons =
                                    device.loadedPlugins.values.flatMap { plugin -> plugin.getUiButtons() }
                                val pluginsNeedPermissions =
                                    device.pluginsWithoutPermissions.values.filter { plugin ->
                                        device.isPluginEnabled(plugin.pluginKey)
                                    }
                                val pluginsNeedOptionalPermissions =
                                    device.pluginsWithoutOptionalPermissions.values.filter { plugin ->
                                        device.isPluginEnabled(plugin.pluginKey)
                                    }
                                PluginsScreen(
                                    header = header,
                                    pluginsWithButtons = pluginsWithButtons,
                                    pluginsNeedPermissions = pluginsNeedPermissions,
                                    pluginsNeedOptionalPermissions = pluginsNeedOptionalPermissions,
                                    onButtonClick = { button -> button.onClick(requireActivity()) },
                                    actionNeedPermissions = { plugin ->
                                        plugin.permissionExplanationDialog.show(
                                            childFragmentManager,
                                            null
                                        )
                                    },
                                    actionNeedOptionalPermissions = { plugin ->
                                        plugin.optionalPermissionExplanationDialog.show(
                                            childFragmentManager,
                                            null
                                        )
                                    }
                                )
                            } else {
                                DeviceErrorScreen(
                                    header = header,
                                    message = stringResource(id = R.string.unreachable_description),
                                    isRefreshing = isRefreshing,
                                    onRefresh = { refreshDevicesAction() }
                                )
                            }
                        }

                        else -> if (!device.isReachable) {
                            DeviceErrorScreen(
                                header = header,
                                message = stringResource(id = R.string.unreachable_unpaired_description),
                                isRefreshing = isRefreshing,
                                onRefresh = { refreshDevicesAction() }
                            )
                        } else {
                            DevicePairingScreen(
                                header = header,
                                pairStatus = device.pairStatus,
                                verificationKey = device.verificationKey ?: "",
                                pairMessage = pairingErrorMessage,
                                onRequestPairing = {
                                    viewModel.setPairingErrorMessage(message =null)
                                    device.requestPairing()
                                    refreshUI()
                                },
                                onAcceptPairing = {
                                    device.acceptPairing()
                                    refreshUI()
                                },
                                onRejectPairing = {
                                    device.apply {
                                        // Remove listener so buttons don't show for an instant before changing the view
                                        removePluginsChangedListener(pluginsChangedListener)
                                        removePairingCallback(pairingCallback)
                                        cancelPairing()
                                    }
                                    (activity as? MainActivity)?.onDeviceSelected(null)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private val pairingCallback: PairingHandler.PairingCallback =
        object : PairingHandler.PairingCallback {
            override fun incomingPairRequest() {
                activity?.runOnUiThread { refreshUI() }
            }

            override fun pairingSuccessful() {
                val accessibilityManager = requireContext().getSystemService<AccessibilityManager>()
                if (accessibilityManager?.isEnabled == true) {
                    @Suppress("DEPRECATION")
                    val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        AccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                    } else {
                        AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                    }
                    event.text.add(getString(R.string.pair_succeeded))
                    accessibilityManager.sendAccessibilityEvent(event)
                }
                activity?.runOnUiThread { refreshUI() }
            }

            override fun pairingFailed(error: String) {
                activity?.runOnUiThread {
                    viewModel.setPairingErrorMessage(message =error)
                    refreshUI()
                }
            }

            override fun unpaired(device: Device) {
                activity?.runOnUiThread {
                    viewModel.setPairingErrorMessage(message =null)
                    refreshUI()
                }
            }
        }


    /**
     * This method tries to display status info (battery and connectivity) for the remote device.
     */
    private fun remoteStatusDetails(): String? {
        val batteryPlugin = device?.getPlugin<BatteryPlugin>()
        val connectivityPlugin = device?.getPlugin<ConnectivityReportPlugin>()

        val batteryInfo = batteryPlugin?.remoteBatteryInfo
        val batteryStatus = if (batteryInfo != null) {
            @StringRes
            val resId = when {
                batteryInfo.isCharging -> R.string.battery_status_charging_format
                BatteryPlugin.isLowBattery(batteryInfo) -> R.string.battery_status_low_format
                else -> R.string.battery_status_format
            }
            activity?.getString(resId, batteryInfo.currentCharge)
        } else {
            null
        }

        val connectivityInfoMap = connectivityPlugin?.remoteConnectivityInfo
        val connectivityStatus = if (!connectivityInfoMap.isNullOrEmpty()) {
            connectivityInfoMap.values
                .map { it.networkType }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
                .ifEmpty { null }
        } else {
            null
        }

        return when {
            batteryStatus != null && connectivityStatus != null -> "$batteryStatus  •  $connectivityStatus"
            batteryStatus != null -> batteryStatus
            connectivityStatus != null -> connectivityStatus
            else -> null
        }

    }
}
