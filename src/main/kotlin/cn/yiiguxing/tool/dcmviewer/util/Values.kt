/*
 * Properties
 *
 * Created by Yii.Guxing on 2018/11/20
 */

package cn.yiiguxing.tool.dcmviewer.util

import javafx.beans.value.*
import kotlin.reflect.KProperty


operator fun ObservableBooleanValue.getValue(obj: Any?, property: KProperty<*>): Boolean {
    return get()
}

operator fun WritableBooleanValue.setValue(obj: Any?, property: KProperty<*>, value: Boolean) {
    set(value)
}

operator fun ObservableIntegerValue.getValue(obj: Any?, property: KProperty<*>): Int {
    return get()
}

operator fun WritableIntegerValue.setValue(obj: Any?, property: KProperty<*>, value: Int) {
    set(value)
}

operator fun ObservableLongValue.getValue(obj: Any?, property: KProperty<*>): Long {
    return get()
}

operator fun WritableLongValue.setValue(obj: Any?, property: KProperty<*>, value: Long) {
    set(value)
}

operator fun ObservableFloatValue.getValue(obj: Any?, property: KProperty<*>): Float {
    return get()
}

operator fun WritableFloatValue.setValue(obj: Any?, property: KProperty<*>, value: Float) {
    set(value)
}

operator fun ObservableDoubleValue.getValue(obj: Any?, property: KProperty<*>): Double {
    return get()
}

operator fun WritableDoubleValue.setValue(obj: Any?, property: KProperty<*>, value: Double) {
    set(value)
}

operator fun ObservableStringValue.getValue(obj: Any?, property: KProperty<*>): String {
    return value
}

operator fun WritableStringValue.setValue(obj: Any?, property: KProperty<*>, value: String) {
    this.value = value
}

operator fun <T> WritableValue<T>.setValue(obj: Any?, property: KProperty<*>, value: T) {
    this.value = value
}

operator fun <T> ObservableValue<T>.getValue(obj: Any?, property: KProperty<*>): T {
    return value
}