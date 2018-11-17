package cn.yiiguxing.dicom.graphics.op

import java.awt.event.MouseEvent

interface Operation {
    var enabled: Boolean
    fun onMouseEvent(event: MouseEvent) {}
}