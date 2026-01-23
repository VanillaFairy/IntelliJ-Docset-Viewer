package io.github.vanillafairy.docsetviewer.actions

import io.github.vanillafairy.docsetviewer.core.index.DocsetIndexService
import io.github.vanillafairy.docsetviewer.core.model.DocsetEntry
import io.github.vanillafairy.docsetviewer.editor.DocsetVirtualFile
import io.github.vanillafairy.docsetviewer.settings.DocsetSettings
import io.github.vanillafairy.docsetviewer.ui.toolwindow.DocsetToolWindowPanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
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

        // Try multiple ways to get the token - needed for Rider compatibility
        val token = getToken(e, project)
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
        val project = e.project
        val hasToken = project != null && getToken(e, project)?.isNotBlank() == true
        val hasDocsets = DocsetIndexService.getInstance().getAllDocsets().isNotEmpty()

        e.presentation.isEnabled = hasToken && hasDocsets
        e.presentation.isVisible = true
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Gets the token to search for using multiple fallback strategies.
     * This is needed for compatibility with Rider and other JetBrains IDEs
     * that may provide editor/caret data differently.
     */
    private fun getToken(e: AnActionEvent, project: Project): String? {
        // Strategy 1: Try CommonDataKeys.EDITOR (works in most IDEs)
        e.getData(CommonDataKeys.EDITOR)?.let { editor ->
            getTokenFromEditor(editor)?.let { return it }
        }

        // Strategy 2: Try PlatformDataKeys.EDITOR (alternative key)
        e.getData(PlatformDataKeys.EDITOR)?.let { editor ->
            getTokenFromEditor(editor)?.let { return it }
        }

        // Strategy 3: Try to get editor from FileEditorManager (works in Rider)
        val fileEditorManager = FileEditorManager.getInstance(project)
        val selectedEditor = fileEditorManager.selectedTextEditor
        if (selectedEditor != null) {
            getTokenFromEditor(selectedEditor)?.let { return it }
        }

        // Strategy 4: Try to get from the selected file editor (another Rider fallback)
        fileEditorManager.selectedEditors.filterIsInstance<TextEditor>().firstOrNull()?.let { textEditor ->
            getTokenFromEditor(textEditor.editor)?.let { return it }
        }

        // Strategy 5: Try to get token from PSI element (works when caret is on identifier)
        e.getData(CommonDataKeys.PSI_ELEMENT)?.let { element ->
            getTokenFromPsiElement(element)?.let { return it }
        }

        return null
    }

    private fun getTokenFromEditor(editor: Editor): String? {
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

    private fun getTokenFromPsiElement(element: PsiElement): String? {
        // Get the text of the PSI element (usually an identifier)
        val text = element.text
        if (text.isNullOrBlank()) {
            return null
        }
        // Clean up the text - remove quotes, parentheses, etc.
        return text.trim().removeSurrounding("\"").removeSurrounding("'")
            .takeIf { it.isNotBlank() && DocsetLookupHelper.isWordChar(it.first()) }
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
        val settings = DocsetSettings.getInstance()

        if (settings.isOpenInPanel()) {
            // Open in the Docsets tool window panel
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Docsets") ?: return
            toolWindow.show {
                DocsetToolWindowPanel.getInstance(project)?.openEntry(entry)
            }
        } else {
            // Open in a new editor tab
            val indexService = DocsetIndexService.getInstance()
            val docset = indexService.getDocset(entry.docsetIdentifier) ?: return
            val virtualFile = DocsetVirtualFile(entry, docset)
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }
}
