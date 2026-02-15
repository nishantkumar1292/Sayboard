package com.elishaazaria.sayboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.speakKeysPreferenceModel
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.ScrollablePreferenceLayout
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

@Composable
fun ApiSettingsUi() = ScrollablePreferenceLayout(speakKeysPreferenceModel()) {
    ApiSectionHeader(stringResource(R.string.api_keys_header))
    TextFieldPreference(
        pref = prefs.openaiApiKey,
        title = stringResource(R.string.openai_api_key_title),
        summary = stringResource(R.string.api_key_not_set),
        isPassword = true,
    )
    TextFieldPreference(
        pref = prefs.sarvamApiKey,
        title = stringResource(R.string.sarvam_api_key_title),
        summary = stringResource(R.string.api_key_not_set),
        isPassword = true,
    )

    ApiSectionHeader(stringResource(R.string.whisper_settings_header))
    TextFieldPreference(
        pref = prefs.whisperLanguage,
        title = stringResource(R.string.whisper_language_title),
        summary = stringResource(R.string.whisper_language_summary),
    )
    TextFieldPreference(
        pref = prefs.whisperPrompt,
        title = stringResource(R.string.whisper_prompt_title),
        summary = stringResource(R.string.whisper_prompt_summary),
    )
    SwitchPreference(
        pref = prefs.whisperTransliterateToRoman,
        title = stringResource(R.string.whisper_transliterate_title),
        summary = stringResource(R.string.whisper_transliterate_summary),
    )

    ApiSectionHeader(stringResource(R.string.sarvam_settings_header))
    ListPreference(
        listPref = prefs.sarvamMode,
        title = stringResource(R.string.sarvam_mode_title),
        entries = listPrefEntries {
            entry(key = "translit", label = "Transliterate (Roman)")
            entry(key = "native", label = "Native Script")
        },
    )
    TextFieldPreference(
        pref = prefs.sarvamLanguage,
        title = stringResource(R.string.sarvam_language_title),
        summary = stringResource(R.string.sarvam_language_summary),
    )
}

@Composable
private fun ApiSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.subtitle1,
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
    )
}
