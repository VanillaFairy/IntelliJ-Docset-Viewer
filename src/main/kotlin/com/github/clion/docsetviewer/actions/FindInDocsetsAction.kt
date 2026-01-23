package com.github.clion.docsetviewer.actions

import com.github.clion.docsetviewer.core.index.DocsetIndexService
import com.github.clion.docsetviewer.core.model.DocsetEntry
import com.github.clion.docsetviewer.editor.DocsetVirtualFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import javax.swing.Icon

/**
 * Action to search for the token under the caret in docsets.
 * Available via keyboard shortcut and context menu.
 */
class FindInDocsetsAction : AnAction(
    "Find in Docsets",
    "Search for the current token in documentation",
    null
), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val token = getTokenUnderCaret(editor)
        if (token.isNullOrBlank()) {
            return
        }

        val indexService = DocsetIndexService.getInstance()
        var results = indexService.searchAll(token, 50)

        // If no results and token contains ::, also try the unqualified name
        // e.g., "std::vector" -> also search for "vector"
        if (results.isEmpty() && token.contains("::")) {
            val unqualifiedName = token.substringAfterLast("::")
            if (unqualifiedName.isNotBlank()) {
                results = indexService.searchAll(unqualifiedName, 50)
            }
        }

        when (val selection = DocsetLookupHelper.selectEntry(results, token)) {
            is DocsetLookupHelper.SelectionResult.NoResults -> {
                showNoResultsPopup(project, token)
            }
            is DocsetLookupHelper.SelectionResult.ExactMatch -> {
                openEntry(project, selection.entry)
            }
            is DocsetLookupHelper.SelectionResult.SingleResult -> {
                openEntry(project, selection.entry)
            }
            is DocsetLookupHelper.SelectionResult.MultipleResults -> {
                showResultsPopup(project, token, selection.entries)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasToken = editor != null && getTokenUnderCaret(editor)?.isNotBlank() == true
        val hasDocsets = DocsetIndexService.getInstance().getAllDocsets().isNotEmpty()

        e.presentation.isEnabled = hasToken && hasDocsets
        e.presentation.isVisible = true
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun getTokenUnderCaret(editor: Editor): String? {
        // First check if there's a selection
        if (editor.selectionModel.hasSelection()) {
            return editor.selectionModel.selectedText
        }

        val document = editor.document
        val offset = editor.caretModel.offset

        if (offset >= document.textLength) {
            return null
        }

        return DocsetLookupHelper.extractTokenAt(document.text, offset)
    }

    private fun showNoResultsPopup(project: Project, token: String) {
        JBPopupFactory.getInstance()
            .createMessage("No documentation found for '$token'")
            .showCenteredInCurrentWindow(project)
    }

    private fun showResultsPopup(project: Project, token: String, results: List<DocsetEntry>) {
        val step = object : BaseListPopupStep<DocsetEntry>("Documentation for '$token'", results) {
            override fun getTextFor(value: DocsetEntry): String {
                return "${value.name} (${value.type.displayName}) - ${value.docsetIdentifier}"
            }

            override fun getIconFor(value: DocsetEntry): Icon? = null

            override fun onChosen(selectedValue: DocsetEntry, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice) {
                    openEntry(project, selectedValue)
                }
                return FINAL_CHOICE
            }

            override fun isSpeedSearchEnabled(): Boolean = true
        }

        JBPopupFactory.getInstance()
            .createListPopup(step)
            .showCenteredInCurrentWindow(project)
    }

    private fun openEntry(project: Project, entry: DocsetEntry) {
        val indexService = DocsetIndexService.getInstance()
        val docset = indexService.getDocset(entry.docsetIdentifier) ?: return
        val virtualFile = DocsetVirtualFile(entry, docset)
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }
}
