package com.github.clion.docsetviewer.core.model

import java.nio.file.Path

/**
 * Represents a loaded docset bundle.
 *
 * @property name The display name of the docset (CFBundleName)
 * @property identifier The unique identifier (CFBundleIdentifier)
 * @property path The file system path to the .docset bundle
 * @property platformFamily The platform family (DocSetPlatformFamily)
 * @property indexFilePath Optional default index file path (dashIndexFilePath)
 * @property isJavaScriptEnabled Whether JavaScript is enabled for this docset
 * @property entries The list of documentation entries in this docset
 */
data class Docset(
    val name: String,
    val identifier: String,
    val path: Path,
    val platformFamily: String? = null,
    val indexFilePath: String? = null,
    val isJavaScriptEnabled: Boolean = true,
    val entries: List<DocsetEntry> = emptyList()
) {
    /**
     * Path to the Documents directory containing HTML files.
     */
    val documentsPath: Path
        get() = path.resolve("Contents/Resources/Documents")

    /**
     * Path to the SQLite index file.
     */
    val indexDatabasePath: Path
        get() = path.resolve("Contents/Resources/docSet.dsidx")

    /**
     * Path to the Info.plist file.
     */
    val infoPlistPath: Path
        get() = path.resolve("Contents/Info.plist")

    /**
     * Returns the absolute path to a document given its relative path.
     */
    fun resolveDocumentPath(relativePath: String): Path {
        return documentsPath.resolve(relativePath)
    }

    /**
     * Returns entry count by type.
     */
    fun countByType(): Map<DocsetType, Int> {
        return entries.groupingBy { it.type }.eachCount()
    }

    /**
     * Returns the total number of entries.
     */
    val entryCount: Int
        get() = entries.size
}
