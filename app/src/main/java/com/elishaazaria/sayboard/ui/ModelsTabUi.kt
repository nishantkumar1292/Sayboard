package com.elishaazaria.sayboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.theme.SpaceAccentBlue
import com.elishaazaria.sayboard.theme.SpaceAccentCyan
import com.elishaazaria.sayboard.theme.SpaceBackdrop
import com.elishaazaria.sayboard.theme.SpaceControlIcon
import com.elishaazaria.sayboard.theme.SpaceFieldFill
import com.elishaazaria.sayboard.theme.SpaceOutline
import com.elishaazaria.sayboard.theme.SpaceOutlineStrong
import com.elishaazaria.sayboard.theme.SpacePanel
import com.elishaazaria.sayboard.theme.SpaceTextPrimary
import com.elishaazaria.sayboard.theme.SpaceTextSecondary
import com.elishaazaria.sayboard.theme.SpaceWarningAmber
import com.elishaazaria.sayboard.theme.spacePanelBrush

@Composable
fun ModelsTabUi(
    selectedEngine: String,
    isSignedIn: Boolean,
    hasSarvamKey: Boolean,
    onSelectEngine: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    // Whisper settings
    whisperLanguage: String,
    onWhisperLanguageChange: (String) -> Unit,
    whisperPrompt: String,
    onWhisperPromptChange: (String) -> Unit,
    whisperTransliterate: Boolean,
    onWhisperTransliterateChange: (Boolean) -> Unit,
    // Sarvam settings
    sarvamApiKey: String,
    onSarvamApiKeyChange: (String) -> Unit,
    sarvamMode: String,
    onSarvamModeChange: (String) -> Unit,
    sarvamLanguage: String,
    onSarvamLanguageChange: (String) -> Unit
) {
    var expandedEngine by remember { mutableStateOf<String?>(null) }
    val sarvamModeLabel =
        if (sarvamMode == "translit") "Transliterate (Roman)" else "Native Script"

    SpaceBackdrop(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.tab_models),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Black,
                color = SpaceTextPrimary
            )
            Text(
                text = stringResource(R.string.models_header_subtitle),
                style = MaterialTheme.typography.body1,
                color = SpaceTextSecondary,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.models_available_engines),
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = SpaceTextSecondary.copy(alpha = 0.86f),
                letterSpacing = 1.2.sp
            )

            EngineCard(
                name = stringResource(R.string.engine_speakkeys_auto),
                badge = stringResource(R.string.engine_badge_hosted),
                badgeColor = SpaceAccentCyan,
                icon = Icons.Default.AutoAwesome,
                isSelected = selectedEngine == "proxied",
                isAvailable = isSignedIn,
                isExpanded = expandedEngine == "proxied",
                unavailableLabel = stringResource(R.string.engine_requires_sign_in),
                onClick = {
                    if (isSignedIn) onSelectEngine("proxied") else onNavigateToProfile()
                },
                onToggleExpand = {
                    expandedEngine = if (expandedEngine == "proxied") null else "proxied"
                }
            )

            AnimatedVisibility(
                visible = expandedEngine == "proxied",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                EngineSettingsPanel {
                    DropdownSettingRow(
                        label = stringResource(R.string.sarvam_mode_title),
                        value = sarvamModeLabel,
                        options = listOf(
                            "translit" to "Transliterate (Roman)",
                            "transcribe" to "Native Script"
                        ),
                        onSelect = onSarvamModeChange
                    )
                    EditableSettingRow(
                        label = stringResource(R.string.sarvam_language_title),
                        value = sarvamLanguage.ifEmpty { "Auto-detect" },
                        dialogTitle = stringResource(R.string.sarvam_language_title),
                        currentValue = sarvamLanguage,
                        onSave = onSarvamLanguageChange
                    )
                }
            }

            EngineCard(
                name = stringResource(R.string.engine_sarvam),
                badge = stringResource(R.string.engine_badge_byok),
                badgeColor = SpaceAccentBlue,
                icon = Icons.Default.Translate,
                isSelected = selectedEngine == "sarvam",
                isAvailable = hasSarvamKey,
                isExpanded = expandedEngine == "sarvam",
                unavailableLabel = stringResource(R.string.engine_requires_api_key),
                onClick = {
                    if (hasSarvamKey) onSelectEngine("sarvam")
                    else expandedEngine = "sarvam"
                },
                onToggleExpand = {
                    expandedEngine = if (expandedEngine == "sarvam") null else "sarvam"
                }
            )

            AnimatedVisibility(
                visible = expandedEngine == "sarvam",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                EngineSettingsPanel {
                    EditableSettingRow(
                        label = stringResource(R.string.sarvam_api_key_title),
                        value = if (sarvamApiKey.isNotEmpty()) "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022" else "Not set",
                        dialogTitle = stringResource(R.string.sarvam_api_key_title),
                        currentValue = sarvamApiKey,
                        isPassword = true,
                        onSave = { key ->
                            onSarvamApiKeyChange(key)
                            if (key.isNotEmpty()) onSelectEngine("sarvam")
                        }
                    )
                    DropdownSettingRow(
                        label = stringResource(R.string.sarvam_mode_title),
                        value = sarvamModeLabel,
                        options = listOf(
                            "translit" to "Transliterate (Roman)",
                            "transcribe" to "Native Script"
                        ),
                        onSelect = onSarvamModeChange
                    )
                    EditableSettingRow(
                        label = stringResource(R.string.sarvam_language_title),
                        value = sarvamLanguage.ifEmpty { "Auto-detect" },
                        dialogTitle = stringResource(R.string.sarvam_language_title),
                        currentValue = sarvamLanguage,
                        onSave = onSarvamLanguageChange
                    )
                }
            }
        }
    }
}

@Composable
private fun EngineCard(
    name: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    isSelected: Boolean,
    isAvailable: Boolean,
    isExpanded: Boolean,
    unavailableLabel: String,
    onClick: () -> Unit,
    onToggleExpand: () -> Unit
) {
    val borderColor = if (isSelected) SpaceAccentCyan else SpaceOutline
    val cardAlpha = if (isAvailable) 1f else 0.72f

    SpacePanel(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        shape = RoundedCornerShape(22.dp),
        borderColor = borderColor.copy(alpha = if (isSelected) 0.95f else 0.74f),
        backgroundBrush = spacePanelBrush(alpha = if (isSelected) 0.97f else 0.92f)
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 10.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpaceControlIcon(
                    icon = icon,
                    tint = if (isAvailable) SpaceTextPrimary else SpaceTextSecondary,
                    glowColor = badgeColor,
                    modifier = Modifier.size(28.dp),
                    enabled = isAvailable
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.SemiBold,
                            color = SpaceTextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = badgeColor.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                text = badge,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (!isAvailable) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = unavailableLabel,
                            style = MaterialTheme.typography.caption,
                            color = SpaceWarningAmber,
                            fontSize = 12.sp
                        )
                    }
                }
                if (isSelected) {
                    SpaceControlIcon(
                        icon = Icons.Default.CheckCircle,
                        tint = SpaceTextPrimary,
                        glowColor = SpaceAccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            SpaceControlIcon(
                icon = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                tint = SpaceTextSecondary,
                glowColor = if (isSelected) SpaceAccentCyan else SpaceAccentBlue,
                modifier = Modifier
                    .clickable(onClick = onToggleExpand)
                    .padding(10.dp)
                    .size(18.dp)
            )
        }
    }
}

@Composable
private fun EngineSettingsPanel(content: @Composable () -> Unit) {
    SpacePanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        borderColor = SpaceOutline.copy(alpha = 0.7f),
        backgroundBrush = spacePanelBrush(alpha = 0.86f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun EditableSettingRow(
    label: String,
    value: String,
    dialogTitle: String,
    currentValue: String,
    isPassword: Boolean = false,
    onSave: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                editValue = currentValue
                showDialog = true
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = SpaceTextPrimary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.caption,
                color = SpaceTextSecondary
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(dialogTitle, color = SpaceTextPrimary) },
            text = {
                TextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (isPassword) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = SpaceTextPrimary,
                        backgroundColor = SpaceFieldFill,
                        cursorColor = SpaceAccentCyan,
                        focusedIndicatorColor = SpaceAccentCyan,
                        unfocusedIndicatorColor = SpaceOutlineStrong,
                        placeholderColor = SpaceTextSecondary
                    )
                )
            },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        onSave("")
                        showDialog = false
                    }) {
                        Text(stringResource(R.string.button_clear), color = SpaceTextSecondary)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.button_cancel), color = SpaceTextSecondary)
                    }
                    TextButton(onClick = {
                        onSave(editValue.trim())
                        showDialog = false
                    }) {
                        Text(stringResource(R.string.button_save), color = SpaceAccentCyan)
                    }
                    Spacer(Modifier.width(8.dp))
                }
            },
            backgroundColor = Color(0xFF101927),
            contentColor = SpaceTextPrimary
        )
    }
}

@Composable
private fun DropdownSettingRow(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = SpaceTextPrimary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.caption,
                color = SpaceTextSecondary
            )
        }
        SpaceControlIcon(
            icon = Icons.Default.ExpandMore,
            tint = SpaceTextSecondary,
            glowColor = SpaceAccentBlue,
            modifier = Modifier.size(16.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (key, displayLabel) ->
                DropdownMenuItem(onClick = {
                    onSelect(key)
                    expanded = false
                }) {
                    Text(displayLabel)
                }
            }
        }
    }
}
