package cn.yiiguxing.dicom.image

import org.dcm4che3.image.StoredValue
import java.awt.image.ComponentSampleModel
import java.awt.image.DataBuffer
import java.awt.image.Raster

abstract class LookupTable(protected var inBits: StoredValue, var outBits: Int, protected var offset: Int) {

    abstract val length: Int

    fun lookup(srcRaster: Raster, destRaster: Raster) {
        val srcSampleModel = srcRaster.sampleModel as ComponentSampleModel
        val destSampleModel = destRaster.sampleModel as ComponentSampleModel
        val srcBuffer = srcRaster.dataBuffer
        val destBuffer = destRaster.dataBuffer
        lookup(srcSampleModel, srcBuffer, destSampleModel, destBuffer)
    }

    private fun lookup(sampleModel: ComponentSampleModel, src: DataBuffer,
                       destSampleModel: ComponentSampleModel, dest: DataBuffer) {
        val w = sampleModel.width
        val h = sampleModel.height
        val stride = sampleModel.scanlineStride
        val destStride = destSampleModel.scanlineStride
        for (y in 0 until h) {
            lookup(src, y * stride, dest, y * destStride, w)
        }
    }

    fun lookup(src: DataBuffer, srcPos: Int, dest: DataBuffer, destPos: Int, length: Int) {
        var i = srcPos
        val endPos = srcPos + length
        var j = destPos
        while (i < endPos) {
            dest[j++] = lookupPixel(src[i++])
        }
    }

    abstract fun lookupPixel(pixel: Int): Int

    abstract fun adjustOutBits(outBits: Int): LookupTable

    abstract fun inverse()

    abstract fun combine(other: LookupTable): LookupTable

}
