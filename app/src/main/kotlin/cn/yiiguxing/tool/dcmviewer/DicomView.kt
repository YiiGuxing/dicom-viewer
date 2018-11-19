package cn.yiiguxing.tool.dcmviewer

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color


/**
 * DicomView
 *
 * Created by Yii.Guxing on 2018/11/19
 */
class DicomView : AnchorPane() {

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

    init {
        val loader = FXMLLoader(javaClass.getResource("/DicomView.fxml"))
        loader.setRoot(this)
        loader.setController(this)
        loader.load<AnchorPane>()

        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())

        canvas.widthProperty().addListener { _, _, _ ->
            drawContent()
        }
        canvas.heightProperty().addListener { _, _, _ ->
            drawContent()
        }
    }

    private fun drawContent() {
        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, width, height)
        gc.fill = Color.RED
        gc.fillRect(0.0, 0.0, width - 2, height - 2)
    }

}