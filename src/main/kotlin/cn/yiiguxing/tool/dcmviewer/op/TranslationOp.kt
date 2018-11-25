package cn.yiiguxing.tool.dcmviewer.op

import cn.yiiguxing.tool.dcmviewer.DicomViewSkin
import javafx.scene.input.MouseEvent
import javafx.scene.transform.Affine

/**
 * TranslationOp
 *
 * Created by Yii.Guxing on 2018/11/24
 */
class TranslationOp(skin: DicomViewSkin) : MouseOperation(skin) {

    private var lastX = 0.0
    private var lastY = 0.0

    private val tempTransform = Affine()
    private val points = DoubleArray(8) { 0.0 }

    override fun handle(skin: DicomViewSkin, event: MouseEvent) {
        if (event.eventType == MouseEvent.MOUSE_DRAGGED) {
            val points = points
            points[0] = lastX
            points[1] = lastY
            points[2] = event.x
            points[3] = event.y

            tempTransform.apply {
                setToTransform(skin.transform)
                invert()
                transform2DPoints(points, 0, points, 4, 2)
            }

            val dx = points[6] - points[4]
            val dy = points[7] - points[5]
            skin.translate(dx, dy)
        }

        lastX = event.x
        lastY = event.y
    }

}