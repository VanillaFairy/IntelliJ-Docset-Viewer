# Design: Back/Forward Buttons with Right-Click History

**Date:** 2026-02-23
**Status:** Approved

## Problem

The existing `BackActionGroup` and `ForwardActionGroup` use `DefaultActionGroup` with a split-button hack (`putClientProperty("actionGroup.perform.firstAction", true)`) that does not reliably produce a `[back|▼]` visual. The dropdown also redundantly includes a "Back"/"Forward" item as its first entry.

## Decision

Replace the split-button approach with simple buttons that use mouse-button semantics:

- **Left click** → primary action (go back / go forward)
- **Right click** → history dropdown popup

## Design

### New classes

**`BackAction`** — replaces `BackActionGroup`

- Extends `AnAction`, implements `CustomComponentAction`
- `actionPerformed()`: calls `browserPanel.goBack()`
- `update()`: enables/disables based on `browserPanel.canGoBack()`; also updates the custom component's `isEnabled`
- `createCustomComponent()`: returns an icon button with:
  - Left click → `goBack()`
  - Right click → `JPopupMenu` from `NavigationHistory.getBackEntries()`, each item calls `browserPanel.goBackTo(index)`

**`ForwardAction`** — replaces `ForwardActionGroup`

- Same pattern as `BackAction`, using `goForward()` and `getForwardEntries()`

### Deleted files

- `BackActionGroup.kt`
- `ForwardActionGroup.kt`

### Unchanged

- `DocsetNavigationToolbar` — adds actions to the group; no structural change needed beyond using the new class names
- `NavigationHistory` — unchanged
- `DocsetBrowserPanel` — unchanged

## Trade-offs

| Concern | Assessment |
|---------|------------|
| Right-click is less discoverable than a visible arrow | Acceptable; toolbar space is tight and history is a secondary feature |
| `CustomComponentAction` requires manual enabled-state sync | Handled in `update()` via `COMPONENT_KEY` |
| No split-button visual affordance | Intentional; simpler UX, no arrow clutter |
