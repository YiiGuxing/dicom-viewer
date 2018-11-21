package cn.yiiguxing.tool.dcmviewer

import com.sun.javafx.scene.control.MultiplePropertyChangeListenerHandler
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.paint.Color

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

    private val changeHandler = MultiplePropertyChangeListenerHandler { handlePropertyChanged(it);null }

    @FXML
    private fun initialize() {
        canvas.widthProperty().bind(view.widthProperty())
        canvas.heightProperty().bind(view.heightProperty())

        changeHandler.apply {
            registerChangeListener(view.dicomImagePriority, REF_IMAGE)
            registerChangeListener(view.inverseProperty, REF_INVERSE)
            registerChangeListener(view.widthProperty(), REF_SIZE)
            registerChangeListener(view.heightProperty(), REF_SIZE)
        }
    }

    private fun handlePropertyChanged(reference: String) {
        when (reference) {
            REF_SIZE -> onSizeChanged()
        }

        drawContent()
    }

    private fun onSizeChanged() {

    }

    private fun drawContent() {
        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
        gc.fill = Color.RED
        gc.fillRect(0.0, 0.0, canvas.width - 2, canvas.height - 2)
    }

    companion object {
        private const val REF_IMAGE = "IMAGE"
        private const val REF_INVERSE = "INVERSE"
        private const val REF_SIZE = "SIZE"
    }
}