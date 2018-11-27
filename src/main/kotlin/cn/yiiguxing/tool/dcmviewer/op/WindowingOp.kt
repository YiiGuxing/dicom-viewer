package cn.yiiguxing.tool.dcmviewer.op

import cn.yiiguxing.tool.dcmviewer.control.skin.DicomViewSkin
import javafx.scene.input.MouseEvent

/**
 * WindowingOp
 *
 * Created by Yii.Guxing on 2018/11/24
 */
class WindowingOp(skin: DicomViewSkin) : MouseOperation(skin) {

    private var lastX = 0.0
    private var lastY = 0.0

    override fun handle(skin: DicomViewSkin, event: MouseEvent) {
        if (event.eventType == MouseEvent.MOUSE_DRAGGED) {
            val dx = (event.x - lastX).toFloat()
            val dy = (event.y - lastY).toFloat()
            skin.skinnable.apply {
                val ww = windowWidth
                val wc = windowCenter
                if (ww != null && wc != null) {
                    setColorWindowing(ww + dx, wc + dy)
                }
            }
        }

        lastX = event.x
        lastY = event.y
    }
}