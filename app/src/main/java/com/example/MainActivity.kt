package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatwave.data.model.ScreenDestination
import com.example.chatwave.ui.screens.auth.CreateAccountScreen
import com.example.chatwave.ui.screens.auth.FirstLaunchScreen
import com.example.chatwave.ui.screens.auth.UnlockScreen
import com.example.chatwave.ui.screens.calls.ActiveAudioCallScreen
import com.example.chatwave.ui.screens.calls.ActiveVideoCallScreen
import com.example.chatwave.ui.screens.calls.CallHistoryScreen
import com.example.chatwave.ui.screens.calls.CallSummaryScreen
import com.example.chatwave.ui.screens.calls.IncomingCallScreen
import com.example.chatwave.ui.screens.calls.OutgoingCallScreen
import com.example.chatwave.ui.screens.contacts.AddContactScreen
import com.example.chatwave.ui.screens.contacts.IncomingInvitesScreen
import com.example.chatwave.ui.screens.contacts.InviteContactScreen
import com.example.chatwave.ui.screens.home.HomeScreen
import com.example.chatwave.ui.screens.settings.DeviceSecurityScreen
import com.example.chatwave.ui.screens.settings.DiagnosticsScreen
import com.example.chatwave.ui.screens.settings.SettingsScreen
import com.example.chatwave.ui.screens.splash.SplashScreen
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val vm: ChatWaveViewModel = viewModel()
      val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
      LaunchedEffect(Unit) {
        val req = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) req += Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= 31) req += Manifest.permission.BLUETOOTH_CONNECT
        permissions.launch(req.toTypedArray())
      }
      MyApplicationTheme {
        Surface(Modifier.fillMaxSize()) { ChatWaveApp(vm) }
      }
    }
  }
}

@Composable
fun ChatWaveApp(vm: ChatWaveViewModel) {
  val screen by vm.screen.collectAsState()
  BackHandler(enabled = screen != ScreenDestination.HOME && screen != ScreenDestination.SPLASH && screen != ScreenDestination.FIRST_LAUNCH && screen != ScreenDestination.UNLOCK) {
    when (screen) {
      ScreenDestination.ADD_CONTACT, ScreenDestination.INVITE_CONTACT, ScreenDestination.INCOMING_INVITES, ScreenDestination.CALL_HISTORY, ScreenDestination.SETTINGS -> vm.navigate(ScreenDestination.HOME)
      ScreenDestination.DEVICE_SECURITY, ScreenDestination.DIAGNOSTICS -> vm.navigate(ScreenDestination.SETTINGS)
      ScreenDestination.CALL_SUMMARY, ScreenDestination.OUTGOING_CALL -> vm.navigate(ScreenDestination.HOME)
      else -> vm.navigate(ScreenDestination.HOME)
    }
  }
  when (screen) {
    ScreenDestination.SPLASH -> SplashScreen()
    ScreenDestination.FIRST_LAUNCH -> FirstLaunchScreen(vm)
    ScreenDestination.CREATE_ACCOUNT -> CreateAccountScreen(vm)
    ScreenDestination.UNLOCK -> UnlockScreen(vm)
    ScreenDestination.HOME -> HomeScreen(vm)
    ScreenDestination.ADD_CONTACT -> AddContactScreen(vm)
    ScreenDestination.INVITE_CONTACT -> InviteContactScreen(vm)
    ScreenDestination.INCOMING_INVITES -> IncomingInvitesScreen(vm)
    ScreenDestination.CALL_HISTORY -> CallHistoryScreen(vm)
    ScreenDestination.OUTGOING_CALL -> OutgoingCallScreen(vm)
    ScreenDestination.INCOMING_CALL -> IncomingCallScreen(vm)
    ScreenDestination.ACTIVE_AUDIO_CALL -> ActiveAudioCallScreen(vm)
    ScreenDestination.ACTIVE_VIDEO_CALL -> ActiveVideoCallScreen(vm)
    ScreenDestination.CALL_SUMMARY -> CallSummaryScreen(vm)
    ScreenDestination.SETTINGS -> SettingsScreen(vm)
    ScreenDestination.DEVICE_SECURITY -> DeviceSecurityScreen(vm)
    ScreenDestination.DIAGNOSTICS -> DiagnosticsScreen(vm)
  }
}
