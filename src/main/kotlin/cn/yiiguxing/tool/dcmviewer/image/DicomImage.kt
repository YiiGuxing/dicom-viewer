@file:Suppress("MemberVisibilityCanBePrivate")

package cn.yiiguxing.tool.dcmviewer.image

import cn.yiiguxing.tool.dcmviewer.util.getValue
import cn.yiiguxing.tool.dcmviewer.util.setValue
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyFloatProperty
import javafx.beans.property.ReadOnlyFloatWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.WritableImage
import org.dcm4che3.data.Tag
import org.dcm4che3.imageio.plugins.dcm.DicomMetaData
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.WritableRaster

class DicomImage(
    val metadata: DicomMetaData,
    private val sourceRaster: WritableRaster,
    private val colorModel: ColorModel,
    val frame: Int
) {

    val width: Int = sourceRaster.width
    val height: Int = sourceRaster.height

    private val processor = DicomImageProcessor(metadata)
    private val buffer: IntArray = IntArray(width * 3)
    private val displayImage: WritableImage

    private val defaultWindowWidth: Float
    private val defaultWindowCenter: Float

    private val _windowWidthProperty: ReadOnlyFloatWrapper
    private val _windowCenterProperty: ReadOnlyFloatWrapper

    val windowWidthProperty: ReadOnlyFloatProperty
    val windowCenterProperty: ReadOnlyFloatProperty

    val windowWidth get() = windowWidthProperty.get()
    val windowCenter get() = windowCenterProperty.get()

    val inverseProperty: BooleanProperty = object : SimpleBooleanProperty(this, "inverse", false) {
        override fun invalidated() {
            updateImage()
        }
    }
    var inverse: Boolean by inverseProperty

    private var updateLazy = false
    private var changed = false

    val orientation: DoubleArray? = metadata.attributes
        .takeIf { it.contains(Tag.ImageOrientationPatient) }
        ?.getDoubles(Tag.ImageOrientationPatient)

    init {
        val (ww, wc) = processor.getDefaultWindowing(sourceRaster)
        defaultWindowWidth = ww
        defaultWindowCenter = wc
        displayImage = createDisplayImage(ww, wc)

        _windowWidthProperty = ReadOnlyFloatWrapper(this, "windowWidth", ww)
        _windowCenterProperty = ReadOnlyFloatWrapper(this, "windowCenter", wc)
        windowWidthProperty = _windowWidthProperty.readOnlyProperty
        windowCenterProperty = _windowCenterProperty.readOnlyProperty
    }

    private fun createDisplayImage(ww: Float, wc: Float): WritableImage {
        val colorModel = colorModel
        val sourceRaster = sourceRaster
        val image = WritableImage(width, height)
        if (colorModel.numComponents == 3) {
            val bufferedImage = BufferedImage(colorModel, sourceRaster, false, null)
            SwingFXUtils.toFXImage(bufferedImage, image)
        } else {
            processor.run {
                windowWidth = ww
                windowCenter = wc
                process(sourceRaster, image.pixelWriter, frame, buffer)
            }
        }

        return image
    }

    fun setColorWindowing(windowWidth: Float = this.windowWidth, windowCenter: Float = this.windowCenter) {
        if (colorModel.numComponents == 3) {
            return
        }
        val ww = maxOf(windowWidth, 1.0f)
        if (ww == this.windowWidth && windowCenter == this.windowCenter) {
            return
        }

        _windowWidthProperty.value = ww
        _windowCenterProperty.value = windowCenter
        updateImage()
    }

    fun resetImage() {
        updateLazy = true
        inverse = false
        setColorWindowing(defaultWindowWidth, defaultWindowCenter)
        updateLazy = false
        if (changed) {
            updateImage()
        }
    }

    private fun updateImage() {
        if (updateLazy) {
            changed = true
            return
        }

        changed = false

        if (colorModel.numComponents == 3) {
            return
        }

        val processor = processor
        processor.windowWidth = windowWidth
        processor.windowCenter = windowCenter
        processor.inverse = inverse
        processor.process(sourceRaster, displayImage.pixelWriter, frame, buffer)
    }

    fun draw(gc: GraphicsContext, x: Double = 0.0, y: Double = 0.0) {
        gc.drawImage(displayImage, x, y)
    }

}