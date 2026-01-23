package com.github.clion.docsetviewer.core

import java.nio.file.Files
import java.nio.file.Path

/**
 * Utility class for validating docset bundles.
 * Consolidates validation logic used across the codebase.
 */
object DocsetValidator {

    /**
     * Validates that a path points to a valid docset bundle.
     * A valid docset must have:
     * - Contents/Info.plist
     * - Contents/Resources/docSet.dsidx
     * - Contents/Resources/Documents directory
     *
     * @param docsetPath Path to the .docset directory
     * @return true if the path is a valid docset bundle
     */
    fun isValidDocset(docsetPath: Path): Boolean {
        if (!Files.exists(docsetPath) || !Files.isDirectory(docsetPath)) {
            return false
        }

        val infoPlistPath = docsetPath.resolve("Contents/Info.plist")
        val indexPath = docsetPath.resolve("Contents/Resources/docSet.dsidx")
        val documentsPath = docsetPath.resolve("Contents/Resources/Documents")

        return Files.exists(infoPlistPath) &&
                Files.exists(indexPath) &&
                Files.exists(documentsPath) &&
                Files.isDirectory(documentsPath)
    }

    /**
     * Validates that a path has the correct docset extension.
     *
     * @param path Path to check
     * @return true if the path ends with .docset
     */
    fun hasDocsetExtension(path: String): Boolean {
        return path.endsWith(".docset", ignoreCase = true)
    }

    /**
     * Validates that a path has the correct docset extension.
     *
     * @param path Path to check
     * @return true if the path ends with .docset
     */
    fun hasDocsetExtension(path: Path): Boolean {
        return hasDocsetExtension(path.toString())
    }

    /**
     * Performs basic validation that the required files exist.
     * This is a lighter check than isValidDocset that doesn't verify Documents directory.
     *
     * @param docsetPath Path to the .docset directory
     * @return true if Info.plist and docSet.dsidx exist
     */
    fun hasRequiredFiles(docsetPath: Path): Boolean {
        val infoPlistPath = docsetPath.resolve("Contents/Info.plist")
        val indexPath = docsetPath.resolve("Contents/Resources/docSet.dsidx")

        return Files.exists(infoPlistPath) && Files.exists(indexPath)
    }
}
