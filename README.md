# IntelliJ Docset Viewer

A JetBrains IDE plugin for viewing Dash/Zeal-compatible documentation docsets directly in your IDE.

## Disclaimer

This plugin is **fully AI-written** using Claude Code. No guarantees are provided regarding functionality, stability, or fitness for any particular purpose. Use at your own risk.

## Features

- Open `.docset` bundles and browse documentation
- Search across all loaded docsets
- Find documentation for the token under caret (F1 or Ctrl+Shift+D)
- Import docsets from Zeal
- Render HTML documentation using JCEF
- Navigate with back/forward history
- **Automatic theme support** - documentation adapts to your IDE's light/dark theme

## Compatibility

This plugin is compatible with all JetBrains IDEs:
- IntelliJ IDEA
- CLion
- Rider
- PyCharm
- WebStorm
- GoLand
- RubyMine
- PhpStorm
- And others based on the IntelliJ Platform

Compatible with docsets from [Dash](https://kapeli.com/dash) and [Zeal](https://zealdocs.org).

## Installation

### From ZIP

1. Download the latest release (`IntelliJ-docset-viewer-x.x.x.zip`)
2. In your IDE, go to **Settings > Plugins > Gear icon > Install Plugin from Disk...**
3. Select the downloaded ZIP file
4. Restart the IDE

### Building from Source

#### Requirements

- **JDK 21** (JetBrains Runtime recommended, bundled with JetBrains IDEs at `<IDE>/jbr`)
- **Gradle 8.10+** (wrapper included)

#### Build Commands

```bash
# Clone the repository
git clone https://github.com/VanillaFairy/IntelliJ-Docset-Viewer.git
cd IntelliJ-Docset-Viewer

# Build the plugin (output: build/distributions/*.zip)
./gradlew buildPlugin

# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "io.github.vanillafairy.docsetviewer.core.parser.SqliteIndexReaderTest"

# Launch IDE with plugin installed for manual testing
./gradlew runIde

# Clean build artifacts
./gradlew clean
```

#### Versioning

The plugin version is automatically derived from git tags:
- Tag `1.2.0` or `v1.2.0` → version `1.2.0`
- No tags → version `0.0.0-SNAPSHOT`

To release a new version, create a git tag:
```bash
git tag 1.3.0
git push origin 1.3.0
./gradlew buildPlugin
```

#### Build Output

| Artifact | Location |
|----------|----------|
| Plugin ZIP | `build/distributions/IntelliJ-docset-viewer-<version>.zip` |
| Test reports | `build/reports/tests/test/index.html` |

## How to Use

1. **Get docsets** - Download `.docset` bundles from [Zeal](https://zealdocs.org) or [Dash](https://kapeli.com/dash) for the languages and frameworks you use.
2. **Add docsets to the plugin** - Go to **Settings > Tools > Docset Viewer** and click **Add** to select a `.docset` folder, or click **Import from Zeal** if you already have Zeal installed.
3. **Look up documentation** - Place your caret on any symbol and press **Ctrl+Shift+D** (or **F1**). If there's an exact match, the docs open immediately; otherwise you'll get a selection popup.
4. **Search across all docsets** - Press **Ctrl+Alt+D** to open a global search popup where you can type any query and browse results from all loaded docsets.
5. **Browse in the tool window** - Open the **Docsets** panel from the right sidebar to search and browse documentation interactively.

## Usage Details

### Adding Docsets

1. Go to **Settings > Tools > Docset Viewer**
2. Click **Add** to add a docset folder (`.docset` directory)
3. Or click **Import from Zeal** to auto-detect Zeal docsets

### Finding Documentation

#### For Token Under Caret

- Press **F1** or **Ctrl+Shift+D**
- Or right-click and select **Find in Docsets**

The plugin will search for the token under your caret and:
- Jump directly to the documentation if an exact match is found
- Show a popup to select from multiple results otherwise

#### Global Search

- Press **Ctrl+Alt+D**
- Or go to **Tools > Docsets > Search Docsets**

### Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Find in Docsets | F1, Ctrl+Shift+D |
| Search Docsets | Ctrl+Alt+D |

> **Note:** Some shortcuts like F1 may already be assigned to other actions (e.g., Quick Documentation). When multiple actions share a shortcut, the IDE executes the one with the highest priority. To customize this, go to **Settings > Keymap**, search for "Find in Docsets", and assign your preferred shortcut or remove conflicting bindings.

### Tool Window

The **Docsets** tool window (accessible from the right sidebar) allows you to:
- Browse loaded docsets
- Search within docsets
- View documentation pages

## License

MIT

## Author

VanillaFairy
