package io.github.vanillafairy.docsetviewer.actions

import io.github.vanillafairy.docsetviewer.core.index.DocsetIndexService
import io.github.vanillafairy.docsetviewer.core.model.DocsetEntry
import io.github.vanillafairy.docsetviewer.editor.DocsetVirtualFile
import com.intellij.ide.actions.GotoActionBase
import com.intellij.ide.util.gotoByName.ChooseByNamePopup
import com.intellij.ide.util.gotoByName.ChooseByNameItemProvider
import com.intellij.ide.util.gotoByName.ChooseByNameModel
import com.intellij.ide.util.gotoByName.ChooseByNameViewModel
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.Processor
import javax.swing.ListCellRenderer

/**
 * Action to search across all docsets using a popup.
 */
class SearchDocsetAction : GotoActionBase(), DumbAware {

    override fun gotoActionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val model = DocsetSearchModel(project)

        showNavigationPopup(e, model, object : GotoActionCallback<Any>() {
            override fun elementChosen(popup: ChooseByNamePopup?, element: Any?) {
                if (element is DocsetEntryWrapper) {
                    openEntry(project, element)
                }
            }
        }, "Search Docsets", true)
    }

    private fun openEntry(project: Project, wrapper: DocsetEntryWrapper) {
        val indexService = DocsetIndexService.getInstance()
        val docset = indexService.getDocset(wrapper.entry.docsetIdentifier) ?: return
        val virtualFile = DocsetVirtualFile(wrapper.entry, docset)
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }
}

/**
 * Wrapper for DocsetEntry to implement NavigationItem.
 */
class DocsetEntryWrapper(val entry: DocsetEntry) : NavigationItem {

    override fun getName(): String = entry.name

    override fun getPresentation(): com.intellij.navigation.ItemPresentation {
        return object : com.intellij.navigation.ItemPresentation {
            override fun getPresentableText(): String = entry.name

            override fun getLocationString(): String = "${entry.type.displayName} in ${entry.docsetIdentifier}"

            override fun getIcon(unused: Boolean): javax.swing.Icon? = null
        }
    }

    override fun navigate(requestFocus: Boolean) {
        // Navigation handled by the action
    }

    override fun canNavigate(): Boolean = true

    override fun canNavigateToSource(): Boolean = true
}

/**
 * Model for the docset search popup.
 */
class DocsetSearchModel(private val project: Project) : ChooseByNameModel {

    override fun getPromptText(): String = "Search docsets:"

    override fun getNotInMessage(): String = "No matching entries"

    override fun getNotFoundMessage(): String = "No entries found"

    override fun getCheckBoxName(): String? = null

    override fun getHelpId(): String? = null

    override fun loadInitialCheckBoxState(): Boolean = false

    override fun saveInitialCheckBoxState(state: Boolean) {}

    override fun getSeparators(): Array<String> = emptyArray()

    override fun getFullName(element: Any): String? {
        return if (element is DocsetEntryWrapper) {
            "${element.entry.name} (${element.entry.docsetIdentifier})"
        } else null
    }

    override fun willOpenEditor(): Boolean = true

    override fun useMiddleMatching(): Boolean = true

    override fun getListCellRenderer(): ListCellRenderer<*> {
        return javax.swing.DefaultListCellRenderer()
    }

    override fun getNames(checkBoxState: Boolean): Array<String> {
        // Return empty array to avoid loading all entry names into memory.
        // Actual search is performed by DocsetItemProvider.filterElements()
        return emptyArray()
    }

    override fun getElementsByName(
        name: String,
        checkBoxState: Boolean,
        pattern: String
    ): Array<Any> {
        val indexService = DocsetIndexService.getInstance()
        return indexService.searchAll(pattern, 100)
            .map { DocsetEntryWrapper(it) }
            .toTypedArray()
    }

    override fun getElementName(element: Any): String? {
        return if (element is DocsetEntryWrapper) {
            element.entry.name
        } else null
    }
}

/**
 * Item provider for the docset search popup.
 */
class DocsetItemProvider : ChooseByNameItemProvider {

    override fun filterNames(
        base: ChooseByNameViewModel,
        names: Array<out String>,
        pattern: String
    ): List<String> {
        return names.filter { it.contains(pattern, ignoreCase = true) }
    }

    override fun filterElements(
        base: ChooseByNameViewModel,
        pattern: String,
        everywhere: Boolean,
        indicator: ProgressIndicator,
        consumer: Processor<in Any>
    ): Boolean {
        val indexService = DocsetIndexService.getInstance()
        val results = indexService.searchAll(pattern, 100)

        for (entry in results) {
            if (indicator.isCanceled) return false
            consumer.process(DocsetEntryWrapper(entry))
        }

        return true
    }
}
