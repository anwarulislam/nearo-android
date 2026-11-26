// SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
package org.kde.kdeconnect.ui.compose.components

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.kde.kdeconnect.ui.MainActivity
import org.kde.kdeconnect_tp.R

@Composable
internal fun FloatingNavigation(selectedId: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val light = colors.surface.luminance() > 0.5f
    val container = if (light) colors.inverseSurface else colors.surfaceContainerHigh
    val content = if (light) colors.inverseOnSurface else colors.onSurfaceVariant
    val accent = if (light) colors.inversePrimary else colors.primary

    Surface(
        modifier = Modifier.padding(6.dp),
        shape = CircleShape,
        color = container.copy(alpha = 0.96f),
        contentColor = content,
        border = BorderStroke(1.dp, content.copy(alpha = 0.2f)),
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier.width(260.dp).height(IntrinsicSize.Min).selectableGroup().padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (id in listOf(R.id.destination_devices, R.id.destination_settings)) {
                val devices = id == R.id.destination_devices
                val selected = id == selectedId
                val tint by animateColorAsState(if (selected) accent else content)
                val fill by animateColorAsState(if (selected) accent.copy(alpha = 0.16f) else Color.Transparent)
                val stroke by animateColorAsState(if (selected) accent.copy(alpha = 0.65f) else Color.Transparent)
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                        .background(fill).border(1.dp, stroke, CircleShape)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(id) })
                        .heightIn(min = 54.dp).padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                ) {
                    Icon(painterResource(if (devices) R.drawable.ic_phonelink_36dp else R.drawable.ic_settings_24dp),
                        contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    Text(stringResource(if (devices) R.string.devices else R.string.settings),
                        color = tint, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Scroll clearance for the floating bar. The activity handles the system bars separately. */
@Composable
internal fun mainNavigationBottomPadding(): Dp {
    val activity = LocalActivity.current as? MainActivity ?: return 0.dp
    val height by activity.navigationClearance.collectAsStateWithLifecycle()
    return with(LocalDensity.current) { height.toDp() }
}
