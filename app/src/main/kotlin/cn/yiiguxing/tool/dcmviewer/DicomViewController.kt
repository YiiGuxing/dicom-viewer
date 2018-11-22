package cn.yiiguxing.tool.dcmviewer

import cn.yiiguxing.dicom.BodyOrientation
import cn.yiiguxing.dicom.opposites
import cn.yiiguxing.dicom.toLabel
import com.sun.javafx.scene.control.MultiplePropertyChangeListenerHandler
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.effect.BoxBlur
import javafx.scene.transform.Affine
import javafx.scene.transform.TransformChangedEvent
import javax.vecmath.Vector3d

/**
 * DicomViewController
 *
 * Created by Yii.Guxing on 2018/11/20
 */
internal class DicomViewController(private val view: DicomView) {

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

    private val width: Double get() = canvas.width
    private val height: Double get() = canvas.height

    private val transform: Affine = Affine()
    private val imageTransform: Affine = Affine()

    private var rotationRadian: Double = 0.0
    private var isHorizontalFlip: Boolean = false
    private var isVerticalFlip: Boolean = false

    private var interceptDrawing = false

    internal val actualSizeProperty: BooleanProperty = SimpleBooleanProperty(false)
    internal val canZoomInProperty: BooleanProperty = SimpleBooleanProperty(false)
    internal val canZoomOutProperty: BooleanProperty = SimpleBooleanProperty(false)

    private val changeHandler = MultiplePropertyChangeListenerHandler { handlePropertyChanged(it);null }

    @FXML
    private fun initialize() {
        interceptDrawing = true
        bindVisible()
        registerChangeListener()
        initCanvas()

        transform.addEventHandler(TransformChangedEvent.ANY) {
            drawContent()
        }

        interceptDrawing = false
        drawContent()
    }

    private fun initCanvas() {
        // FIXME ANTI-ALIASING: Is there a better implementation?
        val blur = BoxBlur().apply {
            width = 1.0
            height = 1.0
            iterations = 1
        }
        canvas.graphicsContext2D.setEffect(blur)

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
        val cw = width / 2.0
        val ch = height / 2.0
        val point = transform.transform(cw, ch)
        transform.prependTranslation(cw - point.x, ch - point.y)
    }

    fun scale(scale: Double) {
        val cx = width / 2.0
        val cy = height / 2.0
        transform.appendScale(scale, scale, cx, cy)
    }

    fun scaleToActualSize() {

    }

    fun rotate(radian: Double) {
        val cx = width / 2.0
        val cy = height / 2.0
        rotationRadian = (rotationRadian + radian) % (2.0 * Math.PI)
        transform.appendRotation(radian, cx, cy)
        updateOrientation()
    }

    fun horizontalFlip() {
        val cx = width / 2.0
        val cy = height / 2.0
        val rotation = rotationRadian

        interceptDrawing = true
        transform.apply {
            if (rotation != 0.0) {
                appendRotation(2.0 * (Math.PI - rotation), cx, cy)
            }
            appendScale(-1.0, 1.0, cx, cy)
        }
        interceptDrawing = false

        isHorizontalFlip = !isHorizontalFlip
        drawContent()
        updateOrientation()
    }

    fun verticalFlip() {
        val cx = width / 2.0
        val cy = height / 2.0
        val rotation = rotationRadian

        interceptDrawing = true
        transform.apply {
            if (rotation != 0.0) {
                appendRotation(-2.0 * rotation, cx, cy)
            }
            appendScale(1.0, -1.0, cx, cy)
        }
        interceptDrawing = false

        isVerticalFlip = !isVerticalFlip
        drawContent()
        updateOrientation()
    }

    fun reset() {
        interceptDrawing = true
        rotationRadian = 0.0
        isHorizontalFlip = false
        isVerticalFlip = false
        transform.setToIdentity()
        view.dicomImage?.resetImage()
        interceptDrawing = false

        drawContent()
        updateOrientation()
    }

    private fun updateOrientation() {
        val image = view.dicomImage ?: return
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
        leftOrientation.text = left.toLabel()
        topOrientation.text = top.toLabel()
        rightOrientation.text = left.opposites().toLabel()
        bottomOrientation.text = top.opposites().toLabel()
    }

    private fun handlePropertyChanged(reference: String) {
        interceptDrawing = true
        when (reference) {
            REF_SIZE -> onSizeChanged()
        }

        interceptDrawing = false
        drawContent()
    }

    private fun onSizeChanged() {
        updateImageTransform()
        locate()
    }

    private fun updateImageTransform() {
        if (width > 0 && height > 0) {
            view.dicomImage?.let { img ->
                imageTransform.calculateTransform(width, height, img.width.toDouble(), img.height.toDouble())
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

    private fun drawContent() {
        if (interceptDrawing || width <= 0 || height <= 0) {
            return
        }

        val image = view.dicomImage ?: return
        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, width, height)
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
    }
}