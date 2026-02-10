package com.elishaazaria.sayboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.elishaazaria.sayboard.R
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.material.ui.JetPrefListItem

@Composable
fun TextFieldPreference(
    pref: PreferenceData<String>,
    title: String,
    summary: String = "",
    isPassword: Boolean = false,
) {
    val currentValue = pref.get()
    var showDialog by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf("") }

    val displaySummary = if (isPassword && currentValue.isNotEmpty()) {
        "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"
    } else if (currentValue.isNotEmpty()) {
        currentValue
    } else {
        summary.ifEmpty { stringResource(R.string.api_key_not_set) }
    }

    JetPrefListItem(
        modifier = Modifier.clickable(
            role = Role.Button,
            onClick = {
                editValue = currentValue
                showDialog = true
            },
        ),
        text = title,
        secondaryText = displaySummary,
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    TextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isPassword) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        }
                    )
                }
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        pref.set("")
                        showDialog = false
                    }) {
                        Text(stringResource(R.string.button_clear))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.button_cancel))
                    }
                    TextButton(onClick = {
                        pref.set(editValue.trim())
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
