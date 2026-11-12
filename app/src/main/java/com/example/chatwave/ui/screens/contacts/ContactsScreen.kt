package com.example.chatwave.ui.screens.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatwave.data.model.User
import com.example.chatwave.ui.components.CallLauncherDialog
import com.example.chatwave.ui.components.ChatWaveAvatar
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel
import com.example.chatwave.ui.viewmodel.ScreenDestination
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal

@Composable
fun ContactsTabContent(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val contacts by viewModel.contacts.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var callTarget by remember { mutableStateOf<Pair<User, Boolean>?>(null) }

  val filteredContacts = remember(contacts, searchQuery) {
    if (searchQuery.isBlank()) contacts
    else contacts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery) }
  }

  Column(modifier = modifier.fillMaxSize()) {
    // Quick Search in tab
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search ${contacts.size} contacts...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WhatsAppGreen) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear")
          }
        }
      },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("contacts_search_input")
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      item {
        // New group action
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.navigateTo(ScreenDestination.CREATE_GROUP) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(WhatsAppGreen),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color.White)
          }
          Spacer(modifier = Modifier.width(14.dp))
          Text("New group", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // New contact action
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.navigateTo(ScreenDestination.ADD_CONTACT) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(WhatsAppGreen),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
          }
          Spacer(modifier = Modifier.width(14.dp))
          Text("New contact", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Text(
          text = "Contacts on ChatWave",
          fontWeight = FontWeight.Bold,
          fontSize = 13.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
      }

      items(filteredContacts, key = { it.id }) { contact ->
        ContactItemRow(
          contact = contact,
          onClick = { viewModel.startChatWithContact(contact) },
          onVoiceCall = { callTarget = Pair(contact, false) },
          onVideoCall = { callTarget = Pair(contact, true) }
        )
        HorizontalDivider(
          modifier = Modifier.padding(start = 74.dp, end = 16.dp),
          thickness = 0.5.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
      }
    }

    callTarget?.let { (contact, isVideo) ->
      CallLauncherDialog(
        peerName = contact.name,
        peerPhone = contact.phoneNumber,
        peerAvatarColor = contact.avatarColorHex,
        isVideo = isVideo,
        onDismiss = { callTarget = null },
        onStartInAppCall = {
          viewModel.startCallWithUser(contact, isVideo = isVideo)
        }
      )
    }
  }
}

@Composable
fun ContactItemRow(
  contact: User,
  onClick: () -> Unit,
  onVoiceCall: () -> Unit,
  onVideoCall: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    ChatWaveAvatar(
      name = contact.name,
      colorHex = contact.avatarColorHex,
      size = 46,
      isOnline = contact.isOnline
    )

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp)
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = contact.about,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )
    }

    IconButton(onClick = onVoiceCall) {
      Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = WhatsAppGreen)
    }

    IconButton(onClick = onVideoCall) {
      Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = WhatsAppGreen)
    }
  }
}

@Composable
fun AddContactScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var about by remember { mutableStateOf("Hey there! I am using ChatWave.") }

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
          text = "New Contact",
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
        .padding(24.dp)
        .testTag("add_contact_screen"),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Contact Name") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = WhatsAppGreen) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("contact_name_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Phone Number") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WhatsAppGreen) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = about,
        onValueChange = { about = it },
        label = { Text("About Bio") },
        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = WhatsAppGreen) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(32.dp))

      Button(
        onClick = {
          if (name.isNotBlank() && phone.isNotBlank()) {
            viewModel.addContact(name, phone, about)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_contact_button")
      ) {
        Text("Save & Message", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun CreateGroupScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val contacts by viewModel.contacts.collectAsState()
  var groupName by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  val selectedContactIds = remember { mutableStateListOf<String>() }

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
          text = "New Group",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Center)
        )
      }
    },
    floatingActionButton = {
      if (selectedContactIds.isNotEmpty() && groupName.isNotBlank()) {
        FloatingActionButton(
          onClick = {
            viewModel.createGroup(groupName, selectedContactIds.toList(), description)
          },
          containerColor = WhatsAppGreen,
          contentColor = Color.White,
          shape = CircleShape,
          modifier = Modifier.testTag("submit_create_group")
        ) {
          Icon(Icons.Default.Check, contentDescription = "Create Group")
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("create_group_screen")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
          value = groupName,
          onValueChange = { groupName = it },
          label = { Text("Group Subject") },
          placeholder = { Text("Type group subject here...") },
          leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = WhatsAppGreen) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("group_name_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Group Description (Optional)") },
          placeholder = { Text("What is this group about?") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }

      Text(
        text = "Select Participants (${selectedContactIds.size} selected)",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      )

      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(contacts, key = { it.id }) { contact ->
          val isSelected = selectedContactIds.contains(contact.id)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                if (isSelected) selectedContactIds.remove(contact.id)
                else selectedContactIds.add(contact.id)
              }
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            ChatWaveAvatar(name = contact.name, colorHex = contact.avatarColorHex, size = 44)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
              Text(contact.about, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Checkbox(
              checked = isSelected,
              onCheckedChange = { checked ->
                if (checked) selectedContactIds.add(contact.id)
                else selectedContactIds.remove(contact.id)
              },
              colors = CheckboxDefaults.colors(checkedColor = WhatsAppGreen)
            )
          }
          HorizontalDivider(modifier = Modifier.padding(start = 74.dp, end = 16.dp), thickness = 0.5.dp)
        }
      }
    }
  }
}
