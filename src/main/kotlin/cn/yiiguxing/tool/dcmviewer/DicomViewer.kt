package cn.yiiguxing.tool.dcmviewer

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.stage.Stage
import javafx.util.Callback

/**
 * DicomViewer
 *
 * Created by Yii.Guxing on 2018/11/24
 */
class DicomViewer : Application() {
    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(javaClass.getResource("/main-frame.fxml"))
        loader.controllerFactory = Callback { DicomViewerController(primaryStage) }
        val mainFrame = loader.load<Pane>()

        with(primaryStage) {
            minWidth = 900.0
            minHeight = 500.0

            icons.addAll(Image("/icons/icon16x16.png"), Image("/icons/icon32x32.png"))

            title = "Dicom Viewer"
            scene = Scene(mainFrame)
            show()
        }
    }

    companion object {
        fun launch(vararg args: String) {
            Application.launch(DicomViewer::class.java, *args)
        }
    }
}

fun main(args: Array<String>) {
    DicomViewer.launch(*args)
}