package cn.yiiguxing.dicom.image

import cn.yiiguxing.dicom.getValue
import cn.yiiguxing.dicom.setValue
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyFloatProperty
import javafx.beans.property.ReadOnlyFloatWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import org.dcm4che3.data.Tag
import org.dcm4che3.imageio.plugins.dcm.DicomMetaData
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
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

    val orientation: DoubleArray? = metadata.attributes.getDoubles(Tag.ImageOrientationPatient)

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
        val width = width
        val height = height
        val buffer = buffer
        val colorModel = colorModel
        val sourceRaster = sourceRaster
        val image = WritableImage(width, height)
        val pixelWriter = image.pixelWriter

        if (colorModel.numComponents == 3) {
            val bufferHeight = buffer.size / width
            val pixelFormat = PixelFormat.getIntArgbInstance()
            for (h in 0 until height step bufferHeight) {
                val bh = minOf(height - h, bufferHeight)
                for (bHeight in 0 until bh) {
                    var i = 0
                    val cHeight = h + bHeight
                    for (w in 0 until width) {
                        buffer[i++] = colorModel.getRGB(sourceRaster.getSample(w, cHeight, 0))
                    }
                }

                pixelWriter.setPixels(0, h, width, bh, pixelFormat, buffer, 0, width)
            }
        } else {
            processor.run {
                windowWidth = ww
                windowCenter = wc
                process(sourceRaster, pixelWriter, frame, buffer)
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

        val processor = processor
        processor.windowWidth = windowWidth
        processor.windowCenter = windowCenter
        processor.inverse = inverse
        processor.process(sourceRaster, displayImage.pixelWriter, frame, buffer)
    }

    fun draw(g: Graphics2D, xform: AffineTransform? = null) {
    }

    fun draw(gc: GraphicsContext, x: Double = 0.0, y: Double = 0.0) {
        gc.drawImage(displayImage, x, y)
    }

}