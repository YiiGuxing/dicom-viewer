package cn.yiiguxing.plugin.dicom.actions

import cn.yiiguxing.dicom.graphics.op.Op
import cn.yiiguxing.plugin.dicom.actionSystem.dicomComponentDecorator
import cn.yiiguxing.plugin.dicom.actionSystem.updateEnabled
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware

open class OperateAction(private val op: Op) : ToggleAction(), DumbAware {

    override fun isSelected(e: AnActionEvent): Boolean {
        return e.dicomComponentDecorator?.op == op
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.dicomComponentDecorator?.takeIf { state }?.op = op
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.updateEnabled()
    }
}

class WindowingOperateAction : OperateAction(Op.WINDOWING)
class ZoomOperateAction : OperateAction(Op.SCALE)
class TranslateOperateAction : OperateAction(Op.TRANSLATE)
class RotateOperateAction : OperateAction(Op.ROTATE)
class SelectOperateAction : OperateAction(Op.SELECT)