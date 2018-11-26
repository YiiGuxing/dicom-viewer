package cn.yiiguxing.tool.dcmviewer.util

import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.scene.transform.Affine
import javafx.scene.transform.Transform
import java.util.concurrent.Callable

/**
 * The scaling factor. Only for 2D transform, and scaleX=scaleY, and no shear transform.
 */
val Transform.scalingFactor: Double get() = Math.sqrt(mxx * mxx + mxy * mxy + mxz * mxz)

/**
 * Creates a new binding of [scaling factor][scalingFactor].
 */
fun Affine.createScalingFactorBinding(): DoubleBinding {
    return Bindings.createDoubleBinding(Callable { scalingFactor }, mxxProperty(), mxyProperty(), mxzProperty())
}