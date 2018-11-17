package cn.yiiguxing.dicom.graphics.op

import cn.yiiguxing.dicom.graphics.Viewport
import java.awt.event.MouseEvent

class WindowingOp(viewport: Viewport) : AbsOperation(viewport) {

    private var lastX = 0
    private var lastY = 0

    override fun handleEvent(event: MouseEvent, viewport: Viewport) {
        when (event.id) {
            MouseEvent.MOUSE_PRESSED -> {
                lastX = event.x
                lastY = event.y
            }
            MouseEvent.MOUSE_DRAGGED -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                viewport.run {
                    setWindowing(windowWidth + dx * 3, windowCenter + dy * 3)
                }

                lastX = event.x
                lastY = event.y
            }
        }
    }

}