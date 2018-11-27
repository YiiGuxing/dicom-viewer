package cn.yiiguxing.tool.dcmviewer

import javafx.scene.layout.HBox

class FakeFocusHBox : HBox() {
    override fun requestFocus() {
    }

    fun setFakeFocus(b: Boolean) {
        isFocused = b
    }
}