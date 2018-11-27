package cn.yiiguxing.tool.dcmviewer.layout

import javafx.scene.layout.HBox

class FakeFocusHBox : HBox() {
    override fun requestFocus() {
    }

    fun setFakeFocus(b: Boolean) {
        isFocused = b
    }
}