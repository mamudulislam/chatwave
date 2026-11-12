package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatwave.data.model.ThemeMode
import com.example.chatwave.ui.screens.auth.*
import com.example.chatwave.ui.screens.calls.ActiveCallScreen
import com.example.chatwave.ui.screens.calls.IncomingCallDialog
import com.example.chatwave.ui.screens.chat.ChatDetailScreen
import com.example.chatwave.ui.screens.chat.GroupInfoScreen
import com.example.chatwave.ui.screens.contacts.AddContactScreen
import com.example.chatwave.ui.screens.contacts.CreateGroupScreen
import com.example.chatwave.ui.screens.home.HomeScreen
import com.example.chatwave.ui.screens.settings.PrivacySettingsScreen
import com.example.chatwave.ui.screens.settings.ProfileScreen
import com.example.chatwave.ui.screens.settings.SettingsScreen
import com.example.chatwave.ui.screens.settings.WallpaperSettingsScreen
import com.example.chatwave.ui.screens.splash.SplashScreen
import com.example.chatwave.ui.screens.status.CreateStatusScreen
import com.example.chatwave.ui.screens.status.StatusViewerScreen
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel
import com.example.chatwave.ui.viewmodel.ScreenDestination
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val chatWaveViewModel: ChatWaveViewModel = viewModel()
      val settings by chatWaveViewModel.settings.collectAsState()
      val systemInDark = isSystemInDarkTheme()

      val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }

      MyApplicationTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize()) {
          ChatWaveApp(viewModel = chatWaveViewModel)
        }
      }
    }
  }
}

@Composable
fun ChatWaveApp(viewModel: ChatWaveViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val activeCall by viewModel.activeCall.collectAsState()
  val incomingCall by viewModel.incomingCall.collectAsState()

  // Handle system back navigation gracefully
  if (currentScreen != ScreenDestination.HOME && currentScreen != ScreenDestination.SPLASH && currentScreen != ScreenDestination.AUTH_LOGIN) {
    BackHandler {
      when (currentScreen) {
        ScreenDestination.CHAT_DETAIL -> viewModel.navigateTo(ScreenDestination.HOME)
        ScreenDestination.GROUP_INFO -> viewModel.navigateTo(ScreenDestination.CHAT_DETAIL)
        ScreenDestination.SETTINGS_PRIVACY, ScreenDestination.SETTINGS_WALLPAPER, ScreenDestination.PROFILE -> viewModel.navigateTo(ScreenDestination.SETTINGS)
        ScreenDestination.SETTINGS -> viewModel.navigateTo(ScreenDestination.HOME)
        ScreenDestination.CREATE_STATUS, ScreenDestination.STATUS_VIEWER -> viewModel.navigateTo(ScreenDestination.HOME)
        ScreenDestination.CREATE_GROUP, ScreenDestination.ADD_CONTACT -> viewModel.navigateTo(ScreenDestination.HOME)
        ScreenDestination.AUTH_OTP, ScreenDestination.AUTH_FORGOT_PASSWORD, ScreenDestination.AUTH_REGISTER -> viewModel.navigateTo(ScreenDestination.AUTH_LOGIN)
        else -> viewModel.navigateTo(ScreenDestination.HOME)
      }
    }
  }

  // Active Screen Routing
  when (currentScreen) {
    ScreenDestination.SPLASH -> SplashScreen()
    ScreenDestination.AUTH_LOGIN -> LoginScreen(viewModel = viewModel)
    ScreenDestination.AUTH_REGISTER -> RegisterScreen(viewModel = viewModel)
    ScreenDestination.AUTH_OTP -> OtpVerificationScreen(viewModel = viewModel)
    ScreenDestination.AUTH_FORGOT_PASSWORD -> ForgotPasswordScreen(viewModel = viewModel)
    ScreenDestination.HOME -> HomeScreen(viewModel = viewModel)
    ScreenDestination.CHAT_DETAIL -> ChatDetailScreen(viewModel = viewModel)
    ScreenDestination.GROUP_INFO -> GroupInfoScreen(viewModel = viewModel)
    ScreenDestination.CREATE_GROUP -> CreateGroupScreen(viewModel = viewModel)
    ScreenDestination.ADD_CONTACT -> AddContactScreen(viewModel = viewModel)
    ScreenDestination.CREATE_STATUS -> CreateStatusScreen(viewModel = viewModel)
    ScreenDestination.STATUS_VIEWER -> StatusViewerScreen(viewModel = viewModel)
    ScreenDestination.PROFILE -> ProfileScreen(viewModel = viewModel)
    ScreenDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
    ScreenDestination.SETTINGS_PRIVACY -> PrivacySettingsScreen(viewModel = viewModel)
    ScreenDestination.SETTINGS_WALLPAPER -> WallpaperSettingsScreen(viewModel = viewModel)
  }

  // Active call overlay
  if (activeCall != null) {
    ActiveCallScreen(viewModel = viewModel)
  }

  // Incoming call modal dialog
  if (incomingCall != null) {
    IncomingCallDialog(viewModel = viewModel)
  }
}
