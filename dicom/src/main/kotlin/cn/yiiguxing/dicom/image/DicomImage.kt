package cn.yiiguxing.dicom.image

import org.dcm4che3.data.Tag
import org.dcm4che3.imageio.plugins.dcm.DicomMetaData
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.WritableRaster

class DicomImage(
    val metadata: DicomMetaData,
    private val sourceRaster: WritableRaster,
    private val colorModel: ColorModel,
    val frame: Int
) {

    private val processor = DicomImageProcessor(metadata)

    private var displayRaster: WritableRaster
    private var displayImage: BufferedImage

    private val defaultWindowWidth: Float
    private val defaultWindowCenter: Float

    val width: Int = sourceRaster.width
    val height: Int = sourceRaster.height
    val orientation: DoubleArray? = metadata.attributes.getDoubles(Tag.ImageOrientationPatient)

    val windowWidth get() = processor.windowWidth ?: defaultWindowWidth
    val windowCenter get() = processor.windowCenter ?: defaultWindowCenter
    val inverse get() = processor.inverse

    init {
        val (ww, wc) = processor.getDefaultWindowing(sourceRaster)
        defaultWindowWidth = ww
        defaultWindowCenter = wc

        if (colorModel.numComponents == 3) {
            displayRaster = sourceRaster
        } else {
            displayRaster = processor.run {
                windowWidth = ww
                windowCenter = wc
                process(sourceRaster, null, frame)
            }
        }
        @Suppress("UndesirableClassUsage")
        displayImage = BufferedImage(colorModel, displayRaster, false, null)
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

        processor.let {
            it.windowWidth = ww
            it.windowCenter = windowCenter
            it.inverse = inverse
        }
        updateImage()
    }

    fun resetImage() {
        updateImage(defaultWindowWidth, defaultWindowCenter, false)
    }

    private fun updateImage() {
        val dstRaster = processor.process(sourceRaster, displayRaster, frame)
        if (dstRaster !== displayRaster) {
            displayRaster = dstRaster
            @Suppress("UndesirableClassUsage")
            displayImage = BufferedImage(colorModel, dstRaster, false, null)
        }
    }

    fun draw(g: Graphics2D, xform: AffineTransform? = null) {
        g.drawRenderedImage(displayImage, xform)
    }

}