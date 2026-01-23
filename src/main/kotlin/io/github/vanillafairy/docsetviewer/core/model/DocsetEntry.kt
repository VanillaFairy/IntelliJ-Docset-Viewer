package io.github.vanillafairy.docsetviewer.core.model

/**
 * Represents a single documentation entry within a docset.
 *
 * @property id The unique identifier from the SQLite database
 * @property name The display name of the entry
 * @property type The type of entry (Class, Method, Function, etc.)
 * @property path The relative path to the HTML documentation file
 * @property anchor Optional anchor within the HTML file
 * @property docsetIdentifier The identifier of the parent docset
 */
data class DocsetEntry(
    val id: Long,
    val name: String,
    val type: DocsetType,
    val path: String,
    val anchor: String? = null,
    val docsetIdentifier: String
) {
    /**
     * Returns the path including anchor if present.
     */
    val fullPath: String
        get() = if (anchor != null) "$path#$anchor" else path

    /**
     * Parses anchor from path if embedded (e.g., "path.html#anchor").
     * Also strips Dash metadata prefix if present.
     */
    companion object {
        // Regex to match Dash metadata tags: <dash_entry_name=...><dash_entry_originalName=...>...
        private val DASH_METADATA_REGEX = Regex("^(<dash_entry_[^>]+>)+")

        fun create(
            id: Long,
            name: String,
            type: DocsetType,
            path: String,
            docsetIdentifier: String
        ): DocsetEntry {
            // Strip Dash metadata prefix if present
            val cleanPath = stripDashMetadata(path)

            val anchorIndex = cleanPath.indexOf('#')
            return if (anchorIndex >= 0) {
                DocsetEntry(
                    id = id,
                    name = name,
                    type = type,
                    path = cleanPath.substring(0, anchorIndex),
                    anchor = cleanPath.substring(anchorIndex + 1),
                    docsetIdentifier = docsetIdentifier
                )
            } else {
                DocsetEntry(
                    id = id,
                    name = name,
                    type = type,
                    path = cleanPath,
                    anchor = null,
                    docsetIdentifier = docsetIdentifier
                )
            }
        }

        /**
         * Strips Dash metadata prefix from path.
         * Dash paths can have format: <dash_entry_name=...><dash_entry_originalName=...>actual/path.html
         */
        private fun stripDashMetadata(path: String): String {
            if (!path.startsWith("<")) {
                return path
            }
            // Find the last '>' and take everything after it
            val lastTagEnd = path.lastIndexOf('>')
            return if (lastTagEnd >= 0 && lastTagEnd < path.length - 1) {
                path.substring(lastTagEnd + 1)
            } else {
                // Fallback: try regex
                DASH_METADATA_REGEX.replace(path, "")
            }
        }
    }
}
