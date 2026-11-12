package com.example.chatwave.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatwave.data.model.PrivacyAudience
import com.example.chatwave.data.model.ThemeMode
import com.example.chatwave.ui.components.ChatWaveAvatar
import com.example.chatwave.ui.components.parseHexColor
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel
import com.example.chatwave.ui.viewmodel.ScreenDestination
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal

@Composable
fun SettingsScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val currentUser by viewModel.currentUser.collectAsState()
  var showLogoutDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(WhatsAppTeal)
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 14.dp)
      ) {
        IconButton(
          onClick = { viewModel.navigateTo(ScreenDestination.HOME) },
          modifier = Modifier.align(Alignment.CenterStart)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
          text = "Settings",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Center)
        )
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("settings_screen")
    ) {
      // Profile Banner
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.navigateTo(ScreenDestination.PROFILE) }
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ChatWaveAvatar(
            name = currentUser?.name ?: "You",
            colorHex = currentUser?.avatarColorHex ?: "#00A884",
            size = 64
          )

          Spacer(modifier = Modifier.width(16.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = currentUser?.name ?: "User",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = currentUser?.about ?: "",
              fontSize = 13.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1
            )
          }

          IconButton(onClick = { viewModel.navigateTo(ScreenDestination.PROFILE) }) {
            Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = WhatsAppGreen)
          }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
      }

      // Settings Rows
      item {
        SettingsRowItem(
          icon = Icons.Default.Key,
          title = "Account",
          subtitle = "Security notifications, change number",
          onClick = { viewModel.navigateTo(ScreenDestination.SETTINGS_PRIVACY) }
        )

        SettingsRowItem(
          icon = Icons.Default.Lock,
          title = "Privacy",
          subtitle = "Block contacts, disappearing messages",
          onClick = { viewModel.navigateTo(ScreenDestination.SETTINGS_PRIVACY) }
        )

        SettingsRowItem(
          icon = Icons.Default.Chat,
          title = "Chats",
          subtitle = "Theme, wallpapers, chat history",
          onClick = { viewModel.navigateTo(ScreenDestination.SETTINGS_WALLPAPER) }
        )

        SettingsRowItem(
          icon = Icons.Default.Notifications,
          title = "Notifications",
          subtitle = "Message, group & call tones",
          onClick = { }
        )

        SettingsRowItem(
          icon = Icons.Default.DataUsage,
          title = "Storage and data",
          subtitle = "Network usage, auto-download",
          onClick = { }
        )

        SettingsRowItem(
          icon = Icons.Default.Help,
          title = "Help",
          subtitle = "Help center, contact us, privacy policy",
          onClick = { }
        )

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Log out
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showLogoutDialog = true }
            .padding(horizontal = 20.dp, vertical = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFE53935))
          Spacer(modifier = Modifier.width(20.dp))
          Text("Log out", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      }
    }
  }

  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = { Text("Log out of ChatWave?") },
      text = { Text("Your local encrypted database will remain safe and secure on device.") },
      confirmButton = {
        TextButton(onClick = {
          showLogoutDialog = false
          viewModel.logout()
        }) {
          Text("Log out", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun SettingsRowItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
    Spacer(modifier = Modifier.width(20.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
      Spacer(modifier = Modifier.height(2.dp))
      Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun ProfileScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val currentUser by viewModel.currentUser.collectAsState()
  var name by remember { mutableStateOf(currentUser?.name ?: "You") }
  var about by remember { mutableStateOf(currentUser?.about ?: "Living life one wave at a time 🌊") }
  val phone = currentUser?.phoneNumber ?: "+1 555 019 2834"
  var savedSuccess by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(WhatsAppTeal)
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 14.dp)
      ) {
        IconButton(
          onClick = { viewModel.navigateTo(ScreenDestination.SETTINGS) },
          modifier = Modifier.align(Alignment.CenterStart)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
          text = "Profile",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Center)
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 24.dp)
        .testTag("profile_screen"),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      Box(contentAlignment = Alignment.BottomEnd) {
        ChatWaveAvatar(name = name, colorHex = currentUser?.avatarColorHex ?: "#00A884", size = 110)
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(WhatsAppGreen)
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = "Change avatar", tint = Color.White)
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Name") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = WhatsAppGreen) },
        supportingText = { Text("This is not your username or pin. This name will be visible to your ChatWave contacts.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = about,
        onValueChange = { about = it },
        label = { Text("About") },
        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = WhatsAppGreen) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("profile_about_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = phone,
        onValueChange = {},
        readOnly = true,
        label = { Text("Phone") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WhatsAppGreen) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(28.dp))

      if (savedSuccess) {
        Text("Profile updated successfully!", color = WhatsAppGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
      }

      Button(
        onClick = {
          viewModel.updateProfile(name, about, phone)
          savedSuccess = true
        },
        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_profile_button")
      ) {
        Text("Save Changes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun PrivacySettingsScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val settings by viewModel.settings.collectAsState()
  val persistenceError by viewModel.persistenceError.collectAsState()

  Scaffold(
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(WhatsAppTeal)
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 14.dp)
      ) {
        IconButton(
          onClick = { viewModel.navigateTo(ScreenDestination.SETTINGS) },
          modifier = Modifier.align(Alignment.CenterStart)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
          text = "Privacy",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Center)
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("privacy_settings_screen")
    ) {
      Text(
        text = "Who can see my personal info",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = WhatsAppGreen,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
      )

      PrivacyOptionRow(
        title = "Last seen and online",
        value = when (settings.lastSeenPrivacy) {
          PrivacyAudience.EVERYONE -> "Everyone"
          PrivacyAudience.MY_CONTACTS -> "My contacts"
          PrivacyAudience.NOBODY -> "Nobody"
        },
        onClick = {
          val next = when (settings.lastSeenPrivacy) {
            PrivacyAudience.EVERYONE -> PrivacyAudience.MY_CONTACTS
            PrivacyAudience.MY_CONTACTS -> PrivacyAudience.NOBODY
            PrivacyAudience.NOBODY -> PrivacyAudience.EVERYONE
          }
          viewModel.updateSettings(settings.copy(lastSeenPrivacy = next))
        }
      )

      PrivacyOptionRow(
        title = "Status privacy",
        value = when (settings.statusPrivacy) {
          PrivacyAudience.EVERYONE -> "Everyone"
          PrivacyAudience.MY_CONTACTS -> "My contacts"
          PrivacyAudience.NOBODY -> "Nobody"
        },
        onClick = {
          val next = if (settings.statusPrivacy == PrivacyAudience.MY_CONTACTS) PrivacyAudience.EVERYONE else PrivacyAudience.MY_CONTACTS
          viewModel.updateSettings(settings.copy(statusPrivacy = next))
        }
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            viewModel.updateSettings(settings.copy(readReceiptsEnabled = !settings.readReceiptsEnabled))
          }
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Read receipts", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            "If turned off, you won't send or receive read receipts. Read receipts are always sent for group chats.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = settings.readReceiptsEnabled,
          onCheckedChange = { viewModel.updateSettings(settings.copy(readReceiptsEnabled = it)) },
          colors = SwitchDefaults.colors(checkedThumbColor = WhatsAppGreen)
        )
      }

      HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

      Text(
        text = "Security & Encryption",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = WhatsAppGreen,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text("On-device data encryption", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
          Text(
            persistenceError ?: "Active (AES-256-GCM; Android Keystore)",
            fontSize = 12.5.sp,
            color = if (persistenceError == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
          )
        }
      }
    }
  }
}

@Composable
fun PrivacyOptionRow(
  title: String,
  value: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Text(value, color = WhatsAppGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun WallpaperSettingsScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val settings by viewModel.settings.collectAsState()
  val wallpapers = listOf(
    null to "Classic Doodle",
    "#075E54" to "Dark Teal",
    "#111B21" to "Deep Charcoal",
    "#EFEAE2" to "Light Warm",
    "#E8F5E9" to "Soft Mint",
    "#E3F2FD" to "Sky Blue",
    "#F3E5F5" to "Lavender"
  )

  Scaffold(
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(WhatsAppTeal)
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 14.dp)
      ) {
        IconButton(
          onClick = { viewModel.navigateTo(ScreenDestination.SETTINGS) },
          modifier = Modifier.align(Alignment.CenterStart)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
          text = "Chat Wallpaper & Theme",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Center)
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp)
        .testTag("wallpaper_settings_screen")
    ) {
      Text(
        text = "Theme mode",
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = WhatsAppGreen
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        FilterChip(
          selected = settings.themeMode == ThemeMode.SYSTEM,
          onClick = { viewModel.updateSettings(settings.copy(themeMode = ThemeMode.SYSTEM)) },
          label = { Text("System") }
        )
        FilterChip(
          selected = settings.themeMode == ThemeMode.LIGHT,
          onClick = { viewModel.updateSettings(settings.copy(themeMode = ThemeMode.LIGHT)) },
          label = { Text("Light") }
        )
        FilterChip(
          selected = settings.themeMode == ThemeMode.DARK,
          onClick = { viewModel.updateSettings(settings.copy(themeMode = ThemeMode.DARK)) },
          label = { Text("Dark") }
        )
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

      Text(
        text = "Chat Wallpapers",
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = WhatsAppGreen,
        modifier = Modifier.padding(bottom = 12.dp)
      )

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(wallpapers) { (hex, name) ->
          val isSelected = settings.chatWallpaperColorHex == hex
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable {
              viewModel.updateSettings(settings.copy(chatWallpaperColorHex = hex))
            }
          ) {
            Box(
              modifier = Modifier
                .size(72.dp, 110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (hex != null) parseHexColor(hex) else Color(0xFFEFEAE2))
                .then(
                  if (isSelected) Modifier.padding(3.dp).clip(RoundedCornerShape(6.dp)) else Modifier
                ),
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = WhatsAppGreen)
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, fontSize = 11.5.sp)
          }
        }
      }
    }
  }
}
