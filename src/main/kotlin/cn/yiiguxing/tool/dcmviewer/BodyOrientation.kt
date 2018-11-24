package cn.yiiguxing.tool.dcmviewer

import java.lang.Math.abs
import javax.vecmath.Vector3d

/**
 * 身体方位
 */
enum class BodyOrientation(val labelChar: Char) {

    /** 头 */
    HEAD('H') {
        override val opposite: BodyOrientation get() = FEET
    },
    /** 脚 */
    FEET('F') {
        override val opposite: BodyOrientation get() = HEAD
    },
    /** 左 */
    LEFT('L') {
        override val opposite: BodyOrientation get() = RIGHT
    },
    /** 右 */
    RIGHT('R') {
        override val opposite: BodyOrientation get() = LEFT
    },
    /** 前胸 */
    ANTERIOR('A') {
        override val opposite: BodyOrientation get() = POSTERIOR
    },
    /** 后背 */
    POSTERIOR('P') {
        override val opposite: BodyOrientation get() = ANTERIOR
    };

    abstract val opposite: BodyOrientation

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        private const val DEFAULT_DEVIATION = 0.0001

        fun valueOfLabelChar(labelChar: Char): BodyOrientation = when (labelChar) {
            HEAD.labelChar -> HEAD
            FEET.labelChar -> FEET
            LEFT.labelChar -> LEFT
            RIGHT.labelChar -> RIGHT
            ANTERIOR.labelChar -> ANTERIOR
            POSTERIOR.labelChar -> POSTERIOR
            else -> throw IllegalArgumentException("Unknown body orientation label: $labelChar")
        }

        fun valuesOfLabel(label: String): Array<out BodyOrientation> {
            val orientations = label.map(Companion::valueOfLabelChar).toHashSet().toTypedArray()
            require(isValidCombination(*orientations)) { "Invalid body orientation label: $label" }

            return orientations
        }

        fun isValidCombination(vararg orientations: BodyOrientation): Boolean {
            if (orientations.size > 2) {
                return false
            }

            for (i in 0 until orientations.size) {
                for (j in i + 1 until orientations.size) {
                    if (orientations[i].opposite == orientations[j]) {
                        return false
                    }
                }
            }

            return true
        }

        fun toLabel(vararg orientations: BodyOrientation): String {
            return orientations.joinToString(separator = "") { it.labelChar.toString() }
        }

        fun toLabel(orientations: Iterable<BodyOrientation>): String {
            return orientations.joinToString(separator = "") { it.labelChar.toString() }
        }

        fun getBodyOrientations(vector: Vector3d, deviation: Double = DEFAULT_DEVIATION): List<BodyOrientation> {
            return getBodyOrientations(vector.x, vector.y, vector.z, deviation)
        }

        fun getBodyOrientations(vector: DoubleArray, deviation: Double = DEFAULT_DEVIATION): List<BodyOrientation> {
            return getBodyOrientations(vector[0], vector[1], vector[2], deviation)
        }

        fun getBodyOrientations(
            vectorX: Double,
            vectorY: Double,
            vectorZ: Double,
            deviation: Double = DEFAULT_DEVIATION
        )
                : List<BodyOrientation> {
            val result = ArrayList<BodyOrientation>(2)
            val ox = if (vectorX < 0) RIGHT else LEFT
            val oy = if (vectorY < 0) ANTERIOR else POSTERIOR
            val oz = if (vectorZ < 0) FEET else HEAD

            val absX = abs(vectorX)
            val absY = abs(vectorY)
            val absZ = abs(vectorZ)

            if (absX >= deviation) {
                result += ox
            }
            if (absY >= deviation) {
                result += oy
            }
            if (absZ >= deviation) {
                result += oz
            }

            return result.sortedBy {
                when (it) {
                    HEAD, FEET -> -absZ
                    LEFT, RIGHT -> -absX
                    ANTERIOR, POSTERIOR -> -absY
                }
            }
        }

        fun computeNormalVector(vector: DoubleArray): Vector3d {
            val x = vector[1] * vector[5] - vector[2] * vector[4]
            val y = vector[2] * vector[3] - vector[0] * vector[5]
            val z = vector[0] * vector[4] - vector[1] * vector[3]
            return Vector3d(x, y, z).apply { normalize() }
        }

        fun rotate(vectorSrc: Vector3d, axis: Vector3d, radians: Double, vectorDst: Vector3d) {
            val x = vectorSrc.x
            val y = vectorSrc.y
            val z = vectorSrc.z
            vectorDst.x = (axis.x * (axis.x * x + axis.y * y + axis.z * z) * (1 - Math.cos(radians))
                    + x * Math.cos(radians) + (-axis.z * y + axis.y * z) * Math.sin(radians))
            vectorDst.y = (axis.y * (axis.x * x + axis.y * y + axis.z * z) * (1 - Math.cos(radians))
                    + y * Math.cos(radians) + (axis.z * x - axis.x * z) * Math.sin(radians))
            vectorDst.z = (axis.z * (axis.x * x + axis.y * y + axis.z * z) * (1 - Math.cos(radians))
                    + z * Math.cos(radians) + (-axis.y * x + axis.x * y) * Math.sin(radians))
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline val BodyOrientation.label: String
    get() = labelChar.toString()

@Suppress("NOTHING_TO_INLINE")
inline fun Array<out BodyOrientation>.toLabel(): String = BodyOrientation.toLabel(*this)

@Suppress("NOTHING_TO_INLINE")
inline fun Iterable<BodyOrientation>.toLabel(): String = BodyOrientation.toLabel(this)

@Suppress("NOTHING_TO_INLINE")
inline fun Iterable<BodyOrientation>.opposites(): List<BodyOrientation> = map { it.opposite }

@Suppress("NOTHING_TO_INLINE")
inline fun Array<out BodyOrientation>.opposites(): Array<out BodyOrientation> = Array(size) { this[it].opposite }