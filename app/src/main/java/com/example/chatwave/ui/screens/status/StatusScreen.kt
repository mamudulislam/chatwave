package com.example.chatwave.ui.screens.status

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatwave.data.model.UserStatusStory
import com.example.chatwave.ui.components.ChatWaveAvatar
import com.example.chatwave.ui.components.parseHexColor
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel
import com.example.chatwave.ui.viewmodel.ScreenDestination
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatusTabContent(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val userStories by viewModel.userStories.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val myStory = userStories.find { it.isCurrentUser }
  val contactStories = userStories.filterNot { it.isCurrentUser }
  val recentUpdates = contactStories.filter { it.hasUnseen }
  val viewedUpdates = contactStories.filter { !it.hasUnseen }

  val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

  LazyColumn(modifier = modifier.fillMaxSize()) {
    // My Status Item
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            if (myStory != null && myStory.stories.isNotEmpty()) {
              viewModel.openStory(myStory)
            } else {
              viewModel.navigateTo(ScreenDestination.CREATE_STATUS)
            }
          }
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(contentAlignment = Alignment.BottomEnd) {
          ChatWaveAvatar(
            name = currentUser?.name ?: "You",
            colorHex = currentUser?.avatarColorHex ?: "#00A884",
            size = 54,
            hasUnseenStatus = myStory?.hasUnseen == true
          )
          Box(
            modifier = Modifier
              .size(20.dp)
              .clip(CircleShape)
              .background(WhatsAppGreen)
              .padding(2.dp),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.Add,
              contentDescription = "Add status",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "My Status",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = if (myStory != null && myStory.stories.isNotEmpty()) {
              val latest = myStory.stories.first()
              "Today, ${timeFormat.format(Date(latest.timestamp))} • ${latest.viewers.size} views"
            } else {
              "Tap to add status update"
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (myStory != null && myStory.stories.isNotEmpty()) {
          IconButton(onClick = { viewModel.navigateTo(ScreenDestination.CREATE_STATUS) }) {
            Icon(Icons.Default.Edit, contentDescription = "New status", tint = WhatsAppGreen)
          }
        }
      }

      HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
      )
    }

    // Recent Updates
    if (recentUpdates.isNotEmpty()) {
      item {
        Text(
          text = "Recent updates",
          fontWeight = FontWeight.Bold,
          fontSize = 13.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
      }

      items(recentUpdates, key = { it.userId }) { story ->
        StatusItemRow(
          story = story,
          onClick = { viewModel.openStory(story) }
        )
      }
    }

    // Viewed Updates
    if (viewedUpdates.isNotEmpty()) {
      item {
        Text(
          text = "Viewed updates",
          fontWeight = FontWeight.Bold,
          fontSize = 13.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
      }

      items(viewedUpdates, key = { it.userId }) { story ->
        StatusItemRow(
          story = story,
          onClick = { viewModel.openStory(story) }
        )
      }
    }

    // Privacy security notice
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.Lock,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Your status updates are end-to-end encrypted",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun StatusItemRow(
  story: UserStatusStory,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    ChatWaveAvatar(
      name = story.userName,
      colorHex = story.userAvatarColorHex,
      size = 52,
      hasUnseenStatus = story.hasUnseen
    )

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = story.userName,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Today, ${timeFormat.format(Date(story.latestTimestamp))}",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun CreateStatusScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  var statusText by remember { mutableStateOf("") }
  val colorPalette = listOf("#075E54", "#2E7D32", "#1565C0", "#6A1B9A", "#C2185B", "#E65100", "#37474F")
  var selectedColorHex by remember { mutableStateOf(colorPalette[0]) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(parseHexColor(selectedColorHex))
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("create_status_screen")
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = { viewModel.navigateTo(ScreenDestination.HOME) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
      }

      // Color picker cycle button
      IconButton(onClick = {
        val currentIndex = colorPalette.indexOf(selectedColorHex)
        val nextIndex = (currentIndex + 1) % colorPalette.size
        selectedColorHex = colorPalette[nextIndex]
      }) {
        Icon(Icons.Default.Palette, contentDescription = "Change color", tint = Color.White)
      }
    }

    // Center input text
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 32.dp),
      contentAlignment = Alignment.Center
    ) {
      TextField(
        value = statusText,
        onValueChange = { if (it.length <= 280) statusText = it },
        placeholder = {
          Text(
            "Type a status...",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )
        },
        textStyle = LocalTextStyle.current.copy(
          color = Color.White,
          fontSize = 26.sp,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        ),
        colors = TextFieldDefaults.colors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          cursorColor = Color.White
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("status_text_field")
      )
    }

    // Bottom Action Row
    Row(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(24.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      FloatingActionButton(
        onClick = {
          if (statusText.isNotBlank()) {
            viewModel.postStatus(statusText, selectedColorHex)
          }
        },
        containerColor = WhatsAppGreen,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier.testTag("post_status_button")
      ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post Status")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusViewerScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val story by viewModel.selectedStory.collectAsState()
  val activeStory = story ?: run {
    viewModel.navigateTo(ScreenDestination.HOME)
    return
  }

  var currentIndex by remember { mutableIntStateOf(0) }
  val currentItem = activeStory.stories.getOrNull(currentIndex) ?: return
  var isPaused by remember { mutableStateOf(false) }
  var showViewersSheet by remember { mutableStateOf(false) }

  // Mark as viewed
  LaunchedEffect(currentItem.id) {
    viewModel.markStatusViewed(currentItem.id)
  }

  // Story Timer
  val progress = remember { Animatable(0f) }
  LaunchedEffect(currentIndex, isPaused) {
    if (!isPaused) {
      progress.animateTo(
        targetValue = 1f,
        animationSpec = tween(
          durationMillis = ((1f - progress.value) * 5000).toInt(),
          easing = LinearEasing
        )
      )
      if (currentIndex < activeStory.stories.size - 1) {
        progress.snapTo(0f)
        currentIndex++
      } else {
        viewModel.navigateTo(ScreenDestination.HOME)
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(parseHexColor(currentItem.backgroundColorHex))
      .statusBarsPadding()
      .navigationBarsPadding()
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            isPaused = true
            tryAwaitRelease()
            isPaused = false
          },
          onTap = { offset ->
            if (offset.x < size.width / 3f) {
              // Tap Left
              if (currentIndex > 0) {
                currentIndex--
                kotlinx.coroutines.runBlocking { progress.snapTo(0f) }
              }
            } else {
              // Tap Right
              if (currentIndex < activeStory.stories.size - 1) {
                currentIndex++
                kotlinx.coroutines.runBlocking { progress.snapTo(0f) }
              } else {
                viewModel.navigateTo(ScreenDestination.HOME)
              }
            }
          }
        )
      }
      .testTag("status_viewer_screen")
  ) {
    // Top Segments & Header
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      // Segments
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        activeStory.stories.forEachIndexed { idx, _ ->
          val segmentProgress = when {
            idx < currentIndex -> 1f
            idx == currentIndex -> progress.value
            else -> 0f
          }
          LinearProgressIndicator(
            progress = { segmentProgress },
            modifier = Modifier
              .weight(1f)
              .height(3.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.35f)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = { viewModel.navigateTo(ScreenDestination.HOME) }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        ChatWaveAvatar(
          name = activeStory.userName,
          colorHex = activeStory.userAvatarColorHex,
          size = 40
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = activeStory.userName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
          Text(
            text = "Today",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp
          )
        }
      }
    }

    // Story Text Content
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 32.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = currentItem.textContent,
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        lineHeight = 34.sp
      )
    }

    // Bottom Viewers trigger if my story
    if (activeStory.isCurrentUser) {
      Surface(
        color = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .clickable { showViewersSheet = true }
      ) {
        Row(
          modifier = Modifier.padding(vertical = 12.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Visibility, contentDescription = "Viewers", tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${currentItem.viewers.size} viewers",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }

  // Viewers Bottom Sheet
  if (showViewersSheet) {
    ModalBottomSheet(
      onDismissRequest = { showViewersSheet = false }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp)
      ) {
        Text(
          text = "Viewed by ${currentItem.viewers.size}",
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        if (currentItem.viewers.isEmpty()) {
          Text(
            text = "No views yet",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp)
          )
        } else {
          val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
          currentItem.viewers.forEach { viewer ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              ChatWaveAvatar(name = viewer.userName, colorHex = "#00A884", size = 42)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(viewer.userName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(timeFormat.format(Date(viewer.viewedAt)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  }
}
