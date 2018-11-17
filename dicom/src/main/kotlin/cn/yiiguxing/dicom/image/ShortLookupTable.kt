package cn.yiiguxing.dicom.image

import org.dcm4che3.image.StoredValue
import java.awt.image.DataBufferShort

class ShortLookupTable internal constructor(
        inBits: StoredValue,
        outBits: Int,
        offset: Int,
        private val lut: ShortArray
) : LookupTable(inBits, outBits, offset) {

    internal constructor(inBits: StoredValue, outBits: Int, offset: Int, size: Int, flip: Boolean) :
            this(inBits, outBits, offset, ShortArray(size)) {
        val maxOut = (1 shl outBits) - 1
        val maxIndex = size - 1
        val midIndex = size / 2
        if (flip) {
            for (i in 0 until size) {
                lut[maxIndex - i] = ((i * maxOut + midIndex) / maxIndex).toShort()
            }
        } else {
            for (i in 0 until size) {
                lut[i] = ((i * maxOut + midIndex) / maxIndex).toShort()
            }
        }
    }

    override val length: Int = lut.size

    override fun lookupPixel(pixel: Int): Int {
        var index = inBits.valueOf(pixel) - offset
        index = Math.min(Math.max(0, index), lut.size - 1)
        return lut[index].toInt()
    }

    override fun adjustOutBits(outBits: Int): LookupTable {
        var diff = outBits - this.outBits
        if (diff != 0) {
            val lut = this.lut
            if (diff < 0) {
                diff = -diff
                for (i in lut.indices) {
                    lut[i] = (lut[i].toInt() and 0xffff shr diff).toShort()
                }
            } else {
                for (i in lut.indices) {
                    lut[i] = (lut[i].toInt() shl diff).toShort()
                }
            }
            this.outBits = outBits
        }
        return this
    }

    override fun inverse() {
        val lut = this.lut
        val maxOut = (1 shl outBits) - 1
        for (i in lut.indices) {
            lut[i] = (maxOut - lut[i]).toShort()
        }
    }

    override fun combine(other: LookupTable): LookupTable {
        val lut = DataBufferShort(lut, lut.size)
        other.lookup(lut, 0, lut, 0, lut.size)
        this.outBits = other.outBits
        return this
    }
}
