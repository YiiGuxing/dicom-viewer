package cn.yiiguxing.dicom.graphics

import java.awt.*
import java.awt.geom.AffineTransform


inline fun Graphics2D.drawWith(
    dx: Int = 0, dy: Int = 0,
    color: Color? = null,
    stroke: Stroke? = null,
    restore: Boolean = true,
    draw: () -> Unit
) {
    val oldColor = this.color
    val oldStroke = this.stroke

    color?.let { this.color = it }
    stroke?.let { this.stroke = it }

    if (dx != 0 || dy != 0) {
        translate(dx, dy)
    }

    draw()

    if (restore) {
        this.color = oldColor
        this.stroke = oldStroke
        if (dx != 0 || dy != 0) {
            translate(-dx, -dy)
        }
    }
}

inline fun Graphics2D.drawWithTranslate(dx: Int = 0, dy: Int = 0, restore: Boolean = true, draw: () -> Unit) {
    if (dx != 0 || dy != 0) {
        translate(dx, dy)
    }
    draw()
    if (restore && (dx != 0 || dy != 0)) {
        translate(-dx, -dy)
    }
}

inline fun Graphics2D.drawWithTranslate(dx: Double = 0.0, dy: Double = 0.0, restore: Boolean = true, draw: () -> Unit) {
    if (dx != 0.0 || dy != 0.0) {
        translate(dx, dy)
    }
    draw()
    if (restore && (dx != 0.0 || dy != 0.0)) {
        translate(-dx, -dy)
    }
}

inline fun Graphics2D.drawWithClip(clipShape: Shape?, restore: Boolean = true, draw: () -> Unit) {
    clipShape?.let { clip(it) }
    draw()
    if (restore && clipShape != null) {
        clip = null
    }
}

inline fun Graphics2D.drawWithColor(usingColor: Color, restore: Boolean = true, draw: () -> Unit) {
    val oldColor = color
    color = usingColor
    draw()
    if (restore) {
        color = oldColor
    }
}

inline fun Graphics2D.drawWithStroke(usingStroke: Stroke, restore: Boolean = true, draw: () -> Unit) {
    val oldStroke = stroke
    stroke = usingStroke
    draw()
    if (restore) {
        stroke = oldStroke
    }
}

inline fun Graphics2D.drawWithTransform(usingTransform: AffineTransform, restore: Boolean = true, draw: () -> Unit) {
    val savedTransform = transform
    transform = transform.apply { concatenate(usingTransform) }
    draw()
    if (restore) {
        transform = savedTransform
    }
}

inline fun Graphics2D.drawWithComposite(usingComposite: Composite, restore: Boolean = true, draw: () -> Unit) {
    val savedComposite = composite
    composite = usingComposite
    draw()
    if (restore) {
        composite = savedComposite
    }
}