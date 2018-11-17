package cn.yiiguxing.dicom.graphics.op

import cn.yiiguxing.dicom.graphics.Viewport
import cn.yiiguxing.dicom.graphics.fixedScaleX
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.lang.StrictMath.abs

private const val SCALE_POW: Double = 1.7
private const val SCALE_TICKS: Double = 100.0

private fun Viewport.scale(dy: Int) {
    val oldScale = abs(transform.fixedScaleX.takeIf { it.isFinite() } ?: 1.0)
    val ticks = dy / SCALE_TICKS
    val oldFactor = Math.log(oldScale) / Math.log(SCALE_POW)
    val factor = oldFactor + ticks
    var scale = Math.pow(SCALE_POW, factor)

    scale = maxOf(Viewport.MIN_SCALE, minOf(scale, Viewport.MAX_SCALE))
    val ds = scale / oldScale
    if (ds.isFinite()) {
        scale(ds, ds)
    }
}

class ScaleOp(viewport: Viewport) : AbsOperation(viewport) {
    private var lastY = 0

    override fun handleEvent(event: MouseEvent, viewport: Viewport) {
        when (event.id) {
            MouseEvent.MOUSE_PRESSED -> {
                lastY = event.y
            }
            MouseEvent.MOUSE_DRAGGED -> {
                val dy = event.y - lastY
                viewport.scale(dy)

                lastY = event.y
            }
        }
    }
}

class ScaleWheelOp(viewport: Viewport) : AbsOperation(viewport) {
    override fun handleEvent(event: MouseEvent, viewport: Viewport) {
        if (event is MouseWheelEvent) {
            viewport.scale(event.wheelRotation * 5)
        }
    }
}