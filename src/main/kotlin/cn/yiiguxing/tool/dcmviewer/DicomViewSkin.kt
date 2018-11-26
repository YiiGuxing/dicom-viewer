package cn.yiiguxing.tool.dcmviewer

import cn.yiiguxing.tool.dcmviewer.op.*
import com.sun.javafx.scene.control.behavior.BehaviorBase
import com.sun.javafx.scene.control.skin.BehaviorSkinBase
import javafx.beans.binding.Bindings
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.scene.transform.Affine
import javafx.scene.transform.TransformChangedEvent
import org.dcm4che3.data.Tag
import java.text.SimpleDateFormat
import java.util.concurrent.Callable
import javax.vecmath.Vector3d
import kotlin.math.abs

/**
 * DicomViewSkin
 *
 * Created by Yii.Guxing on 2018/11/20
 */
class DicomViewSkin(control: DicomView) :
    BehaviorSkinBase<DicomView, BehaviorBase<DicomView>>(control, BehaviorBase(control, emptyList())) {

    private val canvas: Canvas = Canvas()
    private val leftTopAnnotation: Label = Label()
    private val rightTopAnnotation: Label = Label()
    private val leftBottomAnnotation: Label = Label()
    private val rightBottomAnnotation: Label = Label()
    private val leftOrientation: Label = Label()
    private val topOrientation: Label = Label()
    private val rightOrientation: Label = Label()
    private val bottomOrientation: Label = Label()

    private val contents: Array<out Node> = arrayOf(
        canvas,
        leftOrientation,
        topOrientation,
        rightOrientation,
        bottomOrientation,
        leftTopAnnotation,
        rightTopAnnotation,
        leftBottomAnnotation,
        rightBottomAnnotation
    )

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

    private val ops: Array<out MouseOperation> = arrayOf(
        WindowingOp(this),
        ScaleOp(this),
        TranslationOp(this),
        RotationOp(this)
    )

    init {
        pushDrawInterceptSignal()
        initContents()
        bindVisible()
        bindViewport()
        setupOp()
        registerChangeListener()
        transform.addEventHandler(TransformChangedEvent.ANY) {
            drawContent()
        }
        popDrawInterceptSignal()
    }

    private fun initContents() {
        children.addAll(contents)
        val shadow = DropShadow(BlurType.ONE_PASS_BOX, Color.BLACK, 1.0, 1.0, 1.0, 1.0)
        for (content in contents) {
            (content as? Label)?.apply {
                textFill = Color.WHITE
                effect = shadow
            }
        }

        rightTopAnnotation.textAlignment = TextAlignment.RIGHT
        rightBottomAnnotation.textAlignment = TextAlignment.RIGHT
    }

    private fun bindVisible() {
        val hasImage = skinnable.dicomImagePriority.isNotNull
        leftTopAnnotation.visibleProperty().bind(hasImage)
        rightTopAnnotation.visibleProperty().bind(hasImage)
        leftBottomAnnotation.visibleProperty().bind(hasImage)
        rightBottomAnnotation.visibleProperty().bind(hasImage)

        val hasOrientation = Bindings.createBooleanBinding(Callable {
            skinnable.dicomImage?.orientation != null
        }, skinnable.dicomImagePriority)
        leftOrientation.visibleProperty().bind(hasOrientation)
        topOrientation.visibleProperty().bind(hasOrientation)
        rightOrientation.visibleProperty().bind(hasOrientation)
        bottomOrientation.visibleProperty().bind(hasOrientation)
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
            val ww = skinnable.windowWidth?.let { Math.round(it) } ?: "-"
            val wc = skinnable.windowCenter?.let { Math.round(it) } ?: "-"
            "WW/WC: $ww/$wc"
        }, skinnable.windowWidthProperty, skinnable.windowCenterProperty)
        rightBottomAnnotation.textProperty().bind(Bindings.concat(scaleString, colorWindowingString))
    }

    private fun setupOp() {
        canvas.addEventHandler(MouseEvent.ANY, ops[skinnable.op.ordinal])
        canvas.addEventHandler(ScrollEvent.ANY, ScaleWheelOp(this))
        skinnable.opProperty.addListener { _, old, new ->
            canvas.removeEventHandler(MouseEvent.ANY, ops[old.ordinal])
            canvas.addEventHandler(MouseEvent.ANY, ops[new.ordinal])
        }
    }

    private fun registerChangeListener() {
        registerChangeListener(canvas.widthProperty(), REF_SIZE)
        registerChangeListener(canvas.heightProperty(), REF_SIZE)
        registerChangeListener(skinnable.dicomImagePriority, REF_IMAGE)
        registerChangeListener(skinnable.windowWidthProperty, REF_COLOR_WINDOWING)
        registerChangeListener(skinnable.windowCenterProperty, REF_COLOR_WINDOWING)
        registerChangeListener(skinnable.inverseProperty, REF_COLOR_WINDOWING)
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
        skinnable.dicomImage?.resetImage()
        popDrawInterceptSignal()

        drawContent()
        updateOrientation()
    }

    private fun updateOrientation() {
        val image = skinnable.dicomImage ?: return
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

    override fun handleControlPropertyChanged(reference: String) {
        pushDrawInterceptSignal()
        when (reference) {
            REF_IMAGE -> onImageChanged()
            REF_SIZE -> onSizeChanged()
        }

        popDrawInterceptSignal()
        drawContent()
    }

    private fun onImageChanged() {
        val image = skinnable.dicomImage
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
            skinnable.dicomImage?.let { img ->
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

        val image = skinnable.dicomImage ?: return
        gc.save()
        gc.transform(transform)
        gc.transform(imageTransform)
        image.draw(gc)
        gc.restore()
    }

    override fun computeMinWidth(
        height: Double,
        topInset: Double,
        rightInset: Double,
        bottomInset: Double,
        leftInset: Double
    ): Double {
        val minWidth = super.computeMinWidth(height, topInset, rightInset, bottomInset, leftInset)

        val left = maxOf(
            leftTopAnnotation.minWidth(-1.0),
            leftOrientation.minWidth(-1.0),
            leftBottomAnnotation.minWidth(-1.0)
        )
        val mid = maxOf(
            topOrientation.minWidth(-1.0),
            bottomOrientation.minWidth(-1.0)
        )
        val right = maxOf(
            rightTopAnnotation.minWidth(-1.0),
            rightOrientation.minWidth(-1.0),
            rightBottomAnnotation.minWidth(-1.0)
        )

        return maxOf(minWidth, leftInset + left + mid + right + rightInset)
    }

    override fun computeMinHeight(
        width: Double,
        topInset: Double,
        rightInset: Double,
        bottomInset: Double,
        leftInset: Double
    ): Double {
        val minHeight = super.computeMinHeight(width, topInset, rightInset, bottomInset, leftInset)

        val top = maxOf(
            leftTopAnnotation.minHeight(-1.0),
            topOrientation.minHeight(-1.0),
            rightTopAnnotation.minHeight(-1.0)
        )
        val mid = maxOf(
            leftOrientation.minHeight(-1.0),
            rightOrientation.minHeight(-1.0)
        )
        val bottom = maxOf(
            leftBottomAnnotation.minHeight(-1.0),
            bottomOrientation.minHeight(-1.0),
            rightBottomAnnotation.minHeight(-1.0)
        )

        return maxOf(minHeight, topInset + top + mid + bottom + bottomInset)
    }

    override fun computePrefWidth(
        height: Double,
        topInset: Double,
        rightInset: Double,
        bottomInset: Double,
        leftInset: Double
    ): Double {
        var minX = 0.0
        var maxX = 0.0
        var firstManagedChild = true
        val children = children
        for (i in children.indices) {
            val node = children[i]
            if (node !== canvas && node.isManaged) {
                val x = node.layoutBounds.minX + node.layoutX
                if (!firstManagedChild) {  // branch prediction favors most often used condition
                    minX = Math.min(minX, x)
                    maxX = Math.max(maxX, x + node.prefWidth(-1.0))
                } else {
                    minX = x
                    maxX = x + node.prefWidth(-1.0)
                    firstManagedChild = false
                }
            }
        }
        return maxX - minX
    }

    override fun computePrefHeight(
        width: Double,
        topInset: Double,
        rightInset: Double,
        bottomInset: Double,
        leftInset: Double
    ): Double {
        var minY = 0.0
        var maxY = 0.0
        var firstManagedChild = true
        val children = children
        for (i in children.indices) {
            val node = children[i]
            if (node !== canvas && node.isManaged) {
                val y = node.layoutBounds.minY + node.layoutY
                if (!firstManagedChild) {  // branch prediction favors most often used condition
                    minY = Math.min(minY, y)
                    maxY = Math.max(maxY, y + node.prefHeight(-1.0))
                } else {
                    minY = y
                    maxY = y + node.prefHeight(-1.0)
                    firstManagedChild = false
                }
            }
        }
        return maxY - minY
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        val contents = contents
        val children = children
        for (i in children.indices) {
            val child = children[i]
            if (child.isManaged && child !in contents) {
                layoutInArea(child, contentX, contentY, contentWidth, contentHeight, -1.0, HPos.CENTER, VPos.CENTER)
            }
        }

        layoutInArea(
            canvas,
            contentX, contentY, contentWidth, contentHeight, -1.0,
            HPos.LEFT, VPos.TOP
        )
        layoutInArea(
            leftTopAnnotation,
            contentX, contentY, contentWidth, contentHeight, -1.0, PADDING, true, true,
            HPos.LEFT, VPos.TOP
        )
        layoutInArea(
            rightTopAnnotation,
            contentX, contentY, contentWidth, contentHeight, -1.0, PADDING, true, true,
            HPos.RIGHT, VPos.TOP
        )
        layoutInArea(
            leftBottomAnnotation,
            contentX, contentY, contentWidth, contentHeight, -1.0, PADDING, true, true,
            HPos.LEFT, VPos.BOTTOM
        )
        layoutInArea(
            rightBottomAnnotation,
            contentX, contentY, contentWidth, contentHeight, -1.0, PADDING, true, true,
            HPos.RIGHT, VPos.BOTTOM
        )
        layoutInArea(
            leftOrientation,
            contentX, contentY, contentWidth, contentHeight, -1.0, PADDING, true, true,
            HPos.LEFT, VPos.CENTER
        )
        layoutInArea(
            topOrientation,
            contentX, contentY, contentWidth, contentHeight, -1.0, PADDING, true, true,
            HPos.CENTER, VPos.TOP
        )
        layoutInArea(
            rightOrientation,
            contentX, contentY, contentWidth, contentHeight, -1.0, PADDING, true, true,
            HPos.RIGHT, VPos.CENTER
        )
        layoutInArea(
            bottomOrientation,
            contentX, contentY, contentWidth, contentHeight, -1.0, PADDING, true, true,
            HPos.CENTER, VPos.BOTTOM
        )

        pushDrawInterceptSignal()
        canvas.width = contentWidth
        canvas.height = contentHeight
        popDrawInterceptSignal()
        drawContent()
    }

    companion object {
        private const val REF_IMAGE = "IMAGE"
        private const val REF_COLOR_WINDOWING = "COLOR_WINDOWING"
        private const val REF_SIZE = "SIZE"

        private val PADDING: Insets = Insets(10.0)

        private const val ZOOM_IN_STEP = 2.0
        private const val ZOOM_OUT_STEP = 0.5

        private val DATE_FORMATTER = SimpleDateFormat("yyyy/MM/dd")
        private val TIME_FORMATTER = SimpleDateFormat("HH:mm:ss")
    }
}