package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.dicom.graphics.Viewport
import cn.yiiguxing.plugin.dicom.actionSystem.dicomComponentDecorator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * ViewportAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
abstract class ViewportAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        e.dicomComponentDecorator?.viewport?.onActionPerformed(e)
    }

    protected abstract fun Viewport.onActionPerformed(e: AnActionEvent)

    override fun update(e: AnActionEvent) {
        super.update(e)
        val decorator = e.dicomComponentDecorator
        if (decorator != null) {
            e.presentation.isEnabled = decorator.viewport.onUpdate(e)
        } else {
            e.presentation.isEnabled = false
        }
    }

    protected open fun Viewport.onUpdate(e: AnActionEvent): Boolean {
        return true
    }
}