// SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
package org.kde.kdeconnect.ui.compose.components

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.kde.kdeconnect.ui.MainActivity
import org.kde.kdeconnect_tp.R

@Composable
internal fun FloatingNavigation(selectedId: Int, onSelect: (Int) -> Unit) {
    Surface(
        modifier = Modifier.padding(6.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp
    ) {
        ShortNavigationBar(
            modifier = Modifier.widthIn(max = 232.dp),
            containerColor = Color.Transparent,
            windowInsets = WindowInsets(0)
        ) {
            for (id in listOf(R.id.destination_devices, R.id.destination_settings)) {
                val devices = id == R.id.destination_devices
                ShortNavigationBarItem(
                    selected = selectedId == id,
                    onClick = { onSelect(id) },
                    icon = {
                        Icon(
                            painter = painterResource(if (devices) R.drawable.ic_phonelink_36dp else R.drawable.ic_settings_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(stringResource(if (devices) R.string.devices else R.string.settings)) }
                )
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
