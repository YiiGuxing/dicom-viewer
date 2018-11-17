package cn.yiiguxing.plugin.dicom.editor

import cn.yiiguxing.dicom.graphics.DicomImageViewModel
import cn.yiiguxing.dicom.image.DicomImage
import com.intellij.openapi.application.ApplicationManager

internal object DicomImageLoaderFactory : DicomImageViewModel.ImageLoaderFactory {

    override fun create(vm: DicomImageViewModel, input: Any): DicomImageViewModel.ImageLoader {
        return ImageLoader(vm, input)
    }

    private class ImageLoader(vm: DicomImageViewModel, input: Any) : DicomImageViewModel.ImageLoader(vm, input) {
        private val application = ApplicationManager.getApplication()

        override fun loadImage() {
            application.executeOnPooledThread { super.loadImage() }
        }

        override fun onLoading() {
            application.invokeLater { super.onLoading() }
        }

        override fun onCancelled() {
            application.invokeLater { super.onCancelled() }
        }

        override fun onFailed() {
            application.invokeLater { super.onFailed() }
        }

        override fun onSucceeded(image: DicomImage) {
            application.invokeLater { super.onSucceeded(image) }
        }
    }
}