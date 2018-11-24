package cn.yiiguxing.tool.dcmviewer.image

import javafx.scene.image.PixelWriter
import org.dcm4che3.data.Attributes
import org.dcm4che3.data.Tag
import org.dcm4che3.image.Overlays
import org.dcm4che3.image.PhotometricInterpretation
import org.dcm4che3.image.StoredValue
import org.dcm4che3.imageio.plugins.dcm.DicomMetaData
import org.dcm4che3.util.TagUtils
import java.awt.image.DataBuffer
import java.awt.image.Raster
import java.awt.image.SampleModel
import java.awt.image.WritableRaster
import java.util.*

class DicomImageProcessor(private val metadata: DicomMetaData) {

    private val frames: Int

    private val width: Int

    private val height: Int

    private val samples: Int

    private val banded: Boolean

    private val bitsAllocated: Int

    private val bitsStored: Int

    private val dataType: Int

    private val rescaleIntercept: Float

    private val rescaleSlope: Float

    private val storedValue: StoredValue

    private val lutFactory: LookupTableFactory

    private val pmi: PhotometricInterpretation

    var windowWidth: Float? = null
    var windowCenter: Float? = null
    var windowIndex: Int = 0
    var voiLUTIndex: Int = 0
    var isPreferWindow: Boolean = true
    var inverse: Boolean = false
    var overlayActivationMask: Int = DEFAULT_OVERLAY_ACTIVATION_MASK
    var overlayGrayScaleValue: Int = DEFAULT_OVERLAY_GRAYSCALE_VALUE
    var presentationState: Attributes? = null

    init {
        val ds = metadata.attributes
        frames = ds.getInt(Tag.NumberOfFrames, 1)
        width = ds.getInt(Tag.Columns, 0)
        height = ds.getInt(Tag.Rows, 0)
        samples = ds.getInt(Tag.SamplesPerPixel, 1)
        banded = samples > 1 && ds.getInt(Tag.PlanarConfiguration, 0) != 0
        bitsAllocated = ds.getInt(Tag.BitsAllocated, 8)
        bitsStored = ds.getInt(Tag.BitsStored, bitsAllocated)
        dataType = if (bitsAllocated <= 8) DataBuffer.TYPE_BYTE else DataBuffer.TYPE_USHORT
        rescaleIntercept = ds.getFloat(Tag.RescaleIntercept, 0f)
        rescaleSlope = ds.getFloat(Tag.RescaleSlope, 1f)
        storedValue = StoredValue.valueOf(ds)
        lutFactory = LookupTableFactory(storedValue)
        pmi = PhotometricInterpretation.fromString(ds.getString(Tag.PhotometricInterpretation, "MONOCHROME2"))
    }

    fun getDefaultWindowing(raster: Raster): Pair<Float, Float> {
        val img = metadata.attributes
        var windowWidth = img.getFloat(Tag.WindowWidth, 0f)
        var windowCenter = img.getFloat(Tag.WindowCenter, 0f)
        if (windowWidth == 0f) {
            var min = img.getInt(Tag.SmallestImagePixelValue, 0)
            var max = img.getInt(Tag.LargestImagePixelValue, 0)
            if (max == 0) {
                LookupTableFactory.calcMinMax(storedValue, raster).let { (minVal, maxVal) ->
                    min = minVal
                    max = maxVal
                }
            }

            windowCenter = (min + max + 1) / 2 * rescaleSlope + rescaleIntercept
            windowWidth = Math.abs((max + 1 - min) * rescaleSlope)
        }

        return windowWidth to windowCenter
    }

    fun process(srcRaster: Raster, dst: PixelWriter, frameIndex: Int, processingBuffer: IntArray) {
        val overlayGroupOffsets = getActiveOverlayGroupOffsets()
        val overlayData = arrayOfNulls<ByteArray>(overlayGroupOffsets.size)
        for (i in overlayGroupOffsets.indices) {
            overlayData[i] = extractOverlay(overlayGroupOffsets[i], srcRaster)
        }
        applyLUTs(srcRaster, dst, frameIndex, 8, processingBuffer)
        for (i in overlayGroupOffsets.indices) {
            applyOverlay(overlayGroupOffsets[i], dst, frameIndex, 8, overlayData[i])
        }
    }

    private fun applyLUTs(raster: Raster, dst: PixelWriter, frameIndex: Int, outBits: Int, processingBuffer: IntArray) {
        val imgAttrs = metadata.attributes
        val psAttrs = presentationState
        val lutParam = lutFactory
        if (psAttrs != null) {
            lutParam.setModalityLUT(psAttrs)
            lutParam.setVOI(
                selectVOILUT(psAttrs, imgAttrs.getString(Tag.SOPInstanceUID), frameIndex + 1),
                0, 0, false
            )
            lutParam.setPresentationLUT(psAttrs)
        } else {
            val sharedFctGroups = imgAttrs.getNestedDataset(
                Tag.SharedFunctionalGroupsSequence
            )
            val frameFctGroups = imgAttrs.getNestedDataset(
                Tag.PerFrameFunctionalGroupsSequence, frameIndex
            )
            lutParam.setModalityLUT(
                selectFctGroup(
                    imgAttrs, sharedFctGroups, frameFctGroups,
                    Tag.PixelValueTransformationSequence
                )
            )
            val ww = windowWidth
            val wc = windowCenter
            if (ww == null || ww == 0.0f) {
                lutParam.setVOI(
                    selectFctGroup(imgAttrs, sharedFctGroups, frameFctGroups, Tag.FrameVOILUTSequence),
                    windowIndex,
                    voiLUTIndex,
                    isPreferWindow
                )
            }
            if (ww == null || wc == null) {
                lutParam.autoWindowing(imgAttrs, raster)
            } else {
                lutParam.setWindowWidth(ww)
                lutParam.setWindowCenter(wc)
            }
            lutParam.setPresentationLUT(imgAttrs)
        }
        val lut = lutParam.createLUT(outBits)
        if (inverse) {
            lut.inverse()
        }

        lut.lookup(raster, dst,processingBuffer)
    }

    private fun applyOverlay(
        gg0000: Int, raster: PixelWriter,
        frameIndex: Int, outBits: Int, ovlyData: ByteArray?
    ) {
        var ovlyAttrs = metadata.attributes
        val grayscaleValue: Int
        val psAttrs = presentationState
        if (psAttrs != null) {
            if (psAttrs.containsValue(Tag.OverlayData or gg0000))
                ovlyAttrs = psAttrs
            grayscaleValue = Overlays.getRecommendedDisplayGrayscaleValue(psAttrs, gg0000)
        } else
            grayscaleValue = overlayGrayScaleValue
        applyOverlay(
            if (ovlyData != null) 0 else frameIndex, raster,
            ovlyAttrs, gg0000, grayscaleValue.ushr(16 - outBits), ovlyData
        )
    }

    fun applyOverlay(
        frameIndex: Int, writer: PixelWriter,
        attrs: Attributes, gg0000: Int, pixelValue: Int, ovlyData: ByteArray?
    ) {
        var ovlyData = ovlyData

        val imageFrameOrigin = attrs.getInt(Tag.ImageFrameOrigin or gg0000, 1)
        val framesInOverlay = attrs.getInt(Tag.NumberOfFramesInOverlay or gg0000, 1)
        val ovlyFrameIndex = frameIndex - imageFrameOrigin + 1
        if (ovlyFrameIndex < 0 || ovlyFrameIndex >= framesInOverlay)
            return

        val tagOverlayRows = Tag.OverlayRows or gg0000
        val tagOverlayColumns = Tag.OverlayColumns or gg0000
        val tagOverlayData = Tag.OverlayData or gg0000
        val tagOverlayOrigin = Tag.OverlayOrigin or gg0000

        val ovlyRows = attrs.getInt(tagOverlayRows, -1)
        val ovlyColumns = attrs.getInt(tagOverlayColumns, -1)
        val ovlyOrigin = attrs.getInts(tagOverlayOrigin)
        if (ovlyData == null)
            ovlyData = attrs.getSafeBytes(tagOverlayData)

        if (ovlyData == null)
            throw IllegalArgumentException(
                "Missing "
                        + TagUtils.toString(tagOverlayData)
                        + " Overlay Data"
            )
        if (ovlyRows <= 0)
            throw IllegalArgumentException(
                TagUtils.toString(tagOverlayRows)
                        + " Overlay Rows [" + ovlyRows + "]"
            )
        if (ovlyColumns <= 0)
            throw IllegalArgumentException(
                TagUtils.toString(tagOverlayColumns)
                        + " Overlay Columns [" + ovlyColumns + "]"
            )
        if (ovlyOrigin == null)
            throw IllegalArgumentException(
                "Missing "
                        + TagUtils.toString(tagOverlayOrigin)
                        + " Overlay Origin"
            )
        if (ovlyOrigin.size != 2)
            throw IllegalArgumentException(
                TagUtils.toString(tagOverlayOrigin)
                        + " Overlay Origin " + Arrays.toString(ovlyOrigin)
            )

        val x0 = ovlyOrigin[1] - 1
        val y0 = ovlyOrigin[0] - 1

        val ovlyLen = ovlyRows * ovlyColumns
        val ovlyOff = ovlyLen * ovlyFrameIndex
        var i = ovlyOff.ushr(3)
        val end = (ovlyOff + ovlyLen + 7).ushr(3)
        while (i < end) {
            val ovlyBits = ovlyData[i].toInt() and 0xff
            var j = 0
            while (ovlyBits.ushr(j) != 0) {
                if (ovlyBits and (1 shl j) == 0) {
                    j++
                    continue
                }

                val ovlyIndex = (i shl 3) + j - ovlyOff
                if (ovlyIndex >= ovlyLen) {
                    j++
                    continue
                }

                val y = y0 + ovlyIndex / ovlyColumns
                val x = x0 + ovlyIndex % ovlyColumns
                try {
                    writer.setArgb(x, y, pixelValue)
                } catch (ignore: ArrayIndexOutOfBoundsException) {
                }
                j++
            }
            i++
        }
    }

    fun process(srcRaster: Raster, destRaster: WritableRaster?, frameIndex: Int): WritableRaster {
        val overlayGroupOffsets = getActiveOverlayGroupOffsets()
        val overlayData = arrayOfNulls<ByteArray>(overlayGroupOffsets.size)
        for (i in overlayGroupOffsets.indices) {
            overlayData[i] = extractOverlay(overlayGroupOffsets[i], srcRaster)
        }
        val sm = createSampleModel(DataBuffer.TYPE_BYTE, false)
        val result = applyLUTs(srcRaster, destRaster, frameIndex, sm, 8)
        for (i in overlayGroupOffsets.indices) {
            applyOverlay(overlayGroupOffsets[i], result, frameIndex, 8, overlayData[i])
        }
        return result
    }

    private fun createSampleModel(dataType: Int, banded: Boolean): SampleModel {
        return pmi.createSampleModel(dataType, width, height, samples, banded)
    }

    private fun getActiveOverlayGroupOffsets(): IntArray {
        return if (presentationState != null)
            Overlays.getActiveOverlayGroupOffsets(presentationState)
        else
            Overlays.getActiveOverlayGroupOffsets(metadata.attributes, overlayActivationMask)
    }

    private fun extractOverlay(gg0000: Int, raster: Raster): ByteArray? {
        val attrs = metadata.attributes
        if (attrs.getInt(Tag.OverlayBitsAllocated or gg0000, 1) == 1)
            return null

        val ovlyRows = attrs.getInt(Tag.OverlayRows or gg0000, 0)
        val ovlyColumns = attrs.getInt(Tag.OverlayColumns or gg0000, 0)
        val bitPosition = attrs.getInt(Tag.OverlayBitPosition or gg0000, 0)

        val mask = 1 shl bitPosition
        val length = ovlyRows * ovlyColumns

        val ovlyData = ByteArray((length + 7).ushr(3) + 1 and 1.inv())
        val bitsAllocated = attrs.getInt(Tag.BitsAllocated, 8)
        val bitsStored = attrs.getInt(Tag.BitsStored, bitsAllocated)

        if (bitPosition >= bitsStored)
            Overlays.extractFromPixeldata(raster, mask, ovlyData, 0, length)
        return ovlyData
    }

    private fun applyOverlay(
        gg0000: Int, raster: WritableRaster,
        frameIndex: Int, outBits: Int, ovlyData: ByteArray?
    ) {
        var ovlyAttrs = metadata.attributes
        val grayscaleValue: Int
        val psAttrs = presentationState
        if (psAttrs != null) {
            if (psAttrs.containsValue(Tag.OverlayData or gg0000))
                ovlyAttrs = psAttrs
            grayscaleValue = Overlays.getRecommendedDisplayGrayscaleValue(psAttrs, gg0000)
        } else
            grayscaleValue = overlayGrayScaleValue
        Overlays.applyOverlay(
            if (ovlyData != null) 0 else frameIndex, raster,
            ovlyAttrs, gg0000, grayscaleValue.ushr(16 - outBits), ovlyData
        )
    }

    private fun applyLUTs(
        raster: Raster, dest: WritableRaster?,
        frameIndex: Int, sm: SampleModel, outBits: Int
    ): WritableRaster {
        val destRaster = if (sm.dataType == dest?.sampleModel?.dataType)
            dest
        else
            Raster.createWritableRaster(sm, null)
        val imgAttrs = metadata.attributes
        val psAttrs = presentationState
        val lutParam = lutFactory
        if (psAttrs != null) {
            lutParam.setModalityLUT(psAttrs)
            lutParam.setVOI(
                selectVOILUT(psAttrs, imgAttrs.getString(Tag.SOPInstanceUID), frameIndex + 1),
                0, 0, false
            )
            lutParam.setPresentationLUT(psAttrs)
        } else {
            val sharedFctGroups = imgAttrs.getNestedDataset(
                Tag.SharedFunctionalGroupsSequence
            )
            val frameFctGroups = imgAttrs.getNestedDataset(
                Tag.PerFrameFunctionalGroupsSequence, frameIndex
            )
            lutParam.setModalityLUT(
                selectFctGroup(
                    imgAttrs, sharedFctGroups, frameFctGroups,
                    Tag.PixelValueTransformationSequence
                )
            )
            val ww = windowWidth
            val wc = windowCenter
            if (ww == null || ww == 0.0f) {
                lutParam.setVOI(
                    selectFctGroup(imgAttrs, sharedFctGroups, frameFctGroups, Tag.FrameVOILUTSequence),
                    windowIndex,
                    voiLUTIndex,
                    isPreferWindow
                )
            }
            if (ww == null || wc == null) {
                lutParam.autoWindowing(imgAttrs, raster)
            } else {
                lutParam.setWindowWidth(ww)
                lutParam.setWindowCenter(wc)
            }
            lutParam.setPresentationLUT(imgAttrs)
        }
        val lut = lutParam.createLUT(outBits)
        if (inverse) {
            lut.inverse()
        }

        lut.lookup(raster, destRaster)
        return destRaster
    }

    private fun selectFctGroup(
        imgAttrs: Attributes,
        sharedFctGroups: Attributes?,
        frameFctGroups: Attributes?,
        tag: Int
    ): Attributes {
        if (frameFctGroups == null) {
            return imgAttrs
        }
        var group: Attributes? = frameFctGroups.getNestedDataset(tag)
        if (group == null && sharedFctGroups != null) {
            group = sharedFctGroups.getNestedDataset(tag)
        }
        return group ?: imgAttrs
    }

    private fun selectVOILUT(psAttrs: Attributes, iuid: String, frame: Int): Attributes? {
        val voiLUTs = psAttrs.getSequence(Tag.SoftcopyVOILUTSequence)
        if (voiLUTs != null)
            for (voiLUT in voiLUTs) {
                val refImgs = voiLUT.getSequence(Tag.ReferencedImageSequence)
                if (refImgs == null || refImgs.isEmpty)
                    return voiLUT
                for (refImg in refImgs) {
                    if (iuid == refImg.getString(Tag.ReferencedSOPInstanceUID)) {
                        val refFrames = refImg.getInts(Tag.ReferencedFrameNumber)
                        if (refFrames == null || refFrames.isEmpty())
                            return voiLUT

                        for (refFrame in refFrames)
                            if (refFrame == frame)
                                return voiLUT
                    }
                }
            }
        return null
    }

    companion object {
        private const val DEFAULT_OVERLAY_ACTIVATION_MASK = 0xf
        private const val DEFAULT_OVERLAY_GRAYSCALE_VALUE = 0xffff
    }
}