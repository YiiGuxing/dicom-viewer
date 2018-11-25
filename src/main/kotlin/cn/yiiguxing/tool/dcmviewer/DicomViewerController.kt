package cn.yiiguxing.tool.dcmviewer

import cn.yiiguxing.tool.dcmviewer.image.DicomImage
import cn.yiiguxing.tool.dcmviewer.image.DicomImageIO
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import javafx.stage.Stage
import org.dcm4che3.data.Tag
import java.io.File

/**
 * DicomViewerController
 *
 * Created by Yii.Guxing on 2018/11/24
 */
class DicomViewerController(private val stage: Stage) {

    @FXML
    private lateinit var dicomView: DicomView

    @FXML
    private lateinit var dropLabel: Label
    @FXML
    private lateinit var opGroup: ToggleGroup
    @FXML
    private lateinit var invertButton: ToggleButton
    @FXML
    private lateinit var zoomInButton: Button
    @FXML
    private lateinit var zoomOutButton: Button
    @FXML
    private lateinit var zoomToActualSizeButton: Button

    @FXML
    private fun initialize() {
        dicomView.inverseProperty.bindBidirectional(invertButton.selectedProperty())
        dropLabel.visibleProperty().bind(dicomView.dicomImagePriority.isNull)
        zoomInButton.disableProperty().bind(dicomView.canZoomInProperty.not())
        zoomOutButton.disableProperty().bind(dicomView.canZoomOutProperty.not())
        zoomToActualSizeButton.disableProperty().bind(dicomView.actualSizeProperty)
        opGroup.selectedToggleProperty().addListener { _, _, op ->
            (op as? OpRadioButton)?.op?.let { dicomView.op = it }
        }
    }

    fun open(file: File) {
        val image = DicomImageIO().run {
            setFile(file)
            read(0)
        }
        setDicomImage(image)
    }

    private fun setDicomImage(image: DicomImage?) {
        dicomView.dicomImage = image
        val patientName = image?.let { "${it.metadata.attributes.getString(Tag.PatientName)} - " } ?: ""
        stage.title = "${patientName}Dicom Viewer"
    }

    @FXML
    private fun openNewFile() {
        println("openNewFile")
    }

    @FXML
    private fun locate() {
        dicomView.locate()
    }

    @FXML
    private fun zoomIn() {
        dicomView.zoomIn()
    }

    @FXML
    private fun zoomOut() {
        dicomView.zoomOut()
    }

    @FXML
    private fun zoomToActualSize() {
        dicomView.zoomToActualSize()
    }

    @FXML
    private fun clockwiseRotate() {
        dicomView.clockwiseRotate()
    }

    @FXML
    private fun counterclockwiseRotate() {
        dicomView.counterclockwiseRotate()
    }

    @FXML
    private fun horizontalFlip() {
        dicomView.horizontalFlip()
    }

    @FXML
    private fun verticalFlip() {
        dicomView.verticalFlip()
    }

    @FXML
    private fun reset() {
        dicomView.reset()
    }

    @FXML
    private fun onDragOver(event: DragEvent) {
        val db = event.dragboard
        if (db.hasFiles()) {
            val isSupportedFile = db
                .files
                .firstOrNull()
                ?.extension
                .let { ext ->
                    FILE_EXTENSION_DCM.equals(ext, true) || FILE_EXTENSION_DICOM.equals(ext, true)
                }
            if (isSupportedFile) {
                event.acceptTransferModes(TransferMode.LINK)
            }
        }
        event.consume()
    }

    @FXML
    private fun onDragDropped(event: DragEvent) {
        var success = false
        val db = event.dragboard
        if (db.hasFiles()) {
            db.files
                .firstOrNull()
                ?.let {
                    open(it)
                    success = true
                }
        }
        event.isDropCompleted = success
        event.consume()
    }

    companion object {
        private const val FILE_EXTENSION_DCM = "dcm"
        private const val FILE_EXTENSION_DICOM = "dicom"
    }

}