package com.elishaazaria.sayboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.speakKeysPreferenceModel
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.ScrollablePreferenceLayout

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun KeyboardSettingsUi() = ScrollablePreferenceLayout(speakKeysPreferenceModel()) {
    PreferenceGroup(title = stringResource(id = R.string.keyboard_height_header)) {
        DialogSliderPreference(
            pref = prefs.keyboardHeightPortrait,
            title = stringResource(id = R.string.keyboard_height_portrait_title),
            min = 0.01f,
            max = 1f,
            stepIncrement = 0.01f
        )
        DialogSliderPreference(
            pref = prefs.keyboardHeightLandscape,
            title = stringResource(id = R.string.keyboard_height_landscape_title),
            min = 0.01f,
            max = 1f,
            stepIncrement = 0.01f
        )
    }
}
