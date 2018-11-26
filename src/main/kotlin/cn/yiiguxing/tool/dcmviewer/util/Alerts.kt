@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.tool.dcmviewer.util

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.Window

object Alerts {

    private val DEFAULT_BUTTONS = arrayOf(ButtonType.CLOSE)
    private val DEFAULT_CONFIRM_BUTTONS = arrayOf(ButtonType.OK)

    private val DEFAULT_RESULT_CONVERTER: ((ButtonType) -> Unit) = { }

    fun <R> alert(
        type: Alert.AlertType = Alert.AlertType.NONE,
        headerText: String? = null,
        content: String? = null,
        window: Window? = null,
        icon: Image? = null,
        vararg buttons: ButtonType = DEFAULT_BUTTONS,
        resultConverter: ((ButtonType) -> R)? = null
    ): R? {
        val alert = Alert(type, content, *buttons).apply {
            window?.let { initOwner(window) }
            icon?.let { (dialogPane.scene.window as Stage).icons.add(it) }
            this.headerText = headerText
        }

        return if (resultConverter != null) {
            alert.showAndWait()
                .map { resultConverter(it) }
                .orElse(null)
        } else {
            alert.show()
            null
        }
    }

    fun alert(
        type: Alert.AlertType = Alert.AlertType.NONE,
        headerText: String? = null,
        content: String? = null,
        window: Window? = null,
        icon: Image? = null,
        wait: Boolean = false,
        vararg buttons: ButtonType = DEFAULT_BUTTONS
    ) {
        alert(
            type, headerText, content, window, icon, buttons = *buttons,
            resultConverter = if (wait) DEFAULT_RESULT_CONVERTER else null
        )
    }

    fun <R> info(
        headerText: String? = null,
        content: String? = null,
        window: Window? = null,
        icon: Image? = null,
        vararg buttons: ButtonType = DEFAULT_BUTTONS,
        resultConverter: ((ButtonType) -> R)? = null
    ): R? {
        return alert(
            Alert.AlertType.INFORMATION, headerText, content, window, icon,
            buttons = *buttons, resultConverter = resultConverter
        )
    }

    fun info(
        headerText: String? = null,
        content: String? = null,
        window: Window? = null,
        icon: Image? = null,
        wait: Boolean = false,
        vararg buttons: ButtonType = DEFAULT_BUTTONS
    ) {
        info<Any>(
            headerText, content, window, icon, buttons = *buttons,
            resultConverter = if (wait) DEFAULT_RESULT_CONVERTER else null
        )
    }

    fun <R> warn(
        headerText: String? = null,
        content: String? = null,
        window: Window? = null,
        icon: Image? = null,
        vararg buttons: ButtonType = DEFAULT_BUTTONS,
        resultConverter: ((ButtonType) -> R)? = null
    ): R? {
        return alert(
            Alert.AlertType.WARNING, headerText, content, window, icon,
            buttons = *buttons, resultConverter = resultConverter
        )
    }

    fun warn(
        headerText: String? = null,
        content: String? = null,
        window: Window? = null,
        icon: Image? = null,
        wait: Boolean = false,
        vararg buttons: ButtonType = DEFAULT_BUTTONS
    ) {
        warn<Any>(
            headerText, content, window, icon, buttons = *buttons,
            resultConverter = if (wait) DEFAULT_RESULT_CONVERTER else null
        )
    }


    fun <R> confirm(
        headerText: String? = null,
        content: String? = null,
        window: Window? = null,
        icon: Image? = null,
        vararg buttons: ButtonType = DEFAULT_CONFIRM_BUTTONS,
        resultConverter: ((ButtonType) -> R)? = null
    ): R? {
        return alert(
            Alert.AlertType.CONFIRMATION, headerText, content, window, icon,
            buttons = *buttons, resultConverter = resultConverter
        )
    }

    fun confirm(
        headerText: String? = null,
        content: String? = null,
        window: Window? = null,
        icon: Image? = null,
        wait: Boolean = false,
        vararg buttons: ButtonType = DEFAULT_CONFIRM_BUTTONS
    ) {
        confirm<Any>(
            headerText, content, window, icon, buttons = *buttons,
            resultConverter = if (wait) DEFAULT_RESULT_CONVERTER else null
        )
    }

    fun <R> error(
        headerText: String? = null,
        content: String? = null,
        window: Window? = null,
        icon: Image? = null,
        vararg buttons: ButtonType = DEFAULT_BUTTONS,
        resultConverter: ((ButtonType) -> R)? = null
    ): R? {
        return alert(
            Alert.AlertType.ERROR, headerText, content, window, icon,
            buttons = *buttons, resultConverter = resultConverter
        )
    }

    fun error(
        headerText: String? = null,
        content: String? = null,
        window: Window? = null,
        icon: Image? = null,
        wait: Boolean = false,
        vararg buttons: ButtonType = DEFAULT_BUTTONS
    ) {
        error<Any>(
            headerText, content, window, icon, buttons = *buttons,
            resultConverter = if (wait) DEFAULT_RESULT_CONVERTER else null
        )
    }

}

