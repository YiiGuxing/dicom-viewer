package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.plugin.dicom.actionSystem.dicomComponentDecorator
import cn.yiiguxing.plugin.dicom.actionSystem.updateEnabled
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware

/**
 * InvertAction
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class InvertAction : ToggleAction(), DumbAware {

    override fun isSelected(e: AnActionEvent): Boolean {
        return e.dicomComponentDecorator?.viewport?.invert ?: false
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.dicomComponentDecorator?.viewport?.setInverse(state)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.updateEnabled()
    }
}