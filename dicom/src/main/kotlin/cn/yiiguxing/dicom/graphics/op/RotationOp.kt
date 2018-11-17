package cn.yiiguxing.dicom.graphics.op

import cn.yiiguxing.dicom.graphics.Viewport
import cn.yiiguxing.dicom.graphics.fixedScaleY
import java.awt.event.MouseEvent
import java.awt.geom.Point2D

class RotationOp(viewport: Viewport) : AbsOperation(viewport) {

    private var lastX = 0.0
    private var lastY = 0.0

    private val centerPoint = Point2D.Double()

    override fun handleEvent(event: MouseEvent, viewport: Viewport) {
        when (event.id) {
            MouseEvent.MOUSE_PRESSED -> {
                lastX = event.x.toDouble()
                lastY = event.y.toDouble()
            }
            MouseEvent.MOUSE_DRAGGED -> {
                val bounds = viewport.bounds
                val centerPoint = centerPoint.apply {
                    x = bounds.centerX
                    y = bounds.centerY
                }
                viewport.transform.transform(centerPoint, centerPoint)

                val cx = centerPoint.x
                val cy = centerPoint.y
                val x = event.x.toDouble()
                val y = event.y.toDouble()
                val k1 = (y - cy) / (x - cx)
                val k2 = (lastY - cy) / (lastX - cx)
                val fixedScaleY = viewport.transform.fixedScaleY
                val radian = (fixedScaleY / Math.abs(fixedScaleY)) * -Math.atan((k2 - k1) / (1 + k1 * k2))
                if (!radian.isNaN()) {
                    viewport.rotate(radian)
                }

                lastX = x
                lastY = y
            }
        }
    }
}