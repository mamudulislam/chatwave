package com.example.chatwave.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal

@Composable
fun CallLauncherDialog(
  peerName: String,
  peerPhone: String,
  peerAvatarColor: String,
  isVideo: Boolean,
  onDismiss: () -> Unit,
  onStartInAppCall: () -> Unit
) {
  val context = LocalContext.current
  val cleanPhone = remember(peerPhone) {
    peerPhone.replace(" ", "").replace("-", "")
  }
  val roomId = remember(peerName) {
    "ChatWave-" + peerName.filter { it.isLetterOrDigit() } + "-" + (1000..9999).random()
  }
  val webRtcUrl = "https://meet.jit.si/$roomId"

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        ChatWaveAvatar(name = peerName, colorHex = peerAvatarColor, size = 60)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Call $peerName",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
        Text(
          text = peerPhone,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Option 1: Real Cellular Phone Call
        CallOptionItem(
          icon = Icons.Default.PhoneForwarded,
          iconBg = Color(0xFF1E88E5),
          title = "Dial Real Phone Number",
          subtitle = "Opens Android dialer to call $cleanPhone over cellular network",
          onClick = {
            onDismiss()
            try {
              val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
              context.startActivity(intent)
            } catch (e: Exception) {
              Toast.makeText(context, "Cannot open phone dialer: ${e.message}", Toast.LENGTH_SHORT).show()
            }
          }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 2: Real 2-Way WebRTC Video/Voice Meeting
        CallOptionItem(
          icon = Icons.Default.Language,
          iconBg = WhatsAppGreen,
          title = "Real 2-Way WebRTC Meeting",
          subtitle = "Live video room link you can share with anyone to connect in real time",
          onClick = {
            onDismiss()
            try {
              val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webRtcUrl))
              context.startActivity(browserIntent)
            } catch (e: Exception) {
              Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
            }
          }
        )

        // Share WebRTC Link button
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, top = 4.dp, bottom = 4.dp),
          horizontalArrangement = Arrangement.Start
        ) {
          TextButton(
            onClick = {
              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Join my ChatWave Call")
                putExtra(Intent.EXTRA_TEXT, "Join my real-time video/voice call on ChatWave: $webRtcUrl")
              }
              context.startActivity(Intent.createChooser(shareIntent, "Share Call Link to $peerName"))
            },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = WhatsAppGreen)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Share Link to Friend", fontSize = 12.sp, color = WhatsAppGreen)
          }

          TextButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("Call Link", webRtcUrl))
              Toast.makeText(context, "Call link copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = WhatsAppGreen)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy Link", fontSize = 12.sp, color = WhatsAppGreen)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Option 3: In-App Call with Real CameraX
        CallOptionItem(
          icon = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
          iconBg = WhatsAppTeal,
          title = if (isVideo) "In-App Video (CameraX)" else "In-App Voice Call",
          subtitle = "Activates device camera & microphone inside ChatWave",
          onClick = {
            onDismiss()
            onStartInAppCall()
          }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Cancel")
        }
      }
    }
  }
}

@Composable
fun CallOptionItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconBg: Color,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(iconBg),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
      Spacer(modifier = Modifier.height(2.dp))
      Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
    }
  }
}
