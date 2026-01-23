package com.github.clion.docsetviewer.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistent settings for the Docset Viewer plugin.
 * Stores the list of configured docset paths.
 */
@Service(Service.Level.APP)
@State(
    name = "DocsetViewerSettings",
    storages = [Storage("DocsetViewerSettings.xml")]
)
class DocsetSettings : PersistentStateComponent<DocsetSettings.State> {

    data class State(
        var docsetPaths: MutableList<String> = mutableListOf()
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    /**
     * Gets the list of configured docset paths.
     * Returns a defensive copy to prevent external modification.
     */
    @Synchronized
    fun getDocsetPaths(): List<String> = state.docsetPaths.toList()

    /**
     * Adds a docset path to the configuration.
     * Synchronized to prevent race conditions.
     *
     * @param path The path to add
     * @return true if the path was added, false if it already existed
     */
    @Synchronized
    fun addDocsetPath(path: String): Boolean {
        val normalizedPath = normalizePath(path)
        if (!state.docsetPaths.contains(normalizedPath)) {
            state.docsetPaths.add(normalizedPath)
            return true
        }
        return false
    }

    /**
     * Removes a docset path from the configuration.
     * Synchronized to prevent race conditions.
     *
     * @param path The path to remove
     * @return true if the path was removed
     */
    @Synchronized
    fun removeDocsetPath(path: String): Boolean {
        val normalizedPath = normalizePath(path)
        return state.docsetPaths.remove(normalizedPath)
    }

    /**
     * Checks if a docset path is configured.
     */
    @Synchronized
    fun containsDocsetPath(path: String): Boolean {
        val normalizedPath = normalizePath(path)
        return state.docsetPaths.contains(normalizedPath)
    }

    /**
     * Clears all configured docset paths.
     */
    @Synchronized
    fun clearDocsetPaths() {
        state.docsetPaths.clear()
    }

    /**
     * Sets all docset paths at once.
     * Synchronized to prevent race conditions.
     */
    @Synchronized
    fun setDocsetPaths(paths: List<String>) {
        state.docsetPaths.clear()
        paths.map { normalizePath(it) }.distinct().forEach { state.docsetPaths.add(it) }
    }

    private fun normalizePath(path: String): String {
        return path.replace('\\', '/').trimEnd('/')
    }

    companion object {
        @JvmStatic
        fun getInstance(): DocsetSettings {
            return ApplicationManager.getApplication().getService(DocsetSettings::class.java)
        }
    }
}
