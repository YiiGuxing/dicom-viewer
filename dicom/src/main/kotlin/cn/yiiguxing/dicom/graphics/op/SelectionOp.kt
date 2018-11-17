package cn.yiiguxing.dicom.graphics.op

import cn.yiiguxing.dicom.graphics.Viewport
import java.awt.Rectangle
import java.awt.event.MouseEvent

class SelectionOp(viewport: Viewport) : AbsOperation(viewport) {

    private val selectedRegion = Rectangle()
    private var startX: Int = 0
    private var startY: Int = 0

    override fun handleEvent(event: MouseEvent, viewport: Viewport) {
        val cx = event.x
        val cy = event.y
        when (event.id) {
            MouseEvent.MOUSE_PRESSED -> {
                startX = cx
                startY = cy
                selectedRegion.setBounds(0, 0, 0, 0)
            }
            MouseEvent.MOUSE_DRAGGED -> {
                onUpdateRegion(viewport, selectedRegion, cx, cy)
                viewport.selectedRegion = selectedRegion
            }
            MouseEvent.MOUSE_RELEASED -> {
                viewport.selectedRegion = null
            }
        }
    }

    private fun onUpdateRegion(viewport: Viewport, region: Rectangle, x: Int, y: Int) {
        val x1 = maxOf(minOf(startX, x), 0)
        val y1 = maxOf(minOf(startY, y), 0)
        val x2 = minOf(maxOf(startX, x), viewport.width - 1)
        val y2 = minOf(maxOf(startY, y), viewport.height - 1)
        region.setBounds(x1, y1, x2 - x1, y2 - y1)
    }
}