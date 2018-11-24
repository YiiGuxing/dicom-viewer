package cn.yiiguxing.tool.dcmviewer.image

import org.dcm4che3.image.StoredValue
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferShort

class ByteLookupTable internal constructor(
        inBits: StoredValue,
        outBits: Int,
        offset: Int,
        private val lut: ByteArray
) : LookupTable(inBits, outBits, offset) {

    internal constructor(inBits: StoredValue, outBits: Int, offset: Int, size: Int, flip: Boolean) :
            this(inBits, outBits, offset, ByteArray(size)) {
        val maxOut = (1 shl outBits) - 1
        val maxIndex = size - 1
        val midIndex = maxIndex / 2
        if (flip) {
            for (i in 0 until size) {
                lut[maxIndex - i] = ((i * maxOut + midIndex) / maxIndex).toByte()
            }
        } else {
            for (i in 0 until size) {
                lut[i] = ((i * maxOut + midIndex) / maxIndex).toByte()
            }
        }
    }

    override val length: Int = lut.size

    override fun lookupPixel(pixel: Int): Int {
        var index = inBits.valueOf(pixel) - offset
        index = minOf(maxOf(0, index), lut.size - 1)

        return lut[index].toInt()
    }

    override fun adjustOutBits(outBits: Int): LookupTable {
        var diff = outBits - this.outBits
        if (diff != 0) {
            val lut = this.lut
            if (outBits > 8) {
                val ss = ShortArray(lut.size)
                for (i in lut.indices) {
                    ss[i] = (lut[i].toInt() and 0xff shl diff).toShort()
                }
                return ShortLookupTable(inBits, outBits, offset, ss)
            }
            if (diff < 0) {
                diff = -diff
                for (i in lut.indices) {
                    lut[i] = (lut[i].toInt() and 0xff shr diff).toByte()
                }
            } else {
                for (i in lut.indices) {
                    lut[i] = (lut[i].toInt() shl diff).toByte()
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
            lut[i] = (maxOut - lut[i]).toByte()
        }
    }


    override fun combine(other: LookupTable): LookupTable {
        val lut = DataBufferByte(lut, lut.size)
        if (other.outBits > 8) {
            val result = DataBufferShort(lut.size)
            other.lookup(lut, 0, result, 0, lut.size)
            return ShortLookupTable(inBits, other.outBits, offset, result.data)
        }

        other.lookup(lut, 0, lut, 0, lut.size)
        this.outBits = other.outBits
        return this
    }

}
