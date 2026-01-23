package com.github.clion.docsetviewer.core.index

import com.github.clion.docsetviewer.core.model.Docset
import com.github.clion.docsetviewer.core.model.DocsetEntry
import com.github.clion.docsetviewer.core.parser.DocsetParser
import com.github.clion.docsetviewer.settings.DocsetSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Application-level service for managing loaded docsets and searching.
 */
@Service(Service.Level.APP)
class DocsetIndexService {

    private val log = Logger.getInstance(DocsetIndexService::class.java)
    private val docsets = ConcurrentHashMap<String, Docset>()
    private val parser = DocsetParser()

    /**
     * Listeners for docset changes.
     */
    interface DocsetChangeListener {
        fun onDocsetLoaded(docset: Docset)
        fun onDocsetUnloaded(identifier: String)
    }

    private val listeners = CopyOnWriteArrayList<DocsetChangeListener>()

    fun addChangeListener(listener: DocsetChangeListener) {
        listeners.add(listener)
    }

    fun removeChangeListener(listener: DocsetChangeListener) {
        listeners.remove(listener)
    }

    /**
     * Loads a docset from the given path.
     *
     * @param path Path to the .docset bundle
     * @return The loaded docset, or null if loading failed
     */
    fun loadDocset(path: String): Docset? {
        return try {
            val docsetPath = Paths.get(path)
            val docset = parser.parse(docsetPath)
            docsets[docset.identifier] = docset
            log.info("Loaded docset: ${docset.name} (${docset.entryCount} entries)")
            listeners.forEach { it.onDocsetLoaded(docset) }
            docset
        } catch (e: Exception) {
            log.warn("Failed to load docset from $path: ${e.message}")
            null
        }
    }

    /**
     * Unloads a docset by identifier.
     *
     * @param identifier The docset identifier to unload
     * @return true if the docset was unloaded
     */
    fun unloadDocset(identifier: String): Boolean {
        val removed = docsets.remove(identifier)
        if (removed != null) {
            log.info("Unloaded docset: ${removed.name}")
            listeners.forEach { it.onDocsetUnloaded(identifier) }
            return true
        }
        return false
    }

    /**
     * Gets a docset by identifier.
     */
    fun getDocset(identifier: String): Docset? = docsets[identifier]

    /**
     * Gets all loaded docsets.
     */
    fun getAllDocsets(): List<Docset> = docsets.values.toList()

    /**
     * Checks if a docset is loaded.
     */
    fun isDocsetLoaded(identifier: String): Boolean = docsets.containsKey(identifier)

    /**
     * Searches across all loaded docsets.
     *
     * @param query The search query
     * @param limit Maximum results per docset
     * @return List of matching entries from all docsets
     */
    fun searchAll(query: String, limit: Int = 100): List<DocsetEntry> {
        if (query.isBlank()) {
            return docsets.values.flatMap { it.entries.take(limit) }
        }

        val normalizedQuery = query.lowercase().trim()
        return docsets.values.flatMap { docset ->
            docset.entries
                .filter { it.name.lowercase().contains(normalizedQuery) }
                .sortedWith(compareBy(
                    { !it.name.lowercase().equals(normalizedQuery) },
                    { !it.name.lowercase().startsWith(normalizedQuery) },
                    { it.name }
                ))
                .take(limit)
        }
    }

    /**
     * Searches within a specific docset.
     *
     * @param query The search query
     * @param docsetIdentifier The identifier of the docset to search
     * @param limit Maximum results
     * @return List of matching entries
     */
    fun searchInDocset(query: String, docsetIdentifier: String, limit: Int = 100): List<DocsetEntry> {
        val docset = docsets[docsetIdentifier] ?: return emptyList()

        if (query.isBlank()) {
            return docset.entries.take(limit)
        }

        val normalizedQuery = query.lowercase().trim()
        return docset.entries
            .filter { it.name.lowercase().contains(normalizedQuery) }
            .sortedWith(compareBy(
                { !it.name.lowercase().equals(normalizedQuery) },
                { !it.name.lowercase().startsWith(normalizedQuery) },
                { it.name }
            ))
            .take(limit)
    }

    /**
     * Loads all docsets configured in settings.
     */
    fun loadFromSettings() {
        val settings = DocsetSettings.getInstance()
        settings.getDocsetPaths().forEach { path ->
            if (!docsets.values.any { it.path.toString().replace('\\', '/') == path }) {
                loadDocset(path)
            }
        }
    }

    /**
     * Reloads all docsets from settings, clearing existing ones first.
     */
    fun reloadAll() {
        val identifiers = docsets.keys.toList()
        identifiers.forEach { unloadDocset(it) }
        loadFromSettings()
    }

    /**
     * Gets the total entry count across all loaded docsets.
     */
    fun getTotalEntryCount(): Int = docsets.values.sumOf { it.entryCount }

    companion object {
        @JvmStatic
        fun getInstance(): DocsetIndexService {
            return ApplicationManager.getApplication().getService(DocsetIndexService::class.java)
        }
    }
}
