@file:Suppress("unused")

package cn.yiiguxing.dicom.graphics

import java.awt.Rectangle
import java.awt.geom.AffineTransform

interface Viewport {

    val x: Int
    val y: Int
    val width: Int
    val height: Int
    val bounds: Rectangle
    val windowWidth: Float
    val windowCenter: Float
    val invert: Boolean

    val transform: AffineTransform
    val rotationRadian: Double
    val isHorizontalFlip: Boolean
    val isVerticalFlip: Boolean
    var selectedRegion: Rectangle?

    val isActualSize: Boolean
    val canZoomOut: Boolean get() = transform.fixedScaleX > MIN_SCALE
    val canZoomIn: Boolean get() = transform.fixedScaleX < MAX_SCALE

    fun setColorWindow(
        windowWidth: Float = this.windowWidth,
        windowCenter: Float = this.windowCenter,
        invert: Boolean = this.invert
    )

    fun setWindowing(windowWidth: Float, windowCenter: Float) {
        setColorWindow(windowWidth, windowCenter)
    }

    fun setInverse(inverse: Boolean) {
        setColorWindow(invert = inverse)
    }

    fun translate(dx: Double, dy: Double)

    fun locate()

    fun scale(scaleX: Double, scaleY: Double)

    fun zoomOut() {
        if (canZoomOut) {
            scale(0.5, 0.5)
        }
    }

    fun zoomIn() {
        if (canZoomIn) {
            scale(2.0, 2.0)
        }
    }

    fun zoomToActualSize()

    fun rotate(radian: Double)

    fun clockwiseRotate() {
        rotate(Math.toRadians(90.0))
    }

    fun counterclockwiseRotate() {
        rotate(Math.toRadians(-90.0))
    }

    fun horizontalFlip()

    fun verticalFlip()

    fun reset()

    companion object {
        const val MIN_SCALE = 0.05
        const val MAX_SCALE = 20.0
    }
}