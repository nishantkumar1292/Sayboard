package com.elishaazaria.sayboard.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.theme.ActiveGreen
import com.elishaazaria.sayboard.theme.DarkSurfaceVariant
import com.elishaazaria.sayboard.theme.Primary
import com.elishaazaria.sayboard.theme.WarningOrange

@Composable
fun TestTabUi(
    isActive: Boolean,
    needsAuth: Boolean,
    currentModelName: String,
    onNavigateToModels: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            text = stringResource(R.string.test_tagline),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Status cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // STATUS card
            Card(
                modifier = Modifier.weight(1f),
                backgroundColor = DarkSurfaceVariant,
                elevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.test_status_label),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isActive) ActiveGreen else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isActive)
                                stringResource(R.string.test_status_active)
                            else
                                stringResource(R.string.test_status_inactive),
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) ActiveGreen else Color.Gray
                        )
                    }
                }
            }

            // CURRENT ENGINE card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToModels),
                backgroundColor = DarkSurfaceVariant,
                elevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.test_current_model),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (needsAuth) {
                            val scale = remember { Animatable(1f) }
                            LaunchedEffect(Unit) {
                                scale.animateTo(1.15f, animationSpec = tween(200))
                                scale.animateTo(1f, animationSpec = tween(200))
                            }
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningOrange,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(scale.value)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (needsAuth)
                                stringResource(R.string.test_engine_needs_setup)
                            else
                                currentModelName,
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (needsAuth) WarningOrange else Color.Unspecified
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Test Drive section
        Text(
            text = stringResource(R.string.test_drive_title),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.SemiBold
        )

        var testText by remember { mutableStateOf("") }

        OutlinedTextField(
            value = testText,
            onValueChange = { testText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.test_drive_hint),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colors.onSurface,
                cursorColor = Primary,
                focusedBorderColor = Primary,
                unfocusedBorderColor = DarkSurfaceVariant,
                backgroundColor = DarkSurfaceVariant
            )
        )

        OutlinedButton(
            onClick = { testText = "" },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.test_drive_clear))
        }
    }
}
