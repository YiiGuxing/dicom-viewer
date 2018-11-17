package cn.yiiguxing.plugin.dicom.editor

import cn.yiiguxing.plugin.dicom.DicomFileTypeManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class DicomFileEditorProvider(private val typeManager: DicomFileTypeManager) : FileEditorProvider, DumbAware {

    override fun getEditorTypeId(): String = "dicom-images"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun accept(project: Project, file: VirtualFile): Boolean = typeManager.isDicomFile(file)

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return DicomFileEditor(project, file)
    }
}