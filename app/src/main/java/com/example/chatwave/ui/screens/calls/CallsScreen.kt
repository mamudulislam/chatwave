package com.example.chatwave.ui.screens.calls

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chatwave.data.model.CallRecord
import com.example.chatwave.data.model.CallStatus
import com.example.chatwave.ui.components.CallLauncherDialog
import com.example.chatwave.ui.components.CameraPreviewView
import com.example.chatwave.ui.components.ChatWaveAvatar
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallsTabContent(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val callHistory by viewModel.callHistory.collectAsState()
  val dateFormat = remember { SimpleDateFormat("MMMM d, h:mm a", Locale.getDefault()) }

  var showCreateLinkDialog by remember { mutableStateOf(false) }
  var pendingCallPeer by remember { mutableStateOf<CallTarget?>(null) }

  if (callHistory.isEmpty()) {
    Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
          Icons.Default.Call,
          contentDescription = null,
          modifier = Modifier.size(64.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          "No call history yet",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          "Start a voice or video call with your contacts",
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          fontSize = 13.sp
        )
      }
    }
  } else {
    LazyColumn(modifier = modifier.fillMaxSize()) {
      item {
        // Create call link banner (WhatsApp style)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showCreateLinkDialog = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(CircleShape)
              .background(WhatsAppGreen),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Link, contentDescription = "Call Link", tint = Color.White)
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text("Create a call link", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
              "Share a link for your ChatWave real-time call",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Text(
          text = "Recent",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
      }

      items(callHistory, key = { it.id }) { call ->
        CallItemRow(
          call = call,
          onCallBack = {
            val contact = viewModel.contacts.value.find { it.id == call.peerId }
            pendingCallPeer = CallTarget(
              id = call.peerId,
              name = call.peerName,
              phone = contact?.phoneNumber ?: "+1 555 019 2834",
              avatarColor = call.peerAvatarColorHex,
              isVideo = call.isVideo
            )
          }
        )
        HorizontalDivider(
          modifier = Modifier.padding(start = 76.dp, end = 16.dp),
          thickness = 0.5.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
      }
    }
  }

  // Create Call Link Dialog
  if (showCreateLinkDialog) {
    val roomId = remember { "ChatWave-meet-" + (1000..9999).random() }
    val meetingUrl = "https://meet.jit.si/$roomId"

    Dialog(onDismissRequest = { showCreateLinkDialog = false }) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(WhatsAppGreen),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text("Real-Time Call Link", fontWeight = FontWeight.Bold, fontSize = 18.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            "Anyone with this link can join your call on any phone or browser.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )

          Spacer(modifier = Modifier.height(16.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = meetingUrl,
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Medium,
              color = WhatsAppGreen,
              modifier = Modifier.padding(12.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Buttons: Join Now, Share, Copy
          Button(
            onClick = {
              showCreateLinkDialog = false
              try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl))
                context.startActivity(intent)
              } catch (e: Exception) {
                Toast.makeText(context, "Error opening browser", Toast.LENGTH_SHORT).show()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Join Call Room Now", fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_TEXT, "Join my ChatWave video/voice call: $meetingUrl")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Call Link"))
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Share", fontSize = 12.5.sp)
            }

            OutlinedButton(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Call Link", meetingUrl))
                Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy", fontSize = 12.5.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          TextButton(onClick = { showCreateLinkDialog = false }) {
            Text("Close")
          }
        }
      }
    }
  }

  // Call Launcher Dialog (Cellular / WebRTC / In-App)
  pendingCallPeer?.let { target ->
    CallLauncherDialog(
      peerName = target.name,
      peerPhone = target.phone,
      peerAvatarColor = target.avatarColor,
      isVideo = target.isVideo,
      onDismiss = { pendingCallPeer = null },
      onStartInAppCall = {
        viewModel.repository.startCall(
          peerId = target.id,
          peerName = target.name,
          peerAvatarColor = target.avatarColor,
          isVideo = target.isVideo
        )
      }
    )
  }
}

data class CallTarget(
  val id: String,
  val name: String,
  val phone: String,
  val avatarColor: String,
  val isVideo: Boolean
)

@Composable
fun CallItemRow(
  call: CallRecord,
  onCallBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val dateFormat = remember { SimpleDateFormat("MMMM d, h:mm a", Locale.getDefault()) }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    ChatWaveAvatar(
      name = call.peerName,
      colorHex = call.peerAvatarColorHex,
      size = 50
    )

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = call.peerName,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = if (call.status == CallStatus.MISSED) WhatsAppCallMissed else MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(3.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        when (call.status) {
          CallStatus.MISSED, CallStatus.REJECTED -> {
            Icon(
              Icons.AutoMirrored.Filled.CallMissed,
              contentDescription = "Missed call",
              tint = WhatsAppCallMissed,
              modifier = Modifier.size(15.dp)
            )
          }
          CallStatus.INCOMING -> {
            Icon(
              Icons.AutoMirrored.Filled.CallReceived,
              contentDescription = "Incoming call",
              tint = WhatsAppCallIncoming,
              modifier = Modifier.size(15.dp)
            )
          }
          else -> {
            Icon(
              Icons.AutoMirrored.Filled.CallMade,
              contentDescription = "Outgoing call",
              tint = WhatsAppCallIncoming,
              modifier = Modifier.size(15.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
          text = dateFormat.format(Date(call.timestamp)),
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    IconButton(onClick = onCallBack) {
      Icon(
        imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
        contentDescription = "Call back",
        tint = WhatsAppGreen
      )
    }
  }
}

@Composable
fun ActiveCallScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activeCall by viewModel.activeCall.collectAsState()
  val call = activeCall ?: return

  var isFrontCamera by remember { mutableStateOf(true) }

  val formatSeconds = remember(call.durationSeconds) {
    val m = call.durationSeconds / 60
    val s = call.durationSeconds % 60
    String.format("%02d:%02d", m, s)
  }

  val webRtcUrl = "https://meet.jit.si/ChatWave-${call.peerName.filter { it.isLetterOrDigit() }}"

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("active_call_screen")
  ) {
    // Top Info
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Call preview • remote media not connected", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = call.peerName,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = if (call.isConnected) formatSeconds else "Ringing...",
        fontSize = 15.sp,
        color = Color.White.copy(alpha = 0.85f)
      )
    }

    // Video stream with live CameraX preview or Voice Avatar
    Box(
      modifier = Modifier
        .align(Alignment.Center)
        .padding(horizontal = 20.dp),
      contentAlignment = Alignment.Center
    ) {
      if (call.isVideo && call.isCameraOn) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = Color.Black,
          modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            // Real CameraX live video feed
            CameraPreviewView(
              isFrontCamera = isFrontCamera,
              modifier = Modifier.fillMaxSize()
            )

            // Flip camera button
            IconButton(
              onClick = { isFrontCamera = !isFrontCamera },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
            ) {
              Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip Camera", tint = Color.White)
            }

            // Peer indicator badge in bottom corner
            Box(
              modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                ChatWaveAvatar(name = call.peerName, colorHex = call.peerAvatarColorHex, size = 26)
                Spacer(modifier = Modifier.width(6.dp))
                Text(call.peerName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      } else {
        // Voice Calling Ripple Avatar
        val transition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by transition.animateFloat(
          initialValue = 1f,
          targetValue = 1.15f,
          animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
          ),
          label = "pulse"
        )
        Box(contentAlignment = Alignment.Center) {
          Box(
            modifier = Modifier
              .size(160.dp)
              .scale(pulseScale)
              .clip(CircleShape)
              .background(WhatsAppGreen.copy(alpha = 0.18f))
          )
          ChatWaveAvatar(
            name = call.peerName,
            colorHex = call.peerAvatarColorHex,
            size = 120
          )
        }
      }
    }

    // Bottom Action Controls Bar
    Surface(
      color = Color.Black.copy(alpha = 0.5f),
      shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Speaker toggle
        IconButton(
          onClick = { viewModel.toggleSpeaker() },
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (call.isSpeakerOn) Color.White else Color.White.copy(alpha = 0.2f))
        ) {
          Icon(
            imageVector = if (call.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
            contentDescription = "Speaker",
            tint = if (call.isSpeakerOn) Color.Black else Color.White
          )
        }

        // Camera toggle
        if (call.isVideo) {
          IconButton(
            onClick = { viewModel.toggleCamera() },
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(if (call.isCameraOn) Color.White else Color.White.copy(alpha = 0.2f))
          ) {
            Icon(
              imageVector = if (call.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
              contentDescription = "Toggle Video",
              tint = if (call.isCameraOn) Color.Black else Color.White
            )
          }
        }

        // Mute mic toggle
        IconButton(
          onClick = { viewModel.toggleMute() },
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (call.isMuted) Color.White else Color.White.copy(alpha = 0.2f))
        ) {
          Icon(
            imageVector = if (call.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = "Mute",
            tint = if (call.isMuted) Color.Black else Color.White
          )
        }

        // Share Call Link button
        IconButton(
          onClick = {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, "Join my live ChatWave call: $webRtcUrl")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Invite to Call"))
          },
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(WhatsAppGreen)
        ) {
          Icon(Icons.Default.Share, contentDescription = "Share Call Link", tint = Color.White)
        }

        // End Call button
        IconButton(
          onClick = { viewModel.endCall() },
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color(0xFFE53935))
            .testTag("end_call_button")
        ) {
          Icon(
            Icons.Default.CallEnd,
            contentDescription = "End Call",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
          )
        }
      }
    }
  }
}

@Composable
fun IncomingCallDialog(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  val incomingCall by viewModel.incomingCall.collectAsState()
  val call = incomingCall ?: return

  Dialog(
    onDismissRequest = { viewModel.rejectIncomingCall() },
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF075E54), Color(0xFF054C44), Color(0xFF022B27))
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding()
        .testTag("incoming_call_dialog"),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(top = 50.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ChatWave ${if (call.isVideo) "Video" else "Voice"} Call", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
          }

          Spacer(modifier = Modifier.height(24.dp))

          Text(
            text = call.peerName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Incoming...",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.85f)
          )
        }

        ChatWaveAvatar(
          name = call.peerName,
          colorHex = call.peerAvatarColorHex,
          size = 140
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 60.dp, start = 48.dp, end = 48.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Reject button
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { viewModel.rejectIncomingCall() },
              modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935))
                .testTag("reject_call_button")
            ) {
              Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Decline", color = Color.White, fontSize = 13.sp)
          }

          // Accept button
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { viewModel.acceptIncomingCall() },
              modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(WhatsAppGreen)
                .testTag("accept_call_button")
            ) {
              Icon(
                if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Accept",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Accept", color = Color.White, fontSize = 13.sp)
          }
        }
      }
    }
  }
}
