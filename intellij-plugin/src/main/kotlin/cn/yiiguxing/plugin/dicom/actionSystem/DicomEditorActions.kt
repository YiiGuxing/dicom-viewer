/*
 * DicomEditorActions
 *
 * Created by Yii.Guxing on 2018/11/16
 */

package cn.yiiguxing.plugin.dicom.actionSystem

import cn.yiiguxing.plugin.dicom.editor.DicomComponentDecorator
import com.intellij.openapi.actionSystem.AnActionEvent

const val GROUP_TOOLBAR = "Dicom.EditorToolbar"
const val ACTION_PLACE = "Dicom.Editor"


val AnActionEvent.dicomComponentDecorator: DicomComponentDecorator?
    get() = DicomComponentDecorator.DATA_KEY.getData(dataContext)

fun AnActionEvent.updateEnabled(): Boolean {
    val decorator = dicomComponentDecorator
    presentation.isEnabled = decorator != null
    return presentation.isEnabled
}

