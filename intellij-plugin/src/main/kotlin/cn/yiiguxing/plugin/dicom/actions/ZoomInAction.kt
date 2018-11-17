package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.dicom.graphics.Viewport
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * ZoomInAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class ZoomInAction : ViewportAction() {
    override fun Viewport.onActionPerformed(e: AnActionEvent) {
        zoomIn()
    }

    override fun Viewport.onUpdate(e: AnActionEvent): Boolean {
        return canZoomIn
    }
}