package cn.yiiguxing.tool.dcmviewer

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import javafx.util.Callback
import java.nio.file.Paths

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
            minWidth = 650.0
            minHeight = 500.0
            scene = Scene(mainFrame)
            show()
        }

        parameters.raw.firstOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                val controller: DicomViewerController = loader.getController()
                controller.open(Paths.get(it))
            }
    }
}

fun main(args: Array<String>) {
    Application.launch(DicomViewer::class.java, *args)
}