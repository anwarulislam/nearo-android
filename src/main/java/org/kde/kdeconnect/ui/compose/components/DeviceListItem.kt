/*
 * SPDX-FileCopyrightText: 2026 Saul Cintero Chocarro <scintero@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.model.device.DeviceUiModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeviceIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier = modifier.size(48.dp)
            .background(containerColor, MaterialShapes.Cookie6Sided.toShape()),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), null, Modifier.size(24.dp), tint = contentColor)
    }
}

@Composable
fun DeviceListItem(
    device: DeviceUiModel,
    index: Int,
    count: Int,
    supportingText: String?,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    SegmentedListItem(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(containerColor = colors.surfaceContainerLow),
        leadingContent = {
            if (device.icon > 0) {
                DeviceIcon(
                    icon = device.icon,
                    containerColor = if (device.isReachable) colors.primaryContainer else colors.surfaceContainerHighest,
                    contentColor = if (device.isReachable) colors.onPrimaryContainer else colors.onSurfaceVariant
                )
            }
        },
        supportingContent = supportingText?.let { { Text(it, color = colors.error) } },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
    ) {
        Text(device.name, style = MaterialTheme.typography.titleMediumEmphasized)
    }
}
