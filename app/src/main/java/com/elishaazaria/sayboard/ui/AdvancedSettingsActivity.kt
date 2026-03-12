package com.elishaazaria.sayboard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.theme.AppTheme
import com.elishaazaria.sayboard.theme.DarkSurface

class AdvancedSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme(darkTheme = true) {
                AdvancedSettingsContent(
                    onBack = { finish() }
                )
            }
        }
    }

    @Composable
    private fun AdvancedSettingsContent(onBack: () -> Unit) {
        val tabTitles = listOf(
            stringResource(R.string.title_keyboard),
            stringResource(R.string.title_logic),
            stringResource(R.string.title_ui),
            stringResource(R.string.title_api)
        )
        var selectedTab by remember { mutableIntStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = { Text(stringResource(R.string.title_advanced_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                backgroundColor = DarkSurface,
                contentColor = MaterialTheme.colors.onSurface,
                elevation = 0.dp
            )

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                backgroundColor = DarkSurface,
                contentColor = MaterialTheme.colors.primary,
                edgePadding = 16.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        selectedContentColor = MaterialTheme.colors.primary,
                        unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Each tab gets its own scroll context via its own composable
            when (selectedTab) {
                0 -> KeyboardSettingsUi()
                1 -> LogicSettingsUi(this@AdvancedSettingsActivity)
                2 -> UISettingsUi()
                3 -> ApiSettingsUi()
            }
        }
    }
}
