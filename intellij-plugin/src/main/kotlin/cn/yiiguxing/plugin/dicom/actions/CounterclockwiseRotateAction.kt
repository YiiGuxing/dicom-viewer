package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.dicom.graphics.Viewport
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * CounterclockwiseRotateAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class CounterclockwiseRotateAction : ViewportAction() {

    override fun Viewport.onActionPerformed(e: AnActionEvent) {
        counterclockwiseRotate()
    }
}