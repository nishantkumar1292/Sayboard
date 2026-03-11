package com.elishaazaria.sayboard.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.auth.AuthManager

@Composable
fun AccountSettingsUi(
    activity: android.app.Activity,
    isSignedIn: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val brandBlue = Color(0xFF1F5DD7)

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
            SignedOutCard(brandBlue, onSignIn)
        } else {
            SignedInCard(brandBlue)

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.account_sign_out))
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

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
