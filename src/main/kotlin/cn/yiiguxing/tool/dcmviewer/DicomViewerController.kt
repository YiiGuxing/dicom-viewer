package cn.yiiguxing.tool.dcmviewer

import cn.yiiguxing.tool.dcmviewer.image.DicomImage
import cn.yiiguxing.tool.dcmviewer.image.DicomImageIO
import cn.yiiguxing.tool.dcmviewer.util.Alerts
import cn.yiiguxing.tool.dcmviewer.util.AttributeItem
import cn.yiiguxing.tool.dcmviewer.util.attributeItems
import cn.yiiguxing.tool.dcmviewer.util.getAttributesAsGBKString
import com.sun.javafx.binding.StringConstant
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import org.dcm4che3.data.Tag
import org.dcm4che3.data.VR
import org.dcm4che3.util.TagUtils
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
    private lateinit var attributesTable: TableView<AttributeItem>
    @FXML
    private lateinit var tagColumn: TableColumn<AttributeItem, String>
    @FXML
    private lateinit var vrColumn: TableColumn<AttributeItem, String>
    @FXML
    private lateinit var descColumn: TableColumn<AttributeItem, String>
    @FXML
    private lateinit var valueColumn: TableColumn<AttributeItem, String>

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

        tagColumn.cellValueFactory = Callback { StringConstant.valueOf(TagUtils.toString(it.value.tag)) }
        vrColumn.cellValueFactory = Callback { StringConstant.valueOf(it.value.vr.name) }
        descColumn.cellValueFactory = Callback { StringConstant.valueOf(it.value.description) }
        valueColumn.cellValueFactory = Callback { StringConstant.valueOf(it.value.valueString) }
    }

    fun open(file: File) {
        val image: DicomImage = try {
            DicomImageIO().use { io ->
                io.setFile(file)
                io.read(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Alerts.error("Can't open this file!", window = stage)
            return
        }
        try {
            setDicomImage(image)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setDicomImage(image: DicomImage?) {
        dicomView.dicomImage = image

        val metadata = image?.metadata
        val patientName = metadata?.let { "${it.getAttributesAsGBKString(Tag.PatientName, VR.PN) as String} - " } ?: ""
        stage.title = "${patientName}Dicom Viewer"

        if (metadata != null) {
            attributesTable.items.addAll(metadata.attributeItems.sortedBy { it.tag })
        } else {
            attributesTable.items.clear()
        }
    }

    @FXML
    private fun openNewFile() {
        with(FileChooser()) {
            extensionFilters.add(
                FileChooser.ExtensionFilter(
                    "Dicom files",
                    "*.$FILE_EXTENSION_DCM", "*.$FILE_EXTENSION_DICOM"
                )
            )
            showOpenDialog(stage)
        }?.let { open(it) }
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
                event.acceptTransferModes(TransferMode.MOVE)
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