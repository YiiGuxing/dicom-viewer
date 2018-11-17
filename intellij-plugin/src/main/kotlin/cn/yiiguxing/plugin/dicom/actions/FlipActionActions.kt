/*
 * FlipActionActions
 *
 * Created by Yii.Guxing on 2018/11/17
 */

package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.dicom.graphics.Viewport
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * HorizontalFlipAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class HorizontalFlipAction : ViewportAction() {

    override fun Viewport.onActionPerformed(e: AnActionEvent) {
        horizontalFlip()
    }
}

/**
 * VerticalFlipAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class VerticalFlipAction : ViewportAction() {

    override fun Viewport.onActionPerformed(e: AnActionEvent) {
        verticalFlip()
    }
}