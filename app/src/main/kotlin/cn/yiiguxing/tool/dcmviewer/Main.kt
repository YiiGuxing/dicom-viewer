/*
 * Main
 *
 * Created by Yii.Guxing on 2018/11/17
 */

package cn.yiiguxing.tool.dcmviewer

import cn.yiiguxing.dicom.image.DicomImageIO
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.effect.BoxBlur
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage


class App : Application() {
    override fun start(primaryStage: Stage) {
        val image = DicomImageIO().run {
            setUri(javaClass.getResource("/test.dcm").toURI())
            read(0)
        }

        val canvas = Canvas()
        val gc = canvas.graphicsContext2D

        // FIXME ANTI-ALIASING: Is there a better implementation?
        val blur = BoxBlur()
        blur.width = 1.0
        blur.height = 1.0
        blur.iterations = 1
        gc.setEffect(blur)

        gc.fill = Color.RED
        gc.fillRect(0.0, 0.0, 512.0, 512.0)
        canvas.width = 512.0
        canvas.height = 512.0

        //gc.rotate(30.0)
        image.updateImage(2023f, 70f)
        image.draw(gc)

        primaryStage.scene = Scene(StackPane(canvas), 512.0, 512.0)
        primaryStage.show()
    }
}


fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}