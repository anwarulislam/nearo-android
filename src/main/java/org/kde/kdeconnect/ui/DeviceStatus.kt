// SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
package org.kde.kdeconnect.ui

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.kde.kdeconnect.Device
import org.kde.kdeconnect_tp.R

@get:StringRes
internal val Device.connectionLabel: Int
    get() = connectionLabel(isReachable)

@StringRes
private fun Device.connectionLabel(reachable: Boolean) = when {
    !reachable -> R.string.device_status_offline
    isPaired -> R.string.device_status_connected
    else -> R.string.device_status_nearby
}

internal fun AppCompatActivity.showDeviceStatus(device: Device) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            device.isReachableFlow.collect { reachable ->
                supportActionBar?.subtitle = getString(R.string.device_status_short, device.name, getString(device.connectionLabel(reachable)))
            }
        }
    }
}

@Composable
internal fun deviceStatusText(device: Device): String {
    val reachable by device.isReachableFlow.collectAsStateWithLifecycle()
    return stringResource(R.string.device_status_short, device.name, stringResource(device.connectionLabel(reachable)))
}
