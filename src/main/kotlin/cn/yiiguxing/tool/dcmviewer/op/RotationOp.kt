package cn.yiiguxing.tool.dcmviewer.op

import cn.yiiguxing.tool.dcmviewer.DicomViewSkin
import javafx.scene.input.MouseEvent

/**
 * RotationOp
 *
 * Created by Yii.Guxing on 2018/11/24
 */
class RotationOp(skin: DicomViewSkin) : MouseOperation(skin) {

    private var lastX = 0.0
    private var lastY = 0.0

    private val centerPoints = DoubleArray(4) { 0.0 }

    override fun handle(skin: DicomViewSkin, event: MouseEvent) {
        if (event.eventType == MouseEvent.MOUSE_DRAGGED) {
            val centerPoints = centerPoints
            centerPoints[0] = skin.viewWidth / 2.0
            centerPoints[1] = skin.viewHeight / 2.0

            skin.transform.transform2DPoints(centerPoints, 0, centerPoints, 2, 1)

            val cx = centerPoints[2]
            val cy = centerPoints[3]
            val x = event.x
            val y = event.y
            val k1 = (y - cy) / (x - cx)
            val k2 = (lastY - cy) / (lastX - cx)
            val radian = -Math.atan((k2 - k1) / (1 + k1 * k2))
            val angle = Math.toDegrees(radian)
            if (!angle.isNaN()) {
                skin.rotate(angle)
            }
        }

        lastX = event.x
        lastY = event.y
    }
}