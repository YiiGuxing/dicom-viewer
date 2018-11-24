package cn.yiiguxing.tool.dcmviewer.op

import cn.yiiguxing.tool.dcmviewer.DicomViewController
import javafx.event.Event
import javafx.event.EventHandler
import javafx.scene.input.MouseEvent

/**
 * Operation
 *
 * Created by Yii.Guxing on 2018/11/24
 */
abstract class Operation<T : Event>(private val controller: DicomViewController) : EventHandler<T> {

    final override fun handle(event: T) = handle(controller, event)

    abstract fun handle(controller: DicomViewController, event: T)

}

abstract class MouseOperation(controller: DicomViewController) : Operation<MouseEvent>(controller)