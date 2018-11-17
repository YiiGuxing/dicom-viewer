package cn.yiiguxing.dicom.graphics

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.lang.ref.WeakReference

abstract class ViewModel : Drawable {

    var bounds = Rectangle()
        set(value) = setBounds(value.x, value.y, value.width, value.height)

    val x: Int get() = bounds.x
    val y: Int get() = bounds.y
    val width: Int get() = bounds.width
    val height: Int get() = bounds.height

    var background: Color? = Color.BLACK

    private var _callback: WeakReference<Callback>? = null

    var callback: Callback?
        get() = _callback?.get()
        set(value) {
            _callback = if (value == null) null else WeakReference(value)
        }

    fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        val bounds = bounds
        if (x != bounds.x || y != bounds.y || width != bounds.width || height != bounds.height) {
            val old = bounds.bounds
            bounds.setBounds(x, y, width, height)
            onBoundsChanged(old, bounds)
        }
    }

    fun copyBounds(bounds: Rectangle = Rectangle()): Rectangle {
        return bounds.also { it.bounds = this@ViewModel.bounds }
    }

    fun invalidateSelf() {
        invalidate(this)
    }

    protected fun invalidate(who: ViewModel) {
        _callback?.get()?.invalidateViewModel(who)
    }

    protected open fun onBoundsChanged(old: Rectangle, new: Rectangle) {
    }

    override fun draw(g2d: Graphics2D) {
        if (bounds.isEmpty) {
            return
        }

        g2d.color = background
        g2d.fillRect(0, 0, width, height)
        onDraw(g2d)
    }


    protected abstract fun onDraw(g2d: Graphics2D)

    interface Callback {
        fun invalidateViewModel(who: ViewModel)
    }
}