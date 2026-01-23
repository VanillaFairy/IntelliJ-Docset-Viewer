package io.github.vanillafairy.docsetviewer.core.parser

import io.github.vanillafairy.docsetviewer.core.model.DocsetEntry
import io.github.vanillafairy.docsetviewer.core.model.DocsetType
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

/**
 * Reads the docSet.dsidx SQLite database to load documentation entries.
 */
class SqliteIndexReader(private val databasePath: Path) : AutoCloseable {

    private var connection: Connection? = null

    init {
        if (!Files.exists(databasePath)) {
            throw SqliteIndexException("Database not found: $databasePath")
        }
    }

    /**
     * Opens a connection to the SQLite database.
     * Synchronized to prevent race conditions when multiple threads access the connection.
     */
    @Synchronized
    private fun getConnection(): Connection {
        val conn = connection
        if (conn == null || conn.isClosed) {
            Class.forName("org.sqlite.JDBC")
            val newConn = DriverManager.getConnection("jdbc:sqlite:$databasePath")
            connection = newConn
            return newConn
        }
        return conn
    }

    /**
     * Reads all entries from the docset database.
     * Supports both simple schema (searchIndex) and Dash schema (ZTOKEN tables).
     *
     * @param docsetIdentifier The identifier of the parent docset
     * @return List of all documentation entries
     */
    fun readAllEntries(docsetIdentifier: String): List<DocsetEntry> {
        // Try simple searchIndex schema first
        val simpleEntries = tryReadSimpleSchema(docsetIdentifier)
        if (simpleEntries.isNotEmpty()) {
            return simpleEntries
        }

        // Fall back to Dash ZTOKEN schema
        return tryReadDashSchema(docsetIdentifier)
    }

    /**
     * Tries to read entries from the simple searchIndex table.
     */
    private fun tryReadSimpleSchema(docsetIdentifier: String): List<DocsetEntry> {
        val entries = mutableListOf<DocsetEntry>()
        val sql = "SELECT id, name, type, path FROM searchIndex ORDER BY name"

        try {
            getConnection().createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    while (resultSet.next()) {
                        val id = resultSet.getLong("id")
                        val name = resultSet.getString("name")
                        val typeStr = resultSet.getString("type")
                        val path = resultSet.getString("path")

                        val entry = DocsetEntry.create(
                            id = id,
                            name = name,
                            type = DocsetType.fromString(typeStr),
                            path = path,
                            docsetIdentifier = docsetIdentifier
                        )
                        entries.add(entry)
                    }
                }
            }
        } catch (e: Exception) {
            // Table doesn't exist or other error - return empty list to try other schema
            return emptyList()
        }

        return entries
    }

    /**
     * Tries to read entries from the Dash ZTOKEN schema.
     * This schema uses ZTOKEN and ZTOKENMETAINFORMATION tables.
     */
    private fun tryReadDashSchema(docsetIdentifier: String): List<DocsetEntry> {
        val entries = mutableListOf<DocsetEntry>()

        // Dash schema query - joins ZTOKEN with ZTOKENMETAINFORMATION
        val sql = """
            SELECT
                t.Z_PK as id,
                t.ZTOKENNAME as name,
                ty.ZTYPENAME as type,
                f.ZPATH as path,
                m.ZANCHOR as anchor
            FROM ZTOKEN t
            LEFT JOIN ZTOKENMETAINFORMATION m ON t.ZMETAINFORMATION = m.Z_PK
            LEFT JOIN ZFILEPATH f ON m.ZFILE = f.Z_PK
            LEFT JOIN ZTOKENTYPE ty ON t.ZTOKENTYPE = ty.Z_PK
            ORDER BY t.ZTOKENNAME
        """.trimIndent()

        try {
            getConnection().createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    while (resultSet.next()) {
                        val id = resultSet.getLong("id")
                        val name = resultSet.getString("name") ?: continue
                        val typeStr = resultSet.getString("type") ?: "Unknown"
                        val path = resultSet.getString("path") ?: continue
                        val anchor = resultSet.getString("anchor")

                        val entry = DocsetEntry.create(
                            id = id,
                            name = name,
                            type = DocsetType.fromString(typeStr),
                            path = if (anchor != null) "$path#$anchor" else path,
                            docsetIdentifier = docsetIdentifier
                        )
                        entries.add(entry)
                    }
                }
            }
        } catch (e: Exception) {
            throw SqliteIndexException("Failed to read entries from both schemas: ${e.message}", e)
        }

        return entries
    }

    /**
     * Searches for entries matching the given query.
     * Supports both simple schema (searchIndex) and Dash schema (ZTOKEN tables).
     *
     * @param query The search query
     * @param docsetIdentifier The identifier of the parent docset
     * @param limit Maximum number of results to return
     * @return List of matching entries, sorted by relevance
     */
    fun searchEntries(
        query: String,
        docsetIdentifier: String,
        limit: Int = 100
    ): List<DocsetEntry> {
        if (query.isBlank()) {
            return readAllEntries(docsetIdentifier).take(limit)
        }

        // Try simple schema first
        val simpleResults = trySearchSimpleSchema(query, docsetIdentifier, limit)
        if (simpleResults.isNotEmpty()) {
            return simpleResults
        }

        // Fall back to Dash schema
        return trySearchDashSchema(query, docsetIdentifier, limit)
    }

    /**
     * Searches using the simple searchIndex schema.
     */
    private fun trySearchSimpleSchema(
        query: String,
        docsetIdentifier: String,
        limit: Int
    ): List<DocsetEntry> {
        val entries = mutableListOf<DocsetEntry>()
        val normalizedQuery = query.trim().lowercase()

        val sql = """
            SELECT id, name, type, path,
                CASE
                    WHEN LOWER(name) = ? THEN 1
                    WHEN LOWER(name) LIKE ? THEN 2
                    WHEN LOWER(name) LIKE ? THEN 3
                    ELSE 4
                END as relevance
            FROM searchIndex
            WHERE LOWER(name) LIKE ?
            ORDER BY relevance, name
            LIMIT ?
        """.trimIndent()

        try {
            getConnection().prepareStatement(sql).use { statement ->
                statement.setString(1, normalizedQuery)
                statement.setString(2, "$normalizedQuery%")
                statement.setString(3, "%$normalizedQuery%")
                statement.setString(4, "%$normalizedQuery%")
                statement.setInt(5, limit)

                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val id = resultSet.getLong("id")
                        val name = resultSet.getString("name")
                        val typeStr = resultSet.getString("type")
                        val path = resultSet.getString("path")

                        val entry = DocsetEntry.create(
                            id = id,
                            name = name,
                            type = DocsetType.fromString(typeStr),
                            path = path,
                            docsetIdentifier = docsetIdentifier
                        )
                        entries.add(entry)
                    }
                }
            }
        } catch (e: Exception) {
            // Table doesn't exist - return empty to try other schema
            return emptyList()
        }

        return entries
    }

    /**
     * Searches using the Dash ZTOKEN schema.
     */
    private fun trySearchDashSchema(
        query: String,
        docsetIdentifier: String,
        limit: Int
    ): List<DocsetEntry> {
        val entries = mutableListOf<DocsetEntry>()
        val normalizedQuery = query.trim().lowercase()

        val sql = """
            SELECT
                t.Z_PK as id,
                t.ZTOKENNAME as name,
                ty.ZTYPENAME as type,
                f.ZPATH as path,
                m.ZANCHOR as anchor,
                CASE
                    WHEN LOWER(t.ZTOKENNAME) = ? THEN 1
                    WHEN LOWER(t.ZTOKENNAME) LIKE ? THEN 2
                    WHEN LOWER(t.ZTOKENNAME) LIKE ? THEN 3
                    ELSE 4
                END as relevance
            FROM ZTOKEN t
            LEFT JOIN ZTOKENMETAINFORMATION m ON t.ZMETAINFORMATION = m.Z_PK
            LEFT JOIN ZFILEPATH f ON m.ZFILE = f.Z_PK
            LEFT JOIN ZTOKENTYPE ty ON t.ZTOKENTYPE = ty.Z_PK
            WHERE LOWER(t.ZTOKENNAME) LIKE ?
            ORDER BY relevance, t.ZTOKENNAME
            LIMIT ?
        """.trimIndent()

        try {
            getConnection().prepareStatement(sql).use { statement ->
                statement.setString(1, normalizedQuery)
                statement.setString(2, "$normalizedQuery%")
                statement.setString(3, "%$normalizedQuery%")
                statement.setString(4, "%$normalizedQuery%")
                statement.setInt(5, limit)

                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val id = resultSet.getLong("id")
                        val name = resultSet.getString("name") ?: continue
                        val typeStr = resultSet.getString("type") ?: "Unknown"
                        val path = resultSet.getString("path") ?: continue
                        val anchor = resultSet.getString("anchor")

                        val entry = DocsetEntry.create(
                            id = id,
                            name = name,
                            type = DocsetType.fromString(typeStr),
                            path = if (anchor != null) "$path#$anchor" else path,
                            docsetIdentifier = docsetIdentifier
                        )
                        entries.add(entry)
                    }
                }
            }
        } catch (e: Exception) {
            throw SqliteIndexException("Failed to search entries: ${e.message}", e)
        }

        return entries
    }

    /**
     * Gets the total count of entries in the database.
     */
    fun getEntryCount(): Int {
        val sql = "SELECT COUNT(*) FROM searchIndex"
        try {
            getConnection().createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.getInt(1)
                    }
                }
            }
        } catch (e: Exception) {
            throw SqliteIndexException("Failed to count entries: ${e.message}", e)
        }
        return 0
    }

    /**
     * Gets counts of entries grouped by type.
     */
    fun getEntryCounts(): Map<DocsetType, Int> {
        val counts = mutableMapOf<DocsetType, Int>()
        val sql = "SELECT type, COUNT(*) as count FROM searchIndex GROUP BY type"

        try {
            getConnection().createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    while (resultSet.next()) {
                        val typeStr = resultSet.getString("type")
                        val count = resultSet.getInt("count")
                        counts[DocsetType.fromString(typeStr)] = count
                    }
                }
            }
        } catch (e: Exception) {
            throw SqliteIndexException("Failed to get entry counts: ${e.message}", e)
        }

        return counts
    }

    @Synchronized
    override fun close() {
        try {
            connection?.close()
        } catch (e: Exception) {
            // Ignore close exceptions
        } finally {
            connection = null
        }
    }
}

/**
 * Exception thrown when reading SQLite index fails.
 */
class SqliteIndexException(message: String, cause: Throwable? = null) : Exception(message, cause)
