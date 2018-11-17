package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.dicom.graphics.Viewport
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * ResetViewportAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class ResetViewportAction : ViewportAction() {

    override fun Viewport.onActionPerformed(e: AnActionEvent) {
        reset()
    }
}