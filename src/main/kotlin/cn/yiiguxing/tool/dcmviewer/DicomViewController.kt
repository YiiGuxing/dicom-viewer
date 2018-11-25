package cn.yiiguxing.tool.dcmviewer

import cn.yiiguxing.tool.dcmviewer.op.*
import com.sun.javafx.scene.control.MultiplePropertyChangeListenerHandler
import javafx.beans.binding.Bindings
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.transform.Affine
import javafx.scene.transform.TransformChangedEvent
import org.dcm4che3.data.Tag
import java.text.SimpleDateFormat
import java.util.concurrent.Callable
import javax.vecmath.Vector3d
import kotlin.math.abs

/**
 * DicomViewController
 *
 * Created by Yii.Guxing on 2018/11/20
 */
class DicomViewController(val view: DicomView) {

    @FXML
    private lateinit var canvas: Canvas
    @FXML
    private lateinit var leftTopAnnotation: Label
    @FXML
    private lateinit var rightTopAnnotation: Label
    @FXML
    private lateinit var leftBottomAnnotation: Label
    @FXML
    private lateinit var rightBottomAnnotation: Label
    @FXML
    private lateinit var leftOrientation: Label
    @FXML
    private lateinit var topOrientation: Label
    @FXML
    private lateinit var rightOrientation: Label
    @FXML
    private lateinit var bottomOrientation: Label

    val viewWidth: Double get() = canvas.width
    val viewHeight: Double get() = canvas.height

    internal val transform: Affine = Affine()
    private val imageTransform: Affine = Affine()
    private val tempTransform: Affine = Affine()

    private var rotationAngle: Double = 0.0
    private var isHorizontalFlip: Boolean = false
    private var isVerticalFlip: Boolean = false

    private var drawInterceptSignal = 0

    internal val actualSizeProperty: BooleanProperty = SimpleBooleanProperty(false)
    internal val canZoomInProperty: BooleanProperty = SimpleBooleanProperty(false)
    internal val canZoomOutProperty: BooleanProperty = SimpleBooleanProperty(false)

    private val changeHandler = MultiplePropertyChangeListenerHandler { handlePropertyChanged(it);null }

    private val ops: Array<out MouseOperation> = arrayOf(
        WindowingOp(this),
        ScaleOp(this),
        TranslationOp(this),
        RotationOp(this)
    )

    @FXML
    private fun initialize() {
        pushDrawInterceptSignal()
        bindVisible()

        bindViewport()
        setupOp()
        registerChangeListener()
        initCanvas()

        transform.addEventHandler(TransformChangedEvent.ANY) {
            drawContent()
        }

        popDrawInterceptSignal()
    }

    private fun initCanvas() {
        // FIXME ANTI-ALIASING: Is there a better implementation?
        // FIXME Crash when zoomed in
        // val blur = BoxBlur().apply {
        //     width = 1.0
        //     height = 1.0
        //     iterations = 1
        // }
        // canvas.graphicsContext2D.setEffect(blur)

        canvas.widthProperty().bind(view.widthProperty())
        canvas.heightProperty().bind(view.heightProperty())
    }

    private fun bindVisible() {
        val hasImage = view.dicomImagePriority.isNotNull
        leftTopAnnotation.visibleProperty().bind(hasImage)
        rightTopAnnotation.visibleProperty().bind(hasImage)
        leftBottomAnnotation.visibleProperty().bind(hasImage)
        rightBottomAnnotation.visibleProperty().bind(hasImage)
        leftOrientation.visibleProperty().bind(hasImage)
        topOrientation.visibleProperty().bind(hasImage)
        rightOrientation.visibleProperty().bind(hasImage)
        bottomOrientation.visibleProperty().bind(hasImage)
    }

    private fun bindViewport() {
        val canvasScale = transform.createScalingFactorBinding()
        val imageScale = imageTransform.createScalingFactorBinding()
        val actualScale = canvasScale.multiply(imageScale)

        var nextScale = canvasScale.multiply(ZOOM_IN_STEP)
        canZoomInProperty.bind(nextScale.lessThanOrEqualTo(MAX_SCALE))
        nextScale = canvasScale.multiply(ZOOM_OUT_STEP)
        canZoomOutProperty.bind(nextScale.greaterThanOrEqualTo(MIN_SCALE))
        actualSizeProperty.bind(Bindings.createBooleanBinding(Callable {
            abs(actualScale.get() - 1.0) in 0.0..1.0E-12
        }, actualScale))

        val scaleString = actualScale.asString("Scale: %.4f\n")
        val colorWindowingString = Bindings.createStringBinding(Callable {
            val ww = view.windowWidth?.let { Math.round(it) } ?: "-"
            val wc = view.windowCenter?.let { Math.round(it) } ?: "-"
            "WW/WC: $ww/$wc"
        }, view.windowWidthProperty, view.windowCenterProperty)
        rightBottomAnnotation.textProperty().bind(Bindings.concat(scaleString, colorWindowingString))
    }

    private fun setupOp() {
        canvas.addEventHandler(MouseEvent.ANY, ops[view.op.ordinal])
        canvas.addEventHandler(ScrollEvent.ANY, ScaleWheelOp(this))
        view.opProperty.addListener { _, old, new ->
            canvas.removeEventHandler(MouseEvent.ANY, ops[old.ordinal])
            canvas.addEventHandler(MouseEvent.ANY, ops[new.ordinal])
        }
    }

    private fun registerChangeListener() = with(changeHandler) {
        registerChangeListener(view.widthProperty(), REF_SIZE)
        registerChangeListener(view.heightProperty(), REF_SIZE)
        registerChangeListener(view.dicomImagePriority, REF_IMAGE)
        registerChangeListener(view.windowWidthProperty, REF_COLOR_WINDOWING)
        registerChangeListener(view.windowCenterProperty, REF_COLOR_WINDOWING)
        registerChangeListener(view.inverseProperty, REF_COLOR_WINDOWING)
    }

    fun translate(dx: Double, dy: Double) {
        transform.appendTranslation(dx, dy)
    }

    fun locate() {
        val cw = viewWidth / 2.0
        val ch = viewHeight / 2.0
        val point = transform.transform(cw, ch)
        transform.prependTranslation(cw - point.x, ch - point.y)
    }

    fun scale(scale: Double) {
        val cx = viewWidth / 2.0
        val cy = viewHeight / 2.0
        transform.appendScale(scale, scale, cx, cy)
    }

    fun zoomIn() {
        if (canZoomInProperty.value) {
            scale(ZOOM_IN_STEP)
        }
    }

    fun zoomOut() {
        if (canZoomOutProperty.value) {
            scale(ZOOM_OUT_STEP)
        }
    }

    fun scaleToActualSize() {
        if (!actualSizeProperty.value) {
            val scale = 1.0 / (transform.scalingFactor * imageTransform.scalingFactor)
            scale(scale)
        }
    }

    fun rotate(angle: Double) {
        val cx = viewWidth / 2.0
        val cy = viewHeight / 2.0
        val r = if (isHorizontalFlip != isVerticalFlip) -angle else angle

        rotationAngle = (rotationAngle + r) % 360.0
        transform.appendRotation(r, cx, cy)
        updateOrientation()
    }

    fun horizontalFlip() {
        val cx = viewWidth / 2.0
        val cy = viewHeight / 2.0
        val rotation = rotationAngle

        pushDrawInterceptSignal()
        transform.apply {
            if (rotation != 0.0) {
                appendRotation(360.0 - 2.0 * rotation, cx, cy)
            }
            appendScale(-1.0, 1.0, cx, cy)
        }
        popDrawInterceptSignal()

        isHorizontalFlip = !isHorizontalFlip
        drawContent()
        updateOrientation()
    }

    fun verticalFlip() {
        val cx = viewWidth / 2.0
        val cy = viewHeight / 2.0
        val rotation = rotationAngle

        pushDrawInterceptSignal()
        transform.apply {
            if (rotation != 0.0) {
                appendRotation(-2.0 * rotation, cx, cy)
            }
            appendScale(1.0, -1.0, cx, cy)
        }
        popDrawInterceptSignal()

        isVerticalFlip = !isVerticalFlip
        drawContent()
        updateOrientation()
    }

    fun reset() {
        pushDrawInterceptSignal()
        rotationAngle = 0.0
        isHorizontalFlip = false
        isVerticalFlip = false
        transform.setToIdentity()
        view.dicomImage?.resetImage()
        popDrawInterceptSignal()

        drawContent()
        updateOrientation()
    }

    private fun updateOrientation() {
        val image = view.dicomImage ?: return
        val orientation = image.orientation ?: return

        // Set the opposite vector direction (otherwise label should be placed in mid-right and mid-bottom
        val vr = Vector3d(-orientation[0], -orientation[1], -orientation[2])
        val vc = Vector3d(-orientation[3], -orientation[4], -orientation[5])

        val rotation = Math.toRadians(rotationAngle)
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
        leftOrientation.text = left.toLabel()
        topOrientation.text = top.toLabel()
        rightOrientation.text = left.opposites().toLabel()
        bottomOrientation.text = top.opposites().toLabel()
    }

    private fun handlePropertyChanged(reference: String) {
        pushDrawInterceptSignal()
        when (reference) {
            REF_IMAGE -> onImageChanged()
            REF_SIZE -> onSizeChanged()
        }

        popDrawInterceptSignal()
        drawContent()
    }

    private fun onImageChanged() {
        val image = view.dicomImage
        if (image != null) {
            val attrs = image.metadata.attributes

            val name = attrs.getString(Tag.PatientName) ?: ""
            val patientInfo = arrayOf(
                attrs.getString(Tag.PatientSex),
                attrs.getString(Tag.PatientAge),
                attrs.getDate(Tag.PatientBirthDate)?.let { DATE_FORMATTER.format(it) }
            ).joinToString(separator = ",") { it ?: "-" }
            val seriesNumber = attrs.getInt(Tag.SeriesNumber, -1).takeIf { it >= 0 }?.let { "SN: $it" }
            val instanceNumber = attrs.getInt(Tag.InstanceNumber, -1).takeIf { it >= 0 }?.let { "IN: $it" }
            leftTopAnnotation.text = "$name\n$patientInfo\n$seriesNumber\n$instanceNumber"

            rightTopAnnotation.text = arrayOf(
                attrs.getString(Tag.InstitutionName),
                attrs.getString(Tag.Manufacturer),
                attrs.getString(Tag.ManufacturerModelName),
                attrs.getDate(Tag.ContentDate)?.let { DATE_FORMATTER.format(it) },
                attrs.getDate(Tag.ContentTime)?.let { TIME_FORMATTER.format(it) }
            ).joinToString(separator = "\n") { it ?: "" }

            leftBottomAnnotation.text = arrayOf(
                attrs.getFloat(Tag.KVP, -1.0f).takeIf { it >= 0f }?.let { "${it}kV" },
                attrs.getInt(Tag.Exposure, -1).takeIf { it >= 0 }?.let { "${it}mAx" },
                "W:${image.width}/H:${image.height}"
            ).filterNotNull().joinToString(separator = "\n")
        } else {
            leftTopAnnotation.text = null
            leftBottomAnnotation.text = null
            rightTopAnnotation.text = null
            rightBottomAnnotation.text = null
            leftOrientation.text = null
            topOrientation.text = null
            rightOrientation.text = null
            bottomOrientation.text = null
        }

        updateImageTransform()
        reset()
    }

    private fun onSizeChanged() {
        updateImageTransform()
        locate()
    }

    private fun updateImageTransform() {
        if (viewWidth > 0 && viewHeight > 0) {
            view.dicomImage?.let { img ->
                val imageTransform = imageTransform
                val tempTransform = tempTransform.apply {
                    setToTransform(imageTransform)
                    calculateTransform(viewWidth, viewHeight, img.width.toDouble(), img.height.toDouble())
                }
                imageTransform.setToTransform(tempTransform)
            }
        }
    }

    private fun Affine.calculateTransform(cWidth: Double, cHeight: Double, targetWidth: Double, targetHeight: Double) {
        val scale = Math.min(cWidth / targetWidth, cHeight / targetHeight)
        val dx = (cWidth - targetWidth * scale) * 0.5
        val dy = (cHeight - targetHeight * scale) * 0.5
        setToIdentity()
        appendTranslation(dx, dy)
        appendScale(scale, scale)
    }

    private fun pushDrawInterceptSignal() {
        drawInterceptSignal++
    }

    private fun popDrawInterceptSignal() {
        drawInterceptSignal--
    }

    private fun drawContent() {
        if (drawInterceptSignal > 0 || viewWidth <= 0 || viewHeight <= 0) {
            return
        }

        val gc = canvas.graphicsContext2D
        gc.fillRect(0.0, 0.0, viewWidth, viewHeight)

        val image = view.dicomImage ?: return
        gc.save()
        gc.transform(transform)
        gc.transform(imageTransform)
        image.draw(gc)
        gc.restore()
    }

    companion object {
        private const val REF_IMAGE = "IMAGE"
        private const val REF_COLOR_WINDOWING = "COLOR_WINDOWING"
        private const val REF_SIZE = "SIZE"

        private const val ZOOM_IN_STEP = 2.0
        private const val ZOOM_OUT_STEP = 0.5

        private val DATE_FORMATTER = SimpleDateFormat("yyyy/MM/dd")
        private val TIME_FORMATTER = SimpleDateFormat("HH:mm:ss")
    }
}