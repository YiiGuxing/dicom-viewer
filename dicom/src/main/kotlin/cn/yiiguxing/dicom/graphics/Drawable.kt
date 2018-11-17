@file:Suppress("NOTHING_TO_INLINE")

package cn.yiiguxing.dicom.graphics

import java.awt.Graphics2D
import java.awt.geom.AffineTransform

/**
 * A [Drawable] is an interface for "something that can be drawn."
 */
interface Drawable {
    /**
     * Draws this [Drawable] using the settings of the [Graphics2D] context.
     */
    fun draw(g2d: Graphics2D)
}

/**
 * Draws the specified [drawable].
 */
inline fun Graphics2D.draw(drawable: Drawable) {
    drawable.draw(this)
}

/**
 * Draws the specified [drawable] at the specified position ([x], [y]) in the User Space.
 */
inline fun Graphics2D.draw(drawable: Drawable, x: Int, y: Int) {
    translate(x, y)
    drawable.draw(this)
    translate(-x, -y)
}

/**
 * Draws the specified [drawable] at the specified position ([x], [y]) in the User Space.
 */
inline fun Graphics2D.draw(drawable: Drawable, x: Double, y: Double) {
    translate(x, y)
    drawable.draw(this)
    translate(-x, -y)
}

/**
 * Draws the specified [drawable], applying the specified [transform] from drawable space
 * into user space before drawing.
 */
inline fun Graphics2D.draw(drawable: Drawable, transform: AffineTransform) {
    drawWithTransform(transform) {
        drawable.draw(this)
    }
}
