package com.elishaazaria.sayboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.media.AudioDeviceInfo
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material.lightColors
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.elishaazaria.sayboard.AppPrefs
import com.elishaazaria.sayboard.Constants
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.recognition.recognizers.RecognizerState
import com.elishaazaria.sayboard.speakKeysPreferenceModel
import com.elishaazaria.sayboard.theme.DarkSurfaceVariant
import com.elishaazaria.sayboard.theme.ErrorRed
import com.elishaazaria.sayboard.theme.ListeningBlue
import com.elishaazaria.sayboard.theme.Shapes
import com.elishaazaria.sayboard.ui.utils.MyTextButton
import com.elishaazaria.sayboard.utils.AudioDevices
import com.elishaazaria.sayboard.utils.describe
import com.elishaazaria.sayboard.utils.toIcon
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("ViewConstructor")
class ViewManager(private val ime: Context) : AbstractComposeView(ime),
    Observer<RecognizerState> {
    private val prefs by speakKeysPreferenceModel()
    val stateLD = MutableLiveData(STATE_INITIAL)
    val errorMessageLD = MutableLiveData(R.string.mic_info_error)
    private var listener: Listener? = null
    val recognizerNameLD = MutableLiveData("")
    val enterActionLD = MutableLiveData(EditorInfo.IME_ACTION_UNSPECIFIED)

    val recordDevice: MutableLiveData<AudioDeviceInfo?> = MutableLiveData()

    private var devices: List<AudioDeviceInfo> = listOf()

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val stateS = stateLD.observeAsState()
        val errorMessageS = errorMessageLD.observeAsState(R.string.mic_info_error)
        val recognizerNameS = recognizerNameLD.observeAsState(initial = "")
        val height =
            (LocalConfiguration.current.screenHeightDp * when (LocalConfiguration.current.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> prefs.keyboardHeightLandscape.get()
                else -> prefs.keyboardHeightPortrait.get()
            }).toInt().dp
        var showDevicesPopup by remember { mutableStateOf(false) }
        val recordDeviceS by recordDevice.observeAsState()

        IMETheme(prefs) {
            val primary = MaterialTheme.colors.primary
            val bg = MaterialTheme.colors.background
            val onBg = MaterialTheme.colors.onBackground
            val isDark = isSystemInDarkTheme()
            val controlsEnabled =
                stateS.value != STATE_LISTENING &&
                        stateS.value != STATE_PROCESSING &&
                        stateS.value != STATE_LIMIT_WARNING

            // Animated accent color based on state
            val stateColor by animateColorAsState(
                targetValue = when (stateS.value) {
                    STATE_LISTENING -> ListeningBlue
                    STATE_LIMIT_WARNING -> ErrorRed
                    STATE_ERROR -> ErrorRed
                    else -> primary
                },
                animationSpec = tween(300),
                label = "stateColor"
            )

            CompositionLocalProvider(
                LocalContentColor provides onBg
            ) {
                // Outer container with rounded top corners
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(bg)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // ── Top row: back, custom keys, backspace ──
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(onClick = { listener?.backClicked() }) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardHide,
                                    contentDescription = null,
                                    tint = onBg.copy(alpha = 0.7f)
                                )
                            }
                            // Model chip (moved to top row)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isDark) DarkSurfaceVariant
                                        else Color(0xFFE0E0E0)
                                    )
                                    .clickable(enabled = controlsEnabled) { listener?.modelClicked() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = recognizerNameS.value,
                                        fontSize = 12.sp,
                                        color = onBg.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // Audio device
                            IconButton(
                                onClick = {
                                    devices = AudioDevices.validAudioDevices(ime)
                                    showDevicesPopup = true
                                },
                                enabled = controlsEnabled,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = recordDeviceS?.toIcon()
                                        ?: Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = onBg.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .pointerInput(controlsEnabled) {
                                        detectTapGestures(onPress = {
                                            if (!controlsEnabled) return@detectTapGestures
                                            var down = true
                                            coroutineScope {
                                                val repeatJob = launch {
                                                    delay(Constants.BackspaceRepeatStartDelay)
                                                    var repeatDelay = Constants.BackspaceRepeatDelay
                                                    while (down) {
                                                        listener?.backspaceClicked()
                                                        delay(repeatDelay)
                                                        // Accelerate: reduce delay down to minimum 20ms
                                                        repeatDelay = (repeatDelay * 85 / 100).coerceAtLeast(20)
                                                    }
                                                }
                                                launch {
                                                    tryAwaitRelease()
                                                    down = false
                                                    repeatJob.cancel()
                                                }
                                            }
                                        }, onTap = {
                                            if (controlsEnabled) {
                                                listener?.backspaceClicked()
                                            }
                                        })
                                    }
                                    .pointerInput(controlsEnabled) {
                                        detectHorizontalDragGestures(onDragStart = {
                                            if (controlsEnabled) listener?.backspaceTouchStart(it)
                                        }, onDragCancel = {
                                            if (controlsEnabled) listener?.backspaceTouchEnd()
                                        }, onDragEnd = {
                                            if (controlsEnabled) listener?.backspaceTouchEnd()
                                        }, onHorizontalDrag = { change, amount ->
                                            if (controlsEnabled) listener?.backspaceTouched(change, amount)
                                        })
                                    }
                                    .minimumInteractiveComponentSize()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val contentAlpha = LocalContentAlpha.current
                                CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = null,
                                        tint = onBg.copy(alpha = if (controlsEnabled) 0.7f else 0.35f)
                                    )
                                }
                            }
                        }

                        // ── Middle section: side keys + mic ──
                        Row(modifier = Modifier.weight(1f)) {
                            val leftKeys by prefs.keyboardKeysLeft.observeAsState()
                            FlowColumn(
                                modifier = Modifier.padding(start = 4.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                for (key in leftKeys) {
                                    MyTextButton(
                                        onClick = { listener?.buttonClicked(key.text) },
                                        enabled = controlsEnabled
                                    ) {
                                        Text(
                                            text = key.label,
                                            color = onBg.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            // Center: mic button + status
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Mic button with circular background
                                val micButtonSize = 80.dp

                                // Pulse animation for listening state
                                val pulseScale = if (stateS.value == STATE_LISTENING) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val scale by infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 1.08f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(800),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "pulseScale"
                                    )
                                    scale
                                } else {
                                    1f
                                }

                                Box(
                                    modifier = Modifier
                                        .size(micButtonSize)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(
                                            stateColor.copy(alpha = if (isDark) 0.15f else 0.1f),
                                            CircleShape
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = stateColor.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    listener?.micPressStart()
                                                    try {
                                                        tryAwaitRelease()
                                                    } finally {
                                                        listener?.micPressEnd()
                                                    }
                                                }
                                            )
                                        }
                                        .minimumInteractiveComponentSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (stateS.value) {
                                            STATE_LOADING -> Icons.Default.SettingsVoice
                                            STATE_INITIAL, STATE_READY, STATE_PAUSED -> Icons.Default.MicNone
                                            STATE_LISTENING, STATE_LIMIT_WARNING -> Icons.Default.Mic
                                            STATE_PROCESSING -> Icons.Default.SettingsVoice
                                            else -> Icons.Default.MicOff
                                        },
                                        contentDescription = null,
                                        tint = stateColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Status text
                                val statusText = when (stateS.value) {
                                    STATE_LOADING -> stringResource(id = R.string.mic_info_preparing)
                                    STATE_INITIAL, STATE_READY, STATE_PAUSED -> stringResource(id = R.string.mic_info_hold_to_talk)
                                    STATE_LISTENING -> stringResource(id = R.string.mic_info_release_to_send)
                                    STATE_LIMIT_WARNING -> stringResource(id = R.string.mic_info_release_soon)
                                    STATE_PROCESSING -> stringResource(id = R.string.mic_info_processing)
                                    else -> stringResource(id = errorMessageS.value)
                                }
                                val isError = stateS.value == STATE_ERROR
                                Text(
                                    text = if (isError && errorMessageS.value == R.string.mic_error_no_recognizers) {
                                        stringResource(id = R.string.mic_error_no_recognizers_tap_to_configure)
                                    } else {
                                        statusText
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = stateColor.copy(alpha = 0.9f),
                                    modifier = if (isError) {
                                        Modifier.clickable { listener?.settingsClicked() }
                                    } else {
                                        Modifier
                                    }
                                )
                            }

                            val rightKeys by prefs.keyboardKeysRight.observeAsState()
                            FlowColumn(
                                modifier = Modifier.padding(end = 4.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                for (key in rightKeys) {
                                    MyTextButton(
                                        onClick = { listener?.buttonClicked(key.text) },
                                        enabled = controlsEnabled
                                    ) {
                                        Text(
                                            text = key.label,
                                            color = onBg.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        // ── Bottom bar ──
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            // Settings
                            IconButton(
                                onClick = { listener?.settingsClicked() },
                                enabled = controlsEnabled,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = onBg.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Spacebar
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = if (isDark) Color(0xFF2A3A5C) else Color(0xFFD8D8D8),
                                elevation = 3.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .padding(horizontal = 4.dp)
                                    .pointerInput(controlsEnabled) {
                                        detectTapGestures(onPress = {
                                            if (!controlsEnabled) return@detectTapGestures
                                            var down = true
                                            coroutineScope {
                                                val repeatJob = launch {
                                                    delay(Constants.BackspaceRepeatStartDelay)
                                                    while (down) {
                                                        listener?.buttonClicked(" ")
                                                        delay(Constants.BackspaceRepeatDelay)
                                                    }
                                                }
                                                launch {
                                                    tryAwaitRelease()
                                                    down = false
                                                    repeatJob.cancel()
                                                }
                                            }
                                        }, onTap = {
                                            if (controlsEnabled) {
                                                listener?.buttonClicked(" ")
                                            }
                                        })
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "space",
                                        fontSize = 15.sp,
                                        color = onBg.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 1.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Enter/Return
                            IconButton(
                                onClick = { listener?.returnClicked() },
                                enabled = controlsEnabled,
                                modifier = Modifier.size(40.dp)
                            ) {
                                val enterAction by enterActionLD.observeAsState()
                                Icon(
                                    imageVector = when (enterAction) {
                                        EditorInfo.IME_ACTION_GO -> Icons.Default.ArrowRightAlt
                                        EditorInfo.IME_ACTION_SEARCH -> Icons.Default.Search
                                        EditorInfo.IME_ACTION_SEND -> Icons.Default.Send
                                        EditorInfo.IME_ACTION_NEXT -> Icons.Default.NavigateNext
                                        EditorInfo.IME_ACTION_PREVIOUS -> Icons.Default.NavigateBefore
                                        else -> Icons.Default.KeyboardReturn
                                    },
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // ── Audio device popup overlay ──
                    if (showDevicesPopup) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(0.6f))
                                .clickable { showDevicesPopup = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = if (isDark) DarkSurfaceVariant else Color.White,
                                modifier = Modifier
                                    .fillMaxSize(0.8f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        stringResource(id = R.string.mic_audio_device_title),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        items(devices) { device ->
                                            Card(
                                                onClick = {
                                                    showDevicesPopup = false
                                                    recordDevice.postValue(device)
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                backgroundColor = if (isDark)
                                                    Color.White.copy(alpha = 0.08f)
                                                else
                                                    Color.Black.copy(alpha = 0.05f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = device.toIcon(),
                                                        contentDescription = null,
                                                        tint = primary
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        device.describe(),
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onChanged(value: RecognizerState) {
        when (value) {
            RecognizerState.CLOSED, RecognizerState.NONE -> stateLD.setValue(STATE_INITIAL)

            RecognizerState.LOADING -> stateLD.setValue(STATE_LOADING)
            RecognizerState.READY -> stateLD.setValue(STATE_READY)
            RecognizerState.IN_RAM -> stateLD.setValue(STATE_PAUSED)
            RecognizerState.ERROR -> stateLD.setValue(STATE_ERROR)
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    interface Listener {
        fun micPressStart()
        fun micPressEnd()
        fun backClicked()
        fun backspaceClicked()
        fun backspaceTouchStart(offset: Offset)
        fun backspaceTouched(change: PointerInputChange, dragAmount: Float)
        fun backspaceTouchEnd()
        fun returnClicked()
        fun modelClicked()
        fun settingsClicked()
        fun buttonClicked(text: String)
        fun deviceChanged(device: AudioDeviceInfo)
    }

    companion object {
        const val STATE_INITIAL = 0
        const val STATE_LOADING = 1
        const val STATE_READY = 2 // model loaded, ready to start
        const val STATE_LISTENING = 3
        const val STATE_PAUSED = 4
        const val STATE_ERROR = 5
        const val STATE_PROCESSING = 6
        const val STATE_LIMIT_WARNING = 7
    }
}

@Composable
fun IMETheme(
    prefs: AppPrefs,
    content: @Composable () -> Unit
) {
    val colors = if (isSystemInDarkTheme()) {
        darkColors(
            background = Color(prefs.uiNightBackground.get()),
            primary = if (prefs.uiNightForegroundMaterialYou.get()) {
                colorResource(id = R.color.materialYouForeground)
            } else {
                Color(prefs.uiNightForeground.get())
            },
        )
    } else {
        lightColors(
            background = Color(prefs.uiDayBackground.get()),
            primary = if (prefs.uiDayForegroundMaterialYou.get()) {
                colorResource(id = R.color.materialYouForeground)
            } else {
                Color(prefs.uiDayForeground.get())
            },
        )
    }

    MaterialTheme(
        colors = colors,
        shapes = Shapes,
        content = content,
    )
}
