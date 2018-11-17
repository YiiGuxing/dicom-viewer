@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.dicom

import cn.yiiguxing.dicom.image.DicomImage
import cn.yiiguxing.dicom.image.DicomImageIO
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.stream.ImageInputStream

/**
 * DICOM image loader.
 *
 * @param input The input URI.
 */
abstract class DicomImageLoader internal constructor(private val input: Any) {

    /**
     * The state of a [DicomImageLoader].
     */
    enum class State { IDLE, LOADING, SUCCEEDED, CANCELLED, FAILED }

    private val _state = AtomicInteger(State.IDLE.ordinal)

    /**
     * Current loading state.
     */
    val state: State get() = STATES[_state.get()]

    constructor(input: InputStream) : this(input as Any)
    constructor(input: URI) : this(input as Any)
    constructor(input: File) : this(input as Any)

    private fun updateState(expect: State, update: State): Boolean {
        return _state.compareAndSet(expect.ordinal, update.ordinal)
    }

    /**
     * Loads a [DicomImage] by synchronize.
     *
     * @see onSucceeded
     * @see onFailed
     */
    fun load() {
        if (updateState(
                State.IDLE,
                State.LOADING
            )) {
            onLoading()
            loadImage()
        }
    }

    protected open fun loadImage() {
        val image = try {
            load(input)
        } catch (e: Throwable) {
            e.printStackTrace()
            if (updateState(
                    State.LOADING,
                    State.FAILED
                )) {
                onFailed()
            }
            return
        }

        if (updateState(
                State.LOADING,
                State.SUCCEEDED
            )) {
            onSucceeded(image)
        }
    }

    fun cancel() {
        if (updateState(
                State.LOADING,
                State.CANCELLED
            )) {
            onCancelled()
        }
    }

    /**
     * A protected convenience method for subclasses, called when the state of the [DicomImageLoader]
     * has transitioned to the [State.LOADING] state.
     */
    protected open fun onLoading() {
    }

    /**
     * A protected convenience method for subclasses, called when the state of the [DicomImageLoader]
     * has transitioned to the [State.CANCELLED] state.
     */
    protected open fun onCancelled() {
    }

    /**
     * A protected convenience method for subclasses, called when the state of the [DicomImageLoader]
     * has transitioned to the [State.SUCCEEDED] state.
     *
     * This method may be invoked on the background [Thread] after the [DicomImageLoader]
     * has been fully transitioned to the new state.
     */
    protected open fun onSucceeded(image: DicomImage) {
    }

    /**
     * A protected convenience method for subclasses, called when the state of the [DicomImageLoader]
     * has transitioned to the [State.FAILED] state.
     *
     * This method may be invoked on the background [Thread] after the [DicomImageLoader]
     * has been fully transitioned to the new state.
     */
    protected open fun onFailed() {
    }

    companion object {
        private val STATES = State.values()

        private fun load(input: Any, frameIndex: Int = 0): DicomImage {
            return when (input) {
                is ImageInputStream -> load(input, frameIndex)
                is InputStream -> load(input, frameIndex)
                is URI -> load(input, frameIndex)
                is File -> load(input, frameIndex)
                else -> throw IllegalArgumentException("Unsupported input type: ${input.javaClass.name}")
            }
        }

        /**
         * Loads and returns a [DicomImage].
         */
        fun load(input: ImageInputStream, frameIndex: Int = 0): DicomImage {
            return DicomImageIO().use { io ->
                io.setImageInputStream(input)
                io.read(frameIndex)
            }
        }

        /**
         * Loads and returns a [DicomImage].
         */
        fun load(input: InputStream, frameIndex: Int = 0): DicomImage {
            return DicomImageIO().use { io ->
                io.setInputStream(input)
                io.read(frameIndex)
            }
        }

        /**
         * Loads and returns a [DicomImage].
         */
        fun load(input: URI, frameIndex: Int = 0): DicomImage {
            return DicomImageIO().use { io ->
                io.setUri(input)
                io.read(frameIndex)
            }
        }

        /**
         * Loads and returns a [DicomImage].
         */
        fun load(input: File, frameIndex: Int = 0): DicomImage {
            return DicomImageIO().use { io ->
                io.setFile(input)
                io.read(frameIndex)
            }
        }
    }
}