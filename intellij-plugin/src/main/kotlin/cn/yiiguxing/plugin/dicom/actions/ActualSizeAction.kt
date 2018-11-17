package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.dicom.graphics.Viewport
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * ActualSizeAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class ActualSizeAction : ViewportAction() {

    override fun Viewport.onActionPerformed(e: AnActionEvent) {
        zoomToActualSize()
    }

    override fun Viewport.onUpdate(e: AnActionEvent): Boolean {
        return !isActualSize
    }
}