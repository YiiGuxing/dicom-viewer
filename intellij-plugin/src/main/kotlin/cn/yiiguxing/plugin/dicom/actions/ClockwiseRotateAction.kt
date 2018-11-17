package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.dicom.graphics.Viewport
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * ClockwiseRotateAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class ClockwiseRotateAction : ViewportAction() {

    override fun Viewport.onActionPerformed(e: AnActionEvent) {
        clockwiseRotate()
    }
}