package cn.yiiguxing.tool.dcmviewer

import cn.yiiguxing.tool.dcmviewer.control.DicomView
import cn.yiiguxing.tool.dcmviewer.control.OpRadioButton
import cn.yiiguxing.tool.dcmviewer.image.DicomImage
import cn.yiiguxing.tool.dcmviewer.image.DicomImageIO
import cn.yiiguxing.tool.dcmviewer.layout.FakeFocusHBox
import cn.yiiguxing.tool.dcmviewer.util.Alerts
import cn.yiiguxing.tool.dcmviewer.util.AttributeItem
import cn.yiiguxing.tool.dcmviewer.util.getGBKStrings
import cn.yiiguxing.tool.dcmviewer.util.items
import com.sun.javafx.binding.StringConstant
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import org.dcm4che3.data.Tag
import org.dcm4che3.data.VR
import java.io.File

/**
 * DicomViewerController
 *
 * Created by Yii.Guxing on 2018/11/24
 */
class DicomViewerController(private val stage: Stage) {

    @FXML
    private lateinit var frame: VBox
    @FXML
    private lateinit var toolbar: ToolBar
    @FXML
    private lateinit var dicomView: DicomView
    @FXML
    private lateinit var contentPane: SplitPane
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
    private lateinit var filterTextField: TextField
    @FXML
    private lateinit var searchPane: StackPane
    @FXML
    private lateinit var toggleDicomInfoButton: ToggleButton
    @FXML
    private lateinit var dicomInfoToolBar: FakeFocusHBox
    @FXML
    private lateinit var dicomInfoTable: TreeTableView<AttributeItem>
    @FXML
    private lateinit var tagColumn: TreeTableColumn<AttributeItem, String>
    @FXML
    private lateinit var vrColumn: TreeTableColumn<AttributeItem, String>
    @FXML
    private lateinit var descColumn: TreeTableColumn<AttributeItem, String>
    @FXML
    private lateinit var valueColumn: TreeTableColumn<AttributeItem, String>

    private val treeRoot = TreeItem<AttributeItem>(null)
    private var attributeItems: List<AttributeItem>? = null

    private var lastOpenFile: File? = null

    @FXML
    private fun initialize() {
        bindDicomView()
        initDicomInfoToolbar()
        initDicomInfoTreeTable()

        // To Fix: Ah, my eyes!
        frame.children.remove(contentPane)
        frame.children.remove(toolbar)
        Platform.runLater {
            frame.children.add(toolbar)
            frame.children.add(contentPane)
        }
    }

    private fun bindDicomView() {
        dicomView.inverseProperty.bindBidirectional(invertButton.selectedProperty())
        dropLabel.visibleProperty().bind(dicomView.dicomImagePriority.isNull)
        zoomInButton.disableProperty().bind(dicomView.canZoomInProperty.not())
        zoomOutButton.disableProperty().bind(dicomView.canZoomOutProperty.not())
        zoomToActualSizeButton.disableProperty().bind(dicomView.actualSizeProperty)
        opGroup.selectedToggleProperty().addListener { _, _, op ->
            (op as? OpRadioButton)?.op?.let {
                dicomView.op = it
            }
        }
    }

    private fun initDicomInfoToolbar() {
        filterTextField.focusedProperty().addListener { _, _, focused ->
            dicomInfoToolBar.setFakeFocus(focused)
            if (focused) {
                Platform.runLater {
                    filterTextField.selectAll()
                }
            }
        }
        toggleDicomInfoButton.focusedProperty().addListener { _, _, focused ->
            dicomInfoToolBar.setFakeFocus(focused)
        }
        filterTextField.textProperty().addListener { _, _, _ -> updateAttributesTree() }

        contentPane.items.remove(dicomInfoTable)
        dicomInfoToolBar.children.remove(searchPane)
    }

    private fun initDicomInfoTreeTable() {
        dicomInfoTable.root = treeRoot
        tagColumn.cellValueFactory = Callback { StringConstant.valueOf(it.value.value.tagString) }
        vrColumn.cellValueFactory = Callback { StringConstant.valueOf(it.value.value.vrString) }
        descColumn.cellValueFactory = Callback { StringConstant.valueOf(it.value.value.description) }
        valueColumn.cellValueFactory = Callback { StringConstant.valueOf(it.value.value.valueString) }

        tagColumn.cellFactory = CellFactory
        vrColumn.cellFactory = CellFactory
        descColumn.cellFactory = CellFactory
        valueColumn.cellFactory = CellFactory

        fun String.copy() {
            val content = ClipboardContent()
            content.putString(this)
            Clipboard.getSystemClipboard().setContent(content)
        }

        val contextMenu = ContextMenu()
        contextMenu.items.addAll(
            MenuItem("Copy Tag").apply {
                setOnAction { dicomInfoTable.selectionModel.selectedItem?.value?.tagString?.copy() }
            },
            MenuItem("Copy VR").apply {
                setOnAction { dicomInfoTable.selectionModel.selectedItem?.value?.vrString?.copy() }
            },
            MenuItem("Copy Description").apply {
                setOnAction { dicomInfoTable.selectionModel.selectedItem?.value?.description?.copy() }
            },
            MenuItem("Copy Value").apply {
                setOnAction { dicomInfoTable.selectionModel.selectedItem?.value?.valueString?.copy() }
            }
        )

        dicomInfoTable.setOnContextMenuRequested {
            if (!contextMenu.isShowing && dicomInfoTable.selectionModel.selectedItem != null) {
                contextMenu.show(dicomInfoTable, it.screenX, it.screenY)
            }
        }
        dicomInfoTable.setOnMouseClicked {
            if (contextMenu.isShowing) {
                contextMenu.hide()
            }
        }
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
        lastOpenFile = file
        setDicomImage(image, file)
    }

    private fun setDicomImage(image: DicomImage?, file: File) {
        dicomView.dicomImage = image

        val attributes = image?.metadata?.attributes
        val patientName = attributes?.let {
            var patientName = it.getGBKStrings(Tag.PatientName, VR.PN, "") as String
            if (patientName.length > 25) {
                patientName = patientName.take(25) + "..."
            }
            var filePath = file.absolutePath
            if (filePath.length > 65) {
                filePath = filePath.take(30) + "..." + filePath.takeLast(30)
            }

            "$patientName [$filePath] - "
        } ?: ""
        stage.title = "${patientName}Dicom Viewer"

        attributeItems = attributes?.items
        updateAttributesTree()
    }

    private fun updateAttributesTree() {
        treeRoot.let { root ->
            root.children.clear()
            attributeItems?.buildTreeItem(root, filterTextField.text)
        }
    }

    private fun List<AttributeItem>.buildTreeItem(parent: TreeItem<AttributeItem>, filter: String?) {
        val parentsChildren = parent.children
        for (item in this) {
            if (filter.isNullOrBlank()
                || item.tagString.contains(filter, true)
                || item.description.contains(filter, true)
                || item.valueString.contains(filter, true)
            ) {
                val element = TreeItem(item)
                item.children.buildTreeItem(element, filter)
                parentsChildren.add(element)
            }
        }
    }

    @FXML
    private fun openNewFile() {
        with(FileChooser()) {
            lastOpenFile?.parentFile?.let { initialDirectory = it }
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
    private fun toggleDicomInfoPan() {
        if (toggleDicomInfoButton.isSelected) {
            contentPane.items.add(dicomInfoTable)
            contentPane.setDividerPositions(0.6)
            dicomInfoToolBar.children.add(0, searchPane)
            toggleDicomInfoButton.tooltip.text = "Hide DICOM Information"
        } else {
            contentPane.items.remove(dicomInfoTable)
            dicomInfoToolBar.children.remove(searchPane)
            toggleDicomInfoButton.tooltip.text = "Show DICOM Information"
        }
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

    object CellFactory : Callback<TreeTableColumn<AttributeItem, String>, TreeTableCell<AttributeItem, String>> {

        override fun call(param: TreeTableColumn<AttributeItem, String>): TreeTableCell<AttributeItem, String> {
            return object : TreeTableCell<AttributeItem, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    if (item === getItem()) return

                    super.updateItem(item, empty)

                    text = item
                    graphic = null
                    tooltip = (tooltip ?: Tooltip()).also { it.text = item }
                }
            }
        }
    }
}