package com.elishaazaria.sayboard.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.auth.AuthManager
import com.elishaazaria.sayboard.auth.SubscriptionManager

@Composable
fun AccountSettingsUi(
    activity: Activity,
    onSignIn: () -> Unit
) {
    val brandBlue = Color(0xFF1F5DD7)
    var isSignedIn by remember { mutableStateOf(AuthManager.isSignedIn) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.title_account),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.SemiBold,
            color = brandBlue
        )

        if (!isSignedIn) {
            // Signed out state
            SignedOutCard(brandBlue, onSignIn)
        } else {
            // Signed in state
            SignedInCard(brandBlue)

            // Subscription status
            SubscriptionStatusCard(brandBlue)

            // Credits info
            CreditsCard()

            Spacer(modifier = Modifier.height(8.dp))

            // Sign out
            OutlinedButton(
                onClick = {
                    AuthManager.signOut(activity)
                    SubscriptionManager.clearCache()
                    isSignedIn = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.account_sign_out))
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        // Link to API settings
        Text(
            text = stringResource(R.string.account_developer_hint),
            style = MaterialTheme.typography.body2,
            color = Color.Gray
        )
    }
}

@Composable
private fun SignedOutCard(brandBlue: Color, onSignIn: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = brandBlue
            )

            Text(
                text = stringResource(R.string.account_sign_in_title),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(R.string.account_sign_in_description),
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )

            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = brandBlue,
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.account_sign_in_google))
            }
        }
    }
}

@Composable
private fun SignedInCard(brandBlue: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = brandBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = AuthManager.displayName ?: stringResource(R.string.account_user),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold
                )
                AuthManager.email?.let { email ->
                    Text(
                        text = email,
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionStatusCard(brandBlue: Color) {
    val status = SubscriptionManager.subscriptionStatus
    val isTrialActive = SubscriptionManager.isTrialActive
    val trialDays = SubscriptionManager.trialDaysRemaining
    val isTrialPending = status == "none" && SubscriptionManager.credits == 0 &&
        !isTrialActive && SubscriptionManager.hasAccess

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.account_subscription_header),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold,
                color = brandBlue
            )

            when {
                status == "active" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.account_subscription_active),
                            style = MaterialTheme.typography.body1,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                isTrialPending -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = brandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.account_trial_pending),
                            style = MaterialTheme.typography.body1,
                            color = brandBlue
                        )
                    }
                    Text(
                        text = stringResource(R.string.account_trial_pending_description),
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
                isTrialActive -> {
                    Text(
                        text = stringResource(R.string.account_trial_remaining, trialDays),
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        text = stringResource(R.string.account_trial_encouragement),
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.account_trial_expired),
                            style = MaterialTheme.typography.body1,
                            color = Color(0xFFFF9800)
                        )
                    }
                    Text(
                        text = stringResource(R.string.account_subscribe_prompt),
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                    // TODO: Launch RevenueCat paywall
                    Button(
                        onClick = { /* Launch RevenueCat paywall */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.account_subscribe_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditsCard() {
    val credits = SubscriptionManager.credits
    if (credits > 0) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.account_credits_header),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.account_credits_remaining, credits),
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}
