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

- JDK 21 (JetBrains Runtime recommended)
- Gradle 8.10+

#### Build Steps

```bash
# Clone the repository
git clone https://github.com/VanillaFairy/clion-docset-viewer.git
cd clion-docset-viewer

# Build the plugin
./gradlew buildPlugin

# The plugin ZIP will be in build/distributions/
```

#### Running Tests

```bash
./gradlew test
```

## Usage

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
