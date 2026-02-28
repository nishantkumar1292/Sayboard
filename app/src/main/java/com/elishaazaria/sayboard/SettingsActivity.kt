package com.elishaazaria.sayboard

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.elishaazaria.sayboard.auth.AuthManager
import com.elishaazaria.sayboard.auth.SubscriptionManager
import com.elishaazaria.sayboard.theme.AppTheme
import com.elishaazaria.sayboard.ui.AccountSettingsUi
import com.elishaazaria.sayboard.ui.GrantPermissionUi
import com.elishaazaria.sayboard.ui.KeyboardSettingsUi
import com.elishaazaria.sayboard.ui.LogicSettingsUi
import com.elishaazaria.sayboard.ui.ModelsSettingsUi
import com.elishaazaria.sayboard.ui.ApiSettingsUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private val micGranted = MutableLiveData<Boolean>(true)
    private val imeGranted = MutableLiveData<Boolean>(true)

    private val modelSettingsUi = ModelsSettingsUi(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        modelSettingsUi.onCreate()

        // Refresh subscription status on launch
        if (AuthManager.isSignedIn) {
            CoroutineScope(Dispatchers.IO).launch {
                SubscriptionManager.refreshStatus()
            }
        }

        setContent {
            AppTheme {
                val micGrantedState = micGranted.observeAsState(true)
                val imeGrantedState = imeGranted.observeAsState(true)
                if (micGrantedState.value && imeGrantedState.value) {
                    MainUi()
                } else {
                    GrantPermissionUi(
                        mic = micGrantedState,
                        ime = imeGrantedState,
                        requestMic = {
                            ActivityCompat.requestPermissions(
                                this, arrayOf(
                                    Manifest.permission.RECORD_AUDIO
                                ), PERMISSIONS_REQUEST_RECORD_AUDIO
                            )
                        },
                        requestIme = {
                            startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS"))
                        },
                        onSignIn = {
                            @Suppress("deprecation")
                            startActivityForResult(
                                AuthManager.getSignInIntent(this),
                                AuthManager.RC_SIGN_IN
                            )
                        },
                        onSkipSignIn = {
                            // Permissions are granted, skip sign-in and go to main UI
                            checkPermissions()
                        }
                    )
                }
            }
        }
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AuthManager.RC_SIGN_IN) {
            CoroutineScope(Dispatchers.IO).launch {
                val success = AuthManager.handleSignInResult(data)
                if (success) {
                    Log.d("SettingsActivity", "Google Sign-In successful")
                    SubscriptionManager.refreshStatus()
                }
            }
        }
    }

    @Composable
    private fun MainUi() {
        val tabs = listOf<String>(
            stringResource(id = R.string.title_account),
            stringResource(id = R.string.title_models),
            stringResource(id = R.string.title_keyboard),
            stringResource(id = R.string.title_logic),
            stringResource(id = R.string.title_api)
        )
        var selectedIndex by remember {
            mutableIntStateOf(0)
        }

        Scaffold(bottomBar = {
            BottomNavigation(modifier = Modifier.navigationBarsPadding()) {
                tabs.forEachIndexed { index, tab ->
                    BottomNavigationItem(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        icon = {
                            when (index) {
                                0 -> Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null
                                )

                                1 -> Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null
                                )

                                2 -> Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = null
                                )

                                3 -> Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null
                                )

                                4 -> Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null
                                )
                            }
                        }, label = {
                            Text(text = tab)
                        })
                }
            }
        }) {
            Box(
                modifier = Modifier
                    .padding(it)
                    .statusBarsPadding()
                    .padding(10.dp)
            ) {
                when (selectedIndex) {
                    0 -> AccountSettingsUi(
                        activity = this@SettingsActivity,
                        onSignIn = {
                            @Suppress("deprecation")
                            startActivityForResult(
                                AuthManager.getSignInIntent(this@SettingsActivity),
                                AuthManager.RC_SIGN_IN
                            )
                        }
                    )
                    1 -> modelSettingsUi.Content()
                    2 -> KeyboardSettingsUi()
                    3 -> LogicSettingsUi(this@SettingsActivity)
                    4 -> ApiSettingsUi()
                }
            }
        }
    }

    private fun checkPermissions() {
        micGranted.postValue(Tools.isMicrophonePermissionGranted(this))
        imeGranted.postValue(Tools.isIMEEnabled(this))
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        modelSettingsUi.onResume()
    }

    companion object {
        private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }
}
