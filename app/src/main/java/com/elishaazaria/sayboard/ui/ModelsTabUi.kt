package com.elishaazaria.sayboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.theme.DarkSurfaceVariant
import com.elishaazaria.sayboard.theme.Primary
import com.elishaazaria.sayboard.theme.WarningOrange

@Composable
fun ModelsTabUi(
    selectedEngine: String,
    isSignedIn: Boolean,
    hasSarvamKey: Boolean,
    onSelectEngine: (String) -> Unit,
    onRequireSignIn: () -> Unit,
    onRequireApiKey: () -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.tab_models),
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            text = stringResource(R.string.models_header_subtitle),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Section header
        Text(
            text = stringResource(R.string.models_available_engines),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
            letterSpacing = 1.sp
        )

        // SpeakKeys Auto card + settings
        EngineCard(
            name = stringResource(R.string.engine_speakkeys_auto),
            badge = stringResource(R.string.engine_badge_hosted),
            badgeColor = Primary,
            icon = Icons.Default.AutoAwesome,
            isSelected = selectedEngine == "proxied",
            isAvailable = isSignedIn,
            isExpanded = expandedEngine == "proxied",
            unavailableLabel = stringResource(R.string.engine_requires_sign_in),
            onClick = {
                if (isSignedIn) onSelectEngine("proxied") else onRequireSignIn()
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
                    options = listOf("translit" to "Transliterate (Roman)", "transcribe" to "Native Script"),
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

        // Sarvam card + settings
        EngineCard(
            name = stringResource(R.string.engine_sarvam),
            badge = stringResource(R.string.engine_badge_byok),
            badgeColor = Color(0xFF7C4DFF),
            icon = Icons.Default.Translate,
            isSelected = selectedEngine == "sarvam",
            isAvailable = hasSarvamKey,
            isExpanded = expandedEngine == "sarvam",
            unavailableLabel = stringResource(R.string.engine_requires_api_key),
            onClick = {
                if (hasSarvamKey) onSelectEngine("sarvam") else onRequireApiKey()
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
                    onSave = onSarvamApiKeyChange
                )
                DropdownSettingRow(
                    label = stringResource(R.string.sarvam_mode_title),
                    value = sarvamModeLabel,
                    options = listOf("translit" to "Transliterate (Roman)", "transcribe" to "Native Script"),
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
    val borderColor = if (isSelected) Primary else Color.Transparent
    val cardAlpha = if (isAvailable) 1f else 0.6f

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkSurfaceVariant.copy(alpha = cardAlpha),
        border = BorderStroke(if (isSelected) 2.dp else 0.dp, borderColor),
        elevation = if (isSelected) 4.dp else 0.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main body — tapping selects the engine
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isAvailable) Primary else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = badge,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
                            color = WarningOrange,
                            fontSize = 12.sp
                        )
                    }
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Chevron — tapping toggles the settings panel
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse settings" else "Expand settings",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                modifier = Modifier
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun EngineSettingsPanel(content: @Composable () -> Unit) {
    Card(
        backgroundColor = DarkSurfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
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
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(dialogTitle) },
            text = {
                TextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
                )
            },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        onSave("")
                        showDialog = false
                    }) {
                        Text(stringResource(R.string.button_clear))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.button_cancel))
                    }
                    TextButton(onClick = {
                        onSave(editValue.trim())
                        showDialog = false
                    }) {
                        Text(stringResource(R.string.button_save))
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        )
    }
}

@Composable
private fun ToggleSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material.SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = Primary.copy(alpha = 0.5f)
            )
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
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
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
