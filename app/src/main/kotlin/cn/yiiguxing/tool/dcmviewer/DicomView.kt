package cn.yiiguxing.tool.dcmviewer

import cn.yiiguxing.dicom.getValue
import cn.yiiguxing.dicom.image.DicomImage
import cn.yiiguxing.dicom.setValue
import javafx.beans.property.*
import javafx.fxml.FXMLLoader
import javafx.scene.layout.AnchorPane
import javafx.util.Callback


/**
 * DicomView
 *
 * Created by Yii.Guxing on 2018/11/19
 */
class DicomView : AnchorPane() {

    private val controller: DicomViewController

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

    init {
        val loader = FXMLLoader(javaClass.getResource("/DicomView.fxml"))
        loader.setRoot(this)
        loader.controllerFactory = Callback { DicomViewController(this) }
        loader.load<AnchorPane>()
        controller = loader.getController()

        _actualSizeProperty.bind(controller.actualSizeProperty)
        _canZoomInProperty.bind(controller.canZoomInProperty)
        _canZoomOutProperty.bind(controller.canZoomOutProperty)
    }

    fun setColorWindowing(windowWidth: Float, windowCenter: Float) {
        dicomImage?.setColorWindowing(windowWidth, windowCenter)
    }

    fun invert() {
        inverse = !inverse
    }

    fun locate() {
        controller.locate()
    }

    fun zoomIn() {
        if (isCanZoomIn) {
            controller.scale(2.0)
        }
    }

    fun zoomOut() {
        if (isCanZoomOut) {
            controller.scale(0.5)
        }
    }

    fun zoomToActualSize() {
        if (!isActualSize) {
            controller.scaleToActualSize()
        }
    }

    fun clockwiseRotate() {
        controller.rotate(Math.toRadians(90.0))
    }

    fun counterclockwiseRotate() {
        controller.rotate(Math.toRadians(-90.0))
    }

    fun horizontalFlip() {
        controller.horizontalFlip()
    }

    fun verticalFlip() {
        controller.verticalFlip()
    }

    fun reset() {
        controller.reset()
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