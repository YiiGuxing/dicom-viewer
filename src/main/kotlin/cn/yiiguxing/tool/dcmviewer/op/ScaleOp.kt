package cn.yiiguxing.tool.dcmviewer.op

import cn.yiiguxing.tool.dcmviewer.DicomViewController
import cn.yiiguxing.tool.dcmviewer.scalingFactor
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent

const val MIN_SCALE = 0.05
const val MAX_SCALE = 20.0

/**
 * ScaleOp
 *
 * Created by Yii.Guxing on 2018/11/24
 */
class ScaleOp(controller: DicomViewController) : MouseOperation(controller) {

    private var lastY = 0.0

    override fun handle(controller: DicomViewController, event: MouseEvent) {
        if (event.eventType == MouseEvent.MOUSE_DRAGGED) {
            controller.scaleByDistance(event.y - lastY)
        }

        lastY = event.y
    }
}

class ScaleWheelOp(controller: DicomViewController) : Operation<ScrollEvent>(controller) {
    override fun handle(controller: DicomViewController, event: ScrollEvent) {
        val delta = if (event.deltaX != 0.0) event.deltaX else event.deltaY
        controller.scaleByDistance(-delta / 5.0)
    }
}

private const val SCALE_POW: Double = 1.7
private const val SCALE_TICKS: Double = 100.0

private fun DicomViewController.scaleByDistance(dy: Double) {
    val oldScale = transform.scalingFactor
    val ticks = dy / SCALE_TICKS
    val oldFactor = Math.log(oldScale) / Math.log(SCALE_POW)
    val factor = oldFactor + ticks
    var scale = Math.pow(SCALE_POW, factor)

    scale = maxOf(MIN_SCALE, minOf(scale, MAX_SCALE))
    scale(scale / oldScale)
}