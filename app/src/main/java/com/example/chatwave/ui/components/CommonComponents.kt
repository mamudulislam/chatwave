package com.example.chatwave.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatwave.data.model.MessageReaction
import com.example.chatwave.data.model.MessageStatus
import com.example.ui.theme.*

fun parseHexColor(hexString: String, defaultColor: Color = WhatsAppTeal): Color {
  return try {
    val clean = hexString.removePrefix("#")
    if (clean.length == 6) {
      Color(android.graphics.Color.parseColor("#$clean"))
    } else {
      defaultColor
    }
  } catch (e: Exception) {
    defaultColor
  }
}

@Composable
fun ChatWaveAvatar(
  name: String,
  colorHex: String,
  modifier: Modifier = Modifier,
  size: Int = 48,
  isOnline: Boolean = false,
  hasUnseenStatus: Boolean = false,
  onClick: (() -> Unit)? = null
) {
  val avatarColor = remember(colorHex) { parseHexColor(colorHex) }
  val initials = remember(name) {
    name.split(" ")
      .filter { it.isNotBlank() }
      .take(2)
      .mapNotNull { it.firstOrNull()?.uppercase() }
      .joinToString("")
      .ifEmpty { "CW" }
  }

  Box(
    modifier = modifier
      .size(size.dp)
      .then(
        if (hasUnseenStatus) {
          Modifier
            .border(2.5.dp, WhatsAppGreen, CircleShape)
            .padding(2.5.dp)
        } else Modifier
      )
      .then(
        if (onClick != null) {
          Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
        } else Modifier
      ),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clip(CircleShape)
        .background(avatarColor),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = initials,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = (size * 0.38).sp
      )
    }

    if (isOnline) {
      Box(
        modifier = Modifier
          .size((size * 0.28).coerceAtLeast(10.0).dp)
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(Color.White)
          .padding(2.dp)
          .clip(CircleShape)
          .background(WhatsAppOnlineBadge)
      )
    }
  }
}

@Composable
fun MessageStatusTicks(
  status: MessageStatus,
  modifier: Modifier = Modifier
) {
  when (status) {
    MessageStatus.SENDING -> {
      Icon(
        imageVector = Icons.Default.Schedule,
        contentDescription = "Sending",
        modifier = modifier.size(13.dp),
        tint = WhatsAppTickGrey
      )
    }
    MessageStatus.SENT -> {
      Icon(
        imageVector = Icons.Default.Check,
        contentDescription = "Sent",
        modifier = modifier.size(14.dp),
        tint = WhatsAppTickGrey
      )
    }
    MessageStatus.DELIVERED -> {
      // Double grey ticks
      Box(modifier = modifier.width(18.dp), contentAlignment = Alignment.CenterStart) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
          tint = WhatsAppTickGrey
        )
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Delivered",
          modifier = Modifier
            .padding(start = 5.dp)
            .size(14.dp),
          tint = WhatsAppTickGrey
        )
      }
    }
    MessageStatus.READ -> {
      // Double blue ticks
      Box(modifier = modifier.width(18.dp), contentAlignment = Alignment.CenterStart) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
          tint = WhatsAppTickBlue
        )
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Read",
          modifier = Modifier
            .padding(start = 5.dp)
            .size(14.dp),
          tint = WhatsAppTickBlue
        )
      }
    }
  }
}

@Composable
fun ReactionsRow(
  reactions: List<MessageReaction>,
  modifier: Modifier = Modifier
) {
  if (reactions.isEmpty()) return

  val grouped = remember(reactions) {
    reactions.groupBy { it.emoji }.mapValues { it.value.size }
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    tonalElevation = 2.dp,
    shadowElevation = 2.dp,
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier.padding(top = 2.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      grouped.forEach { (emoji, count) ->
        Text(text = emoji, fontSize = 13.sp)
        if (count > 1) {
          Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun EncryptionBanner(
  modifier: Modifier = Modifier,
  text: String = "Messages and calls are end-to-end encrypted. No one outside of this chat, not even ChatWave, can read or listen to them."
) {
  Surface(
    color = Color(0xFFFBE5C8).copy(alpha = 0.85f),
    shape = RoundedCornerShape(8.dp),
    modifier = modifier
      .fillMaxWidth(0.92f)
      .padding(vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = "Encrypted",
        modifier = Modifier.size(13.dp),
        tint = Color(0xFF75511D)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = text,
        color = Color(0xFF5D4018),
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun VoiceWaveformView(
  isPlaying: Boolean,
  progress: Float,
  durationSec: Int,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Canvas(
      modifier = Modifier
        .weight(1f)
        .height(28.dp)
    ) {
      val barWidth = 3.dp.toPx()
      val barSpacing = 2.5.dp.toPx()
      val totalBars = (size.width / (barWidth + barSpacing)).toInt().coerceIn(15, 40)
      val activeBars = (totalBars * progress).toInt()

      for (i in 0 until totalBars) {
        val x = i * (barWidth + barSpacing)
        // Pseudo harmonic height
        val normalizedHeight = 0.25f + 0.75f * kotlin.math.abs(kotlin.math.sin(i * 0.45f)).toFloat()
        val barHeight = size.height * normalizedHeight
        val y = (size.height - barHeight) / 2

        val color = if (i <= activeBars) WhatsAppVoiceWaveform else Color.LightGray

        drawLine(
          color = color,
          start = Offset(x, y),
          end = Offset(x, y + barHeight),
          strokeWidth = barWidth
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = String.format("%02d:%02d", durationSec / 60, durationSec % 60),
      fontSize = 11.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
