package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.dicom.graphics.Viewport
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * ZoomOutAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class ZoomOutAction : ViewportAction() {

    override fun Viewport.onActionPerformed(e: AnActionEvent) {
        zoomOut()
    }

    override fun Viewport.onUpdate(e: AnActionEvent): Boolean {
        return canZoomOut
    }
}