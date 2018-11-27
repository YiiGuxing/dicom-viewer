package cn.yiiguxing.tool.dcmviewer.op

import cn.yiiguxing.tool.dcmviewer.control.skin.DicomViewSkin
import javafx.event.Event
import javafx.event.EventHandler
import javafx.scene.input.MouseEvent

/**
 * Operation
 *
 * Created by Yii.Guxing on 2018/11/24
 */
abstract class Operation<T : Event>(private val skin: DicomViewSkin) : EventHandler<T> {

    final override fun handle(event: T) = handle(skin, event)

    abstract fun handle(skin: DicomViewSkin, event: T)

}

abstract class MouseOperation(skin: DicomViewSkin) : Operation<MouseEvent>(skin)