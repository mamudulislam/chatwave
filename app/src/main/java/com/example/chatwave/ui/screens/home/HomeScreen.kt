package com.example.chatwave.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatwave.data.model.Chat
import com.example.chatwave.data.model.MessageStatus
import com.example.chatwave.ui.components.ChatWaveAvatar
import com.example.chatwave.ui.components.MessageStatusTicks
import com.example.chatwave.ui.screens.calls.CallsTabContent
import com.example.chatwave.ui.screens.contacts.ContactsTabContent
import com.example.chatwave.ui.screens.status.StatusTabContent
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel
import com.example.chatwave.ui.viewmodel.HomeTab
import com.example.chatwave.ui.viewmodel.ScreenDestination
import com.example.ui.theme.WhatsAppFabBadge
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val homeTab by viewModel.homeTab.collectAsState()
  val chats by viewModel.chats.collectAsState()
  val typingStatus by viewModel.typingStatus.collectAsState()
  val isSearching by viewModel.isSearching.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()

  var menuExpanded by remember { mutableStateOf(false) }

  val filteredChats = remember(chats, searchQuery) {
    if (searchQuery.isBlank()) chats
    else chats.filter { it.name.contains(searchQuery, ignoreCase = true) || it.lastMessage?.text?.contains(searchQuery, ignoreCase = true) == true }
  }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(WhatsAppTeal)
          .statusBarsPadding()
      ) {
        if (isSearching) {
          // Search Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = { viewModel.toggleSearch() }) {
              Icon(Icons.Default.ArrowBack, contentDescription = "Close search", tint = Color.White)
            }
            TextField(
              value = searchQuery,
              onValueChange = { viewModel.setSearchQuery(it) },
              placeholder = { Text("Search chats...", color = Color.White.copy(alpha = 0.7f)) },
              colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("chat_search_field")
            )
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.setSearchQuery("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
              }
            }
          }
        } else {
          // Main Top App Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "ChatWave",
              color = Color.White,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { viewModel.toggleSearch() }, modifier = Modifier.testTag("search_icon_button")) {
              Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }

            Box {
              IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag("more_menu_button")) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
              }

              DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
              ) {
                DropdownMenuItem(
                  text = { Text("New group") },
                  leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    viewModel.navigateTo(ScreenDestination.CREATE_GROUP)
                  }
                )
                DropdownMenuItem(
                  text = { Text("New contact") },
                  leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    viewModel.navigateTo(ScreenDestination.ADD_CONTACT)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Simulate Incoming Call") },
                  leadingIcon = { Icon(Icons.Default.RingVolume, contentDescription = null, tint = WhatsAppGreen) },
                  onClick = {
                    menuExpanded = false
                    viewModel.triggerIncomingCallSimulation()
                  }
                )
                HorizontalDivider()
                DropdownMenuItem(
                  text = { Text("Settings") },
                  leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    viewModel.navigateTo(ScreenDestination.SETTINGS)
                  }
                )
              }
            }
          }
        }

        // WhatsApp Navigation Tabs
        TabRow(
          selectedTabIndex = homeTab.ordinal,
          containerColor = WhatsAppTeal,
          contentColor = Color.White,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[homeTab.ordinal]),
              height = 3.dp,
              color = Color.White
            )
          }
        ) {
          Tab(
            selected = homeTab == HomeTab.CHATS,
            onClick = { viewModel.setHomeTab(HomeTab.CHATS) },
            text = {
              val totalUnread = chats.sumOf { it.unreadCount }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CHATS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (totalUnread > 0) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Badge(containerColor = Color.White, contentColor = WhatsAppTeal) {
                    Text(totalUnread.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            },
            modifier = Modifier.testTag("tab_chats")
          )
          Tab(
            selected = homeTab == HomeTab.STATUS,
            onClick = { viewModel.setHomeTab(HomeTab.STATUS) },
            text = { Text("STATUS", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
            modifier = Modifier.testTag("tab_status")
          )
          Tab(
            selected = homeTab == HomeTab.CALLS,
            onClick = { viewModel.setHomeTab(HomeTab.CALLS) },
            text = { Text("CALLS", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
            modifier = Modifier.testTag("tab_calls")
          )
          Tab(
            selected = homeTab == HomeTab.CONTACTS,
            onClick = { viewModel.setHomeTab(HomeTab.CONTACTS) },
            text = { Text("CONTACTS", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
            modifier = Modifier.testTag("tab_contacts")
          )
        }
      }
    },
    floatingActionButton = {
      when (homeTab) {
        HomeTab.CHATS -> {
          FloatingActionButton(
            onClick = { viewModel.navigateTo(ScreenDestination.ADD_CONTACT) },
            containerColor = WhatsAppGreen,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.testTag("fab_new_chat")
          ) {
            Icon(Icons.Default.Chat, contentDescription = "New Chat")
          }
        }
        HomeTab.STATUS -> {
          Column(horizontalAlignment = Alignment.End) {
            SmallFloatingActionButton(
              onClick = { viewModel.navigateTo(ScreenDestination.CREATE_STATUS) },
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              shape = CircleShape,
              modifier = Modifier.padding(bottom = 12.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Text Status")
            }
            FloatingActionButton(
              onClick = { viewModel.navigateTo(ScreenDestination.CREATE_STATUS) },
              containerColor = WhatsAppGreen,
              contentColor = Color.White,
              shape = CircleShape,
              modifier = Modifier.testTag("fab_new_status")
            ) {
              Icon(Icons.Default.CameraAlt, contentDescription = "Add Status")
            }
          }
        }
        HomeTab.CALLS -> {
          FloatingActionButton(
            onClick = { viewModel.setHomeTab(HomeTab.CONTACTS) },
            containerColor = WhatsAppGreen,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.testTag("fab_new_call")
          ) {
            Icon(Icons.Default.Call, contentDescription = "New Call")
          }
        }
        HomeTab.CONTACTS -> {
          FloatingActionButton(
            onClick = { viewModel.navigateTo(ScreenDestination.ADD_CONTACT) },
            containerColor = WhatsAppGreen,
            contentColor = Color.White,
            shape = CircleShape
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (homeTab) {
        HomeTab.CHATS -> {
          if (filteredChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text("No conversations found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
              items(filteredChats, key = { it.id }) { chat ->
                ChatItemRow(
                  chat = chat,
                  isTyping = typingStatus[chat.id] != null,
                  onClick = { viewModel.openChat(chat) }
                )
                HorizontalDivider(
                  modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                  thickness = 0.5.dp,
                  color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )
              }
            }
          }
        }
        HomeTab.STATUS -> {
          StatusTabContent(viewModel = viewModel)
        }
        HomeTab.CALLS -> {
          CallsTabContent(viewModel = viewModel)
        }
        HomeTab.CONTACTS -> {
          ContactsTabContent(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun ChatItemRow(
  chat: Chat,
  isTyping: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
  val lastMsg = chat.lastMessage

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    ChatWaveAvatar(
      name = chat.name,
      colorHex = chat.avatarColorHex,
      size = 52,
      isOnline = chat.isOnline
    )

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = chat.name,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
        )

        if (lastMsg != null) {
          Text(
            text = timeFormat.format(Date(lastMsg.timestamp)),
            fontSize = 11.5.sp,
            color = if (chat.unreadCount > 0) WhatsAppGreen else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(3.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isTyping) {
          Text(
            text = "typing...",
            color = WhatsAppGreen,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
          )
        } else if (lastMsg != null) {
          if (lastMsg.senderId == "me") {
            MessageStatusTicks(status = lastMsg.status)
            Spacer(modifier = Modifier.width(4.dp))
          }
          Text(
            text = if (lastMsg.isDeletedForEveryone) "🚫 Message was deleted" else lastMsg.text,
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
        } else {
          Text(
            text = "Tap to start conversation",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
          )
        }

        if (chat.isPinned) {
          Icon(
            Icons.Default.PushPin,
            contentDescription = "Pinned",
            modifier = Modifier
              .size(14.dp)
              .padding(end = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (chat.unreadCount > 0) {
          Box(
            modifier = Modifier
              .size(20.dp)
              .clip(CircleShape)
              .background(WhatsAppFabBadge),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = chat.unreadCount.toString(),
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
