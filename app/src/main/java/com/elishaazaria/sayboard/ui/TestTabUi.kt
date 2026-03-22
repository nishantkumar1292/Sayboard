package com.elishaazaria.sayboard.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
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
import com.elishaazaria.sayboard.theme.SpaceAccentBlue
import com.elishaazaria.sayboard.theme.SpaceAccentCyan
import com.elishaazaria.sayboard.theme.SpaceBackdrop
import com.elishaazaria.sayboard.theme.SpaceControlIcon
import com.elishaazaria.sayboard.theme.SpaceFieldFill
import com.elishaazaria.sayboard.theme.SpaceOutline
import com.elishaazaria.sayboard.theme.SpaceOutlineStrong
import com.elishaazaria.sayboard.theme.SpacePanel
import com.elishaazaria.sayboard.theme.SpaceSuccessGreen
import com.elishaazaria.sayboard.theme.SpaceTextPrimary
import com.elishaazaria.sayboard.theme.SpaceTextSecondary
import com.elishaazaria.sayboard.theme.SpaceWarningAmber
import com.elishaazaria.sayboard.theme.spacePanelBrush

@Composable
fun TestTabUi(
    isActive: Boolean,
    needsAuth: Boolean,
    currentModelName: String,
    onNavigateToModels: () -> Unit
) {
    var testText by remember { mutableStateOf("") }

    SpaceBackdrop(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Black,
                color = SpaceTextPrimary
            )
            Text(
                text = stringResource(R.string.test_tagline),
                style = MaterialTheme.typography.subtitle1,
                color = SpaceTextSecondary,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SpaceInfoCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.test_status_label),
                    icon = Icons.Default.CheckCircle,
                    iconTint = if (isActive) SpaceSuccessGreen else SpaceTextSecondary,
                    glowColor = if (isActive) SpaceSuccessGreen else SpaceAccentBlue,
                    value = if (isActive) {
                        stringResource(R.string.test_status_active)
                    } else {
                        stringResource(R.string.test_status_inactive)
                    },
                    valueColor = if (isActive) SpaceSuccessGreen else SpaceTextSecondary
                )

                val engineScale = remember { Animatable(1f) }
                LaunchedEffect(needsAuth) {
                    if (needsAuth) {
                        engineScale.animateTo(1.12f, animationSpec = tween(220))
                        engineScale.animateTo(1f, animationSpec = tween(220))
                    }
                }

                SpaceInfoCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToModels),
                    label = stringResource(R.string.test_current_model),
                    icon = if (needsAuth) Icons.Default.Warning else Icons.Default.Mic,
                    iconTint = if (needsAuth) SpaceWarningAmber else SpaceTextPrimary,
                    glowColor = if (needsAuth) SpaceWarningAmber else SpaceAccentBlue,
                    value = if (needsAuth) {
                        stringResource(R.string.test_engine_needs_setup)
                    } else {
                        currentModelName
                    },
                    valueColor = if (needsAuth) SpaceWarningAmber else SpaceTextPrimary,
                    iconScale = engineScale.value
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.test_drive_title),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.SemiBold,
                color = SpaceTextPrimary
            )

            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.test_drive_hint),
                        color = SpaceTextSecondary.copy(alpha = 0.55f)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = SpaceTextPrimary,
                    cursorColor = SpaceAccentCyan,
                    focusedBorderColor = SpaceAccentCyan,
                    unfocusedBorderColor = SpaceOutlineStrong.copy(alpha = 0.75f),
                    backgroundColor = SpaceFieldFill,
                    focusedLabelColor = SpaceTextSecondary,
                    unfocusedLabelColor = SpaceTextSecondary,
                    placeholderColor = SpaceTextSecondary.copy(alpha = 0.55f)
                )
            )

            OutlinedButton(
                onClick = { testText = "" },
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SpaceOutlineStrong),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = SpaceTextPrimary
                ),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text(
                    text = stringResource(R.string.test_drive_clear),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SpaceInfoCard(
    modifier: Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    glowColor: Color,
    value: String,
    valueColor: Color,
    iconScale: Float = 1f
) {
    SpacePanel(
        modifier = modifier.heightIn(min = 120.dp),
        shape = RoundedCornerShape(24.dp),
        borderColor = SpaceOutline,
        backgroundBrush = spacePanelBrush(alpha = 0.92f),
        contentAlignment = Alignment.CenterStart,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.caption,
                color = SpaceTextSecondary.copy(alpha = 0.9f),
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpaceControlIcon(
                    icon = icon,
                    tint = iconTint,
                    glowColor = glowColor,
                    modifier = Modifier
                        .size(26.dp)
                        .scale(iconScale)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = valueColor
                )
            }
        }
    }
}
