package cn.yiiguxing.tool.dcmviewer.op

import cn.yiiguxing.tool.dcmviewer.DicomViewController
import javafx.scene.input.MouseEvent

/**
 * WindowingOp
 *
 * Created by Yii.Guxing on 2018/11/24
 */
class WindowingOp(controller: DicomViewController) : MouseOperation(controller) {

    private var lastX = 0.0
    private var lastY = 0.0

    override fun handle(controller: DicomViewController, event: MouseEvent) {
        if (event.eventType == MouseEvent.MOUSE_DRAGGED) {
            val dx = (event.x - lastX).toFloat()
            val dy = (event.y - lastY).toFloat()
            controller.view.apply {
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