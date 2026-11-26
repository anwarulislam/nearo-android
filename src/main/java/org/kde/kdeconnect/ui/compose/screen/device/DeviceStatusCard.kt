// SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
package org.kde.kdeconnect.ui.compose.screen.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.KdeDropdownMenuItem
import org.kde.kdeconnect.ui.compose.KdeOverflowMenu
import org.kde.kdeconnect.ui.compose.components.DeviceIcon
import org.kde.kdeconnect_tp.R

@Composable
internal fun DeviceStatusCard(
    name: String,
    icon: Int,
    state: String,
    details: String?,
    onDevices: () -> Unit,
    onSettings: (() -> Unit)?,
    actions: List<Pair<String, () -> Unit>>
) {
    val ink = colorResource(R.color.nearo_ink)
    val lime = colorResource(R.color.nearo_lime)
    Surface(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = ink,
        contentColor = colorResource(R.color.nearo_on_ink)
    ) {
        Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDevices) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.devices))
                }
                Spacer(Modifier.weight(1f))
                if (onSettings != null) IconButton(onClick = onSettings) {
                    Icon(painterResource(R.drawable.ic_settings_24dp), stringResource(R.string.device_menu_plugins))
                }
                if (actions.isNotEmpty()) KdeOverflowMenu { dismiss ->
                    actions.forEach { (label, action) ->
                        KdeDropdownMenuItem(label) {
                            dismiss()
                            action()
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DeviceIcon(icon, Modifier.size(52.dp), containerColor = lime, contentColor = ink)
                Column(Modifier.weight(1f)) {
                    Text(state, color = lime, style = MaterialTheme.typography.labelLarge)
                    Text(name, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMediumEmphasized)
                }
            }
            if (details != null) Text(details, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
