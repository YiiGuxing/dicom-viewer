package cn.yiiguxing.plugin.dicom.editor

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class DicomFileEditor(project: Project, file: VirtualFile) : UserDataHolderBase(), FileEditor {

    private val dicomEditor = DicomEditor(project, file)

    init {
        Disposer.register(this, dicomEditor)
    }

    override fun getName(): String = "DicomFileEditor"

    override fun getComponent(): JComponent = dicomEditor.component

    override fun getPreferredFocusedComponent(): JComponent? = dicomEditor.contentComponent

    override fun setState(state: FileEditorState) {
    }

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun selectNotify() = Unit

    override fun deselectNotify() = Unit

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
    }

    override fun dispose() {
    }
}