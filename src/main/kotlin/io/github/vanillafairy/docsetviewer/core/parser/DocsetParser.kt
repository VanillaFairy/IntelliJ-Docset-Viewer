package io.github.vanillafairy.docsetviewer.core.parser

import io.github.vanillafairy.docsetviewer.core.DocsetValidator
import io.github.vanillafairy.docsetviewer.core.model.Docset
import java.nio.file.Files
import java.nio.file.Path

/**
 * Facade for parsing docset bundles, combining plist and SQLite parsing.
 */
class DocsetParser {

    /**
     * Parses a docset bundle at the given path.
     *
     * @param docsetPath Path to the .docset directory
     * @return The parsed Docset
     * @throws DocsetParseException if parsing fails
     */
    fun parse(docsetPath: Path): Docset {
        if (!Files.exists(docsetPath)) {
            throw DocsetParseException("Docset path does not exist: $docsetPath")
        }

        if (!Files.isDirectory(docsetPath)) {
            throw DocsetParseException("Docset path is not a directory: $docsetPath")
        }

        val infoPlistPath = docsetPath.resolve("Contents/Info.plist")
        val indexPath = docsetPath.resolve("Contents/Resources/docSet.dsidx")

        // Parse Info.plist
        val plistParser = InfoPlistParser()
        val plistData = try {
            plistParser.parse(infoPlistPath)
        } catch (e: InfoPlistParseException) {
            throw DocsetParseException("Failed to parse Info.plist: ${e.message}", e)
        }

        // Read SQLite index
        val entries = if (Files.exists(indexPath)) {
            try {
                SqliteIndexReader(indexPath).use { reader ->
                    reader.readAllEntries(plistData.bundleIdentifier)
                }
            } catch (e: SqliteIndexException) {
                throw DocsetParseException("Failed to read docset index: ${e.message}", e)
            }
        } else {
            throw DocsetParseException("docSet.dsidx not found at: $indexPath")
        }

        return Docset(
            name = plistData.bundleName,
            identifier = plistData.bundleIdentifier,
            path = docsetPath,
            platformFamily = plistData.platformFamily,
            indexFilePath = plistData.indexFilePath,
            isJavaScriptEnabled = plistData.isJavaScriptEnabled,
            entries = entries
        )
    }

    /**
     * Validates that a path is a valid docset bundle.
     * Delegates to DocsetValidator for consistent validation across the codebase.
     *
     * @param docsetPath Path to validate
     * @return true if the path is a valid docset bundle
     */
    fun isValidDocset(docsetPath: Path): Boolean {
        return DocsetValidator.isValidDocset(docsetPath)
    }
}

/**
 * Exception thrown when parsing a docset fails.
 */
class DocsetParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
