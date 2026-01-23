package io.github.vanillafairy.docsetviewer.actions

import io.github.vanillafairy.docsetviewer.core.model.DocsetEntry
import io.github.vanillafairy.docsetviewer.core.model.DocsetType

/**
 * Helper class containing testable logic for docset lookups.
 * Extracted from FindInDocsetsAction for unit testing.
 */
object DocsetLookupHelper {

    /**
     * Types that represent class-like entries (classes, structs, types, etc.)
     */
    private val CLASS_LIKE_TYPES = setOf(
        DocsetType.CLASS,
        DocsetType.STRUCT,
        DocsetType.TYPE,
        DocsetType.INTERFACE,
        DocsetType.PROTOCOL,
        DocsetType.ENUM,
        DocsetType.UNION,
        DocsetType.TYPEDEF,
        DocsetType.NAMESPACE,
        DocsetType.MODULE,
        DocsetType.PACKAGE,
        DocsetType.RECORD,
        DocsetType.OBJECT,
        DocsetType.TRAIT
    )

    /**
     * Result of selecting an entry from search results.
     */
    sealed class SelectionResult {
        /** An exact match was found - open directly */
        data class ExactMatch(val entry: DocsetEntry) : SelectionResult()

        /** Single result found (not exact match) - open directly */
        data class SingleResult(val entry: DocsetEntry) : SelectionResult()

        /** Multiple results without exact match - show popup */
        data class MultipleResults(val entries: List<DocsetEntry>) : SelectionResult()

        /** No results found */
        object NoResults : SelectionResult()
    }

    /**
     * Selects the appropriate entry from search results based on the token.
     *
     * Priority:
     * 1. Exact match (case-insensitive) - returns immediately
     * 2. Class-type entry matching the token (for class lookups) - returns immediately
     * 3. Single result - returns that result
     * 4. Multiple results - returns all for popup display
     * 5. Empty results - returns NoResults
     *
     * @param results The search results from DocsetIndexService
     * @param token The token being searched for
     * @return SelectionResult indicating how to proceed
     */
    fun selectEntry(results: List<DocsetEntry>, token: String): SelectionResult {
        if (results.isEmpty()) {
            return SelectionResult.NoResults
        }

        // Check for exact match (case-insensitive)
        val exactMatch = results.find { it.name.equals(token, ignoreCase = true) }
        if (exactMatch != null) {
            return SelectionResult.ExactMatch(exactMatch)
        }

        // If token looks like a class name (no :: or .), look for class-type entry
        if (!token.contains("::") && !token.contains(".")) {
            val classEntry = findClassEntry(results, token)
            if (classEntry != null) {
                return SelectionResult.ExactMatch(classEntry)
            }
        }

        // Single result - return it
        if (results.size == 1) {
            return SelectionResult.SingleResult(results.first())
        }

        // Multiple results without exact match
        return SelectionResult.MultipleResults(results)
    }

    /**
     * Finds a class-type entry that matches the given token.
     * Looks for entries where:
     * - Type is a class-like type (CLASS, STRUCT, TYPE, etc.)
     * - Name matches the token with common naming patterns
     *
     * @param results The search results
     * @param token The token to match (expected to be a type name)
     * @return The matching class entry, or null if not found
     */
    private fun findClassEntry(results: List<DocsetEntry>, token: String): DocsetEntry? {
        val tokenLower = token.lowercase()

        // Look for class-type entries matching common documentation naming patterns:
        // - Exact match: "MyClass" for token "MyClass"
        // - With suffix: "MyClass Class", "MyClass Type"
        // - With prefix: "class MyClass", "struct MyClass"
        // - Generic types: "MyClass<T>"
        // - Nested types: "Outer::MyClass"
        return results
            .filter { it.type in CLASS_LIKE_TYPES }
            .find { entry ->
                val nameLower = entry.name.lowercase()
                nameLower == tokenLower ||
                nameLower.startsWith("$tokenLower ") ||
                nameLower.startsWith("$tokenLower<") ||
                nameLower.endsWith(" $tokenLower") ||
                nameLower.contains(" $tokenLower ") ||
                nameLower.contains("::$tokenLower") && !nameLower.contains("::${tokenLower}::")
            }
    }

    /**
     * Extracts the token at the given position in the text.
     * Handles qualified names (e.g., Namespace::Class, Class::method) and operators (e.g., <<, >>).
     *
     * @param text The full text content
     * @param offset The caret position
     * @return The token at the position, or null if none found
     */
    fun extractTokenAt(text: String, offset: Int): String? {
        if (offset < 0 || offset > text.length || text.isEmpty()) {
            return null
        }

        // Adjust offset if at end of text
        val safeOffset = if (offset >= text.length) text.length - 1 else offset

        // If at a non-word character, try to find adjacent word or extract operator
        if (safeOffset < text.length && !isWordChar(text[safeOffset])) {
            // Check if we're on an operator character
            if (isOperatorChar(text[safeOffset])) {
                return extractOperatorAt(text, safeOffset)
            }
            // Check if we're right after a word
            if (safeOffset > 0 && isWordChar(text[safeOffset - 1])) {
                return extractTokenAt(text, safeOffset - 1)
            }
            return null
        }

        // Find word boundaries
        var start = safeOffset
        var end = safeOffset

        // Move start backwards to find word beginning
        while (start > 0 && isWordChar(text[start - 1])) {
            start--
        }

        // Move end forwards to find word end
        while (end < text.length && isWordChar(text[end])) {
            end++
        }

        if (start == end) {
            return null
        }

        return text.substring(start, end)
    }

    /**
     * Extracts an operator token at the given position.
     * Groups consecutive operator characters together (e.g., <<, >>, ->, ++).
     */
    private fun extractOperatorAt(text: String, offset: Int): String? {
        var start = offset
        var end = offset

        // Move start backwards to find operator beginning
        while (start > 0 && isOperatorChar(text[start - 1])) {
            start--
        }

        // Move end forwards to find operator end
        while (end < text.length && isOperatorChar(text[end])) {
            end++
        }

        if (start == end) {
            return null
        }

        return text.substring(start, end)
    }

    /**
     * Determines if a character is part of a word/token.
     * Includes letters, digits, underscore, hyphen, and colon (for qualified names).
     */
    fun isWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_' || c == '-' || c == ':'
    }

    /**
     * Determines if a character is part of an operator.
     * Includes common C/C++ operator characters for documentation lookups.
     */
    fun isOperatorChar(c: Char): Boolean {
        return c in "<>+*/%&|!=^~"
    }
}
