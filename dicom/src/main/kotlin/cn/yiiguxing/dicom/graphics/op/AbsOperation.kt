package cn.yiiguxing.dicom.graphics.op

import cn.yiiguxing.dicom.graphics.Viewport
import java.awt.event.MouseEvent

abstract class AbsOperation(private val viewport: Viewport) : Operation {
    override var enabled: Boolean = true

    final override fun onMouseEvent(event: MouseEvent) {
        if (enabled) {
            handleEvent(event, viewport)
        }
    }

    abstract fun handleEvent(event: MouseEvent, viewport: Viewport)
}