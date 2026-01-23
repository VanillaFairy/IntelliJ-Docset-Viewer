package com.github.clion.docsetviewer.editor

import com.github.clion.docsetviewer.core.model.Docset
import com.github.clion.docsetviewer.core.model.DocsetEntry
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.testFramework.LightVirtualFile
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files

/**
 * Custom VirtualFile implementation for docset documentation entries.
 * This allows opening docset entries in the IDE's editor system.
 */
class DocsetVirtualFile(
    val entry: DocsetEntry,
    val docset: Docset
) : VirtualFile() {

    private val absolutePath = docset.resolveDocumentPath(entry.path)

    override fun getName(): String = "${docset.name}: ${entry.name}"

    override fun getFileSystem(): VirtualFileSystem = DocsetFileSystem.getInstance()

    override fun getPath(): String = absolutePath.toString()

    override fun isWritable(): Boolean = false

    override fun isDirectory(): Boolean = false

    override fun isValid(): Boolean = Files.exists(absolutePath)

    override fun getParent(): VirtualFile? = null

    override fun getChildren(): Array<VirtualFile>? = null

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        throw UnsupportedOperationException("Docset files are read-only")
    }

    override fun contentsToByteArray(): ByteArray {
        return if (Files.exists(absolutePath)) {
            Files.readAllBytes(absolutePath)
        } else {
            ByteArray(0)
        }
    }

    override fun getTimeStamp(): Long {
        return try {
            Files.getLastModifiedTime(absolutePath).toMillis()
        } catch (e: Exception) {
            0L
        }
    }

    override fun getLength(): Long {
        return try {
            Files.size(absolutePath)
        } catch (e: Exception) {
            0L
        }
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        // No refresh needed for read-only docset files
    }

    override fun getInputStream(): InputStream {
        return Files.newInputStream(absolutePath)
    }

    override fun getModificationStamp(): Long = getTimeStamp()

    /**
     * Gets the full URL to the document including anchor.
     */
    fun getDocumentUrl(): String {
        val baseUrl = absolutePath.toUri().toString()
        return if (entry.anchor != null) {
            "$baseUrl#${entry.anchor}"
        } else {
            baseUrl
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocsetVirtualFile) return false
        return entry.id == other.entry.id && entry.docsetIdentifier == other.entry.docsetIdentifier
    }

    override fun hashCode(): Int {
        return 31 * entry.id.hashCode() + entry.docsetIdentifier.hashCode()
    }
}

/**
 * Virtual file system for docset files.
 */
class DocsetFileSystem : VirtualFileSystem() {

    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile? = null

    override fun refresh(asynchronous: Boolean) {}

    override fun refreshAndFindFileByPath(path: String): VirtualFile? = null

    override fun addVirtualFileListener(listener: com.intellij.openapi.vfs.VirtualFileListener) {}

    override fun removeVirtualFileListener(listener: com.intellij.openapi.vfs.VirtualFileListener) {}

    override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
        throw UnsupportedOperationException("Cannot delete docset files")
    }

    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
        throw UnsupportedOperationException("Cannot move docset files")
    }

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
        throw UnsupportedOperationException("Cannot rename docset files")
    }

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile {
        throw UnsupportedOperationException("Cannot create files in docset")
    }

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile {
        throw UnsupportedOperationException("Cannot create directories in docset")
    }

    override fun copyFile(
        requestor: Any?,
        virtualFile: VirtualFile,
        newParent: VirtualFile,
        copyName: String
    ): VirtualFile {
        throw UnsupportedOperationException("Cannot copy docset files")
    }

    override fun isReadOnly(): Boolean = true

    companion object {
        const val PROTOCOL = "docset"

        @JvmStatic
        fun getInstance(): DocsetFileSystem = INSTANCE

        private val INSTANCE = DocsetFileSystem()
    }
}
