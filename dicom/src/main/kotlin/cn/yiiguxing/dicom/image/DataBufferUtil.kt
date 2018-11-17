/*
 * DataBufferUtil
 * 
 * Created by Yii.Guxing on 2018/10/31.
 */

@file:Suppress("UseWithIndex", "NOTHING_TO_INLINE")

package cn.yiiguxing.dicom.image

import org.dcm4che3.io.DicomInputStream
import java.awt.image.DataBuffer
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferUShort
import java.io.EOFException
import java.io.InputStream
import javax.imageio.stream.ImageInputStream

private const val BUFFER_SIZE = 2048

inline operator fun DataBuffer.get(i: Int) = getElem(i)
inline operator fun DataBuffer.set(i: Int, value: Int) = setElem(i, value)
inline operator fun DataBuffer.get(bank: Int, i: Int) = getElem(bank, i)
inline operator fun DataBuffer.set(bank: Int, i: Int, value: Int) = setElem(bank, i, value)

fun DataBufferByte.setElements(elements: ByteArray, bank: Int = 0, offset: Int = 0,
                               elementsOffset: Int = 0, length: Int = elements.size) {
    var count = 0
    for (i in elementsOffset until length) {
        setElem(bank, offset + count++, elements[i].toInt())
    }
}

fun DataBufferUShort.setElements(elements: ShortArray, bank: Int = 0, offset: Int = 0,
                                 elementsOffset: Int = 0, length: Int = elements.size) {
    var count = 0
    for (i in elementsOffset until length) {
        setElem(bank, offset + count++, elements[i].toInt())
    }
}

fun DataBuffer.swapShorts() {
    var carry = 0
    for (bank in 0 until numBanks) {
        if (carry != 0) {
            swapLastFirst(bank - 1, bank)
        }
        val length = size - carry
        swapShorts(bank, carry, length and 1.inv())
        carry = length and 1
    }
}

fun DataBuffer.swapShorts(bank: Int, offset: Int, length: Int) {
    require(length >= 0 && (length % 2) == 0) { "length: $length" }
    var i = offset
    val n = offset + length
    while (i < n) {
        swap(bank, i, i + 1)
        i += 2
    }
}

private fun DataBuffer.swap(bank: Int, i1: Int, i2: Int) {
    val tmp = this[bank, i1]
    this[bank, i1] = this[bank, i2]
    this[bank, i2] = tmp
}

private fun DataBuffer.swapLastFirst(bank1: Int, bank2: Int) {
    val last = size - 1
    val tmp = this[bank2, 0]
    this[bank2, 0] = this[bank1, last]
    this[bank1, last] = tmp
}

fun InputStream.readFully(buffer: DataBufferByte, bank: Int = 0) {
    require(bank < buffer.numBanks) {
        "bank must be less than buffer.numBanks: bank=$bank, buffer.numBanks=${buffer.numBanks}"
    }

    val buf = ByteArray(BUFFER_SIZE)
    var pos = 0
    var length = buffer.size
    while (length > 0) {
        val count = read(buf, 0, minOf(length, buf.size))
        if (count == -1) {
            throw EOFException()
        }

        buffer.setElements(buf, bank, pos, length = count)
        pos += count
        length -= count
    }
}

fun ImageInputStream.readFully(buffer: DataBufferByte, bank: Int = 0) {
    require(bank < buffer.numBanks) {
        "bank must be less than buffer.numBanks: bank=$bank, buffer.numBanks=${buffer.numBanks}"
    }

    val buf = ByteArray(BUFFER_SIZE)
    var pos = 0
    var length = buffer.size
    while (length > 0) {
        val count = read(buf, 0, minOf(length, buf.size))
        if (count == -1) {
            throw EOFException()
        }

        buffer.setElements(buf, bank, pos, length = count)
        pos += count
        length -= count
    }
}

fun ImageInputStream.readFully(buffer: DataBufferUShort, bank: Int = 0) {
    require(bank < buffer.numBanks) {
        "bank must be less than buffer.numBanks: bank=$bank, buffer.numBanks=${buffer.numBanks}"
    }

    val buf = ShortArray(BUFFER_SIZE)
    var pos = 0
    var length = buffer.size
    while (length > 0) {
        val needLength = minOf(length, buf.size)
        readFully(buf, 0, needLength)
        buffer.setElements(buf, bank, pos, length = needLength)
        pos += needLength
        length -= needLength
    }
}

fun DicomInputStream.readFully(buffer: DataBufferUShort, bank: Int = 0) {
    require(bank < buffer.numBanks) {
        "bank must be less than buffer.numBanks: bank=$bank, buffer.numBanks=${buffer.numBanks}"
    }

    val buf = ShortArray(BUFFER_SIZE)
    var pos = 0
    var length = buffer.size
    while (length > 0) {
        val needLength = minOf(length, buf.size)
        readFully(buf, 0, needLength)
        buffer.setElements(buf, bank, pos, length = needLength)
        pos += needLength
        length -= needLength
    }
}