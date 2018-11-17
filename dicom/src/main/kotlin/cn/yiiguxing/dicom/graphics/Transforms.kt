package cn.yiiguxing.dicom.graphics

import java.awt.geom.AffineTransform

/***
 * 旋转弧度
 */
val AffineTransform.rotation: Double
    get() = Math.atan2(shearY, scaleX)

/***
 * 旋转角度
 */
val AffineTransform.rotateAngle: Double
    get() = Math.toDegrees(rotation)

/**
 * X轴的缩放值
 */
val AffineTransform.fixedScaleX: Double
    get() {
        val rotation = rotation
        return if (scaleX == 0.0) shearY / Math.sin(rotation) else scaleX / Math.cos(rotation)
    }

/**
 * Y轴的缩放值
 */
val AffineTransform.fixedScaleY: Double
    get() {
        val rotation = rotation
        return if (scaleY == 0.0) -shearX / Math.sin(rotation) else scaleY / Math.cos(rotation)
    }