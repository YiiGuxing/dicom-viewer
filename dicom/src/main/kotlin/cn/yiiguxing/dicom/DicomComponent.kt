package cn.yiiguxing.dicom

import cn.yiiguxing.dicom.graphics.DicomImageViewModel
import cn.yiiguxing.dicom.graphics.ViewModel
import cn.yiiguxing.dicom.graphics.Viewport
import cn.yiiguxing.dicom.graphics.draw
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.io.File
import javax.swing.JComponent

/**
 * DicomComponent
 *
 * Created by Yii.Guxing on 2018/11/17
 */
class DicomComponent(
    factory: DicomImageViewModel.ImageLoaderFactory = DicomImageViewModel.DefaultImageLoaderFactory
) : JComponent(), ViewModel.Callback {

    private val viewModel = DicomImageViewModel(factory).apply { callback = this@DicomComponent }

    val viewport: Viewport get() = viewModel.viewport

    fun setSrc(src: String?) {
        viewModel.setSrc(src?.let { File(src) })
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        super.setBounds(x, y, width, height)
        viewModel.setBounds(x, y, width, height)
    }

    override fun invalidateViewModel(who: ViewModel) {
        repaint()
    }

    override fun getComponentGraphics(g: Graphics): Graphics {
        return (super.getComponentGraphics(g) as Graphics2D).apply {
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        }
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.draw(viewModel)
    }
}