package cn.yiiguxing.tool.dcmviewer.image


import org.dcm4che3.data.*
import org.dcm4che3.image.PhotometricInterpretation
import org.dcm4che3.imageio.codec.ImageDescriptor
import org.dcm4che3.imageio.codec.ImageReaderFactory
import org.dcm4che3.imageio.codec.jpeg.PatchJPEGLS
import org.dcm4che3.imageio.codec.jpeg.PatchJPEGLSImageInputStream
import org.dcm4che3.imageio.plugins.dcm.DicomMetaData
import org.dcm4che3.imageio.stream.EncapsulatedPixelDataImageInputStream
import org.dcm4che3.imageio.stream.ImageInputStreamAdapter
import org.dcm4che3.imageio.stream.SegmentedInputImageStream
import org.dcm4che3.io.BulkDataDescriptor
import org.dcm4che3.io.DicomInputStream
import org.dcm4che3.io.DicomInputStream.IncludeBulkData
import org.dcm4che3.util.ByteUtils
import org.slf4j.LoggerFactory
import java.awt.image.*
import java.io.*
import java.net.URI
import java.nio.ByteOrder
import javax.imageio.ImageIO
import javax.imageio.ImageReadParam
import javax.imageio.ImageReader
import javax.imageio.ImageTypeSpecifier
import javax.imageio.stream.FileImageInputStream
import javax.imageio.stream.ImageInputStream

/**
 * Reads header and image data from a DICOM object.
 *
 * Supports compressed and uncompressed images from a DicomMetaData object,
 * an InputStream/DicomInputStream or an ImageInputStream.
 * For ImageInputStream, the access supports random/out of order reading from
 * the input for everything except deflated streams.
 * For InputStream type data, only sequential access to images is supported, including deflated.
 * For DicomMetaData, random access is fully supported, and can have been read from a deflated stream.
 * Objects without pixel data are also supported, although only the metadata can be read from
 * them (mostly for the use case that it is unknown whether or not there is pixel data).
 *
 * Tag values after the pixel data are not read up-front for performance reasons/ability to actually
 * read them up front.  Call the relevant methods below to read that data.
 */
class DicomImageIO : Closeable {

    private var iis: ImageInputStream? = null

    private var dis: DicomInputStream? = null

    private var epdiis: EncapsulatedPixelDataImageInputStream? = null

    private var metadata: DicomMetaData? = null

    private var pixelData: BulkData? = null

    private var pixelDataFragments: Fragments? = null

    private var pixeldataBytes: ByteArray? = null

    private var pixelDataLength: Int = 0

    private var pixelDataVR: VR? = null

    private var pixelDataFile: File? = null

    private var frames: Int = 0

    private var flushedFrames: Int = 0

    private var width: Int = 0

    private var height: Int = 0

    private var decompressor: ImageReader? = null

    private var rle: Boolean = false

    private var patchJpegLS: PatchJPEGLS? = null

    private var samples: Int = 0

    private var banded: Boolean = false

    private var bitsStored: Int = 0

    private var bitsAllocated: Int = 0

    private var dataType: Int = 0

    private var frameLength: Int = 0

    private var pmi: PhotometricInterpretation? = null
    private var pmiAfterDecompression: PhotometricInterpretation? = null
    private var imageDescriptor: ImageDescriptor? = null

    /**
     * Gets the stream metadata.  May not contain post pixel data unless
     * there are no images or the getStreamMetadata has been called with the post pixel data
     * node being specified.
     */
    val streamMetadata: DicomMetaData?
        @Throws(IOException::class)
        get() {
            readMetadata()
            return metadata
        }

    private val transferSyntaxUID: String
        get() = metadata!!.transferSyntaxUID


    fun setFile(file: File) {
        setInput(ImageIO.createImageInputStream(file))
    }

    fun setInputStream(inputStream: InputStream) {
        setInput(ImageIO.createImageInputStream(inputStream))
    }

    fun setImageInputStream(inputStream: ImageInputStream) {
        setInput(inputStream)
    }

    fun setUri(uri: URI) {
        setInput(ImageIO.createImageInputStream(File(uri)))
    }

    private fun setInput(input: Any) {
        resetInternalState()
        when (input) {
            is InputStream -> try {
                dis = input as? DicomInputStream ?: DicomInputStream(input)
            } catch (e: IOException) {
                throw IllegalArgumentException(e.message)
            }
            is DicomMetaData -> {
                initPixelDataFromAttributes(input.attributes)
                initPixelDataFile()
                setMetadata(input)
            }
            else -> iis = input as ImageInputStream
        }
    }

    private fun initPixelDataFromAttributes(ds: Attributes) {
        val holder = VR.Holder()
        val value = ds.getValue(Tag.PixelData, holder)
        if (value != null) {
            imageDescriptor = ImageDescriptor(ds)
            pixelDataVR = holder.vr
            when (value) {
                is BulkData -> {
                    pixelData = value
                    pixelDataLength = pixelData!!.length()
                }
                is ByteArray -> {
                    pixeldataBytes = value
                    pixelDataLength = pixeldataBytes!!.size
                }
                else -> { // commandValue instanceof Fragments
                    pixelDataFragments = value as Fragments
                    pixelDataLength = -1
                }
            }
        }
    }

    private fun initPixelDataFile() {
        if (pixelData != null)
            pixelDataFile = pixelData!!.file
        else if (pixelDataFragments != null && pixelDataFragments!!.size > 1) {
            val frag = pixelDataFragments!![1]
            if (frag is BulkData) {
                pixelDataFile = frag.file
            }
        }
    }

    fun getNumImages(): Int {
        readMetadata()
        return frames
    }

    fun getWidth(frameIndex: Int): Int {
        readMetadata()
        checkIndex(frameIndex)
        return width
    }

    fun getHeight(frameIndex: Int): Int {
        readMetadata()
        checkIndex(frameIndex)
        return height
    }


    fun getRawImageType(frameIndex: Int): ImageTypeSpecifier {
        readMetadata()
        checkIndex(frameIndex)

        if (decompressor == null)
            return createImageType(bitsStored, dataType, banded)

        if (rle)
            return createImageType(bitsStored, dataType, true)

        openiis()
        try {
            decompressor!!.input = iisOfFrame(0)
            return decompressor!!.getRawImageType(0)
        } finally {
            closeiis()
        }
    }

    fun getImageTypes(frameIndex: Int): Iterator<ImageTypeSpecifier> {
        readMetadata()
        checkIndex(frameIndex)

        val imageType: ImageTypeSpecifier
        when {
            pmi!!.isMonochrome -> imageType = createImageType(8, DataBuffer.TYPE_BYTE, false)
            decompressor == null -> imageType = createImageType(bitsStored, dataType, banded)
            rle -> imageType = createImageType(bitsStored, dataType, true)
            else -> {
                openiis()
                try {
                    decompressor!!.input = iisOfFrame(0)
                    return decompressor!!.getImageTypes(0)
                } finally {
                    closeiis()
                }
            }
        }

        return listOf(imageType).iterator()
    }

    @Throws(IOException::class)
    private fun openiis() {
        if (iis == null) {
            if (pixelDataFile != null) {
                iis = FileImageInputStream(pixelDataFile)
            } else if (pixeldataBytes != null) {
                iis = SegmentedInputImageStream(pixeldataBytes)
            }
        }
    }

    @Throws(IOException::class)
    private fun closeiis() {
        if ((pixelDataFile != null || pixeldataBytes != null) && iis != null) {
            iis!!.close()
            iis = null
        }
    }


    @Throws(IOException::class)
    fun readRaster(frameIndex: Int): Raster {
        readMetadata()
        checkIndex(frameIndex)

        openiis()
        try {
            decompressor?.let { decompressor ->
                decompressor.input = iisOfFrame(frameIndex)
                if (LOG.isDebugEnabled) {
                    LOG.debug("Start decompressing frame #" + (frameIndex + 1))
                }
                val pmi = requireNotNull(pmi) { "pmi was null." }
                val raster = if (pmiAfterDecompression === pmi && decompressor.canReadRaster()) {
                    decompressor.readRaster(0, decompressParam())
                } else {
                    decompressor.read(0, decompressParam()).raster
                }
                if (LOG.isDebugEnabled) {
                    LOG.debug("Finished decompressing frame #" + (frameIndex + 1))
                }
                return raster
            }

            val raster = Raster.createWritableRaster(createSampleModel(dataType, banded), null)
            val buffer = raster.dataBuffer

            val dis = dis
            val iis = requireNotNull(iis) { "iis was null." }
            when {
                dis != null -> {
                    dis.skipFully(((frameIndex - flushedFrames) * frameLength).toLong())
                    flushedFrames = frameIndex + 1
                }
                pixeldataBytes != null -> {
                    iis.byteOrder = if (bigEndian()) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
                    iis.seek((frameIndex * frameLength).toLong())
                }
                else -> {
                    iis.byteOrder = if (bigEndian()) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
                    iis.seek(pixelData!!.offset() + frameIndex * frameLength)
                }
            }

            when (buffer) {
                is DataBufferByte -> {
                    for (bank in 0 until buffer.numBanks) {
                        dis?.readFully(buffer, bank) ?: iis.readFully(buffer, bank)
                    }
                    if (pixelDataVR == VR.OW && bigEndian()) {
                        buffer.swapShorts()
                    }
                }
                is DataBufferUShort -> {
                    for (bank in 0 until buffer.numBanks) {
                        dis?.readFully(buffer, bank) ?: iis.readFully(buffer, bank)
                    }
                }
                else -> throw IllegalStateException("Unsupported buffer type: ${buffer.javaClass.name}")
            }

            return raster
        } finally {
            closeiis()
        }
    }

    private fun bigEndian(): Boolean {
        return metadata!!.bigEndian()
    }

    private fun decompressParam(): ImageReadParam {
        val decompressParam = decompressor!!.defaultReadParam
        var imageType: ImageTypeSpecifier? = null
        if (rle)
            imageType = createImageType(bitsStored, dataType, true)
        decompressParam.destinationType = imageType
        return decompressParam
    }

    @Throws(IOException::class)
    fun read(frameIndex: Int): DicomImage {
        readMetadata()
        checkIndex(frameIndex)

        val raster: WritableRaster
        if (decompressor != null) {
            openiis()
            try {
                val iisOfFrame = iisOfFrame(frameIndex)
                // Reading this up front sets the required values so that opencv succeeds - it is less than optimal performance wise
                iisOfFrame!!.length()
                decompressor!!.input = iisOfFrame
                LOG.debug("Start decompressing frame #{}", frameIndex + 1)
                val bi = decompressor!!.read(0, decompressParam())
                LOG.debug("Finished decompressing frame #{}", frameIndex + 1)
                raster = bi.raster
            } finally {
                closeiis()
            }
        } else {
            raster = readRaster(frameIndex) as WritableRaster
        }

        val cm: ColorModel = if (pmi!!.isMonochrome) {
            createColorModel(8, DataBuffer.TYPE_BYTE)
        } else {
            createColorModel(bitsStored, dataType)
        }

        return DicomImage(metadata!!, raster, cm, frameIndex)
    }

    /**
     * Generate an image input stream for the given frame, -1 for all frames (video, multi-component single frame)
     * Does not necessarily support the length operation without seeking/reading to the end of the input.
     *
     * @param frameIndex
     * @return
     * @throws IOException
     */
    fun iisOfFrame(frameIndex: Int): ImageInputStream? {
        val iisOfFrame: ImageInputStream
        val epdiis = epdiis
        when {
            epdiis != null -> {
                seekFrame(frameIndex)
                iisOfFrame = epdiis
            }
            pixelDataFragments == null -> return null
            else -> {
                iisOfFrame = SegmentedInputImageStream(
                        iis, pixelDataFragments, if (frames == 1) -1 else frameIndex)
                iisOfFrame.imageDescriptor = imageDescriptor
            }
        }
        return if (patchJpegLS != null)
            PatchJPEGLSImageInputStream(iisOfFrame, patchJpegLS)
        else
            iisOfFrame
    }

    @Throws(IOException::class)
    private fun seekFrame(frameIndex: Int) {
        assert(frameIndex >= flushedFrames)
        if (frameIndex == flushedFrames)
            epdiis!!.seekCurrentFrame()
        else
            while (frameIndex > flushedFrames) {
                if (!epdiis!!.seekNextFrame()) {
                    throw IOException("Data Fragments only contains " + (flushedFrames + 1) + " frames")
                }
                flushedFrames++
            }
    }

    @Throws(IOException::class)
    private fun readMetadata() {
        if (metadata != null)
            return

        if (dis != null) {
            val fmi = dis!!.readFileMetaInformation()
            val ds = dis!!.readDataset(-1, Tag.PixelData)
            if (dis!!.tag() == Tag.PixelData) {
                imageDescriptor = ImageDescriptor(ds)
                pixelDataVR = dis!!.vr()
                pixelDataLength = dis!!.length()
                if (pixelDataLength == -1)
                    epdiis = EncapsulatedPixelDataImageInputStream(dis!!, imageDescriptor)
            } else {
                try {
                    dis!!.readAttributes(ds, -1, -1)
                } catch (e: EOFException) {
                }

            }
            setMetadata(DicomMetaData(fmi, ds))
            return
        }
        if (iis == null)
            throw IllegalStateException("Input not set")

        val dis = DicomInputStream(ImageInputStreamAdapter(iis))
        dis.includeBulkData = IncludeBulkData.URI
        dis.bulkDataDescriptor = BulkDataDescriptor.PIXELDATA
        dis.uri = "java:iis" // avoid copy of pixeldata to temporary file
        val fmi = dis.readFileMetaInformation()
        val ds = dis.readDataset(-1, Tag.PixelData)
        if (dis.tag() == Tag.PixelData) {
            imageDescriptor = ImageDescriptor(ds)
            pixelDataVR = dis.vr()
            pixelDataLength = dis.length()
        } else {
            try {
                dis.readAttributes(ds, -1, -1)
            } catch (e: EOFException) {
            }

        }
        setMetadata(DicomMetaData(fmi, ds))
        initPixelDataIIS(dis)
    }

    /**
     * Initializes the pixel data reading from an image input stream
     */
    private fun initPixelDataIIS(dis: DicomInputStream) {
        if (pixelDataLength == 0) return
        if (pixelDataLength > 0) {
            pixelData = BulkData("pixeldata://", dis.position, dis.length(), dis.bigEndian())
            metadata!!.attributes.setValue(Tag.PixelData, pixelDataVR, pixelData)
            return
        }
        dis.readItemHeader()
        val b = ByteArray(dis.length())
        dis.readFully(b)

        val start = dis.position
        val pixelDataFragments = Fragments(pixelDataVR, dis.bigEndian(), frames)
        this.pixelDataFragments = pixelDataFragments
        pixelDataFragments.add(b)

        generateOffsetLengths(pixelDataFragments, frames, b, start)
    }

    private fun setMetadata(metadata: DicomMetaData) {
        this.metadata = metadata
        val ds = metadata.attributes
        if (pixelDataLength != 0) {
            frames = ds.getInt(Tag.NumberOfFrames, 1)
            width = ds.getInt(Tag.Columns, 0)
            height = ds.getInt(Tag.Rows, 0)
            samples = ds.getInt(Tag.SamplesPerPixel, 1)
            banded = samples > 1 && ds.getInt(Tag.PlanarConfiguration, 0) != 0
            bitsAllocated = ds.getInt(Tag.BitsAllocated, 8)
            bitsStored = ds.getInt(Tag.BitsStored, bitsAllocated)
            dataType = if (bitsAllocated <= 8)
                DataBuffer.TYPE_BYTE
            else
                DataBuffer.TYPE_USHORT
            pmi = PhotometricInterpretation.fromString(
                    ds.getString(Tag.PhotometricInterpretation, "MONOCHROME2"))
            if (pixelDataLength != -1) {
                pmiAfterDecompression = pmi
                this.frameLength = pmi!!.frameLength(width, height, samples, bitsAllocated)
            } else {
                val fmi = metadata.fileMetaInformation
                        ?: throw IllegalArgumentException("Missing File Meta Information for Data Set with compressed Pixel Data")

                val tsuid = fmi.getString(Tag.TransferSyntaxUID)
                val param = ImageReaderFactory.getImageReaderParam(tsuid)
                        ?: throw UnsupportedOperationException("Unsupported Transfer Syntax: $tsuid")
                pmiAfterDecompression = param.pmiAfterDecompression(pmi)
                this.rle = tsuid == UID.RLELossless
                this.decompressor = ImageReaderFactory.getImageReader(param)
                LOG.debug("Decompressor: {}", decompressor!!.javaClass.name)
                this.patchJpegLS = param.patchJPEGLS
            }
        }
    }

    private fun createSampleModel(dataType: Int, banded: Boolean): SampleModel {
        return pmi!!.createSampleModel(dataType, width, height, samples, banded)
    }

    private fun createImageType(bits: Int, dataType: Int, banded: Boolean): ImageTypeSpecifier {
        return ImageTypeSpecifier(
                createColorModel(bits, dataType),
                createSampleModel(dataType, banded))
    }

    private fun createColorModel(bits: Int, dataType: Int): ColorModel {
        return pmi!!.createColorModel(bits, dataType, metadata!!.attributes)
    }

    private fun resetInternalState() {
        dis = null
        metadata = null
        pixelData = null
        pixelDataFragments = null
        pixelDataVR = null
        pixelDataLength = 0
        pixeldataBytes = null
        pixelDataFile = null
        frames = 0
        flushedFrames = 0
        width = 0
        height = 0
        if (decompressor != null) {
            decompressor!!.dispose()
            decompressor = null
        }
        patchJpegLS = null
        pmi = null
    }

    private fun checkIndex(frameIndex: Int) {
        if (frames == 0)
            throw IllegalStateException("Missing Pixel Data")

        if (frameIndex < 0 || frameIndex >= frames)
            throw IndexOutOfBoundsException("imageIndex: $frameIndex")

        if (dis != null && frameIndex < flushedFrames)
            throw IllegalStateException(
                    "input stream position already after requested frame #" + (frameIndex + 1))
    }

    /**
     * Reads post-pixel data tags, will skip past any remaining images (which may be very slow), and
     * add any post-pixel data information to the attributes object.
     * NOTE: This read will read past image data, and may end up scanning/seeking through multiframe or video data in order to find the
     * post pixel data.  This may be slow.
     *
     *
     * Replaces the attributes object with a new one, thus is thread safe for other uses of the object.
     */
    @Throws(IOException::class)
    fun readPostPixeldata(): Attributes {
        if (frames == 0) return metadata!!.attributes

        if (dis != null) {
            if (flushedFrames > frames) {
                return metadata!!.attributes
            }
            dis!!.skipFully(((frames - flushedFrames) * frameLength).toLong())
            flushedFrames = frames + 1
            return readPostAttr(dis!!)
        }
        val offset: Long
        offset = if (pixelData != null) {
            pixelData!!.offset() + pixelData!!.longLength()
        } else {
            val siis = iisOfFrame(-1) as SegmentedInputImageStream
            siis.offsetPostPixelData
        }
        iis!!.seek(offset)
        val dis = DicomInputStream(ImageInputStreamAdapter(iis), transferSyntaxUID)
        return readPostAttr(dis)
    }

    @Throws(IOException::class)
    private fun readPostAttr(dis: DicomInputStream): Attributes {
        val postAttr = dis.readDataset(-1, -1)
        postAttr.addAll(metadata!!.attributes)
        metadata = DicomMetaData(metadata!!.fileMetaInformation, postAttr)
        return postAttr
    }

    fun dispose() {
        try {
            iis?.close()
        } catch (e: Exception) {
            LOG.error("close iis", e)
        }
        try {
            dis?.close()
        } catch (e: Exception) {
            LOG.error("close dis", e)
        }
        try {
            epdiis?.close()
        } catch (e: Exception) {
            LOG.error("close epdiis", e)
        }
        resetInternalState()
    }

    override fun close() {
        dispose()
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(DicomImageIO::class.java)

        const val POST_PIXEL_DATA = "postPixelData"

        /**
         * Creates an offset/length table based on the frame positions
         */
        fun generateOffsetLengths(pixelData: Fragments, frames: Int, basicOffsetTable: ByteArray, start: Long) {
            @Suppress("LocalVariableName")
            var _start = start
            var lastOffset: Long = 0
            var lastFrag: BulkData? = null
            for (frame in 0 until frames) {
                var offset = (if (frame > 0) 1 else 0).toLong()
                val offsetStart = frame * 4
                if (basicOffsetTable.size >= offsetStart + 4) {
                    offset = ByteUtils.bytesToIntLE(basicOffsetTable, offsetStart).toLong()
                    if (offset != 1L) {
                        // Handle > 4 gb total image size by assuming incrementing modulo 4gb
                        offset = offset or (lastOffset and 0xFFFFFF00000000L)
                        if (offset < lastOffset) offset += 0x100000000L
                        lastOffset = offset
                        LOG.trace("Found offset {} for frame {}", offset, frame)
                    }
                }
                var position: Long = -1
                if (offset != 1L) {
                    position = _start + offset + 8
                }
                val frag = BulkData("compressedPixelData://", position, -1, false)
                if (lastFrag != null && position != -1L) {
                    lastFrag.setLength(position - 8 - lastFrag.offset())
                }
                lastFrag = frag
                pixelData.add(frag)
                if (offset == 0L && frame > 0) {
                    _start = -1
                }
            }
        }
    }
}
