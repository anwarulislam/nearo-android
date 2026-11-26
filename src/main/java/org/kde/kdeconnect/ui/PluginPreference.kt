/*
 * SPDX-FileCopyrightText: 2023 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.ui

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.plugins.PluginFactory.getPluginInfo
import org.kde.kdeconnect_tp.R

class PluginPreference : SwitchPreference {
    private val device: Device
    private val pluginKey: String
    private val listener: View.OnClickListener?

    constructor(context: Context, pluginKey: String, device: Device, callback: PluginPreferenceCallback) : super(context) {
        layoutResource = R.layout.preference_with_button
        this.device = device
        this.pluginKey = pluginKey

        val info = getPluginInfo(pluginKey)
        title = info.displayName
        summary = info.description
        isChecked = device.isPluginEnabled(pluginKey)

        fun createClickListener() = View.OnClickListener {
            val plugin = device.getPluginIncludingWithoutPermissions(pluginKey)
            if (plugin != null) {
                callback.onStartPluginSettingsFragment(plugin)
            }
            else { // Could happen if the device is not connected anymore
                callback.onFinish()
            }
        }
        this.listener = if (info.hasSettings) createClickListener() else null
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val toggleListener = View.OnClickListener {
            val newState = !device.isPluginEnabled(pluginKey)
            isChecked = newState
            onStateChanged(holder, newState)
            device.setPluginEnabled(pluginKey, newState)
        }

        val content = holder.findViewById(R.id.content)
        val widget = holder.findViewById(android.R.id.widget_frame)
        val parent = holder.itemView
        content.setOnClickListener(listener)
        widget.contentDescription = title
        ViewCompat.setAccessibilityDelegate(widget, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = android.widget.Switch::class.java.name
                info.isCheckable = true
                info.setChecked(if (isChecked) AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE else AccessibilityNodeInfoCompat.CHECKED_STATE_FALSE)
            }
        })
        widget.setOnClickListener(toggleListener)
        parent.setOnClickListener(toggleListener)

        // Disable child backgrounds when known to be unneeded to prevent duplicate ripples
        fun getSelectableItemBackgroundResource(): Int {
            val value = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
            return value.resourceId
        }
        val selectableItemBackground: Int = if (listener == null) 0 else getSelectableItemBackgroundResource()
        content.setBackgroundResource(selectableItemBackground)
        widget.setBackgroundResource(selectableItemBackground)

        onStateChanged(holder, isChecked)
    }

    private fun onStateChanged(holder: PreferenceViewHolder, isEnabled: Boolean) {
        val content = holder.findViewById(R.id.content)
        val divider = holder.findViewById(R.id.divider)
        val widget = holder.findViewById(android.R.id.widget_frame)
        val parent = holder.itemView

        val hasDetails = isEnabled && listener != null && device.isReachable && device.isPaired

        holder.findViewById(R.id.open_settings).visibility = if (hasDetails) View.VISIBLE else View.GONE
        widget.importantForAccessibility = if (hasDetails) View.IMPORTANT_FOR_ACCESSIBILITY_YES else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        widget.isFocusable = hasDetails
        content.isFocusable = hasDetails
        val stateLabel = if (isEnabled) androidx.appcompat.R.string.abc_capital_on else androidx.appcompat.R.string.abc_capital_off
        ViewCompat.setStateDescription(parent, if (hasDetails) null else context.getString(stateLabel))
        parent.isFocusable = !hasDetails
        divider.visibility = if (hasDetails) View.VISIBLE else View.GONE
        content.isClickable = hasDetails
        widget.isClickable = hasDetails
        parent.isClickable = !hasDetails

        if (hasDetails) {
            // Cancel duplicate ripple caused by pressed state of parent propagating down
            content.isPressed = false
            content.background.jumpToCurrentState()
            widget.isPressed = false
            widget.background.jumpToCurrentState()
        }
    }

    interface PluginPreferenceCallback {
        fun onStartPluginSettingsFragment(plugin: Plugin)
        fun onFinish()
    }
}
