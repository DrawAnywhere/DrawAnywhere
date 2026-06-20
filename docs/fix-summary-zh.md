# 弹窗与拖拽关闭修复总结

**日期**：2026-06-20 | **平台**：Android (minSdk 26, targetSdk 36)
**Compose**：BOM `2026.05.01`（Compose `1.11.2`）

---

### 图例

| 标记 | 含义 |
|------|------|
| ✅ `已验证` | 在真机上实测过（Android 11 & 16） |
| ⚠️ `理论推断` | 基于源码阅读的诊断，未经 WM 内部调试确认 |

---

## 修改统计

| 文件 | 新增 | 删除 |
|------|------|------|
| `app/build.gradle.kts` | +10 | 0 |
| `DrawViewModel.kt` | +7 | -3 |
| `MainService.kt` | +80 | -2 |
| `DrawToolbar.kt` | +7 | -7 |
| `ToolbarRenderers.kt` | +106 | -69 |
| **合计** | **+210** | **-81** |

净增 **129 行**，涉及 5 个源文件。

---

## 问题 1：弹窗按钮不弹出

### ✅ 表象（已验证）

- **Android 11**：工具栏弹窗按钮（`tool_controls`、`color_picker`、`settings`）可见可点击，但点击后不弹出弹窗。
- **Android 16**：同样按钮正常弹出弹窗。

### ⚠️ 根因（理论诊断）

原代码 `PopupToolbarButton` 使用 `androidx.compose.ui.window.Popup`，配置 `PopupProperties(focusable = true)`。`Popup` 内部通过 `WindowManager.addView()` 从工具栏窗口（标记为 `FLAG_NOT_FOCUSABLE`，因画布不能抢占下层应用焦点）**创建一个新的可获焦子窗口**。

Android 11 上，WindowManager（`WindowManagerService.java`）严格拒绝非焦点父窗口产生的焦点子窗口。Compose 的 `PopupLayout` 内部捕获失败，直接不渲染弹窗。Android 14+ 上，平台改为静默降级：不拒绝窗口创建，但强制去掉子窗口的焦点标识；Compose 1.6+ 做适配将 `onDismissRequest` 改为仅响应返回键。

**源码参考**：
- WM 焦点策略：https://cs.android.com/android/platform/superproject/+/android11-release:frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
- Compose `PopupLayout`：https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/window/PopupLayout.kt
- Compose Android 弹窗实现：https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/window/AndroidPopup.android.kt

### 修复

**完全移除 `Popup` 组合件。** 在独立的、全屏的 `ComposeView` 叠加窗口中直接渲染弹窗内容，不创建任何子窗口。

| 修复前 | 修复后 |
|--------|--------|
| `Popup(focusable=true)` 创建子窗口 → WM 拒绝 | 独立 `ComposeView` 内嵌渲染弹窗 → 无子窗口 |
| 弹窗状态在按钮局部管理 | 弹窗状态（`activePopupId`）通过 ViewModel 的 `UiState` 管理 |
| 无生命周期 → `collectAsState()` 静默失效 | `LifecycleOwner` 在 `setContent()` **之前**绑定 |
| 无主题 → `MaterialTheme.colorScheme` 为 null | `DrawAnywhereTheme` 包装弹窗内容 |
| 切换弹窗时翻页状态残留 | `key(activeId)` 强制重建组合树 |

### 修改文件

| 文件 | 关键改动 |
|------|----------|
| `DrawViewModel.kt` | `UiState` 增加 `activePopupId: String? = null`；新增 `togglePopup(id)`、`closePopup()` |
| `ToolbarRenderers.kt` | 从 `PopupToolbarButton` 移除 `Popup`/`PopupProperties`；`RenderButton` 接受 `activePopupId` + `onTogglePopup`；新增 `PopupOverlay()` 含 `HorizontalPager` + 屏幕边界钳制 + 页面指示点 |
| `DrawToolbar.kt` | `ToolbarButtonsContainer` 传递 `activePopupId` + `onTogglePopup` 到所有 `RenderButton` 调用 |
| `MainService.kt` | 新增 `popupView: ComposeView`（MATCH_PARENT，空闲时 GONE）；生命周期在 `setContent` 前绑定；内容包装 `DrawAnywhereTheme` + `key(activeId)`；观察 `activePopupId` 切换可见性/触摸性 |

---

## 问题 2：弹窗出界

### ✅ 表象

工具栏被拖到屏幕边缘后，弹窗卡片部分或全部渲染在屏幕可见区域之外。

### 修复

**将弹窗卡片位置钳制在屏幕边界内。** 在 `PopupOverlay` 中使用 `BoxWithConstraints` 获取屏幕尺寸，通过 `onSizeChanged` 读取卡片实际大小，对偏移执行 `coerceIn` 钳制到 `[0, screenWidth - cardWidth]` × `[0, screenHeight - cardHeight]`。

```kotlin
val xPx = rawX.coerceIn(0, (screenWidthPx - cardWidthPx).coerceAtLeast(0))
val yPx = rawY.coerceIn(0, (screenHeightPx - cardHeightPx).coerceAtLeast(0))
```

---

## 问题 3：叉号不触发

### ✅ 表象

将工具栏向屏幕底部的 X 叉号拖动时，有时叉号不变红，无法关闭。具体表现为：工具栏在屏幕右侧时，只有从工具栏**左侧**拖动能触发叉号，从右侧拖动无效。

### 根因

原 `onDismissDragMove` 计算的是**手指的**屏幕坐标（`toolbarPosition + fingerOffset`）并判断手指是否在叉号上方。由于叉号在屏幕**底部中央**，手指的**水平**位置至关重要——从工具栏不同位置拖动会产生不同的手指屏幕 X 坐标，多数落在叉号的窄范围（80dp 居中）之外。

### 修复

**判断工具栏自身位置，而非手指位置。** 用户拖的是工具栏，理应以工具栏中心是否进入叉号区域为准。

`DrawViewModel.kt` — `onDismissDragMove`：
```kotlin
// 修复前：手指位置
val active = containsDismissTarget(
    (pos.x + fingerPosInToolbar.x).toInt(),
    (pos.y + fingerPosInToolbar.y).toInt()
)
// 修复后：工具栏位置
val active = containsDismissTarget(pos.x.toInt(), pos.y.toInt())
```

`MainService.kt` — `containsDismissTarget` 回调加上工具栏中心偏移：
```kotlin
// 修复前：
{ x, y -> dismissTargetView.containsScreenPoint(x, y) }
// 修复后：
{ x, y ->
    dismissTargetView.containsScreenPoint(
        x + toolbarView.width / 2,
        y + toolbarView.height / 2
    )
}
```

现在工具栏**中心**进入叉号区域即触发变红，与按住工具栏哪个部位无关。

---

## 问题 4：熄屏后工具栏无响应

### ✅ 表象

手机熄屏再亮屏后，工具栏永久无响应——只能屏幕书写，工具栏按钮点击和拖动均无效。

### 根因

中间版本曾尝试将工具栏的 `FLAG_NOT_TOUCH_MODAL` 替换为 `FLAG_NOT_TOUCHABLE`。`FLAG_NOT_TOUCHABLE` 使工具栏无法接收**任何**触摸事件。工具栏的唤醒机制（`resetToolbarTimer()`）由 `ToolbarCard.kt` 中的 `pointerInput` 触摸事件触发，形成了不可逆的死锁：

```
FLAG_NOT_TOUCHABLE → 收不到触摸事件 → resetToolbarTimer() 不触发
→ toolbarActive 始终为 false → FLAG_NOT_TOUCHABLE 永不解除 → …
```

### 为什么 FLAG_NOT_TOUCH_MODAL 实际上不会打断拖拽

拖拽工具栏时，窗口位置通过 `windowManager.updateViewLayout()` 持续更新——窗口边界**跟随手指移动**。由于手指始终在移动中的窗口边界内，`FLAG_NOT_TOUCH_MODAL`（"将窗口范围外事件路由至下层窗口"）在拖拽过程中**永不被触发**。此 flag 从不会中断拖拽手势。

### 修复

**恢复原始 `FLAG_NOT_TOUCH_MODAL`。** 叉号不触发问题（问题 3）与窗口 flag 无关，已独立通过"手指位置改为工具栏位置"的修复解决。

---

## 版本表现汇总

| | Android 11 | Android 16 |
|---|---|---|
| 弹窗按钮（修复前） | ❌ 弹窗不出现 | ✅ 弹窗出现（平台+Compose 静默降级） |
| 弹窗按钮（修复后） | ✅ 正常 | ✅ 正常 |
| 叉号关闭（修复后） | ✅ 任意触摸位置均触发 | ✅ 任意触摸位置均触发 |
| 熄屏恢复（修复后） | ✅ 工具栏正常唤醒 | ✅ 工具栏正常唤醒 |

---

## 测试验证（均通过）

### ✅ Android 11
1. 点击弹窗按钮 → 弹窗卡片出现在工具栏附近，不越界
2. 点击卡片外部 → 弹窗关闭
3. 拖工具栏到底部中央 → 叉号变红 → 松手 → 工具栏关闭
4. 熄屏 → 亮屏 → 工具栏仍可正常交互

### ✅ Android 16
行为完全一致。

---

## 参考资料

- AOSP WindowManagerService：https://cs.android.com/android/platform/superproject/+/android11-release:frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
- Compose PopupLayout：https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/window/PopupLayout.kt
- Compose AndroidPopup：https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/window/AndroidPopup.android.kt