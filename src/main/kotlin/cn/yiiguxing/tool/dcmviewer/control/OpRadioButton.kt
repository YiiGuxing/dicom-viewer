package cn.yiiguxing.tool.dcmviewer.control

import cn.yiiguxing.tool.dcmviewer.op.Op
import javafx.scene.AccessibleAttribute
import javafx.scene.control.ToggleButton

/**
 * OpRadioButton
 *
 * Created by Yii.Guxing on 2018/11/25
 */
class OpRadioButton : ToggleButton() {

    var op: Op = Op.WINDOWING

    override fun fire() {
        if (toggleGroup == null || !isSelected) {
            super.fire()
        }
    }

    override fun queryAccessibleAttribute(attribute: AccessibleAttribute, vararg parameters: Any): Any {
        return when (attribute) {
            AccessibleAttribute.SELECTED -> isSelected
            else -> super.queryAccessibleAttribute(attribute, *parameters)
        }
    }

}