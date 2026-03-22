package com.elishaazaria.sayboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.auth.AuthManager
import com.elishaazaria.sayboard.theme.SpaceAccentBlue
import com.elishaazaria.sayboard.theme.SpaceAccentCyan
import com.elishaazaria.sayboard.theme.SpaceBackdrop
import com.elishaazaria.sayboard.theme.SpaceControlIcon
import com.elishaazaria.sayboard.theme.SpaceOutline
import com.elishaazaria.sayboard.theme.SpaceOutlineStrong
import com.elishaazaria.sayboard.theme.SpacePanel
import com.elishaazaria.sayboard.theme.SpaceTextPrimary
import com.elishaazaria.sayboard.theme.SpaceTextSecondary
import com.elishaazaria.sayboard.theme.spacePanelBrush

@Composable
fun ProfileTabUi(
    isSignedIn: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onAdvancedSettings: () -> Unit
) {
    SpaceBackdrop(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpaceControlIcon(
                    icon = Icons.Default.Person,
                    tint = SpaceTextPrimary,
                    glowColor = SpaceAccentCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.title_account),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Black,
                    color = SpaceTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isSignedIn) {
                SpacePanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    borderColor = SpaceOutline,
                    backgroundBrush = spacePanelBrush(alpha = 0.92f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SpaceControlIcon(
                            icon = Icons.Default.Person,
                            tint = SpaceTextPrimary,
                            glowColor = SpaceAccentBlue,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = stringResource(R.string.account_sign_in_title),
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.SemiBold,
                            color = SpaceTextPrimary
                        )
                        Text(
                            text = stringResource(R.string.account_sign_in_description),
                            style = MaterialTheme.typography.body2,
                            color = SpaceTextSecondary
                        )
                        Button(
                            onClick = onSignIn,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = SpaceAccentCyan,
                                contentColor = Color(0xFF021019)
                            )
                        ) {
                            Text(stringResource(R.string.account_sign_in_google), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                SpacePanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    borderColor = SpaceOutline,
                    backgroundBrush = spacePanelBrush(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpaceControlIcon(
                            icon = Icons.Default.Person,
                            tint = SpaceTextPrimary,
                            glowColor = SpaceAccentBlue,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AuthManager.displayName ?: stringResource(R.string.account_user),
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.SemiBold,
                                color = SpaceTextPrimary
                            )
                            AuthManager.email?.let { email ->
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.body2,
                                    color = SpaceTextSecondary
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SpaceOutlineStrong),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = SpaceAccentCyan
                    )
                ) {
                    Text(
                        stringResource(R.string.account_sign_out),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SpaceOutline.copy(alpha = 0.45f))
            )

            Text(
                text = stringResource(R.string.profile_other_settings),
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = SpaceTextSecondary.copy(alpha = 0.86f),
                letterSpacing = 1.2.sp
            )

            SpacePanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAdvancedSettings),
                shape = RoundedCornerShape(22.dp),
                borderColor = SpaceOutline,
                backgroundBrush = spacePanelBrush(alpha = 0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpaceControlIcon(
                        icon = Icons.Default.Settings,
                        tint = SpaceTextPrimary,
                        glowColor = SpaceAccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.profile_advanced_settings),
                        style = MaterialTheme.typography.body1,
                        color = SpaceTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    SpaceControlIcon(
                        icon = Icons.Default.ChevronRight,
                        tint = SpaceTextSecondary,
                        glowColor = SpaceAccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
