package com.example.chatwave.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import com.example.chatwave.data.model.*
import com.example.chatwave.ui.components.*
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel
import com.example.chatwave.ui.viewmodel.ScreenDestination
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val selectedChat by viewModel.selectedChat.collectAsState()
  val chat = selectedChat ?: run {
    viewModel.navigateTo(ScreenDestination.HOME)
    return
  }

  val messagesMap by viewModel.messagesMap.collectAsState()
  val messages = messagesMap[chat.id] ?: emptyList()
  val typingStatus by viewModel.typingStatus.collectAsState()
  val isPeerTyping = typingStatus[chat.id] != null
  val peerTypingText = typingStatus[chat.id]

  val replyingTo by viewModel.replyingToMessage.collectAsState()
  val editingMessage by viewModel.editingMessage.collectAsState()
  val selectedMessage by viewModel.selectedMessage.collectAsState()
  val settings by viewModel.settings.collectAsState()

  var inputText by remember { mutableStateOf("") }
  var showAttachmentSheet by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var menuExpanded by remember { mutableStateOf(false) }
  var showCallLauncher by remember { mutableStateOf<Boolean?>(null) }

  // Sync editing text
  LaunchedEffect(editingMessage) {
    if (editingMessage != null) {
      inputText = editingMessage?.text ?: ""
    }
  }

  val listState = rememberLazyListState()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

  // Wallpaper background
  val wallpaperColor = if (settings.chatWallpaperColorHex != null) {
    parseHexColor(settings.chatWallpaperColorHex!!, MaterialTheme.colorScheme.background)
  } else {
    if (MaterialTheme.colorScheme.background == ChatWaveDarkBackground) ChatWaveDarkChatBackground else ChatWaveLightChatBackground
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = WhatsAppTeal,
          titleContentColor = Color.White,
          actionIconContentColor = Color.White
        ),
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clickable {
                if (chat.isGroup) viewModel.navigateTo(ScreenDestination.GROUP_INFO)
              }
              .fillMaxWidth()
          ) {
            ChatWaveAvatar(
              name = chat.name,
              colorHex = chat.avatarColorHex,
              size = 40,
              isOnline = chat.isOnline
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
              Text(
                text = chat.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = if (isPeerTyping) peerTypingText ?: "typing..." else if (chat.isOnline) "Online" else chat.lastSeenText.ifEmpty { "tap for info" },
                fontSize = 11.5.sp,
                color = if (isPeerTyping) WhatsAppOnlineBadge else Color.White.copy(alpha = 0.85f),
                maxLines = 1
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateTo(ScreenDestination.HOME) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        actions = {
          IconButton(onClick = { showCallLauncher = true }, modifier = Modifier.testTag("chat_video_call_button")) {
            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
          }
          IconButton(onClick = { showCallLauncher = false }, modifier = Modifier.testTag("chat_voice_call_button")) {
            Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color.White)
          }
          Box {
            IconButton(onClick = { menuExpanded = true }) {
              Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
              if (chat.isGroup) {
                DropdownMenuItem(
                  text = { Text("Group info") },
                  onClick = {
                    menuExpanded = false
                    viewModel.navigateTo(ScreenDestination.GROUP_INFO)
                  }
                )
              }
              DropdownMenuItem(
                text = { Text("Change wallpaper") },
                onClick = {
                  menuExpanded = false
                  viewModel.navigateTo(ScreenDestination.SETTINGS_WALLPAPER)
                }
              )
              DropdownMenuItem(
                text = { Text("Simulate peer reply") },
                onClick = {
                  menuExpanded = false
                  viewModel.sendMessage("Hey! Are you free right now?")
                }
              )
              HorizontalDivider()
              DropdownMenuItem(
                text = { Text("Clear chat") },
                onClick = {
                  menuExpanded = false
                  viewModel.repository.deleteMessageForMe(chat.id, "")
                }
              )
            }
          }
        }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(wallpaperColor)
        .padding(innerPadding)
        .testTag("chat_detail_screen")
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Message List
        LazyColumn(
          state = listState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
          contentPadding = PaddingValues(vertical = 8.dp)
        ) {
          // Encryption Banner
          item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
              EncryptionBanner()
            }
          }

          // Date Pill
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text(
                  text = "TODAY",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }
          }

          items(messages, key = { it.id }) { msg ->
            val isMe = msg.senderId == "me"
            ChatBubble(
              message = msg,
              isMe = isMe,
              isGroup = chat.isGroup,
              onLongClick = { viewModel.setSelectedMessage(msg) },
              onReply = { viewModel.setReplyingTo(msg) }
            )
          }
        }

        // Selected Message Action Overlay (Reactions & Actions)
        if (selectedMessage != null) {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              // Quick Emoji Reactions
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
                  Text(
                    text = emoji,
                    fontSize = 24.sp,
                    modifier = Modifier
                      .clickable { viewModel.reactToSelectedMessage(emoji) }
                      .padding(6.dp)
                  )
                }
              }

              HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

              // Actions: Reply, Edit, Delete, Cancel
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
              ) {
                TextButton(onClick = { viewModel.setReplyingTo(selectedMessage) }) {
                  Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = WhatsAppGreen)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Reply", color = WhatsAppGreen)
                }

                if (selectedMessage?.senderId == "me" && selectedMessage?.isDeletedForEveryone == false) {
                  TextButton(onClick = { viewModel.setEditingMessage(selectedMessage) }) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = WhatsAppGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = WhatsAppGreen)
                  }

                  TextButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = Color(0xFFE53935))
                  }
                }

                TextButton(onClick = { viewModel.setSelectedMessage(null) }) {
                  Text("Close")
                }
              }
            }
          }
        }

        // Reply / Edit Banner above input
        if (replyingTo != null) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .width(4.dp)
                  .height(36.dp)
                  .background(WhatsAppGreen, RoundedCornerShape(2.dp))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = replyingTo?.senderName ?: "",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = WhatsAppGreen
                )
                Text(
                  text = replyingTo?.text ?: "",
                  fontSize = 12.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              IconButton(onClick = { viewModel.cancelReply() }) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        if (editingMessage != null) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, tint = WhatsAppGreen)
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = "Editing message", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
              IconButton(onClick = { viewModel.cancelEditing() }) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        // WhatsApp Chat Input Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .navigationBarsPadding(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Input pill
          Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.weight(1f)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              IconButton(onClick = { inputText += "😊" }) {
                Icon(
                  Icons.Outlined.SentimentSatisfiedAlt,
                  contentDescription = "Emojis",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message", fontSize = 15.sp) },
                colors = TextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  focusedIndicatorColor = Color.Transparent,
                  unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 4,
                modifier = Modifier
                  .weight(1f)
                  .testTag("chat_input_text_field")
              )

              IconButton(onClick = { showAttachmentSheet = true }) {
                Icon(
                  Icons.Default.AttachFile,
                  contentDescription = "Attach",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              if (inputText.isBlank()) {
                IconButton(onClick = {
                  viewModel.sendMessage("📸 Photo", type = MessageType.IMAGE, mediaCaption = "Photo attachment")
                }) {
                  Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          // FAB: Mic or Send
          FloatingActionButton(
            onClick = {
              if (inputText.isNotBlank()) {
                viewModel.sendMessage(inputText)
                inputText = ""
              } else {
                // Send voice message note
                viewModel.sendMessage("Voice note (0:14)", type = MessageType.AUDIO, audioDurationSec = 14)
              }
            },
            containerColor = WhatsAppGreen,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
              .size(48.dp)
              .testTag("send_message_button")
          ) {
            Icon(
              imageVector = if (inputText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
              contentDescription = if (inputText.isNotBlank()) "Send" else "Voice Note",
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }
  }

  // Delete Dialog (Delete for me vs Delete for everyone)
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete message?") },
      text = { Text("You can delete this message for everyone or just for yourself.") },
      confirmButton = {
        TextButton(onClick = {
          showDeleteDialog = false
          viewModel.deleteSelectedMessage(forEveryone = true)
        }) {
          Text("Delete for everyone", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Row {
          TextButton(onClick = {
            showDeleteDialog = false
            viewModel.deleteSelectedMessage(forEveryone = false)
          }) {
            Text("Delete for me")
          }
          TextButton(onClick = { showDeleteDialog = false }) {
            Text("Cancel")
          }
        }
      }
    )
  }

  // Attachment Bottom Sheet
  if (showAttachmentSheet) {
    ModalBottomSheet(
      onDismissRequest = { showAttachmentSheet = false }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp)
      ) {
        Text("Share content", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
          AttachmentIcon(
            icon = Icons.Default.Description,
            label = "Document",
            color = Color(0xFF5F66CD),
            onClick = {
              showAttachmentSheet = false
              viewModel.sendMessage("Annual_Report_2026.pdf (2.4 MB)", type = MessageType.DOCUMENT)
            }
          )
          AttachmentIcon(
            icon = Icons.Default.CameraAlt,
            label = "Camera",
            color = Color(0xFFD3396D),
            onClick = {
              showAttachmentSheet = false
              viewModel.sendMessage("Live Camera Capture 📸", type = MessageType.IMAGE)
            }
          )
          AttachmentIcon(
            icon = Icons.Default.Image,
            label = "Gallery",
            color = Color(0xFFAC44CF),
            onClick = {
              showAttachmentSheet = false
              viewModel.sendMessage("Photo from Gallery 🖼️", type = MessageType.IMAGE)
            }
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
          AttachmentIcon(
            icon = Icons.Default.Headphones,
            label = "Audio",
            color = Color(0xFFE05B27),
            onClick = {
              showAttachmentSheet = false
              viewModel.sendMessage("Audio Track 🎵 (3:20)", type = MessageType.AUDIO, audioDurationSec = 200)
            }
          )
          AttachmentIcon(
            icon = Icons.Default.LocationOn,
            label = "Location",
            color = Color(0xFF1F9E56),
            onClick = {
              showAttachmentSheet = false
              viewModel.sendMessage("📍 Shared live location (Silicon Valley Tech Park)")
            }
          )
          AttachmentIcon(
            icon = Icons.Default.Person,
            label = "Contact",
            color = Color(0xFF0097A7),
            onClick = {
              showAttachmentSheet = false
              viewModel.sendMessage("👤 Contact card: Sarah Connor (+1 555 918 3320)")
            }
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  showCallLauncher?.let { isVideo ->
    val contact = viewModel.contacts.value.find { it.name == chat.name || chat.participantIds.contains(it.id) }
    val phone = contact?.phoneNumber ?: "+1 555 019 2834"
    CallLauncherDialog(
      peerName = chat.name,
      peerPhone = phone,
      peerAvatarColor = chat.avatarColorHex,
      isVideo = isVideo,
      onDismiss = { showCallLauncher = null },
      onStartInAppCall = {
        viewModel.startCallFromCurrentChat(isVideo = isVideo)
      }
    )
  }
}

@Composable
fun AttachmentIcon(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  color: Color,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .size(54.dp)
        .clip(CircleShape)
        .background(color),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun ChatBubble(
  message: Message,
  isMe: Boolean,
  isGroup: Boolean,
  onLongClick: () -> Unit,
  onReply: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background == ChatWaveDarkBackground
  val bubbleColor = if (isMe) {
    if (isDark) ChatWaveDarkBubbleOutgoing else ChatWaveLightBubbleOutgoing
  } else {
    if (isDark) ChatWaveDarkBubbleIncoming else ChatWaveLightBubbleIncoming
  }

  val bubbleShape = if (isMe) {
    RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
  } else {
    RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
  }

  val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
  ) {
    Surface(
      shape = bubbleShape,
      color = bubbleColor,
      shadowElevation = 1.dp,
      modifier = Modifier
        .widthIn(min = 60.dp, max = 300.dp)
        .clickable(onClick = onLongClick)
    ) {
      Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
        // Group sender name
        if (!isMe && isGroup) {
          Text(
            text = message.senderName,
            color = parseHexColor("#00A884"),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 2.dp)
          )
        }

        // Quoted Reply preview if any
        if (message.replyToText != null) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 4.dp)
          ) {
            Row(modifier = Modifier.padding(4.dp)) {
              Box(
                modifier = Modifier
                  .width(3.dp)
                  .height(30.dp)
                  .background(WhatsAppGreen, RoundedCornerShape(2.dp))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  text = message.replyToSenderName ?: "",
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = WhatsAppGreen
                )
                Text(
                  text = message.replyToText,
                  fontSize = 11.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

        // Message Content based on type
        when (message.type) {
          MessageType.TEXT -> {
            Text(
              text = message.text,
              fontSize = 14.5.sp,
              lineHeight = 19.sp,
              color = if (message.isDeletedForEveryone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
          }
          MessageType.IMAGE -> {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color.DarkGray,
              modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
            ) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(message.text, color = Color.White, fontSize = 12.sp)
                }
              }
            }
          }
          MessageType.AUDIO -> {
            var isPlaying by remember { mutableStateOf(false) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(WhatsAppGreen)
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = "Play voice note",
                  tint = Color.White
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              VoiceWaveformView(
                isPlaying = isPlaying,
                progress = if (isPlaying) 0.65f else 0.15f,
                durationSec = message.audioDurationSec.coerceAtLeast(14),
                modifier = Modifier.weight(1f)
              )
            }
          }
          MessageType.DOCUMENT -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(8.dp)
            ) {
              Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(30.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(message.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
              Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          MessageType.VIDEO -> {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color.Black,
              modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
            ) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayCircle, contentDescription = "Play video", tint = Color.White, modifier = Modifier.size(48.dp))
              }
            }
          }
        }

        // Timestamp & Status Ticks
        Row(
          modifier = Modifier
            .align(Alignment.End)
            .padding(top = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (message.isEdited) {
            Text(
              text = "edited ",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }

          Text(
            text = timeFormat.format(Date(message.timestamp)),
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (isMe) {
            Spacer(modifier = Modifier.width(4.dp))
            MessageStatusTicks(status = message.status)
          }
        }
      }
    }

    // Reactions pill below bubble
    if (message.reactions.isNotEmpty()) {
      ReactionsRow(
        reactions = message.reactions,
        modifier = Modifier.padding(start = if (isMe) 0.dp else 8.dp, end = if (isMe) 8.dp else 0.dp)
      )
    }
  }
}

@Composable
fun GroupInfoScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val selectedChat by viewModel.selectedChat.collectAsState()
  val chat = selectedChat ?: return
  val contacts by viewModel.contacts.collectAsState()

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
          onClick = { viewModel.navigateTo(ScreenDestination.CHAT_DETAIL) },
          modifier = Modifier.align(Alignment.CenterStart)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
          text = "Group info",
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
    ) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          ChatWaveAvatar(name = chat.name, colorHex = chat.avatarColorHex, size = 100)
          Spacer(modifier = Modifier.height(14.dp))
          Text(chat.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Text("Group • ${chat.participantIds.size} participants", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }

        HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)

        // Description
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Group description", color = WhatsAppGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Text(chat.groupDescription.ifEmpty { "No description added yet." }, fontSize = 14.sp)
        }

        HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)

        Text(
          text = "${chat.participantIds.size} participants",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
      }

      items(contacts.filter { chat.participantIds.contains(it.id) }) { participant ->
        val isAdmin = chat.groupAdminIds.contains(participant.id)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ChatWaveAvatar(name = participant.name, colorHex = participant.avatarColorHex, size = 42)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(participant.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(participant.about, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
          }
          if (isAdmin) {
            Surface(
              color = WhatsAppGreen.copy(alpha = 0.15f),
              shape = RoundedCornerShape(4.dp)
            ) {
              Text("Group Admin", color = WhatsAppGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
          }
        }
      }

      item {
        HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        TextButton(
          onClick = { viewModel.navigateTo(ScreenDestination.HOME) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
        ) {
          Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFE53935))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Exit group", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }
    }
  }
}
