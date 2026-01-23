# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IntelliJ Docset Viewer is an IntelliJ platform plugin for all JetBrains IDEs (IntelliJ IDEA, CLion, Rider, PyCharm, WebStorm, etc.) that enables viewing Dash/Zeal-compatible docsets directly in the IDE. It renders HTML documentation using JCEF (Java Chromium Embedded Framework) with automatic theme adaptation.

## Build Requirements

The project requires **JetBrains JDK 21** (or compatible JDK 21). The previous distribution was built with:
- JDK 21.0.9 (JetBrains s.r.o. 21.0.9+1-b1163.86)
- Gradle 8.10.2
- Kotlin 1.9.25
- IntelliJ Platform Gradle Plugin 2.2.1

## Build Commands

```bash
# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "io.github.vanillafairy.docsetviewer.core.parser.SqliteIndexReaderTest"

# Run a single test method
./gradlew test --tests "io.github.vanillafairy.docsetviewer.core.parser.SqliteIndexReaderTest.testSearchEntries"

# Run IDE with plugin for manual testing
./gradlew runIde

# Build plugin distribution (ZIP)
./gradlew buildPlugin
```

## Architecture

### Layered Architecture

```
┌─────────────────────────────────────────┐
│         UI & Presentation Layer         │
│  (toolwindow, editor, actions)          │
├─────────────────────────────────────────┤
│         Service Layer                   │
│  (DocsetIndexService, Settings)         │
├─────────────────────────────────────────┤
│         Core Business Logic             │
│  (parser, model, index)                 │
├─────────────────────────────────────────┤
│         External Dependencies           │
│  (SQLite JDBC, JCEF, IntelliJ APIs)     │
└─────────────────────────────────────────┘
```

### Core Layer (`core/`)

- **Model classes** (`model/`): `Docset`, `DocsetEntry`, `DocsetType` - immutable data classes representing docset structure
- **Parsers** (`parser/`):
  - `DocsetParser` - facade that coordinates parsing (Facade Pattern)
  - `InfoPlistParser` - parses `Contents/Info.plist` for docset metadata
  - `SqliteIndexReader` - reads `Contents/Resources/docSet.dsidx` SQLite database (implements AutoCloseable)
- **Validation** (`DocsetValidator`): Centralized utility for validating docset bundles
- **Index service** (`index/DocsetIndexService`): Application-level service managing loaded docsets and search operations (Observer Pattern for change listeners)

### Editor Layer (`editor/`)

- `DocsetVirtualFile` - virtual file wrapper for docset entries (read-only)
- `DocsetFileSystem` - virtual file system with "docset" protocol (Singleton)
- `DocsetFileEditor` - FileEditor implementation with toolbar (back/forward/reload)
- `DocsetFileEditorProvider` - factory for creating editors (DumbAware)
- `DocsetBrowserPanel` - JCEF browser wrapper with fallback UI if JCEF unavailable, auto-injects CSS to match IDE theme
- `NavigationHistory` - manages back/forward navigation state (bounded to 100 entries)

### Actions (`actions/`)

- `FindInDocsetsAction` - Ctrl+Shift+D: searches for token under caret, exact match opens directly
- `SearchDocsetAction` - Ctrl+Alt+D: global search popup using `ChooseByNamePopup`
- `OpenDocsetAction` - adds a docset from file chooser
- `DocsetStartupActivity` - loads configured docsets on IDE startup (background thread)
- `DocsetLookupHelper` - testable helper with `SelectionResult` sealed class for search results

### Settings (`settings/`)

- `DocsetSettings` - persists docset paths to `DocsetViewerSettings.xml` (PersistentStateComponent)
- `DocsetSettingsConfigurable` - Settings > Tools > Docset Viewer UI

### UI (`ui/toolwindow/`)

- `DocsetToolWindowFactory` / `DocsetToolWindowPanel` - "Docsets" tool window with search debouncing

## Key Data Flow

1. Docsets are loaded from configured paths via `DocsetIndexService.loadFromSettings()`
2. Parsing: `DocsetParser` -> `InfoPlistParser` + `SqliteIndexReader` -> `Docset` with `List<DocsetEntry>`
3. Search: `DocsetIndexService.searchAll(query)` filters entries with relevance ranking (exact → prefix → substring)
4. Selection: `DocsetLookupHelper.selectEntry()` returns `ExactMatch`, `SingleResult`, `MultipleResults`, or `NoResults`
5. Display: `DocsetEntry` -> `DocsetVirtualFile` -> `DocsetFileEditor` -> `DocsetBrowserPanel` (JCEF)

## Design Patterns Used

| Pattern | Usage |
|---------|-------|
| **Facade** | `DocsetParser` coordinates multiple parsers |
| **Factory** | `DocsetEntry.create()`, `DocsetFileEditorProvider` |
| **Observer** | `DocsetIndexService.DocsetChangeListener` |
| **Singleton** | `DocsetFileSystem.getInstance()` |
| **Sealed Class** | `DocsetLookupHelper.SelectionResult` for type-safe results |
| **Template Method** | `SearchDocsetAction` extends `GotoActionBase` |
| **AutoCloseable** | `SqliteIndexReader` for resource management |

## Thread Safety

All thread-sensitive components use proper synchronization:

| Component | Mechanism |
|-----------|-----------|
| `DocsetIndexService.docsets` | `ConcurrentHashMap` |
| `DocsetIndexService.listeners` | `CopyOnWriteArrayList` |
| `SqliteIndexReader.getConnection()` | `@Synchronized` |
| `DocsetSettings` methods | `@Synchronized` |
| `DocsetToolWindowPanel` | Search on pooled thread, UI updates via `SwingUtilities.invokeLater()` |

## Development Principles

### Threading Rules
- Use `ApplicationManager.getApplication().executeOnPooledThread()` for background work
- Marshal UI updates to EDT via `SwingUtilities.invokeLater()`
- Actions must specify `ActionUpdateThread.EDT` or `ActionUpdateThread.BGT`
- All state-modifying methods in services should be synchronized

### IntelliJ Platform Conventions
- Use `@Service(Service.Level.APP)` for application-level services
- Implement `DumbAware` for actions that work during indexing
- Use `PersistentStateComponent` for settings persistence
- Register disposables with `Disposer.register(parent, child)`

### Resource Management
- Implement `AutoCloseable` for database connections
- Use `.use{}` blocks for try-with-resources
- Dispose JCEF browsers properly
- Bound collections to prevent memory leaks (e.g., NavigationHistory max 100)

### Error Handling
- Use custom exception hierarchy (`DocsetParseException`, `InfoPlistParseException`, `SqliteIndexException`)
- Chain exceptions to preserve root cause
- Show user-friendly messages via `Messages.showWarningDialog()`

### SQL Security
- Always use parameterized queries (no string concatenation)
- Disable XXE in XML parsing

### Code Organization
- Extract testable logic to helper classes (e.g., `DocsetLookupHelper`)
- Centralize validation in utility classes (e.g., `DocsetValidator`)
- Use sealed classes for type-safe result handling
- Prefer immutable data classes

## Docset Structure

Standard Dash/Zeal docset bundle:
```
MyDocset.docset/
  Contents/
    Info.plist          # Metadata (name, identifier, platform)
    Resources/
      docSet.dsidx      # SQLite database with searchIndex table
      Documents/        # HTML documentation files
```

## Test Data

Test docsets are located in `src/test/resources/testdata/`:
- `Chai.docset` - JavaScript assertion library (Methods, Styles, Guides)
- `Bash.docset` - Bash shell (Variables, Builtins, Parameters, Guides)
- `Markdown.docset` - Markdown syntax (Guides)

## Test Coverage

- Unit tests: `DocsetLookupHelperTest`, `DocsetEntryTest`, `DocsetTypeTest`, `NavigationHistoryTest`
- Parser tests: `DocsetParserTest`, `InfoPlistParserTest`, `SqliteIndexReaderTest`
- Integration tests: `ExactMatchIntegrationTest`, `SearchIntegrationTest`, `DocsetLoadingIntegrationTest`
