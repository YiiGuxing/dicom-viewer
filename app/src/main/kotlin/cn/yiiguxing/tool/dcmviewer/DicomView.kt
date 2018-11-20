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

    val dicomImagePriority: ObjectProperty<DicomImage?> = DicomImagePriority()
    var dicomImage: DicomImage? by dicomImagePriority

    private val _windowWidthProperty = ReadOnlyObjectWrapper<Float?>(this, "windowWidth", null)
    private val _windowCenterProperty = ReadOnlyObjectWrapper<Float?>(this, "windowCenter", null)

    val windowWidthProperty: ReadOnlyObjectProperty<Float?> = _windowWidthProperty.readOnlyProperty
    val windowWidth: Float? get() = windowWidthProperty.value

    val windowCenterProperty: ReadOnlyObjectProperty<Float?> = _windowCenterProperty.readOnlyProperty
    val windowCenter: Float? get() = windowCenterProperty.value

    val inverseProperty: BooleanProperty = SimpleBooleanProperty(this, "inverse", false)

    init {
        val loader = FXMLLoader(javaClass.getResource("/DicomView.fxml"))
        loader.setRoot(this)
        loader.controllerFactory = Callback { DicomViewController(this) }
        loader.load<AnchorPane>()
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