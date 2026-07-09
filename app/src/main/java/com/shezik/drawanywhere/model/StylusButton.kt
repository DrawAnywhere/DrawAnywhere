package com.shezik.drawanywhere.model

enum class StylusButtonScheme {
    Disabled,
    XiaomiSmartPen,
}

enum class StylusButtonAction {
    None,
    CyclePresetColor,
    ToggleStrokeEraser,
    TogglePixelEraser,
    Undo,
    Redo,
    ToggleCanvasVisibility,
    ToggleCanvasPassthrough,
    ToggleLaser,
}
