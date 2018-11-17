package cn.yiiguxing.plugin.dicom.editor

import cn.yiiguxing.dicom.graphics.Viewport
import cn.yiiguxing.dicom.graphics.op.Op
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.*
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import org.intellij.images.fileTypes.ImageFileTypeManager
import javax.swing.JComponent

class DicomEditor(val project: Project, val file: VirtualFile) : DicomComponentDecorator, Disposable {

    private val editorUI = DicomEditorUI(this)

    val component: JComponent = editorUI
    val contentComponent: JComponent? = editorUI.contentComponent

    override var op: Op by editorUI

    override val viewport: Viewport
        get() = editorUI.viewport

    init {
        Disposer.register(this, editorUI)
        VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileAdapter() {
            override fun propertyChanged(event: VirtualFilePropertyEvent) {
                this@DicomEditor.propertyChanged(event)
            }

            override fun contentsChanged(event: VirtualFileEvent) {
                this@DicomEditor.contentsChanged(event)
            }
        }, this)

        setValue(file)
    }

    private fun setValue(file: VirtualFile?) {
        editorUI.dicomComponent.setSrc(file?.path)
    }

    private fun propertyChanged(event: VirtualFilePropertyEvent) {
        if (file == event.file) {
            // Change document
            file.refresh(true, false) {
                if (ImageFileTypeManager.getInstance().isImage(file)) {
                    setValue(file)
                } else {
                    setValue(null)
                    // Close editor
                    val editorManager = FileEditorManager.getInstance(project)
                    editorManager.closeFile(file)
                }
            }
        }
    }

    private fun contentsChanged(event: VirtualFileEvent) {
        if (file == event.file) {
            val postRunnable = Runnable { setValue(file) }
            RefreshQueue.getInstance().refresh(true, false, postRunnable, ModalityState.current(), file)
        }
    }

    override fun dispose() {
    }
}
