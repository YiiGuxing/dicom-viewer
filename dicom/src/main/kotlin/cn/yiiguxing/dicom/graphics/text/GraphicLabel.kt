package cn.yiiguxing.dicom.graphics.text

import cn.yiiguxing.dicom.graphics.Drawable
import cn.yiiguxing.dicom.graphics.drawWithTranslate
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextLayout

class GraphicLabel(
    private vararg var labels: String,
    var alignment: TextAlignment = TextAlignment.TOP_LEFT,
    var color: Color = Color.WHITE,
    var strokeColor: Color? = Color.BLACK
) : Drawable {

    private var lineHeight: Double = 0.0
    private val lineWidths: MutableList<Double> = ArrayList(labels.size)

    private var lastFont: Font? = null

    fun setLabels(vararg labels: String) {
        if (!this.labels.contentEquals(labels)) {
            this.labels = labels
            lastFont = null
        }
    }

    override fun draw(g2d: Graphics2D) {
        val font = g2d.font
        if (font != lastFont) {
            updateBounds(g2d)
            lastFont = font
        }

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.drawLabels()
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT)
    }


    private fun updateBounds(g2d: Graphics2D) {
        lineHeight = TextLayout("Tg", g2d.font, g2d.fontRenderContext).bounds.height + 2
        lineWidths.clear()
        for (label in labels) {
            if (label.isNotEmpty()) {
                val layout = TextLayout(label, g2d.font, g2d.fontRenderContext)
                lineWidths.add(layout.bounds.width)
            } else {
                lineWidths.add(0.0)
            }
        }
    }

    private fun Graphics2D.drawLabels() {
        val boundsHeight = lineHeight * lineWidths.size
        val dy = when {
            alignment.isCenterVertical -> boundsHeight * 0.5
            alignment.isBottom -> boundsHeight
            else -> 0.0
        }
        val offsetY = when {
            alignment.isTop -> 1.0
            alignment.isBottom -> -1.0
            else -> 0.0
        }
        translate(0.0, -dy + offsetY)

        var py = 0.0
        for (i in labels.indices) {
            py += lineHeight
            translate(0.0, lineHeight)

            val lineWidth = lineWidths[i]
            if (lineWidth <= 0.0) continue

            val dx = when {
                alignment.isCenterHorizontal -> lineWidth * 0.5
                alignment.isRight -> lineWidth
                else -> 0.0
            }
            val offsetX = when {
                alignment.isLeft -> 1.0
                alignment.isRight -> -1.0
                else -> 0.0
            }

            drawWithTranslate(dx = -dx + offsetX) {
                drawText(labels[i])
            }
        }
        translate(0.0, dy - offsetY - py)
    }

    private fun Graphics2D.drawText(text: String) {
        strokeColor?.let {
            color = it
            drawString(text, -1f, -1f)
            drawString(text, -1f, 0f)
            drawString(text, -1f, 1f)
            drawString(text, 0f, -1f)
            drawString(text, 0f, 1f)
            drawString(text, 1f, -1f)
            drawString(text, 1f, 0f)
            drawString(text, 1f, 1f)
        }

        color = this@GraphicLabel.color
        drawString(text, 0f, 0f)
    }
}
