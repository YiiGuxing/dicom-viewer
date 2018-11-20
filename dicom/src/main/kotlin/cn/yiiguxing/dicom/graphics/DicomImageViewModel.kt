@file:Suppress("unused")

package cn.yiiguxing.dicom.graphics

import cn.yiiguxing.dicom.BodyOrientation
import cn.yiiguxing.dicom.DicomImageLoader
import cn.yiiguxing.dicom.graphics.text.GraphicLabel
import cn.yiiguxing.dicom.graphics.text.TextAlignment
import cn.yiiguxing.dicom.image.DicomImage
import cn.yiiguxing.dicom.opposites
import cn.yiiguxing.dicom.toLabel
import org.dcm4che3.data.Tag
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.io.File
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.URI
import java.text.SimpleDateFormat
import javax.vecmath.Vector3d
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class DicomImageViewModel(
    private val factory: ImageLoaderFactory = DefaultImageLoaderFactory
) : ViewModel(), Viewport {

    private var dicomImage: DicomImage? = null
    private var imageLoader: ImageLoader? = null
    private val loadStateLabel = GraphicLabel("Nothing to show", alignment = TextAlignment.CENTER)
    private var fontSize: Float = 0f

    private val imageTransform: AffineTransform = AffineTransform()
    private val tempTransform: AffineTransform = AffineTransform()
    private val tempPoint = Point2D.Double()

    private val cornerText: Array<out GraphicLabel> = arrayOf(
        GraphicLabel(alignment = TextAlignment.TOP_LEFT),
        GraphicLabel(alignment = TextAlignment.TOP_RIGHT),
        GraphicLabel(alignment = TextAlignment.BOTTOM_LEFT),
        GraphicLabel(alignment = TextAlignment.BOTTOM_RIGHT)
    )
    private val orientationLabel: Array<out GraphicLabel> = arrayOf(
        GraphicLabel(alignment = TextAlignment.LEFT),
        GraphicLabel(alignment = TextAlignment.TOP),
        GraphicLabel(alignment = TextAlignment.RIGHT),
        GraphicLabel(alignment = TextAlignment.BOTTOM)
    )
    private val viewportAnnotation: GraphicLabel = cornerText[3]

    /* ╦  ╦╦╔═╗╦ ╦╔═╗╔═╗╦═╗╔╦╗  ╔═╗╦═╗╔═╗╔═╗╔═╗╦═╗╔╦╗╦ ╦ */
    /* ╚╗╔╝║║╣ ║║║╠═╝║ ║╠╦╝ ║   ╠═╝╠╦╝║ ║╠═╝║╣ ╠╦╝ ║ ╚╦╝ */
    /*  ╚╝ ╩╚═╝╚╩╝╩  ╚═╝╩╚═ ╩   ╩  ╩╚═╚═╝╩  ╚═╝╩╚═ ╩  ╩  */
    /* -----------------------START------------------------ */

    val viewport: Viewport = this
    override val transform: AffineTransform = AffineTransform()
    override val windowWidth: Float get() = dicomImage?.windowWidth ?: 256f
    override val windowCenter: Float get() = dicomImage?.windowCenter ?: 127f
    override val invert: Boolean get() = dicomImage?.inverse ?: false
    override var selectedRegion: Rectangle? by Delegates.observable(null) { _: KProperty<Any?>, _: Rectangle?, _: Rectangle? ->
        invalidateSelf()
    }
    override var rotationRadian: Double = 0.0
        private set
    override var isHorizontalFlip: Boolean = false
        private set
    override var isVerticalFlip: Boolean = false
        private set
    override val isActualSize: Boolean
        get() = transform.fixedScaleX * imageTransform.fixedScaleX == 1.0

    /* ╦  ╦╦╔═╗╦ ╦╔═╗╔═╗╦═╗╔╦╗  ╔═╗╦═╗╔═╗╔═╗╔═╗╦═╗╔╦╗╦ ╦ */
    /* ╚╗╔╝║║╣ ║║║╠═╝║ ║╠╦╝ ║   ╠═╝╠╦╝║ ║╠═╝║╣ ╠╦╝ ║ ╚╦╝ */
    /*  ╚╝ ╩╚═╝╚╩╝╩  ╚═╝╩╚═ ╩   ╩  ╩╚═╚═╝╩  ╚═╝╩╚═ ╩  ╩  */
    /* ------------------------END------------------------- */

    /* ╦  ╦╦╔═╗╦ ╦╔═╗╔═╗╦═╗╔╦╗  ╔═╗╦ ╦╔╗╔ */
    /* ╚╗╔╝║║╣ ║║║╠═╝║ ║╠╦╝ ║   ╠╣ ║ ║║║║ */
    /*  ╚╝ ╩╚═╝╚╩╝╩  ╚═╝╩╚═ ╩   ╚  ╚═╝╝╚╝ */
    /* ----------------START--------------- */

    override fun setColorWindow(windowWidth: Float, windowCenter: Float, invert: Boolean) {
        dicomImage?.let { img ->
            img.setColorWindowing(windowWidth, windowCenter)
            img.inverse = invert
            updateViewportAnnotation()
            invalidateSelf()
        }
    }

    override fun translate(dx: Double, dy: Double) {
        transform.translate(dx, dy)
        invalidateSelf()
    }

    override fun locate() {
        adjustLocation()
        invalidateSelf()
    }

    override fun rotate(radian: Double) {
        val cx = width / 2.0
        val cy = height / 2.0
        rotationRadian = (rotationRadian + radian) % (2.0 * Math.PI)
        transform.rotate(radian, cx, cy)
        updateOrientation()
        invalidateSelf()
    }

    override fun scale(scaleX: Double, scaleY: Double) {
        val cx = width / 2.0
        val cy = height / 2.0
        transform.apply {
            translate(cx, cy)
            scale(scaleX, scaleY)
            translate(-cx, -cy)
        }
        updateViewportAnnotation()
        invalidateSelf()
    }

    override fun zoomToActualSize() {
        val scale = 1.0 / (transform.fixedScaleX * imageTransform.fixedScaleX)
        scale(scale, scale)
    }

    override fun horizontalFlip() {
        val cx = bounds.centerX
        val cy = bounds.centerY
        val rotation = rotationRadian

        transform.apply {
            if (rotation != 0.0) {
                rotate(2.0 * (Math.PI - rotation), cx, cy)
            }
            translate(cx, cy)
            scale(-1.0, 1.0)
            translate(-cx, -cy)
        }

        isHorizontalFlip = !isHorizontalFlip
        updateOrientation()
        invalidateSelf()
    }

    override fun verticalFlip() {
        val cx = bounds.centerX
        val cy = bounds.centerY
        val rotation = rotationRadian

        transform.apply {
            if (rotation != 0.0) {
                rotate(-2.0 * rotation, cx, cy)
            }
            translate(cx, cy)
            scale(1.0, -1.0)
            translate(-cx, -cy)
        }

        isVerticalFlip = !isVerticalFlip
        updateOrientation()
        invalidateSelf()
    }

    override fun reset() {
        rotationRadian = 0.0
        isHorizontalFlip = false
        isVerticalFlip = false
        transform.setToIdentity()
        dicomImage?.resetImage()
        updateViewportAnnotation()
        updateOrientation()
        invalidateSelf()
    }

    /* ╦  ╦╦╔═╗╦ ╦╔═╗╔═╗╦═╗╔╦╗  ╔═╗╦ ╦╔╗╔ */
    /* ╚╗╔╝║║╣ ║║║╠═╝║ ║╠╦╝ ║   ╠╣ ║ ║║║║ */
    /*  ╚╝ ╩╚═╝╚╩╝╩  ╚═╝╩╚═ ╩   ╚  ╚═╝╝╚╝ */
    /* -----------------END---------------- */

    private fun AffineTransform.calculateTransform(cWidth: Int, cHeight: Int, targetWidth: Int, targetHeight: Int) {
        val scale = Math.min(cWidth / targetWidth.toDouble(), cHeight / targetHeight.toDouble())
        val dx = (cWidth - targetWidth * scale) * 0.5
        val dy = (cHeight - targetHeight * scale) * 0.5
        setToIdentity()
        translate(dx, dy)
        scale(scale, scale)
    }

    fun setSrc(src: File?) = setSrc(src as Any)

    fun setSrc(src: URI?) = setSrc(src as Any)

    fun setSrc(src: InputStream?) = setSrc(src as Any)

    private fun setSrc(src: Any?) {
        setImage(null)
        if (src != null) {
            imageLoader = factory.create(this, src).apply { load() }
            invalidateSelf()
        }
    }

    fun setImage(image: DicomImage?) {
        imageLoader?.cancel()
        imageLoader = null
        dicomImage = image
        updateImageTransform()
        image?.let { updateCornerText(it) }
        reset()
    }

    fun isCurrentLoader(loader: ImageLoader): Boolean {
        return loader === imageLoader
    }

    private fun updateLoadStateLabel(label: String, color: Color) {
        loadStateLabel.color = color
        loadStateLabel.setLabels(label)
        invalidateSelf()
    }

    private fun updateImageTransform() {
        if (!bounds.isEmpty) {
            dicomImage?.let { img -> imageTransform.calculateTransform(width, height, img.width, img.height) }
        }
    }

    private fun updateCornerText(dicomImage: DicomImage) {
        val (tlLabel, trLabel, blLabel) = cornerText

        val attr = dicomImage.metadata.attributes
        val patientString =
            arrayOf(
                attr.getString(Tag.PatientSex),
                attr.getString(Tag.PatientAge),
                attr.getDate(Tag.PatientBirthDate)?.let { DATE_FORMATTER.format(it) })
                .filterNotNull()
                .takeIf { it.isNotEmpty() }
                ?.joinToString(separator = ",")
        val patientLabels = arrayOf(
            attr.getString(Tag.PatientName) ?: "",
            patientString,
            attr.getInt(Tag.SeriesNumber, -1).takeIf { it >= 0 }?.let { "SN: $it" },
            attr.getInt(Tag.InstanceNumber, -1).takeIf { it >= 0 }?.let { "IN: $it" }
        ).filterNotNull().toTypedArray()
        tlLabel.setLabels(*patientLabels)

        trLabel.setLabels(attr.getString(Tag.InstitutionName) ?: "",
            attr.getString(Tag.Manufacturer) ?: "",
            attr.getString(Tag.ManufacturerModelName) ?: "",
            attr.getDate(Tag.ContentDate)?.let { DATE_FORMATTER.format(it) } ?: "",
            attr.getDate(Tag.ContentTime)?.let { TIME_FORMATTER.format(it) } ?: "")

        val lbLabels = arrayOf(
            attr.getFloat(Tag.KVP, -1.0f).takeIf { it >= 0f }?.let { "${it}kV" },
            attr.getInt(Tag.Exposure, -1).takeIf { it >= 0 }?.let { "${it}mAx" },
            "W:${dicomImage.width}/H:${dicomImage.height}"
        ).filterNotNull().toTypedArray()
        blLabel.setLabels(*lbLabels)

        updateViewportAnnotation()
    }

    private fun updateOrientation() {
        val image = dicomImage ?: return
        val orientation = image.orientation ?: return

        // Set the opposite vector direction (otherwise label should be placed in mid-right and mid-bottom
        val vr = Vector3d(-orientation[0], -orientation[1], -orientation[2])
        val vc = Vector3d(-orientation[3], -orientation[4], -orientation[5])

        val rotation = rotationRadian
        if (rotation != 0.0) {
            val vn = BodyOrientation.computeNormalVector(orientation)
            BodyOrientation.rotate(vr, vn, -rotation, vr)
            BodyOrientation.rotate(vc, vn, -rotation, vc)
        }
        if (isHorizontalFlip) {
            vr.x = -vr.x
            vr.y = -vr.y
            vr.z = -vr.z
        }
        if (isVerticalFlip) {
            vc.x = -vc.x
            vc.y = -vc.y
            vc.z = -vc.z
        }

        val left = BodyOrientation.getBodyOrientations(vr, 0.0005)
        val top = BodyOrientation.getBodyOrientations(vc, 0.0005)
        val right = left.opposites()
        val bottom = top.opposites()
        orientationLabel.let { (l, t, r, b) ->
            l.setLabels(left.toLabel())
            t.setLabels(top.toLabel())
            r.setLabels(right.toLabel())
            b.setLabels(bottom.toLabel())
        }
    }

    private fun updateViewportAnnotation() {
        viewportAnnotation.setLabels(
            "Scale: %.4f".format(transform.fixedScaleX * imageTransform.fixedScaleX),
            "WW/WC: ${windowWidth.toInt()}/${windowCenter.toInt()}"
        )
    }

    override fun onBoundsChanged(old: Rectangle, new: Rectangle) {
        super.onBoundsChanged(old, new)
        fontSize = 0f
        updateImageTransform()
        adjustLocation()
        updateViewportAnnotation()
    }

    private fun adjustLocation() {
        val cw = width.toDouble()
        val ch = height.toDouble()
        val cPoint = tempPoint.apply { setLocation(cw / 2.0, ch / 2.0) }
        tempTransform.apply {
            setTransform(transform)
            transform(cPoint, cPoint)
        }

        transform.preConcatenate(tempTransform.apply {
            setToIdentity()
            translate(cw / 2.0 - cPoint.x, ch / 2.0 - cPoint.y)
        })
    }


    //   ██╗    ██████╗ ██████╗  █████╗ ██╗    ██╗    ███████╗██╗   ██╗███╗   ██╗    ██╗
    //  ██╔╝    ██╔══██╗██╔══██╗██╔══██╗██║    ██║    ██╔════╝██║   ██║████╗  ██║    ╚██╗
    // ██╔╝     ██║  ██║██████╔╝███████║██║ █╗ ██║    █████╗  ██║   ██║██╔██╗ ██║     ╚██╗
    // ╚██╗     ██║  ██║██╔══██╗██╔══██║██║███╗██║    ██╔══╝  ██║   ██║██║╚██╗██║     ██╔╝
    //  ╚██╗    ██████╔╝██║  ██║██║  ██║╚███╔███╔╝    ██║     ╚██████╔╝██║ ╚████║    ██╔╝
    //   ╚═╝    ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝ ╚══╝╚══╝     ╚═╝      ╚═════╝ ╚═╝  ╚═══╝    ╚═╝

    override fun onDraw(g2d: Graphics2D) {
        updateFontSize(g2d)
        drawLoadStateLabel(g2d)
        dicomImage?.let { img ->
            g2d.drawWithTransform(transform) {
                img.draw(g2d, imageTransform)
            }

            drawCornerText(g2d)
            drawOrientation(g2d)
            drawSelectedRegion(g2d)
        }
    }

    private fun updateFontSize(g2d: Graphics2D) {
        if (fontSize == 0f) {
            val stringWidth = (g2d.getFontMetrics(g2d.font.deriveFont(12.0f))).stringWidth("0123456789")
            fontSize = Math.ceil(10 / (stringWidth * 5.0 / minOf(width, height))).toFloat()
            fontSize = minOf(maxFontSize, maxOf(fontSize, minFontSize))
        }
        g2d.font = g2d.font.deriveFont(fontSize)
    }

    private fun drawLoadStateLabel(g2d: Graphics2D) {
        imageLoader?.state?.let {
            if (it != DicomImageLoader.State.SUCCEEDED) {
                g2d.draw(loadStateLabel, width / 2, height / 2)
            }
        }
    }

    private fun drawSelectedRegion(g2d: Graphics2D) {
        selectedRegion?.takeUnless { it.isEmpty }?.let { region ->
            g2d.drawWithColor(SELECTED_STROKE_BG_COLOR) {
                g2d.draw(region)
                g2d.color = SELECTED_STROKE_FG_COLOR
                g2d.drawWithStroke(SELECTED_STROKE) { g2d.draw(region) }
            }
        }
    }

    private fun drawCornerText(g2d: Graphics2D) {
        val padding = maxOf((g2d.font.size * 1.25 / 3.0), MIN_ANNOTATION_PADDING.toDouble())
        for (label in cornerText) {
            val x = when {
                label.alignment.isLeft -> padding
                label.alignment.isRight -> width - padding
                else -> return
            }
            val y = when {
                label.alignment.isTop -> padding
                label.alignment.isBottom -> height - padding
                else -> return
            }

            g2d.draw(label, x, y)
        }
    }

    private fun drawOrientation(g2d: Graphics2D) {
        val padding = maxOf((g2d.font.size * 1.25 / 3.0), MIN_ANNOTATION_PADDING.toDouble())
        for (label in orientationLabel) {
            val alignment = label.alignment
            val x = when {
                alignment.isTop || alignment.isBottom -> width / 2.0
                alignment.isRight -> width - padding
                else -> padding
            }
            val y = when {
                alignment.isLeft || alignment.isRight -> height / 2.0
                alignment.isBottom -> height - padding
                else -> padding
            }

            g2d.draw(label, x, y)
        }
    }

    //  ██╗    ██╗    ██████╗ ██████╗  █████╗ ██╗    ██╗    ███████╗██╗   ██╗███╗   ██╗    ██╗
    // ██╔╝   ██╔╝    ██╔══██╗██╔══██╗██╔══██╗██║    ██║    ██╔════╝██║   ██║████╗  ██║    ╚██╗
    //██╔╝   ██╔╝     ██║  ██║██████╔╝███████║██║ █╗ ██║    █████╗  ██║   ██║██╔██╗ ██║     ╚██╗
    //╚██╗  ██╔╝      ██║  ██║██╔══██╗██╔══██║██║███╗██║    ██╔══╝  ██║   ██║██║╚██╗██║     ██╔╝
    // ╚██╗██╔╝       ██████╔╝██║  ██║██║  ██║╚███╔███╔╝    ██║     ╚██████╔╝██║ ╚████║    ██╔╝
    //  ╚═╝╚═╝        ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝ ╚══╝╚══╝     ╚═╝      ╚═════╝ ╚═╝  ╚═══╝    ╚═╝

    companion object {
        @Volatile
        var minFontSize: Float = 3f
        @Volatile
        var maxFontSize: Float = 16f

        val DATE_FORMATTER = SimpleDateFormat("yyyy/MM/dd")
        val TIME_FORMATTER = SimpleDateFormat("HH:mm:ss")

        private const val MIN_ANNOTATION_PADDING = 1
        private val SELECTED_STROKE_BG_COLOR = Color.BLACK
        private val SELECTED_STROKE_FG_COLOR = Color.WHITE
        private val SELECTED_STROKE =
            BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10f, floatArrayOf(5f), 0f)

    }

    interface ImageLoaderFactory {
        fun create(vm: DicomImageViewModel, input: Any): ImageLoader = ImageLoader(vm, input)
    }

    object DefaultImageLoaderFactory : ImageLoaderFactory

    open class ImageLoader(vm: DicomImageViewModel, input: Any) : DicomImageLoader(input) {
        private val vmRef = WeakReference(vm)

        @Suppress("MemberVisibilityCanBePrivate")
        protected val viewModel: DicomImageViewModel?
            get() = vmRef.get()?.takeIf { it.isCurrentLoader(this) }

        override fun onLoading() {
            viewModel?.updateLoadStateLabel("Loading...", Color.WHITE)
        }

        override fun onFailed() {
            viewModel?.updateLoadStateLabel("Error!", Color.RED)
        }

        override fun onCancelled() {
            viewModel?.updateLoadStateLabel("Nothing to show.", Color.WHITE)
        }

        override fun onSucceeded(image: DicomImage) {
            viewModel?.setImage(image)
        }
    }
}