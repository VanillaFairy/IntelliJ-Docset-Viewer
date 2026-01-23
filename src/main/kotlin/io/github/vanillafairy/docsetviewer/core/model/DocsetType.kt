package io.github.vanillafairy.docsetviewer.core.model

/**
 * Represents the type of a documentation entry in a docset.
 * Based on Dash/Zeal type conventions.
 */
enum class DocsetType(val displayName: String) {
    CLASS("Class"),
    METHOD("Method"),
    FUNCTION("Function"),
    PROPERTY("Property"),
    CONSTANT("Constant"),
    VARIABLE("Variable"),
    TYPE("Type"),
    STRUCT("Struct"),
    ENUM("Enum"),
    INTERFACE("Interface"),
    PROTOCOL("Protocol"),
    CATEGORY("Category"),
    EXTENSION("Extension"),
    MACRO("Macro"),
    TYPEDEF("Typedef"),
    UNION("Union"),
    FIELD("Field"),
    NAMESPACE("Namespace"),
    MODULE("Module"),
    PACKAGE("Package"),
    LIBRARY("Library"),
    FRAMEWORK("Framework"),
    SECTION("Section"),
    GUIDE("Guide"),
    SAMPLE("Sample"),
    FILE("File"),
    RESOURCE("Resource"),
    CONSTRUCTOR("Constructor"),
    DESTRUCTOR("Destructor"),
    OPERATOR("Operator"),
    EVENT("Event"),
    DELEGATE("Delegate"),
    EXCEPTION("Exception"),
    ERROR("Error"),
    ANNOTATION("Annotation"),
    ATTRIBUTE("Attribute"),
    ELEMENT("Element"),
    ENTRY("Entry"),
    PARAMETER("Parameter"),
    RECORD("Record"),
    OBJECT("Object"),
    PROVIDER("Provider"),
    SERVICE("Service"),
    TRAIT("Trait"),
    MIXIN("Mixin"),
    BINDING("Binding"),
    CALLBACK("Callback"),
    DEFINE("Define"),
    WORD("Word"),
    BUILTIN("Builtin"),
    STYLE("Style"),
    UNKNOWN("Unknown");

    companion object {
        private val nameMap: Map<String, DocsetType> = entries.associateBy { it.name.lowercase() }
        private val displayNameMap: Map<String, DocsetType> = entries.associateBy { it.displayName.lowercase() }

        fun fromString(value: String): DocsetType {
            val normalizedValue = value.lowercase().trim()
            return nameMap[normalizedValue]
                ?: displayNameMap[normalizedValue]
                ?: UNKNOWN
        }
    }
}
