/*
 * SPDX-FileCopyrightText: 2026 Tanish Ranjan <tanishranjan4@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.about

import android.content.ClipData
import android.content.ClipboardManager
import android.os.SystemClock
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import org.kde.kdeconnect.ui.compose.components.mainNavigationBottomPadding
import org.kde.kdeconnect.ui.about.AboutData
import org.kde.kdeconnect.ui.about.AboutPerson
import org.kde.kdeconnect.ui.about.applicationAboutData
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect.ui.compose.components.DeviceIcon
import org.kde.kdeconnect.ui.compose.components.KdeThemePreviews
import org.kde.kdeconnect_tp.R

@Composable
fun AboutScreen(
    aboutData: AboutData,
    onBack: () -> Unit,
    onContributorClicked: () -> Unit,
    onKdeConnectSourceClicked: () -> Unit,
    onEasterEggTriggered: () -> Unit,
    onReportBugClicked: () -> Unit,
    onDonateClicked: () -> Unit,
    onSourceCodeClicked: () -> Unit,
    onLicensesClicked: () -> Unit,
    onAboutKdeClicked: () -> Unit,
    onReleasesClicked: () -> Unit
) {
    val bottomPadding = mainNavigationBottomPadding()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = bottomPadding + 16.dp
            )
        ) {
            item {
                TextButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Text(stringResource(R.string.settings), Modifier.padding(start = 8.dp))
                }
                AppInfoCard(
                    aboutData = aboutData,
                    onEasterEggTriggered = onEasterEggTriggered
                )
            }

            item {
                Card(
                    onClick = onContributorClicked,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(stringResource(R.string.main_contributor), style = MaterialTheme.typography.labelLarge)
                        Text(aboutData.mainContributor.name, style = MaterialTheme.typography.headlineSmallEmphasized)
                    }
                }
            }
            item {
                ActionButtons(
                    aboutData = aboutData,
                    onReportBugClicked = onReportBugClicked,
                    onSourceCodeClicked = onSourceCodeClicked,
                    onLicensesClicked = onLicensesClicked,
                    onReleasesClicked = onReleasesClicked
                )
            }

            item {
                KdeConnectCredits(
                    aboutData = aboutData,
                    onSourceClicked = onKdeConnectSourceClicked,
                    onAboutKdeClicked = onAboutKdeClicked,
                    onDonateClicked = onDonateClicked
                )
            }
        }
    }
}

@Composable
private fun AppInfoCard(
    aboutData: AboutData,
    onEasterEggTriggered: () -> Unit
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var firstTapMillis by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        onClick = {
            val clipboard = context.getSystemService<ClipboardManager>()!!
            clipboard.setPrimaryClip(ClipData.newPlainText("Version", aboutData.versionName))
            Toast.makeText(context, R.string.clipboard_toast, Toast.LENGTH_SHORT).show()

            val currentMillis = SystemClock.elapsedRealtime()
            if (firstTapMillis == null) {
                firstTapMillis = currentMillis
            }

            tapCount++

            if (tapCount == 3) {
                if (currentMillis - firstTapMillis!! <= 500) {
                    onEasterEggTriggered()
                }
                tapCount = 0
                firstTapMillis = null
            } else if (currentMillis - firstTapMillis!! > 500) {
                tapCount = 1
                firstTapMillis = currentMillis
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = aboutData.icon),
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.medium)
                    .background(colorResource(R.color.nearo_launcher_background))
            )

            Column(
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(aboutData.name),
                    style = MaterialTheme.typography.headlineSmallEmphasized
                )
                Text(
                    text = stringResource(id = R.string.version, aboutData.versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text(
            text = stringResource(R.string.nearo_origin),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionButtons(
    aboutData: AboutData,
    onReportBugClicked: () -> Unit,
    onSourceCodeClicked: () -> Unit,
    onLicensesClicked: () -> Unit,
    onReleasesClicked: () -> Unit
) {
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        if (aboutData.bugURL != null) ActionIconTextButton(R.string.report_bug, R.drawable.ic_baseline_bug_report_24, onReportBugClicked)
        if (aboutData.sourceCodeURL != null) ActionIconTextButton(R.string.source_code, R.drawable.ic_baseline_code_24, onSourceCodeClicked)
        ActionIconTextButton(R.string.licenses, R.drawable.ic_baseline_gavel_24, onLicensesClicked)
        if (aboutData.releasesURL != null) ActionIconTextButton(R.string.nearo_releases, R.drawable.ic_baseline_web_24, onReleasesClicked)
    }
}

@Composable
private fun ActionIconTextButton(
    @StringRes textRes: Int,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 100.dp).heightIn(min = 92.dp)
            .clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DeviceIcon(iconRes)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = textRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KdeConnectCredits(
    aboutData: AboutData,
    onSourceClicked: () -> Unit,
    onAboutKdeClicked: () -> Unit,
    onDonateClicked: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.kde_connect_development),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLargeEmphasized
            )
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionIconTextButton(R.string.kde_connect_source, R.drawable.ic_baseline_code_24, onSourceClicked)
                ActionIconTextButton(R.string.about_kde, R.drawable.ic_kde_24dp, onAboutKdeClicked)
                if (aboutData.kdeDonateURL != null) ActionIconTextButton(R.string.donate_to_kde, R.drawable.ic_baseline_attach_money_24, onDonateClicked)
            }
            Spacer(modifier = Modifier.height(16.dp))

            aboutData.kdeConnectAuthors.forEach { author ->
                AuthorItemRow(author = author)
            }

            aboutData.kdeConnectAuthorsFooterText?.let { footerResId ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = footerResId),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthorItemRow(author: AboutPerson) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = author.name,
            style = MaterialTheme.typography.bodyLarge
        )
        if (author.task != null) {
            Text(
                text = stringResource(id = author.task),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@KdeThemePreviews
@Composable
private fun AboutScreenPreview() {
    val sampleAboutData = applicationAboutData
    KdeTheme(context = LocalContext.current) {
        Surface {
            AboutScreen(
                aboutData = sampleAboutData,
                onBack = {},
                onContributorClicked = {},
                onKdeConnectSourceClicked = {},
                onEasterEggTriggered = {},
                onReportBugClicked = {},
                onDonateClicked = {},
                onSourceCodeClicked = {},
                onLicensesClicked = {},
                onAboutKdeClicked = {},
                onReleasesClicked = {}
            )
        }
    }
}
