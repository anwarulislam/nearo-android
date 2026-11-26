// SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
package org.kde.kdeconnect.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat
import org.kde.kdeconnect_tp.R

abstract class MaterialSettingsFragment : PreferenceFragmentCompat() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDivider(null)
        val maxWidth = resources.getDimensionPixelSize(R.dimen.settings_content_max_width)
        view.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) view.post {
                val inset = ((view.width - maxWidth) / 2).coerceAtLeast(0)
                if (view.paddingLeft != inset) view.updatePadding(left = inset, right = inset)
            }
        }
    }
}
