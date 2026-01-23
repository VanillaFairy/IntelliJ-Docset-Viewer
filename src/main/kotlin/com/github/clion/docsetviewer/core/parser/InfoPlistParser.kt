package com.github.clion.docsetviewer.core.parser

import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses Info.plist files from docset bundles.
 */
class InfoPlistParser {

    /**
     * Parsed plist data.
     */
    data class PlistData(
        val bundleName: String,
        val bundleIdentifier: String,
        val platformFamily: String? = null,
        val indexFilePath: String? = null,
        val isJavaScriptEnabled: Boolean = true,
        val isDashDocset: Boolean = true
    )

    /**
     * Parses an Info.plist file at the given path.
     *
     * @param path Path to the Info.plist file
     * @return Parsed plist data
     * @throws InfoPlistParseException if parsing fails
     */
    fun parse(path: Path): PlistData {
        if (!Files.exists(path)) {
            throw InfoPlistParseException("Info.plist not found: $path")
        }

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)

            val builder = factory.newDocumentBuilder()
            val document = Files.newInputStream(path).use { builder.parse(it) }

            val dictNode = document.getElementsByTagName("dict").item(0)
                ?: throw InfoPlistParseException("No dict element found in plist")

            val entries = parseDictEntries(dictNode)

            val bundleName = entries["CFBundleName"] as? String
                ?: throw InfoPlistParseException("CFBundleName not found in Info.plist")

            val bundleIdentifier = entries["CFBundleIdentifier"] as? String
                ?: throw InfoPlistParseException("CFBundleIdentifier not found in Info.plist")

            return PlistData(
                bundleName = bundleName,
                bundleIdentifier = bundleIdentifier,
                platformFamily = entries["DocSetPlatformFamily"] as? String,
                indexFilePath = entries["dashIndexFilePath"] as? String,
                isJavaScriptEnabled = entries["isJavaScriptEnabled"] as? Boolean ?: true,
                isDashDocset = entries["isDashDocset"] as? Boolean ?: true
            )
        } catch (e: InfoPlistParseException) {
            throw e
        } catch (e: Exception) {
            throw InfoPlistParseException("Failed to parse Info.plist: ${e.message}", e)
        }
    }

    private fun parseDictEntries(dictNode: org.w3c.dom.Node): Map<String, Any> {
        val entries = mutableMapOf<String, Any>()
        val children = dictNode.childNodes

        var i = 0
        while (i < children.length) {
            val child = children.item(i)
            if (child.nodeName == "key") {
                val key = child.textContent.trim()
                // Find the next sibling that is an element (value)
                var j = i + 1
                while (j < children.length) {
                    val valueNode = children.item(j)
                    if (valueNode.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                        entries[key] = parseValue(valueNode)
                        i = j
                        break
                    }
                    j++
                }
            }
            i++
        }

        return entries
    }

    private fun parseValue(node: org.w3c.dom.Node): Any {
        return when (node.nodeName) {
            "string" -> node.textContent.trim()
            "integer" -> node.textContent.trim().toIntOrNull() ?: 0
            "real" -> node.textContent.trim().toDoubleOrNull() ?: 0.0
            "true" -> true
            "false" -> false
            "array" -> parseArray(node)
            "dict" -> parseDictEntries(node)
            else -> node.textContent.trim()
        }
    }

    private fun parseArray(node: org.w3c.dom.Node): List<Any> {
        val items = mutableListOf<Any>()
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                items.add(parseValue(child))
            }
        }
        return items
    }
}

/**
 * Exception thrown when parsing Info.plist fails.
 */
class InfoPlistParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
