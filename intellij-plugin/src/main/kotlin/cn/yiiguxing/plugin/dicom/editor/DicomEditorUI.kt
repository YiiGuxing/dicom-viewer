package cn.yiiguxing.plugin.dicom.editor

import cn.yiiguxing.dicom.DicomComponent
import cn.yiiguxing.dicom.graphics.Viewport
import cn.yiiguxing.dicom.graphics.op.*
import cn.yiiguxing.plugin.dicom.actionSystem.ACTION_PLACE
import cn.yiiguxing.plugin.dicom.actionSystem.GROUP_TOOLBAR
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import java.awt.BorderLayout
import java.awt.event.*
import javax.swing.JComponent
import javax.swing.JPanel

class DicomEditorUI(private val editor: DicomEditor) : JPanel(), DataProvider, DicomComponentDecorator, Disposable {

    val dicomComponent: DicomComponent = DicomComponent(DicomImageLoaderFactory)

    val contentComponent: JComponent = dicomComponent

    override var op: Op = Op.WINDOWING

    override val viewport: Viewport = dicomComponent.viewport

    private val wheelAdapter = DicomWheelAdapter(this)
    private val mouseAdapter = DicomMouseAdapter(this)

    init {
        layout = BorderLayout()
        setupToolbar()

        dicomComponent.apply {
            addMouseListener(mouseAdapter)
            addMouseMotionListener(mouseAdapter)
            addMouseWheelListener(wheelAdapter)
        }

        contentComponent.addMouseListener(FocusRequester())
        add(contentComponent, BorderLayout.CENTER)
    }

    private fun setupToolbar() {
        val actionManager = ActionManager.getInstance()
        val actionGroup = actionManager.getAction(GROUP_TOOLBAR) as ActionGroup
        val actionToolbar = actionManager.createActionToolbar(ACTION_PLACE, actionGroup, true)

        // Make sure toolbar is 'ready' before it's added to component hierarchy
        // to prevent ActionToolbarImpl.updateActionsImpl(boolean, boolean) from increasing popup size unnecessarily
        actionToolbar.updateActionsImmediately()
        actionToolbar.setTargetComponent(this)

        val toolbarPanel = actionToolbar.component
        toolbarPanel.addMouseListener(FocusRequester())

        add(toolbarPanel, BorderLayout.NORTH)
    }

    override fun getData(dataId: String): Any? {
        return when (dataId) {
            CommonDataKeys.PROJECT.name -> editor.project
            CommonDataKeys.VIRTUAL_FILE.name -> editor.file
            CommonDataKeys.VIRTUAL_FILE_ARRAY.name -> arrayOf(editor.file)
            DicomComponentDecorator.DATA_KEY.name -> editor
            else -> null
        }
    }

    override fun dispose() {
        dicomComponent.apply {
            removeMouseListener(mouseAdapter)
            removeMouseMotionListener(mouseAdapter)
            removeMouseWheelListener(wheelAdapter)
        }
        removeAll()
    }

    private inner class FocusRequester : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            requestFocus()
        }
    }

    private class DicomMouseAdapter(
        private val decorator: DicomComponentDecorator
    ) : MouseListener, MouseMotionListener {
        private val ops = arrayOf(
            WindowingOp(decorator.viewport),
            ScaleOp(decorator.viewport),
            TranslationOp(decorator.viewport),
            RotationOp(decorator.viewport),
            SelectionOp(decorator.viewport)
        )

        private fun onMouseEvent(e: MouseEvent) {
            ops[decorator.op.ordinal].onMouseEvent(e)
        }

        override fun mouseReleased(e: MouseEvent) = onMouseEvent(e)
        override fun mouseEntered(e: MouseEvent) = onMouseEvent(e)
        override fun mouseClicked(e: MouseEvent) = onMouseEvent(e)
        override fun mouseExited(e: MouseEvent) = onMouseEvent(e)
        override fun mousePressed(e: MouseEvent) = onMouseEvent(e)
        override fun mouseMoved(e: MouseEvent) = onMouseEvent(e)
        override fun mouseDragged(e: MouseEvent) = onMouseEvent(e)
    }

    private class DicomWheelAdapter(decorator: DicomComponentDecorator) : MouseWheelListener {
        private val op = ScaleWheelOp(decorator.viewport)

        override fun mouseWheelMoved(e: MouseWheelEvent) {
            op.onMouseEvent(e)
        }
    }
}