package com.elishaazaria.sayboard.ime

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.elishaazaria.sayboard.Constants
import com.elishaazaria.sayboard.data.HapticIntensity
import com.elishaazaria.sayboard.theme.SpaceAccentBlue
import com.elishaazaria.sayboard.theme.SpaceAccentCyan
import com.elishaazaria.sayboard.theme.SpaceAccentPink
import com.elishaazaria.sayboard.theme.SpaceControlIcon
import com.elishaazaria.sayboard.theme.SpaceKeyColor
import com.elishaazaria.sayboard.theme.SpaceOutline
import com.elishaazaria.sayboard.theme.SpaceOutlineStrong
import com.elishaazaria.sayboard.theme.SpaceSpecialKeyColor
import com.elishaazaria.sayboard.theme.SpaceTextPrimary
import com.elishaazaria.sayboard.theme.SpaceWordmark
import com.elishaazaria.sayboard.theme.spacePanelBrush
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ShiftState { LOWER, SHIFTED, CAPS_LOCK }
private enum class SymbolPage { LETTERS, SYMBOLS_1, SYMBOLS_2 }

private val NUMBERS_ROW = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val LETTERS_ROW_1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
private val LETTERS_ROW_2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
private val LETTERS_ROW_3 = listOf("z", "x", "c", "v", "b", "n", "m")

private val SYMBOLS1_ROW_1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val SYMBOLS1_ROW_2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
private val SYMBOLS1_ROW_3 = listOf("*", "\"", "'", ":", ";", "!", "?")

private val SYMBOLS2_ROW_1 = listOf("~", "`", "|", "^", "<", ">", "{", "}")
private val SYMBOLS2_ROW_2 = listOf("[", "]", "\\", "/", "_", "=")
private val SYMBOLS2_ROW_3 = listOf("€", "£", "¥", "•", "°")

private const val PRESSED_ALPHA_BOOST = 0.12f
private const val PRESSED_SCALE = 1.02f

@Composable
fun QwertyLayout(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onCursorLeft: () -> Unit,
    onCursorRight: () -> Unit,
    onInsertSpace: () -> Unit,
    onToggleVoiceMode: () -> Unit,
    actionLabel: String,
    actionVisual: ViewManager.EnterActionVisual,
    onEnter: () -> Unit,
    hapticFeedbackEnabled: Boolean,
    hapticFeedbackIntensity: HapticIntensity,
    modifier: Modifier = Modifier
) {
    var shiftState by remember { mutableStateOf(ShiftState.LOWER) }
    var symbolPage by remember { mutableStateOf(SymbolPage.LETTERS) }

    val hapticFeedback = remember(hapticFeedbackEnabled, hapticFeedbackIntensity) {
        { HapticHelper.tick(hapticFeedbackEnabled, hapticFeedbackIntensity) }
    }

    // Match standard keyboard behavior: tap for one-shot shift, long-press for caps lock.
    val onShiftTap = {
        shiftState = when (shiftState) {
            ShiftState.LOWER -> ShiftState.SHIFTED
            ShiftState.SHIFTED, ShiftState.CAPS_LOCK -> ShiftState.LOWER
        }
    }
    val onShiftLongPress = {
        shiftState = if (shiftState == ShiftState.CAPS_LOCK) {
            ShiftState.LOWER
        } else {
            ShiftState.CAPS_LOCK
        }
    }

    val onChar: (String) -> Unit = { text ->
        val output = when {
            symbolPage != SymbolPage.LETTERS -> text
            shiftState == ShiftState.LOWER -> text.lowercase()
            else -> text.uppercase()
        }
        onKeyPress(output)
        if (shiftState == ShiftState.SHIFTED) {
            shiftState = ShiftState.LOWER
        }
    }

    val keyBg = SpaceKeyColor
    val specialKeyBg = SpaceSpecialKeyColor
    val textColor = SpaceTextPrimary
    val activeSpecialKeyBg = SpaceAccentBlue.copy(alpha = 0.72f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (symbolPage) {
            SymbolPage.LETTERS -> {
                KeyRow {
                    NUMBERS_ROW.forEach { key ->
                        CharKey(
                            key = key,
                            display = key,
                            bg = keyBg,
                            textColor = textColor,
                            onChar = onChar,
                            hapticFeedback = hapticFeedback
                        )
                    }
                }
                KeyRow {
                    LETTERS_ROW_1.forEach { key ->
                        CharKey(
                            key = key,
                            display = key.uppercaseIfNeeded(shiftState != ShiftState.LOWER),
                            bg = keyBg,
                            textColor = textColor,
                            onChar = onChar,
                            hapticFeedback = hapticFeedback
                        )
                    }
                }
                KeyRow {
                    Spacer(modifier = Modifier.weight(0.5f))
                    LETTERS_ROW_2.forEach { key ->
                        CharKey(
                            key = key,
                            display = key.uppercaseIfNeeded(shiftState != ShiftState.LOWER),
                            bg = keyBg,
                            textColor = textColor,
                            onChar = onChar,
                            hapticFeedback = hapticFeedback
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }
                KeyRow {
                    SpecialKey(
                        label = when (shiftState) {
                            ShiftState.LOWER -> "⇧"
                            ShiftState.SHIFTED -> "⇧"
                            ShiftState.CAPS_LOCK -> "⇪"
                        },
                        bg = if (shiftState == ShiftState.LOWER) specialKeyBg else activeSpecialKeyBg,
                        textColor = textColor,
                        weight = 1.35f,
                        onClick = onShiftTap,
                        onLongPress = onShiftLongPress,
                        hapticFeedback = hapticFeedback
                    )
                    LETTERS_ROW_3.forEach { key ->
                        CharKey(
                            key = key,
                            display = key.uppercaseIfNeeded(shiftState != ShiftState.LOWER),
                            bg = keyBg,
                            textColor = textColor,
                            onChar = onChar,
                            hapticFeedback = hapticFeedback
                        )
                    }
                    BackspaceKey(
                        bg = specialKeyBg,
                        textColor = textColor,
                        weight = 1.35f,
                        onBackspace = onBackspace,
                        hapticFeedback = hapticFeedback
                    )
                }
                BottomUtilityRow(
                    modeToggleLabel = "?123",
                    keyBg = keyBg,
                    specialKeyBg = specialKeyBg,
                    textColor = textColor,
                    actionLabel = actionLabel,
                    actionVisual = actionVisual,
                    onModeToggle = { symbolPage = SymbolPage.SYMBOLS_1 },
                    onToggleVoiceMode = onToggleVoiceMode,
                    onInsertSpace = onInsertSpace,
                    onCursorLeft = onCursorLeft,
                    onCursorRight = onCursorRight,
                    onEnter = onEnter,
                    onPeriod = { onChar(".") },
                    hapticFeedback = hapticFeedback
                )
            }

            SymbolPage.SYMBOLS_1 -> {
                KeyRow {
                    SYMBOLS1_ROW_1.forEach { key ->
                        CharKey(
                            key = key,
                            display = key,
                            bg = keyBg,
                            textColor = textColor,
                            onChar = onChar,
                            hapticFeedback = hapticFeedback
                        )
                    }
                }
                KeyRow {
                    Spacer(modifier = Modifier.weight(0.5f))
                    SYMBOLS1_ROW_2.forEach { key ->
                        CharKey(
                            key = key,
                            display = key,
                            bg = keyBg,
                            textColor = textColor,
                            onChar = onChar,
                            hapticFeedback = hapticFeedback
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }
                KeyRow {
                    SpecialKey(
                        label = "#+=",
                        bg = specialKeyBg,
                        textColor = textColor,
                        weight = 1.35f,
                        onClick = { symbolPage = SymbolPage.SYMBOLS_2 },
                        hapticFeedback = hapticFeedback
                    )
                    SYMBOLS1_ROW_3.forEach { key ->
                        CharKey(
                            key = key,
                            display = key,
                            bg = keyBg,
                            textColor = textColor,
                            onChar = onChar,
                            hapticFeedback = hapticFeedback
                        )
                    }
                    BackspaceKey(
                        bg = specialKeyBg,
                        textColor = textColor,
                        weight = 1.35f,
                        onBackspace = onBackspace,
                        hapticFeedback = hapticFeedback
                    )
                }
                BottomUtilityRow(
                    modeToggleLabel = "ABC",
                    keyBg = keyBg,
                    specialKeyBg = specialKeyBg,
                    textColor = textColor,
                    actionLabel = actionLabel,
                    actionVisual = actionVisual,
                    onModeToggle = { symbolPage = SymbolPage.LETTERS },
                    onToggleVoiceMode = onToggleVoiceMode,
                    onInsertSpace = onInsertSpace,
                    onCursorLeft = onCursorLeft,
                    onCursorRight = onCursorRight,
                    onEnter = onEnter,
                    onPeriod = { onChar(".") },
                    hapticFeedback = hapticFeedback
                )
            }

            SymbolPage.SYMBOLS_2 -> {
                KeyRow {
                    Spacer(modifier = Modifier.weight(1f))
                    SYMBOLS2_ROW_1.forEach { key ->
                        CharKey(
                            key = key,
                            display = key,
                            bg = keyBg,
                            textColor = textColor,
                            onChar = onChar,
                            hapticFeedback = hapticFeedback
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                KeyRow {
                    Spacer(modifier = Modifier.weight(2f))
                    SYMBOLS2_ROW_2.forEach { key ->
                        CharKey(
                            key = key,
                            display = key,
                            bg = keyBg,
                            textColor = textColor,
                            onChar = onChar,
                            hapticFeedback = hapticFeedback
                        )
                    }
                    Spacer(modifier = Modifier.weight(2f))
                }
                KeyRow {
                    SpecialKey(
                        label = "123",
                        bg = specialKeyBg,
                        textColor = textColor,
                        weight = 1.35f,
                        onClick = { symbolPage = SymbolPage.SYMBOLS_1 },
                        hapticFeedback = hapticFeedback
                    )
                    SYMBOLS2_ROW_3.forEach { key ->
                        CharKey(
                            key = key,
                            display = key,
                            bg = keyBg,
                            textColor = textColor,
                            onChar = onChar,
                            hapticFeedback = hapticFeedback
                        )
                    }
                    BackspaceKey(
                        bg = specialKeyBg,
                        textColor = textColor,
                        weight = 1.35f,
                        onBackspace = onBackspace,
                        hapticFeedback = hapticFeedback
                    )
                }
                BottomUtilityRow(
                    modeToggleLabel = "ABC",
                    keyBg = keyBg,
                    specialKeyBg = specialKeyBg,
                    textColor = textColor,
                    actionLabel = actionLabel,
                    actionVisual = actionVisual,
                    onModeToggle = { symbolPage = SymbolPage.LETTERS },
                    onToggleVoiceMode = onToggleVoiceMode,
                    onInsertSpace = onInsertSpace,
                    onCursorLeft = onCursorLeft,
                    onCursorRight = onCursorRight,
                    onEnter = onEnter,
                    onPeriod = { onChar(".") },
                    hapticFeedback = hapticFeedback
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
private fun RowScope.CharKey(
    key: String,
    display: String,
    bg: Color,
    textColor: Color,
    onChar: (String) -> Unit,
    hapticFeedback: () -> Unit
) {
    var isPressed by remember(key, display) { mutableStateOf(false) }
    val currentOnChar by rememberUpdatedState(onChar)
    val currentHaptic by rememberUpdatedState(hapticFeedback)

    KeySurface(
        weight = 1f,
        bg = bg,
        isPressed = isPressed,
        popupText = display,
        modifier = Modifier.pointerInput(key, display) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                currentHaptic()
                val up = waitForUpOrCancellation()
                isPressed = false
                if (up != null) {
                    currentOnChar(key)
                }
            }
        }
    ) {
        Text(
            text = display,
            color = textColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RowScope.SpecialKey(
    label: String,
    bg: Color,
    textColor: Color,
    weight: Float,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    hapticFeedback: () -> Unit
) {
    var isPressed by remember(label) { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentHaptic by rememberUpdatedState(hapticFeedback)

    KeySurface(
        weight = weight,
        bg = bg,
        isPressed = isPressed,
        modifier = Modifier.pointerInput(label, onLongPress != null) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    currentHaptic()
                    try {
                        tryAwaitRelease()
                    } finally {
                        isPressed = false
                    }
                },
                onTap = {
                    currentOnClick()
                },
                onLongPress = {
                    currentOnLongPress?.invoke()
                }
            )
        }
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RowScope.BackspaceKey(
    bg: Color,
    textColor: Color,
    weight: Float,
    onBackspace: () -> Unit,
    hapticFeedback: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnBackspace by rememberUpdatedState(onBackspace)
    val currentHaptic by rememberUpdatedState(hapticFeedback)

    KeySurface(
        weight = weight,
        bg = bg,
        isPressed = isPressed,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    currentHaptic()
                    currentOnBackspace()
                    var down = true
                    coroutineScope {
                        val repeatJob = launch {
                            delay(Constants.BackspaceRepeatStartDelay)
                            var repeatDelay = Constants.BackspaceRepeatDelay
                            while (down && isActive) {
                                currentHaptic()
                                currentOnBackspace()
                                delay(repeatDelay)
                                repeatDelay = (repeatDelay * 85 / 100).coerceAtLeast(20)
                            }
                        }
                        try {
                            tryAwaitRelease()
                        } finally {
                            down = false
                            repeatJob.cancel()
                            isPressed = false
                        }
                    }
                }
            )
        }
    ) {
        SpaceControlIcon(
            icon = Icons.Default.Backspace,
            tint = textColor,
            glowColor = SpaceAccentPink,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ColumnScope.BottomUtilityRow(
    modeToggleLabel: String,
    keyBg: Color,
    specialKeyBg: Color,
    textColor: Color,
    actionLabel: String,
    actionVisual: ViewManager.EnterActionVisual,
    onModeToggle: () -> Unit,
    onToggleVoiceMode: () -> Unit,
    onInsertSpace: () -> Unit,
    onCursorLeft: () -> Unit,
    onCursorRight: () -> Unit,
    onEnter: () -> Unit,
    onPeriod: () -> Unit,
    hapticFeedback: () -> Unit
) {
    KeyRow {
        SpecialKey(
            label = modeToggleLabel,
            bg = specialKeyBg,
            textColor = textColor,
            weight = 1.35f,
            onClick = onModeToggle,
            hapticFeedback = hapticFeedback
        )
        IconKey(
            icon = Icons.Default.Mic,
            bg = specialKeyBg,
            tint = textColor,
            weight = 1.1f,
            onClick = onToggleVoiceMode,
            hapticFeedback = hapticFeedback
        )
        SpaceKey(
            bg = keyBg,
            textColor = textColor,
            weight = 4.1f,
            onInsertSpace = onInsertSpace,
            onCursorLeft = onCursorLeft,
            onCursorRight = onCursorRight,
            hapticFeedback = hapticFeedback
        )
        CharKey(
            key = ".",
            display = ".",
            bg = keyBg,
            textColor = textColor,
            onChar = { onPeriod() },
            hapticFeedback = hapticFeedback
        )
        ActionKey(
            label = actionLabel,
            visual = actionVisual,
            bg = specialKeyBg,
            textColor = textColor,
            weight = 1.15f,
            onClick = onEnter,
            hapticFeedback = hapticFeedback
        )
    }
}

@Composable
private fun RowScope.IconKey(
    icon: ImageVector,
    bg: Color,
    tint: Color,
    weight: Float,
    onClick: () -> Unit,
    hapticFeedback: () -> Unit
) {
    var isPressed by remember(icon) { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentHaptic by rememberUpdatedState(hapticFeedback)

    KeySurface(
        weight = weight,
        bg = bg,
        isPressed = isPressed,
        modifier = Modifier.pointerInput(icon) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                currentHaptic()
                val up = waitForUpOrCancellation()
                isPressed = false
                if (up != null) {
                    currentOnClick()
                }
            }
        }
    ) {
        SpaceControlIcon(
            icon = icon,
            tint = tint,
            glowColor = SpaceAccentCyan,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun RowScope.SpaceKey(
    bg: Color,
    textColor: Color,
    weight: Float,
    onInsertSpace: () -> Unit,
    onCursorLeft: () -> Unit,
    onCursorRight: () -> Unit,
    hapticFeedback: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val dragStepPx = with(density) { 18.dp.toPx() }
    val currentOnInsertSpace by rememberUpdatedState(onInsertSpace)
    val currentOnCursorLeft by rememberUpdatedState(onCursorLeft)
    val currentOnCursorRight by rememberUpdatedState(onCursorRight)
    val currentHaptic by rememberUpdatedState(hapticFeedback)

    KeySurface(
        weight = weight,
        bg = bg,
        isPressed = isPressed,
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                currentHaptic()

                var accumulatedDrag = 0f
                var didDrag = false
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = change.positionChange().x
                        if (delta != 0f) {
                            accumulatedDrag += delta
                            while (abs(accumulatedDrag) >= dragStepPx) {
                                didDrag = true
                                if (accumulatedDrag > 0f) {
                                    currentOnCursorRight()
                                    accumulatedDrag -= dragStepPx
                                } else {
                                    currentOnCursorLeft()
                                    accumulatedDrag += dragStepPx
                                }
                            }
                            change.consume()
                        }
                        if (!change.pressed) {
                            if (!didDrag) {
                                currentOnInsertSpace()
                            }
                            break
                        }
                    }
                } finally {
                    isPressed = false
                }
            }
        }
    ) {
        SpaceWordmark(
            text = "SPACE",
            fontSize = 16.sp
        )
    }
}

@Composable
private fun RowScope.ActionKey(
    label: String,
    visual: ViewManager.EnterActionVisual,
    bg: Color,
    textColor: Color,
    weight: Float,
    onClick: () -> Unit,
    hapticFeedback: () -> Unit
) {
    var isPressed by remember(label, visual) { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentHaptic by rememberUpdatedState(hapticFeedback)

    KeySurface(
        weight = weight,
        bg = bg,
        isPressed = isPressed,
        modifier = Modifier.pointerInput(label, visual) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                currentHaptic()
                val up = waitForUpOrCancellation()
                isPressed = false
                if (up != null) {
                    currentOnClick()
                }
            }
        }
    ) {
        SpaceControlIcon(
            icon = enterActionIcon(visual),
            tint = textColor,
            glowColor = SpaceAccentBlue,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun RowScope.KeySurface(
    weight: Float,
    bg: Color,
    isPressed: Boolean,
    modifier: Modifier = Modifier,
    popupText: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var keyBounds by remember { mutableStateOf<Rect?>(null) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "keyScale"
    )
    val animatedBackground by animateColorAsState(
        targetValue = if (isPressed) bg.pressedColor() else bg,
        animationSpec = tween(durationMillis = 90),
        label = "keyBg"
    )
    val cornerRadius = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .onGloballyPositioned { keyBounds = it.boundsInWindow() }
            .zIndex(if (isPressed && popupText != null) 1f else 0f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .shadow(
                    elevation = if (isPressed) 10.dp else 3.dp,
                    shape = cornerRadius,
                    clip = false
                )
                .clip(cornerRadius)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            animatedBackground.liftedColor(),
                            animatedBackground
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isPressed) SpaceOutlineStrong.copy(alpha = 0.92f) else animatedBackground.outlineColor(),
                    shape = cornerRadius
                )
                .then(modifier),
            contentAlignment = Alignment.Center,
            content = content
        )

        if (isPressed && popupText != null) {
            keyBounds?.let { bounds ->
                CharacterPreviewPopup(
                    text = popupText,
                    keyBounds = bounds,
                    textColor = SpaceTextPrimary
                )
            }
        }
    }
}

@Composable
private fun CharacterPreviewPopup(
    text: String,
    keyBounds: Rect,
    textColor: Color
) {
    val density = LocalDensity.current
    val popupWidthPx = keyBounds.width * 1.45f
    val popupHeightPx = keyBounds.height * 1.55f
    val verticalGapPx = with(density) { 10.dp.toPx() }
    val offset = IntOffset(
        x = (keyBounds.left + ((keyBounds.width - popupWidthPx) / 2f)).roundToInt(),
        y = (keyBounds.top - popupHeightPx - verticalGapPx).roundToInt()
    )

    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false
        )
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = with(density) { popupWidthPx.toDp() },
                    height = with(density) { popupHeightPx.toDp() }
                )
                .shadow(18.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(spacePanelBrush(alpha = 0.98f))
                .border(1.dp, SpaceOutlineStrong.copy(alpha = 0.85f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun String.uppercaseIfNeeded(shouldUppercase: Boolean): String =
    if (shouldUppercase) uppercase() else this

private fun Color.pressedColor(): Color =
    lerp(this, SpaceAccentBlue, PRESSED_ALPHA_BOOST)

private fun Color.liftedColor(): Color =
    lerp(this, Color.White, 0.08f)

private fun Color.outlineColor(): Color =
    lerp(this, SpaceOutline, 0.55f).copy(alpha = 0.78f)
