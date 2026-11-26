/*
 * SPDX-FileCopyrightText: 2026 Saul Cintero Chocarro <scintero@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.PairingHandler
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect.ui.compose.components.DeviceIcon
import org.kde.kdeconnect.ui.compose.components.KdeThemePreviews
import org.kde.kdeconnect_tp.R

@Composable
fun DevicePairingScreen(
    pairStatus: PairingHandler.PairState,
    verificationKey: String,
    pairMessage: String? = null,
    onRequestPairing: () -> Unit,
    onAcceptPairing: () -> Unit,
    onRejectPairing: () -> Unit
) {
    DevicePairingScreenContent(
        pairStatus = pairStatus,
        verificationKey = verificationKey,
        pairMessage = pairMessage,
        onRequestPairing = onRequestPairing,
        onAcceptPairing = onAcceptPairing,
        onRejectPairing = onRejectPairing
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DevicePairingScreenContent(
    pairStatus: PairingHandler.PairState,
    verificationKey: String,
    pairMessage: String? = null,
    onRequestPairing: () -> Unit,
    onAcceptPairing: () -> Unit,
    onRejectPairing: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DeviceIcon(R.drawable.ic_phonelink_36dp)
                val message = pairMessage ?: stringResource(
                    if (pairStatus == PairingHandler.PairState.NotPaired) R.string.device_not_paired else R.string.pair_requested
                )
                Text(message, style = MaterialTheme.typography.headlineMediumEmphasized)
                Text(stringResource(R.string.pairing_explanation), style = MaterialTheme.typography.bodyLarge)
                if (pairStatus != PairingHandler.PairState.NotPaired) {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(painterResource(R.drawable.ic_key), null)
                            Text(verificationKey, style = MaterialTheme.typography.headlineSmallEmphasized)
                        }
                    }
                }
                when (pairStatus) {
                    PairingHandler.PairState.NotPaired -> Button(
                        onClick = onRequestPairing, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.request_pairing)) }
                    PairingHandler.PairState.Requested -> LoadingIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    PairingHandler.PairState.RequestedByPeer -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAcceptPairing, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.pairing_accept))
                        }
                        FilledTonalButton(onClick = onRejectPairing, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.pairing_reject))
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}

@KdeThemePreviews
@Composable
private fun DevicePairingScreenNotPairedPreview() {
    KdeTheme(context = LocalContext.current) {
        DevicePairingScreenContent(
            pairStatus = PairingHandler.PairState.NotPaired,
            verificationKey = "",
            onRequestPairing = { /* Do nothing */ },
            onAcceptPairing = { /* Do nothing */ },
            onRejectPairing = { /* Do nothing */ }
        )
    }
}

@KdeThemePreviews
@Composable
private fun DevicePairingScreenRequestedPreview() {
    KdeTheme(context = LocalContext.current) {
        DevicePairingScreenContent(
            pairStatus = PairingHandler.PairState.Requested,
            verificationKey = "123456",
            onRequestPairing = { /* Do nothing */ },
            onAcceptPairing = { /* Do nothing */ },
            onRejectPairing = { /* Do nothing */ }
        )
    }
}

@KdeThemePreviews
@Composable
private fun DevicePairingScreenRequestedByPeerPreview() {
    KdeTheme(context = LocalContext.current) {
        DevicePairingScreenContent(
            pairStatus = PairingHandler.PairState.RequestedByPeer,
            verificationKey = "123456",
            onRequestPairing = { /* Do nothing */ },
            onAcceptPairing = { /* Do nothing */ },
            onRejectPairing = { /* Do nothing */ }
        )
    }
}
