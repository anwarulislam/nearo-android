/*
 * SPDX-FileCopyrightText: 2021 Maxim Leshchenko <cnmaks90@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.about

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect.ui.compose.KdeTopAppBar
import org.kde.kdeconnect.ui.compose.screen.aboutkde.AboutKDEScreen
import org.kde.kdeconnect_tp.R

class AboutKDEActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            KdeTheme(this) {
                Scaffold(
                    topBar = {
                        AboutKDETopBar(
                            onNavigateBack = { onBackPressedDispatcher.onBackPressed() }
                        )
                    }
                ) { padding ->
                    AboutKDEScreen(
                        contentPadding = padding
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutKDETopBar(
    onNavigateBack: () -> Unit
) {
    KdeTopAppBar(
        title = stringResource(R.string.about_kde),
        navIconOnClick = onNavigateBack,
    )
}
