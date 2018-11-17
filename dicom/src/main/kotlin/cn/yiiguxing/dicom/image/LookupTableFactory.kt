package cn.yiiguxing.dicom.image

import org.dcm4che3.data.Attributes
import org.dcm4che3.data.Tag
import org.dcm4che3.image.StoredValue
import org.dcm4che3.util.ByteUtils
import java.awt.image.ComponentSampleModel
import java.awt.image.Raster


class LookupTableFactory(private val storedValue: StoredValue) {

    private var rescaleSlope = 1f
    private var rescaleIntercept = 0f
    private var modalityLUT: LookupTable? = null
    private var windowCenter: Float = 0.toFloat()
    private var windowWidth: Float = 0.toFloat()
    private val voiLUTFunction: String? = null // not yet implemented
    private var voiLUT: LookupTable? = null
    private var presentationLUT: LookupTable? = null
    private var inverse: Boolean = false

    fun setModalityLUT(attrs: Attributes) {
        rescaleIntercept = attrs.getFloat(Tag.RescaleIntercept, 0f)
        rescaleSlope = attrs.getFloat(Tag.RescaleSlope, 1f)
        modalityLUT = createLUT(storedValue, attrs.getNestedDataset(Tag.ModalityLUTSequence))
    }

    fun setPresentationLUT(attrs: Attributes) {
        val pLUT = attrs.getNestedDataset(Tag.PresentationLUTSequence)
        if (pLUT != null) {
            val desc = pLUT.getInts(Tag.LUTDescriptor)
            if (desc != null && desc.size == 3) {
                val len = if (desc[0] == 0) 0x10000 else desc[0]
                val inBits = StoredValue.Unsigned(log2(len))
                presentationLUT = createLUT(inBits, resetOffset(desc), pLUT.getSafeBytes(Tag.LUTData), pLUT.bigEndian())
            }
        } else {
            val pShape = attrs.getString(Tag.PresentationLUTShape)
            inverse = if (pShape != null) {
                "INVERSE" == pShape
            } else {
                "MONOCHROME1" == attrs.getString(Tag.PhotometricInterpretation)
            }
        }
    }

    private fun resetOffset(desc: IntArray): IntArray {
        if (desc[1] == 0) {
            return desc
        }

        val copy = desc.clone()
        copy[1] = 0
        return copy
    }

    fun setWindowCenter(windowCenter: Float) {
        this.windowCenter = windowCenter
    }

    fun setWindowWidth(windowWidth: Float) {
        this.windowWidth = windowWidth
    }

    fun setVOI(img: Attributes?, windowIndex: Int, voiLUTIndex: Int, preferWindow: Boolean) {
        if (img == null)
            return

        val vLUT = img.getNestedDataset(Tag.VOILUTSequence, voiLUTIndex)
        if (preferWindow || vLUT == null) {
            val wcs = img.getFloats(Tag.WindowCenter)
            val wws = img.getFloats(Tag.WindowWidth)
            if (wcs != null && wcs.isNotEmpty() && wws != null && wws.isNotEmpty()) {
                val index = if (windowIndex < Math.min(wcs.size, wws.size)) windowIndex else 0
                windowCenter = wcs[index]
                windowWidth = wws[index]
                return
            }
        }
        if (vLUT != null) {
            val inBits = modalityLUT?.let { StoredValue.Unsigned(it.outBits) } ?: storedValue
            voiLUT = createLUT(inBits, vLUT)
        }
    }

    private fun createLUT(inBits: StoredValue, attrs: Attributes?): LookupTable? {
        return if (attrs == null) null else createLUT(inBits, attrs.getInts(Tag.LUTDescriptor),
                attrs.getSafeBytes(Tag.LUTData), attrs.bigEndian())

    }

    private fun createLUT(inBits: StoredValue, desc: IntArray?, data: ByteArray?, bigEndian: Boolean): LookupTable? {
        var dataArr = data ?: return null
        val descArr = desc?.takeIf { it.size == 3 } ?: return null

        val len = if (descArr[0] == 0) 0x10000 else descArr[0]
        val offset = descArr[1].toShort().toInt()
        val outBits = descArr[2]

        if (dataArr.size == len shl 1) {
            if (outBits > 8) {
                if (outBits > 16) {
                    return null
                }

                val ss = ShortArray(len)
                if (bigEndian)
                    for (i in ss.indices) {
                        ss[i] = ByteUtils.bytesToShortBE(dataArr, i shl 1).toShort()
                    }
                else {
                    for (i in ss.indices) {
                        ss[i] = ByteUtils.bytesToShortLE(dataArr, i shl 1).toShort()
                    }
                }

                return ShortLookupTable(inBits, outBits, offset, ss)
            }
            // padded high bits -> use low bits
            dataArr = halfLength(dataArr, if (bigEndian) 1 else 0)
        }

        if (dataArr.size != len) {
            return null
        }

        return if (outBits > 8) null else ByteLookupTable(inBits, outBits, offset, dataArr)
    }

    fun createLUT(outBits: Int): LookupTable {
        val presentationLUT = presentationLUT
        var lut = combineModalityVOILUT(presentationLUT?.let { log2(it.length) } ?: outBits)
        if (presentationLUT != null) {
            lut = lut.combine(presentationLUT.adjustOutBits(outBits))
        } else if (inverse) {
            lut.inverse()
        }
        return lut
    }

    private fun combineModalityVOILUT(outBits: Int): LookupTable {
        val m = rescaleSlope
        val b = rescaleIntercept
        val modalityLUT = this.modalityLUT
        var lut = this.voiLUT
        if (lut == null) {
            val c = windowCenter
            val w = windowWidth

            if (w == 0f && modalityLUT != null) {
                return modalityLUT.adjustOutBits(outBits)
            }

            val size: Int
            val offset: Int
            val inBits = if (modalityLUT != null) StoredValue.Unsigned(modalityLUT.outBits) else storedValue
            if (w != 0f) {
                size = Math.max(2, Math.abs(Math.round(w / m)))
                offset = Math.round((c - b) / m) - size / 2
            } else {
                offset = inBits.minValue()
                size = inBits.maxValue() - inBits.minValue() + 1
            }
            lut = if (outBits > 8) {
                ShortLookupTable(inBits, outBits, offset, size, m < 0)
            } else {
                ByteLookupTable(inBits, outBits, offset, size, m < 0)
            }
        } else {
            //TODO consider m+b
            lut = lut.adjustOutBits(outBits)
        }

        return modalityLUT?.combine(lut) ?: lut
    }

    fun autoWindowing(img: Attributes, raster: Raster): Boolean {
        if (modalityLUT != null || voiLUT != null || windowWidth != 0f) {
            return false
        }

        var min = img.getInt(Tag.SmallestImagePixelValue, 0)
        var max = img.getInt(Tag.LargestImagePixelValue, 0)
        if (max == 0) {
            calcMinMax(storedValue, raster).let { (minVal, maxVal) ->
                min = minVal
                max = maxVal
            }
        }
        windowCenter = (min + max + 1) / 2 * rescaleSlope + rescaleIntercept
        windowWidth = Math.abs((max + 1 - min) * rescaleSlope)
        return true
    }

    companion object {
        fun calcMinMax(storedValue: StoredValue, raster: Raster): Pair<Int, Int> {
            val sm = raster.sampleModel as ComponentSampleModel
            val data = raster.dataBuffer

            var min = Integer.MAX_VALUE
            var max = Integer.MIN_VALUE
            val w = sm.width
            val h = sm.height
            val stride = sm.scanlineStride
            for (y in 0 until h) {
                var i = y * stride
                val end = i + w
                while (i < end) {
                    val value = storedValue.valueOf(data[i++])
                    if (value < min) min = value
                    if (value > max) max = value
                }
            }
            return min to max
        }

        private fun halfLength(data: ByteArray, hilo: Int): ByteArray {
            val bs = ByteArray(data.size shr 1)
            for (i in bs.indices) {
                bs[i] = data[i shl 1 or hilo]
            }

            return bs
        }

        private fun log2(value: Int): Int {
            var i = 0
            while (value.ushr(i) != 0) {
                ++i
            }
            return i - 1
        }
    }
}
