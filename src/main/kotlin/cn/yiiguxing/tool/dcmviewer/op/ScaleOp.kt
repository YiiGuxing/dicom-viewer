package cn.yiiguxing.tool.dcmviewer.op

import cn.yiiguxing.tool.dcmviewer.DicomViewSkin
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
class ScaleOp(skin: DicomViewSkin) : MouseOperation(skin) {

    private var lastY = 0.0

    override fun handle(skin: DicomViewSkin, event: MouseEvent) {
        if (event.eventType == MouseEvent.MOUSE_DRAGGED) {
            skin.scaleByDistance(event.y - lastY)
        }

        lastY = event.y
    }
}

class ScaleWheelOp(skin: DicomViewSkin) : Operation<ScrollEvent>(skin) {
    override fun handle(skin: DicomViewSkin, event: ScrollEvent) {
        val delta = if (event.deltaX != 0.0) event.deltaX else event.deltaY
        skin.scaleByDistance(-delta / 5.0)
    }
}

private const val SCALE_POW: Double = 1.7
private const val SCALE_TICKS: Double = 100.0

private fun DicomViewSkin.scaleByDistance(dy: Double) {
    val oldScale = transform.scalingFactor
    val ticks = dy / SCALE_TICKS
    val oldFactor = Math.log(oldScale) / Math.log(SCALE_POW)
    val factor = oldFactor + ticks
    var scale = Math.pow(SCALE_POW, factor)

    scale = maxOf(MIN_SCALE, minOf(scale, MAX_SCALE))
    scale(scale / oldScale)
}