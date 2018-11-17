package cn.yiiguxing.dicom.graphics.text

/**
 * An enumerator which indicates where text should be positioned.
 */
enum class TextAlignment {
    /**
     * Text should be aligned to the top-left of the target bounds.
     */
    TOP_LEFT,

    /**
     * Text should be aligned to the top of the target bounds.
     */
    TOP,

    /**
     * Text should be aligned to the top-right of the target bounds.
     */
    TOP_RIGHT,

    /**
     * Text should be aligned to the left of the target bounds.
     */
    LEFT,

    /**
     * Text should be aligned to the center of the target bounds.
     */
    CENTER,

    /**
     * Text should be aligned to the right of the target bounds.
     */
    RIGHT,

    /**
     * Text should be aligned to the bottom-left of the target bounds.
     */
    BOTTOM_LEFT,

    /**
     * Text should be aligned to the bottom of the target bounds.
     */
    BOTTOM,

    /**
     * Text should be aligned to the bottom-right of the target bounds.
     */
    BOTTOM_RIGHT;

    /**
     * Gets whether the text alignment is centered horizontally.
     *
     * @return `true` if the text alignment is horizontally centered; otherwise, `false`
     */
    val isCenterHorizontal: Boolean
        get() = this == TOP || this == CENTER || this == BOTTOM

    /**
     * Gets whether the text alignment is centered vertically.
     *
     * @return `true` if the text alignment is vertically centered; otherwise, `false`
     */
    val isCenterVertical: Boolean
        get() = this == LEFT || this == CENTER || this == RIGHT

    /**
     * Gets whether the text alignment is aligned to the left.
     *
     * @return `true` if the text alignment is aligned to the left; otherwise, `false`
     */
    val isLeft: Boolean
        get() = this == TOP_LEFT || this == LEFT || this == BOTTOM_LEFT

    /**
     * Gets whether the text alignment is aligned to the right.
     *
     * @return `true` if the text alignment is aligned to the right; otherwise, `false`
     */
    val isRight: Boolean
        get() = this == TOP_RIGHT || this == RIGHT || this == BOTTOM_RIGHT

    /**
     * Gets whether the text alignment is aligned to the top.
     *
     * @return `true` if the text alignment is aligned to the top; otherwise, `false`
     */
    val isTop: Boolean
        get() = this == TOP || this == TOP_LEFT || this == TOP_RIGHT

    /**
     * Gets whether the text alignment is aligned to the bottom.
     *
     * @return `true` if the text alignment is aligned to the bottom; otherwise, `false`
     */
    val isBottom: Boolean
        get() = this == BOTTOM || this == BOTTOM_LEFT || this == BOTTOM_RIGHT
}
