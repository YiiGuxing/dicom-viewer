package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.dicom.graphics.Viewport
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * LocateAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class LocateAction : ViewportAction() {

    override fun Viewport.onActionPerformed(e: AnActionEvent) {
        locate()
    }
}