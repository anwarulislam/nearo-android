// SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
package org.kde.kdeconnect.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.Slider
import org.kde.kdeconnect_tp.R
import kotlin.math.roundToInt

/** The input plugins store sensitivity as an integer from 0 to 100. */
class SensitivityPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private var sensitivity = 0

    init {
        layoutResource = R.layout.nearo_slider_preference
        isSelectable = false
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Int = a.getInt(index, 0)

    override fun onSetInitialValue(defaultValue: Any?) {
        sensitivity = getPersistedInt(defaultValue as? Int ?: 0).coerceIn(0, 100)
        persistInt(sensitivity)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val slider = holder.findViewById(R.id.preference_slider) as Slider
        slider.clearOnChangeListeners()
        slider.clearOnSliderTouchListeners()
        slider.contentDescription = title
        slider.value = sensitivity.toFloat()
        var dragging = false
        fun saveValue() {
            val value = slider.value.roundToInt()
            if (value == sensitivity) return
            if (callChangeListener(value)) {
                sensitivity = value
                persistInt(value)
            } else {
                slider.value = sensitivity.toFloat()
            }
        }
        slider.addOnChangeListener { _, _, fromUser -> if (fromUser && !dragging) saveValue() }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) { dragging = true }
            override fun onStopTrackingTouch(slider: Slider) {
                dragging = false
                saveValue()
            }
        })
    }
}
