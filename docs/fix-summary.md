# Popup & Dismiss Fix Summary

**Date**: 2026-06-20 | **Platform**: Android (minSdk 26, targetSdk 36)
**Compose**: BOM `2026.05.01` (Compose `1.11.2`)

---

### Legend

| Marker | Meaning |
|--------|---------|
| ✅ `VERIFIED` | Tested on real device (Android 11 & 16) |
| ⚠️ `THEORETICAL` | Inferred from source code, not confirmed via WM-internal debugging |

---

## Change Statistics

| File | Insertions | Deletions |
|------|-----------|-----------|
| `app/build.gradle.kts` | +10 | 0 |
| `DrawViewModel.kt` | +7 | -3 |
| `MainService.kt` | +80 | -2 |
| `DrawToolbar.kt` | +7 | -7 |
| `ToolbarRenderers.kt` | +106 | -69 |
| **Total** | **+210** | **-81** |

Net change: **+129 lines** across **5 source files**.

---

## Problem 1: Popup Buttons Not Working

### ✅ Symptom (Verified)

- **Android 11**: toolbar popup buttons (`tool_controls`, `color_picker`, `settings`) are visible and clickable, but clicking them does **not** show a popup.
- **Android 16**: same buttons show popups normally.

### ⚠️ Root Cause (Theoretical Diagnosis)

The original `PopupToolbarButton` used `androidx.compose.ui.window.Popup` with `PopupProperties(focusable = true)`. `Popup` internally calls `WindowManager.addView()` to create a **new focusable child window** from the toolbar window, which is flagged `FLAG_NOT_FOCUSABLE` (required — the canvas must not steal focus from the underlying app).

On Android 11, the WindowManager (`WindowManagerService.java`) strictly rejects focusable children from non-focusable parents. Compose's `PopupLayout` catches the failure internally and simply does not render the popup. On Android 14+, the same platform silently degrades: it strips the focus request from the child window instead of rejecting it, and Compose 1.6+ adapts by wiring `onDismissRequest` to back-press only.

**Sources**:
- WM focus policy: https://cs.android.com/android/platform/superproject/+/android11-release:frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
- Compose `PopupLayout`: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/window/PopupLayout.kt
- Compose Android popup impl: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/window/AndroidPopup.android.kt

### Fix

**Remove `Popup` composable entirely.** Render popup content in a separate fullscreen `ComposeView` overlay window that never creates child windows.

| Before | After |
|--------|-------|
| `Popup(focusable=true)` creates child window → WM rejects | Separate `ComposeView` renders popup inline → no child window |
| Popup state managed locally in button | Popup state (`activePopupId`) managed in `UiState` via ViewModel |
| No lifecycle owner → `collectAsState()` silently fails | `LifecycleOwner` attached **before** `setContent()` |
| No theme → `MaterialTheme.colorScheme` null | `DrawAnywhereTheme` wraps popup content |
| Popup page pager state stale on switch | `key(activeId)` forces fresh composition |

### Files Changed

| File | Key Changes |
|------|-------------|
| `DrawViewModel.kt` | `UiState` + `activePopupId: String? = null`; `togglePopup(id)`; `closePopup()` |
| `ToolbarRenderers.kt` | Removed `Popup`/`PopupProperties` from `PopupToolbarButton`; `RenderButton` takes `activePopupId` + `onTogglePopup`; Added `PopupOverlay()` with `HorizontalPager` + screen-bound clamping + page dots |
| `DrawToolbar.kt` | `ToolbarButtonsContainer` passes `activePopupId` + `onTogglePopup` through all `RenderButton` calls |
| `MainService.kt` | New `popupView: ComposeView` (MATCH_PARENT, GONE when idle); lifecycle attached before `setContent`; content wrapped in `DrawAnywhereTheme` + `key(activeId)`; observe `activePopupId` to toggle visibility/touchability |

---

## Problem 2: Popup Position Off-Screen

### ✅ Symptom

When toolbar is dragged to screen edges, the popup card sometimes renders partially or fully outside the visible screen area.

### Fix

**Clamp popup card position to screen bounds.** In `PopupOverlay`, use `BoxWithConstraints` to get screen dimensions, read the card's actual size via `onSizeChanged`, and `coerceIn` the offset to `[0, screenWidth - cardWidth]` × `[0, screenHeight - cardHeight]`.

```kotlin
val xPx = rawX.coerceIn(0, (screenWidthPx - cardWidthPx).coerceAtLeast(0))
val yPx = rawY.coerceIn(0, (screenHeightPx - cardHeightPx).coerceAtLeast(0))
```

---

## Problem 3: Dismiss Target Not Triggering

### ✅ Symptom

Dragging the toolbar toward the X dismiss target at the bottom of the screen sometimes does not turn the X red, preventing dismissal. Specifically: when the toolbar is near the right side of the screen, only dragging from the LEFT part of the toolbar triggers the X.

### Root Cause

The original `onDismissDragMove` computed the **finger's** screen position (`toolbarPosition + fingerOffset`) and checked if the finger was over the dismiss target. Since the dismiss target is at the **bottom center** of the screen, the finger's **horizontal** position matters — dragging from different parts of the toolbar produces different finger screen-X values, many of which fall outside the narrow dismiss target range (80dp at center).

### Fix

**Check the toolbar's position, not the finger's position.** The user is dragging the toolbar widget itself, so the toolbar's center should trigger the dismiss target.

`DrawViewModel.kt` — `onDismissDragMove`:
```kotlin
// Before: finger position
val active = containsDismissTarget(
    (pos.x + fingerPosInToolbar.x).toInt(),
    (pos.y + fingerPosInToolbar.y).toInt()
)
// After: toolbar position
val active = containsDismissTarget(pos.x.toInt(), pos.y.toInt())
```

`MainService.kt` — `containsDismissTarget` callback adds toolbar center offset:
```kotlin
// Before:
{ x, y -> dismissTargetView.containsScreenPoint(x, y) }
// After:
{ x, y ->
    dismissTargetView.containsScreenPoint(
        x + toolbarView.width / 2,
        y + toolbarView.height / 2
    )
}
```

Now the X turns red when the toolbar's **center** enters the dismiss target area, regardless of which part was touched.

---

## Problem 4: Screen-Off Deadlock

### ✅ Symptom

After the phone screen turns off and back on, the toolbar becomes permanently unresponsive — only canvas drawing works, toolbar buttons and drags have no effect.

### Root Cause

An intermediate fix attempted to replace `FLAG_NOT_TOUCH_MODAL` with `FLAG_NOT_TOUCHABLE` on the toolbar window. `FLAG_NOT_TOUCHABLE` prevents the toolbar from receiving **any** touch events. The toolbar's wake-up mechanism (`resetToolbarTimer()`) is triggered by touch events from `pointerInput` in `ToolbarCard.kt`. This created an irreversible deadlock:

```
FLAG_NOT_TOUCHABLE → no touch events delivered → resetToolbarTimer() never called
→ toolbarActive stays false → FLAG_NOT_TOUCHABLE never cleared → ...
```

### Why FLAG_NOT_TOUCH_MODAL is actually correct for drags

During a toolbar drag, the window position is continuously updated via `windowManager.updateViewLayout()` — the window bounds **move with the finger**. Because the finger stays within the moving window bounds, `FLAG_NOT_TOUCH_MODAL` ("route outside events to windows below") never activates during a drag. The drag gesture is never interrupted by this flag.

### Fix

**Restore original `FLAG_NOT_TOUCH_MODAL`.** The dismiss target issue (Problem 3) was independent of the window flag and was fixed separately by changing from finger-position to toolbar-position detection.

---

## Version Behavior Summary

| | Android 11 | Android 16 |
|---|---|---|
| Popup buttons (before fix) | ❌ Popup never appears | ✅ Popup appears (silent degradation) |
| Popup buttons (after fix) | ✅ Works | ✅ Works |
| Dismiss target (after fix) | ✅ Works (any touch position) | ✅ Works (any touch position) |
| Screen-off recovery (after fix) | ✅ Toolbar wakes up | ✅ Toolbar wakes up |

---

## Testing (Both Verified)

### ✅ Android 11
1. Tap popup button → popup card appears near toolbar, within screen bounds
2. Tap outside card → popup dismisses
3. Drag toolbar to bottom center → X turns red → release → toolbar closes
4. Screen off → screen on → toolbar remains responsive

### ✅ Android 16
Identical behavior confirmed.

---

## References

- AOSP WindowManagerService: https://cs.android.com/android/platform/superproject/+/android11-release:frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
- Compose PopupLayout: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/window/PopupLayout.kt
- Compose AndroidPopup: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/window/AndroidPopup.android.kt