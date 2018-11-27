@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.tool.dcmviewer.control

import cn.yiiguxing.tool.dcmviewer.control.skin.DicomViewSkin
import cn.yiiguxing.tool.dcmviewer.image.DicomImage
import cn.yiiguxing.tool.dcmviewer.op.Op
import cn.yiiguxing.tool.dcmviewer.util.*
import javafx.beans.property.*
import javafx.scene.control.Control
import javafx.scene.control.Skin


/**
 * DicomView
 *
 * Created by Yii.Guxing on 2018/11/19
 */
class DicomView : Control() {

    val dicomImagePriority: ObjectProperty<DicomImage?> = DicomImagePriority()
    var dicomImage: DicomImage? by dicomImagePriority

    private val _windowWidthProperty = ReadOnlyObjectWrapper<Float?>(this, "windowWidth", null)
    private val _windowCenterProperty = ReadOnlyObjectWrapper<Float?>(this, "windowCenter", null)

    val windowWidthProperty: ReadOnlyObjectProperty<Float?> = _windowWidthProperty.readOnlyProperty
    val windowWidth: Float? get() = windowWidthProperty.value

    val windowCenterProperty: ReadOnlyObjectProperty<Float?> = _windowCenterProperty.readOnlyProperty
    val windowCenter: Float? get() = windowCenterProperty.value

    val inverseProperty: BooleanProperty = SimpleBooleanProperty(this, "inverse", false)
    var inverse: Boolean by inverseProperty

    private val _actualSizeProperty = ReadOnlyBooleanWrapper(this, "isActualSize", false)
    private val _canZoomInProperty = ReadOnlyBooleanWrapper(this, "canZoomIn", false)
    private val _canZoomOutProperty = ReadOnlyBooleanWrapper(this, "canZoomOut", false)

    val actualSizeProperty: ReadOnlyBooleanProperty = _actualSizeProperty.readOnlyProperty
    val isActualSize: Boolean get() = actualSizeProperty.get()

    val canZoomInProperty: ReadOnlyBooleanProperty = _canZoomInProperty.readOnlyProperty
    val isCanZoomIn: Boolean get() = canZoomInProperty.get()

    val canZoomOutProperty: ReadOnlyBooleanProperty = _canZoomOutProperty.readOnlyProperty
    val isCanZoomOut: Boolean get() = canZoomOutProperty.get()

    val opProperty: ObjectProperty<Op> = SimpleObjectProperty(this, "op", Op.WINDOWING)
    var op: Op by opProperty

    private val skin: DicomViewSkin get() = getSkin() as DicomViewSkin

    init {
        skinProperty().addListener { _, _, _ ->
            _actualSizeProperty.bind(skin.actualSizeProperty)
            _canZoomInProperty.bind(skin.canZoomInProperty)
            _canZoomOutProperty.bind(skin.canZoomOutProperty)
        }
    }

    fun setColorWindowing(windowWidth: Float, windowCenter: Float) {
        dicomImage?.setColorWindowing(windowWidth, windowCenter)
    }

    fun invert() {
        inverse = !inverse
    }

    fun locate() {
        skin.locate()
    }

    fun zoomIn() {
        skin.zoomIn()
    }

    fun zoomOut() {
        skin.zoomOut()
    }

    fun zoomToActualSize() {
        skin.scaleToActualSize()
    }

    fun clockwiseRotate() {
        skin.rotate(90.0)
    }

    fun counterclockwiseRotate() {
        skin.rotate(-90.0)
    }

    fun horizontalFlip() {
        skin.horizontalFlip()
    }

    fun verticalFlip() {
        skin.verticalFlip()
    }

    fun reset() {
        skin.reset()
    }

    override fun createDefaultSkin(): Skin<*> {
        return DicomViewSkin(this)
    }

    private inner class DicomImagePriority : ObjectPropertyBase<DicomImage?>() {
        private var previousValue: DicomImage? = null

        override fun getName(): String = "dicomImage"
        override fun getBean(): Any = this@DicomView

        override fun set(newValue: DicomImage?) {
            previousValue = value
            super.set(newValue)
        }

        override fun invalidated() {
            val value = value
            if (value != null) {
                _windowWidthProperty.bind(value.windowWidthProperty.asObject())
                _windowCenterProperty.bind(value.windowCenterProperty.asObject())
                inverseProperty.bindBidirectional(value.inverseProperty)
            } else {
                _windowWidthProperty.let { p ->
                    p.unbind()
                    p.value = null
                }
                _windowCenterProperty.let { p ->
                    p.unbind()
                    p.value = null
                }
                previousValue?.let { inverseProperty.unbindBidirectional(it.inverseProperty) }
            }
        }
    }

}