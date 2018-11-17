@file:Suppress("unused")

package cn.yiiguxing.plugin.dicom

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.UserBinaryFileType
import com.intellij.openapi.vfs.VirtualFile
import icons.ImagesIcons
import javax.swing.Icon


class DicomFileTypeManager : FileTypeFactory() {

    fun getDicomFileType(): FileType = DicomFileType

    fun isDicomFile(file: VirtualFile): Boolean = file.fileType == DicomFileType

    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(DicomFileType, "dcm;dicom")
    }

    companion object {
        val INSTANCE: DicomFileTypeManager get() = ServiceManager.getService(DicomFileTypeManager::class.java)
    }

    object DicomFileType : UserBinaryFileType() {
        init {
            name = "Dicom"
            description = "DICOM Image"
        }

        override fun getIcon(): Icon = ImagesIcons.ImagesFileType
    }
}