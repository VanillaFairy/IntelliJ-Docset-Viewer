# Browser Navigation Toolbar Design

## Overview

Add an enhanced navigation toolbar with Back (split-button with history dropdown), Forward (split-button with history dropdown), Reload, and Home buttons to both the toolwindow panel and editor tabs. Support mouse button 4/5 (hardware back/forward) in the JCEF browser.

## Requirements

- Back/Forward buttons with split-button dropdown showing page title history
- Clicking dropdown entry jumps directly to that page (no repeated clicking)
- Reload button
- Home button navigating to the docset's index page
- Mouse button 4/5 (side buttons) trigger back/forward
- Applied to both the toolwindow panel and editor tabs
- Shared reusable toolbar component (Approach 1)

## Component Design

### 1. NavigationHistory Enhancement

Current `NavigationHistory` stores `String` URLs. Enhanced version:

- New `data class HistoryEntry(val url: String, val title: String)`
- `backStack` and `forwardStack` become `ArrayDeque<HistoryEntry>`
- `currentEntry: HistoryEntry?` replaces `currentUrl: String?`
- New methods:
  - `getBackEntries(): List<HistoryEntry>` — back stack, most recent first
  - `getForwardEntries(): List<HistoryEntry>` — forward stack as list
  - `goBackTo(index: Int): HistoryEntry?` — jump back N steps, intermediate entries move to forward stack
  - `goForwardTo(index: Int): HistoryEntry?` — jump forward N steps, intermediate entries move to back stack
- `push()` becomes `push(url: String, title: String)`

Title capture: In `DocsetBrowserPanel.onLoadEnd()`, extract `document.title` via `executeJavaScript()` and pass to `history.push(url, title)`. Use URL filename as fallback until title loads.

### 2. Split-Button Back/Forward Actions

`BackActionGroup` and `ForwardActionGroup` extend `DefaultActionGroup`:

- Primary action: navigate one step back/forward
- `isPopup = true` with dropdown arrow
- `getChildren()` returns history entries as `AnAction` items showing page titles
- Each item calls `goBackTo(index)` / `goForwardTo(index)`
- Use `templatePresentation.putClientProperty("actionGroup.perform.firstAction", true)` for split-button behavior

### 3. Reload and Home Actions

- `ReloadAction` — calls `browserPanel.reload()`
- `HomeAction` — calls `browserPanel.goHome()`
  - `DocsetBrowserPanel` gets a `homeUrl: String?` property
  - Home URL derived from docset path: `{docsetPath}/Contents/Resources/Documents/index.html`
  - Enabled only when `homeUrl` is set

### 4. DocsetNavigationToolbar

New reusable component:

```
DocsetNavigationToolbar(browserPanel: DocsetBrowserPanel, parentDisposable: Disposable)
```

- Creates `ActionToolbar` with: BackActionGroup | ForwardActionGroup | ReloadAction | HomeAction
- Exposes `val component: JComponent`
- Used by both `DocsetFileEditor` (replaces current `setupToolbar()`) and `DocsetToolWindowPanel` (added above browser)

### 5. Mouse Button 4/5 Support

Inject JavaScript in `onLoadEnd()` alongside dark mode injection:

```javascript
document.addEventListener('mouseup', function(e) {
    if (e.button === 3) { /* call Java goBack via JBCefJSQuery */ e.preventDefault(); }
    if (e.button === 4) { /* call Java goForward via JBCefJSQuery */ e.preventDefault(); }
});
```

Use `JBCefJSQuery` to bridge JS mouse events back to Java, calling `browserPanel.goBack()` / `goForward()`. Prevent default to avoid Chromium's built-in navigation conflicting with custom `NavigationHistory`.

## Files Modified

| File | Change |
|------|--------|
| `NavigationHistory.kt` | Add `HistoryEntry`, enhance API |
| `DocsetBrowserPanel.kt` | Title capture, `homeUrl`, `goHome()`, mouse button JS, `JBCefJSQuery` |
| `DocsetFileEditor.kt` | Replace inner toolbar classes with `DocsetNavigationToolbar` |
| `DocsetToolWindowPanel.kt` | Add `DocsetNavigationToolbar` above browser |

## New Files

| File | Purpose |
|------|---------|
| `DocsetNavigationToolbar.kt` | Shared toolbar component |
| `BackActionGroup.kt` | Split-button back with dropdown |
| `ForwardActionGroup.kt` | Split-button forward with dropdown |
| `HomeAction.kt` | Home navigation action |

## Testing

- Update `NavigationHistoryTest` for `HistoryEntry`, `goBackTo()`, `goForwardTo()`, `getBackEntries()`, `getForwardEntries()`
- JCEF/mouse button behavior tested manually via `runIde`
