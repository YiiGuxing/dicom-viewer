package cn.yiiguxing.tool.dcmviewer

import com.sun.javafx.scene.control.MultiplePropertyChangeListenerHandler
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.paint.Color
import javafx.scene.transform.Affine
import javafx.scene.transform.TransformChangedEvent
import java.awt.geom.AffineTransform

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
        bindVisible()
        registerChangeListener()

        canvas.widthProperty().bind(view.widthProperty())
        canvas.heightProperty().bind(view.heightProperty())

        transform.addEventHandler(TransformChangedEvent.ANY) {
            drawContent()
        }
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
        registerChangeListener(view.dicomImagePriority, REF_IMAGE)
        registerChangeListener(view.inverseProperty, REF_INVERSE)
        registerChangeListener(view.widthProperty(), REF_SIZE)
        registerChangeListener(view.heightProperty(), REF_SIZE)
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
    }

    fun horizontalFlip() {

    }

    fun verticalFlip() {

    }

    fun reset() {

    }

    private fun handlePropertyChanged(reference: String) {
        when (reference) {
            REF_SIZE -> onSizeChanged()
        }

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

        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)

        gc.save()

        gc.fill = Color.RED
        gc.fillRect(0.0, 0.0, canvas.width - 2, canvas.height - 2)

        gc.restore()
    }

    companion object {
        private const val REF_IMAGE = "IMAGE"
        private const val REF_INVERSE = "INVERSE"
        private const val REF_SIZE = "SIZE"
    }
}