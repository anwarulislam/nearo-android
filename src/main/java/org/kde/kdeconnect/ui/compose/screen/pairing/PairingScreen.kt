/*
 * SPDX-FileCopyrightText: 2026 Saul Cintero Chocarro <scintero@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.extensions.safeDrawingBottomPadding
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect.ui.compose.components.DeviceIcon
import org.kde.kdeconnect.ui.compose.components.DeviceListItem
import org.kde.kdeconnect.ui.compose.components.KdeThemePreviews
import org.kde.kdeconnect.ui.compose.components.SectionHeader
import org.kde.kdeconnect.ui.compose.model.device.DeviceUiModel
import org.kde.kdeconnect_tp.R

@Composable
fun PairingScreen(
    uiState: PairingUiState,
    onClick: (String) -> Unit,
    onWifiSettingsClick: () -> Unit = {},
    onNotificationSettingsClick: () -> Unit = {},
    onLocalNetworkPermissionClick: () -> Unit = {},
    onAddByIpClick: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val hasPermissions = uiState.hasLocalNetworkPermission && uiState.hasNotificationsPermission
    val hasNetwork = uiState.isWifiAvailable || uiState.available.isNotEmpty() || uiState.connected.isNotEmpty()
    PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp,
                bottom = safeDrawingBottomPadding().calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            item {
                DiscoveryPanel(showDescription = hasPermissions && hasNetwork && uiState.isTrustedNetwork, onRefresh)
            }
            if (!uiState.hasLocalNetworkPermission) {
                item { Notice(R.string.pairing_missing_local_network_permission, R.drawable.ic_warning, onLocalNetworkPermissionClick) }
            }
            if (!uiState.hasNotificationsPermission) {
                item { Notice(R.string.no_notifications, R.drawable.ic_warning, onNotificationSettingsClick) }
            }
            if (hasPermissions) {
                if (uiState.hasDuplicateNames) item { Notice(R.string.pairing_duplicate_names, R.drawable.ic_warning) }
                if (!hasNetwork) {
                    item { Notice(R.string.no_wifi, R.drawable.ic_wifi, onWifiSettingsClick) }
                } else if (!uiState.isTrustedNetwork) {
                    item { Notice(R.string.on_non_trusted_message, R.drawable.ic_warning) }
                }
            }

            deviceGroup(R.string.category_connected_devices, uiState.connected, onClick)
            if (uiState.connected.isEmpty()) {
                item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                DeviceIcon(R.drawable.ic_phonelink_36dp)
                                Text(stringResource(R.string.no_connected_devices), style = MaterialTheme.typography.titleMedium)
                            }
                            if (uiState.available.isEmpty()) {
                                TextButton(onClick = onAddByIpClick) { Text(stringResource(R.string.custom_device_list)) }
                            }
                        }
                    }
                }
            }
            if (uiState.available.isNotEmpty()) deviceGroup(R.string.category_not_paired_devices, uiState.available, onClick)
            if (uiState.remembered.isNotEmpty()) deviceGroup(R.string.category_remembered_devices, uiState.remembered, onClick)
        }
    }
}

private fun LazyListScope.deviceGroup(title: Int, devices: List<DeviceUiModel>, onClick: (String) -> Unit) {
    item { SectionHeader(stringResource(title), devices.size) }
    itemsIndexed(devices, key = { _, device -> device.id }) { index, device ->
        DeviceListItem(device, index, devices.size, device.summaryRes.takeIf { it > 0 }?.let { stringResource(it) }) {
            onClick(device.id)
        }
    }
}

@Composable
private fun DiscoveryPanel(showDescription: Boolean, onRefresh: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colorResource(R.color.nearo_ink),
        contentColor = colorResource(R.color.nearo_on_ink)
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.discover_devices),
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.headlineLargeEmphasized
                )
                Image(
                    painterResource(R.drawable.nearo_discovery_art), contentDescription = null,
                    modifier = Modifier.weight(1.08f).height(104.dp)
                )
            }
            if (showDescription) {
                Text(stringResource(R.string.pairing_description), style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = onRefresh,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.nearo_lime),
                    contentColor = colorResource(R.color.nearo_ink)
                )
            ) {
                Icon(painterResource(R.drawable.ic_action_refresh_24dp), null, Modifier.size(20.dp))
                Text(stringResource(R.string.refresh), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun Notice(text: Int, icon: Int, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        val action = if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)
        Row(action.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(icon), null, Modifier.size(24.dp))
            Text(stringResource(text), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (onClick != null) Icon(painterResource(R.drawable.ic_arrow_forward_black_24dp), null, Modifier.size(18.dp))
        }
    }
}

@KdeThemePreviews
@Composable
private fun PreviewCompose() {
    KdeTheme(context = LocalContext.current) {
        PairingScreen(
            uiState = PairingUiState(
                isWifiAvailable = true,
                hasNotificationsPermission = true,
                hasLocalNetworkPermission = true,
                isTrustedNetwork = true,
                hasDuplicateNames = true,
                connected = emptyList(),
                available = listOf(
                    DeviceUiModel(
                        id = "_2504584b_6aa2_3cd6_bd1b_5e958aa6cd23_",
                        icon = R.drawable.ic_device_laptop_32dp,
                        name = "Device 1",
                        summaryRes = 0,
                        isReachable = true,
                        isPaired = false
                    ), DeviceUiModel(
                        id = "_2504584b_6aa2_3cd6_bd1b_5e958aa6cd24_",
                        icon = R.drawable.ic_device_desktop_32dp,
                        name = "Device 2",
                        summaryRes = R.string.protocol_version_newer,
                        isReachable = true,
                        isPaired = false
                    )
                ),
                remembered = emptyList(),
                isRefreshing = false
            ),
            onClick = { /* Do nothing */ },
            onWifiSettingsClick = { /* Do nothing */ },
            onNotificationSettingsClick = { /* Do nothing */ },
            onLocalNetworkPermissionClick = { /* Do nothing */ },
            onRefresh = { /* Do nothing */ }
        )
    }
}
