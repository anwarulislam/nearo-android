// SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
package org.kde.kdeconnect.ui.compose.screen.device

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.plugins.battery.BatteryPlugin
import org.kde.kdeconnect.plugins.battery.DeviceBatteryInfo
import org.kde.kdeconnect.ui.compose.KdeDropdownMenuItem
import org.kde.kdeconnect.ui.compose.KdeOverflowMenu
import org.kde.kdeconnect.ui.compose.components.DeviceIcon
import org.kde.kdeconnect_tp.R
import java.text.NumberFormat

@Composable
internal fun DeviceStatusCard(
    name: String,
    icon: Int,
    state: String,
    battery: DeviceBatteryInfo?,
    mobileNetworks: List<String>,
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
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDevices) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(androidx.appcompat.R.string.abc_action_bar_up_description))
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
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeviceIcon(icon, Modifier.size(48.dp), containerColor = lime, contentColor = ink)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMediumEmphasized)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state, color = lime, style = MaterialTheme.typography.labelLarge)
                        if (battery != null && battery.currentCharge in 0..100) BatteryGauge(battery)
                        if (mobileNetworks.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(painterResource(R.drawable.ic_cell_tower_24), null, Modifier.size(16.dp))
                                Text(mobileNetworks.joinToString(", "), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryGauge(battery: DeviceBatteryInfo) {
    val low = BatteryPlugin.isLowBattery(battery)
    val description = stringResource(
        when {
            battery.isCharging -> R.string.battery_status_charging_format
            low -> R.string.battery_status_low_format
            else -> R.string.battery_status_format
        },
        battery.currentCharge
    )
    val locale = LocalConfiguration.current.locales[0]
    val percent = remember(locale, battery.currentCharge) {
        NumberFormat.getPercentInstance(locale).format(battery.currentCharge / 100.0)
    }
    val color = if (battery.isCharging) colorResource(R.color.nearo_lime) else LocalContentColor.current
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clip(shape)
                .drawBehind {
                    val fillWidth = size.width * battery.currentCharge / 100f
                    val left = if (layoutDirection == LayoutDirection.Rtl) size.width - fillWidth else 0f
                    drawRect(color.copy(alpha = 0.20f), topLeft = Offset(left, 0f), size = Size(fillWidth, size.height))
                }
                .border(1.dp, color.copy(alpha = 0.7f), shape)
                .padding(horizontal = 5.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (battery.isCharging) {
                Icon(painterResource(R.drawable.ic_flash_on_24), null, Modifier.size(14.dp), tint = color)
            } else if (low) {
                Icon(Icons.Filled.Warning, null, Modifier.size(12.dp), tint = color)
            }
            Text(percent, color = color, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.size(width = 2.dp, height = 7.dp).background(color.copy(alpha = 0.7f), RoundedCornerShape(1.dp)))
    }
}
