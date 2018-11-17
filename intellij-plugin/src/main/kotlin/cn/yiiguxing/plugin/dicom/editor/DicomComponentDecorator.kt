package cn.yiiguxing.plugin.dicom.editor

import cn.yiiguxing.dicom.graphics.Viewport
import cn.yiiguxing.dicom.graphics.op.Op
import com.intellij.openapi.actionSystem.DataKey
import kotlin.reflect.KProperty

interface DicomComponentDecorator {

    var op: Op

    val viewport: Viewport

    operator fun getValue(decorator: DicomComponentDecorator, property: KProperty<*>): Op {
        return op
    }

    operator fun setValue(decorator: DicomComponentDecorator, property: KProperty<*>, op: Op) {
        this.op = op
    }

    companion object {
        val DATA_KEY: DataKey<DicomComponentDecorator> = DataKey.create(DicomComponentDecorator::class.java.name)
    }
}