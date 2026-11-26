/*
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.plugins.mousepad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.min

/** Relative scroll wheel. The first finger owns the rail until it lifts. */
class RemoteScrollRail(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {
    var sensitivity = 1.0
    var sendScroll: (Double) -> Unit = {}

    private val density = resources.displayMetrics.density
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private var pointerId = MotionEvent.INVALID_POINTER_ID
    private var lastY = 0f
    private var startY = 0f
    private var remainder = 0.0

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointerId = event.getPointerId(0)
                startY = event.y
                lastY = startY
                remainder = 0.0
                isPressed = true
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(pointerId)
                if (index >= 0) {
                    val y = event.getY(index)
                    remainder += (lastY - y) * sensitivity
                    val pixels = remainder.toInt()
                    if (pixels != 0) {
                        sendScroll(pixels.toDouble())
                        remainder -= pixels.toDouble()
                    }
                    lastY = y
                    invalidate()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> if (event.getPointerId(event.actionIndex) == pointerId) finishDrag()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finishDrag()
        }
        return true
    }

    private fun finishDrag() {
        pointerId = MotionEvent.INVALID_POINTER_ID
        remainder = 0.0
        isPressed = false
        parent.requestDisallowInterceptTouchEvent(false)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val center = width / 2f
        val top = 15f * density
        val bottom = height - paddingBottom - (layout?.height ?: lineHeight) - 10f * density
        if (bottom - top < 24f * density) return

        line.color = currentTextColor
        line.strokeWidth = 2f * density
        val chevron = 4f * density
        canvas.drawLine(center - chevron, top + chevron, center, top, line)
        canvas.drawLine(center, top, center + chevron, top + chevron, line)
        canvas.drawLine(center - chevron, bottom - chevron, center, bottom, line)
        canvas.drawLine(center, bottom, center + chevron, bottom - chevron, line)
        val trackTop = top + 12f * density
        val trackBottom = bottom - 12f * density
        line.alpha = 65
        canvas.drawLine(center, trackTop, center, trackBottom, line)
        val halfGrip = min(10f * density, (trackBottom - trackTop) / 2f)
        val offset = if (pointerId == MotionEvent.INVALID_POINTER_ID) 0f else lastY - startY
        val travel = ((trackBottom - trackTop) / 2f - halfGrip).coerceAtLeast(0f)
        val grip = (trackTop + trackBottom) / 2f + offset.coerceIn(-travel, travel)
        line.alpha = 255
        line.strokeWidth = 6f * density
        canvas.drawLine(center, grip - halfGrip, center, grip + halfGrip, line)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.isScrollable = true
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        val step = 48.0 * density * sensitivity
        when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> sendScroll(-step)
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> sendScroll(step)
            else -> return super.performAccessibilityAction(action, arguments)
        }
        return true
    }
}
