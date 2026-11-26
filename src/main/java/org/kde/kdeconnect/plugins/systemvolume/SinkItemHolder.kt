package org.kde.kdeconnect.plugins.systemvolume

import android.view.View
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt
import org.kde.kdeconnect_tp.R
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.kde.kdeconnect_tp.R.drawable
import org.kde.kdeconnect_tp.databinding.ListItemSystemvolumeBinding

class SinkItemHolder(
    val viewBinding: ListItemSystemvolumeBinding,
    val plugin: SystemVolumePlugin,
    val onTrackingChanged: (Boolean) -> Unit
) : RecyclerView.ViewHolder(viewBinding.root) {

    private fun getOnLongClickAction(sink: Sink) = View.OnLongClickListener { v ->
        Toast.makeText(v.context, sink.name, Toast.LENGTH_SHORT).show()
        true
    }

    fun bind(sink: Sink) {
        viewBinding.apply {
            //Card
            sinkCard.setOnLongClickListener(getOnLongClickAction(sink))
            //Mute
            systemvolumeMute.setOnClickListener { plugin.sendMute(sink.name, !sink.mute) }
            //Volume seek
            systemvolumeSeek.apply {
                clearOnChangeListeners()
                clearOnSliderTouchListeners()
                contentDescription = context.getString(R.string.sink_volume, sink.description)
                valueTo = sink.maxVolume.coerceAtLeast(1).toFloat()
                value = sink.volume.toFloat().coerceIn(valueFrom, valueTo)
                isEnabled = sink.maxVolume > 0
                addOnChangeListener { _, volume, fromUser ->
                    if (fromUser) plugin.sendVolume(sink.name, volume.roundToInt())
                }
                addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) = onTrackingChanged(true)
                    override fun onStopTrackingTouch(slider: Slider) {
                        onTrackingChanged(false)
                        plugin.sendVolume(sink.name, slider.value.roundToInt())
                    }
                })
            }
            //Radio button
            systemvolumeLabel.setOnLongClickListener(getOnLongClickAction(sink))
            systemvolumeLabel.setOnCheckedChangeListener(null)
            systemvolumeLabel.isChecked = sink.isDefault
            systemvolumeLabel.setOnCheckedChangeListener { _, checked -> if (checked) plugin.sendEnable(sink.name) }
            systemvolumeLabel.text = sink.description
            //Icon
            val iconRes = if (sink.mute) drawable.ic_volume_mute else drawable.ic_volume
            systemvolumeMute.setImageResource(iconRes)
        }
    }

}
