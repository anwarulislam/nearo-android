/*
 * SPDX-FileCopyrightText: 2023 Dmitry Yudin <dgyudin@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.kde.kdeconnect_tp.R

@Composable
fun KdeTopAppBar(
    title: String = stringResource(R.string.kde_connect),
    subTitle: String? = null,
    navIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    navIconDescription: String = stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
    navIconOnClick: () -> Unit, // = { onBackPressedDispatcher.onBackPressed() }
    actions: @Composable (RowScope.() -> Unit) = {},
) {
    TopAppBar(
        modifier = Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp).clip(MaterialTheme.shapes.extraLarge),
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        navigationIcon = {
            FilledTonalIconButton(onClick = navIconOnClick, content = { Icon(navIcon, navIconDescription) })
        },
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subTitle != null) {
                    Text(subTitle, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        actions = actions
    )
}
