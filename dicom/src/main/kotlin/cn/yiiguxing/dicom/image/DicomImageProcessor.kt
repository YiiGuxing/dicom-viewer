package cn.yiiguxing.dicom.image

import org.dcm4che3.data.Attributes
import org.dcm4che3.data.Tag
import org.dcm4che3.image.Overlays
import org.dcm4che3.image.PhotometricInterpretation
import org.dcm4che3.image.StoredValue
import org.dcm4che3.imageio.plugins.dcm.DicomMetaData
import java.awt.image.*

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

    private fun calcMinMax(storedValue: StoredValue, sm: ComponentSampleModel, data: ByteArray): IntArray {
        var min = Integer.MAX_VALUE
        var max = Integer.MIN_VALUE
        val w = sm.width
        val h = sm.height
        val stride = sm.scanlineStride
        for (y in 0 until h) {
            var i = y * stride
            val end = i + w
            while (i < end) {
                val `val` = storedValue.valueOf(data[i++].toInt())
                if (`val` < min) min = `val`
                if (`val` > max) max = `val`
            }
        }
        return intArrayOf(min, max)
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

    private fun applyOverlay(gg0000: Int, raster: WritableRaster,
                             frameIndex: Int, outBits: Int, ovlyData: ByteArray?) {
        var ovlyAttrs = metadata.attributes
        val grayscaleValue: Int
        val psAttrs = presentationState
        if (psAttrs != null) {
            if (psAttrs.containsValue(Tag.OverlayData or gg0000))
                ovlyAttrs = psAttrs
            grayscaleValue = Overlays.getRecommendedDisplayGrayscaleValue(psAttrs, gg0000)
        } else
            grayscaleValue = overlayGrayScaleValue
        Overlays.applyOverlay(if (ovlyData != null) 0 else frameIndex, raster,
                ovlyAttrs, gg0000, grayscaleValue.ushr(16 - outBits), ovlyData)
    }

    private fun applyLUTs(raster: Raster, dest: WritableRaster?,
                          frameIndex: Int, sm: SampleModel, outBits: Int): WritableRaster {
        val destRaster = if (sm.dataType == dest?.sampleModel?.dataType)
            dest
        else
            Raster.createWritableRaster(sm, null)
        val imgAttrs = metadata.attributes
        val psAttrs = presentationState
        val lutParam = lutFactory
        if (psAttrs != null) {
            lutParam.setModalityLUT(psAttrs)
            lutParam.setVOI(selectVOILUT(psAttrs, imgAttrs.getString(Tag.SOPInstanceUID), frameIndex + 1),
                    0, 0, false)
            lutParam.setPresentationLUT(psAttrs)
        } else {
            val sharedFctGroups = imgAttrs.getNestedDataset(
                    Tag.SharedFunctionalGroupsSequence)
            val frameFctGroups = imgAttrs.getNestedDataset(
                    Tag.PerFrameFunctionalGroupsSequence, frameIndex)
            lutParam.setModalityLUT(
                    selectFctGroup(imgAttrs, sharedFctGroups, frameFctGroups,
                            Tag.PixelValueTransformationSequence))
            val ww = windowWidth
            val wc = windowCenter
            if (ww == null || ww == 0.0f) {
                lutParam.setVOI(
                        selectFctGroup(imgAttrs, sharedFctGroups, frameFctGroups, Tag.FrameVOILUTSequence),
                        windowIndex,
                        voiLUTIndex,
                        isPreferWindow)
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

    private fun selectFctGroup(imgAttrs: Attributes,
                               sharedFctGroups: Attributes?,
                               frameFctGroups: Attributes?,
                               tag: Int): Attributes {
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