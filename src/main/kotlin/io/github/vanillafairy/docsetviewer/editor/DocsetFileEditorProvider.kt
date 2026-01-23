package io.github.vanillafairy.docsetviewer.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Provides FileEditor instances for DocsetVirtualFile.
 */
class DocsetFileEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file is DocsetVirtualFile
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        require(file is DocsetVirtualFile) { "Expected DocsetVirtualFile but got ${file.javaClass}" }
        return DocsetFileEditor(file)
    }

    override fun getEditorTypeId(): String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    companion object {
        const val EDITOR_TYPE_ID = "docset-viewer"
    }
}
