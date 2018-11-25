/*
 * Main
 *
 * Created by Yii.Guxing on 2018/11/17
 */

package cn.yiiguxing.tool.dcmviewer

import cn.yiiguxing.tool.dcmviewer.image.DicomImageIO
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import java.io.File


class App : Application() {
    override fun start(primaryStage: Stage) {
        val image = DicomImageIO().run {
            setFile(File("data/test.dcm"))
            read(0)
        }

        val dicomView = DicomView()
        dicomView.dicomImage = image
        primaryStage.scene = Scene(dicomView, 512.0, 512.0)
        primaryStage.show()
    }
}


fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}