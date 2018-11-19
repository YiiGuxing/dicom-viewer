package cn.yiiguxing.dicom.image

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

    val windowWidth get() = processor.windowWidth ?: defaultWindowWidth
    val windowCenter get() = processor.windowCenter ?: defaultWindowCenter
    val inverse get() = processor.inverse

    val orientation: DoubleArray? = metadata.attributes.getDoubles(Tag.ImageOrientationPatient)

    init {
        val (ww, wc) = processor.getDefaultWindowing(sourceRaster)
        defaultWindowWidth = ww
        defaultWindowCenter = wc
        displayImage = createDisplayImage(ww, wc)
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

    fun updateImage(
        windowWidth: Float = this.windowWidth,
        windowCenter: Float = this.windowCenter,
        inverse: Boolean = this.inverse
    ) {
        if (colorModel.numComponents == 3) {
            return
        }
        val ww = maxOf(windowWidth, 1.0f)
        if (ww == this.windowWidth && windowCenter == this.windowCenter && inverse == processor.inverse) {
            return
        }

        val processor = processor
        processor.windowWidth = ww
        processor.windowCenter = windowCenter
        processor.inverse = inverse
        processor.process(sourceRaster, displayImage.pixelWriter, frame, buffer)
    }

    fun resetImage() {
        updateImage(defaultWindowWidth, defaultWindowCenter, false)
    }

    fun draw(g: Graphics2D, xform: AffineTransform? = null) {
    }

    fun draw(gc: GraphicsContext, x: Double = 0.0, y: Double = 0.0) {
        gc.drawImage(displayImage, x, y)
    }

}