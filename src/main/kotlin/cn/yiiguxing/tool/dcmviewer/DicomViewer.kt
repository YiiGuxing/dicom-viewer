package cn.yiiguxing.tool.dcmviewer

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage

/**
 * DicomViewer
 *
 * Created by Yii.Guxing on 2018/11/24
 */
class DicomViewer : Application() {
    override fun start(primaryStage: Stage) {
        val pane = FXMLLoader(javaClass.getResource("/main-frame.fxml")).load<Pane>()

        primaryStage.scene = Scene(pane)
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(DicomViewer::class.java, *args)
}