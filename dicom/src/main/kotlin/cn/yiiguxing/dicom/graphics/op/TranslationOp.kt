package cn.yiiguxing.dicom.graphics.op

import cn.yiiguxing.dicom.graphics.Viewport
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform

class TranslationOp(viewport: Viewport) : AbsOperation(viewport) {
    private var lastX = 0
    private var lastY = 0

    private val points = DoubleArray(8) { 0.0 }
    private val tempTransform: AffineTransform = AffineTransform()

    override fun handleEvent(event: MouseEvent, viewport: Viewport) {
        when (event.id) {
            MouseEvent.MOUSE_PRESSED -> {
                lastX = event.x
                lastY = event.y
            }
            MouseEvent.MOUSE_DRAGGED -> {
                val points = points
                points[0] = lastX.toDouble()
                points[1] = lastY.toDouble()
                points[2] = event.x.toDouble()
                points[3] = event.y.toDouble()

                tempTransform.run {
                    setTransform(viewport.transform)
                    invert()
                    transform(points, 0, points, 4, 2)
                }

                val dx = points[6] - points[4]
                val dy = points[7] - points[5]
                viewport.translate(dx, dy)

                lastX = event.x
                lastY = event.y
            }
        }
    }
}