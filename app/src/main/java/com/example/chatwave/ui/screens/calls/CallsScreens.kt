package com.example.chatwave.ui.screens.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.chatwave.data.model.ScreenDestination
import com.example.chatwave.di.AppGraph
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallHistoryScreen(vm: ChatWaveViewModel) {
  val history by vm.history.collectAsState()
  Column(Modifier.fillMaxSize().padding(16.dp)) {
    Text("Call History", style = MaterialTheme.typography.headlineSmall)
    LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(history, key = { it.sessionId + it.startedAt }) { c ->
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
          Text("${c.peerName} (${c.peerId})")
          Text("${c.direction} ${c.callType} • ${c.status}")
          Text("Duration: ${c.durationSeconds}s", style = MaterialTheme.typography.bodySmall)
        } }
      }
      if (history.isEmpty()) item { Text("No calls yet.") }
      item { OutlinedButton(onClick = { vm.navigate(ScreenDestination.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Back") } }
    }
  }
}

@Composable
fun OutgoingCallScreen(vm: ChatWaveViewModel) {
  val call by vm.callState.collectAsState()
  Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Calling ${call.peerName}", style = MaterialTheme.typography.headlineSmall)
    Text(call.peerId, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(call.state, modifier = Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(24.dp))
    Button(onClick = { vm.endCall(call.sessionId) }) { Text("Cancel") }
  }
}

@Composable
fun IncomingCallScreen(vm: ChatWaveViewModel) {
  val incoming by vm.incomingCall.collectAsState()
  val event = incoming
  Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Incoming call", style = MaterialTheme.typography.headlineSmall)
    Text(event?.caller?.displayName ?: "Unknown", modifier = Modifier.padding(top = 8.dp))
    Text(event?.caller?.chatwaveId ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Safety code: ${event?.safetyCode ?: ""}", modifier = Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(onClick = { vm.acceptIncomingCall(event?.sessionId) }) { Text("Accept") }
      OutlinedButton(onClick = { vm.declineIncomingCall(event?.sessionId) }) { Text("Decline") }
    }
  }
}

@Composable
fun ActiveAudioCallScreen(vm: ChatWaveViewModel) {
  val call by vm.callState.collectAsState()
  Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(call.peerName, style = MaterialTheme.typography.headlineSmall)
    Text(call.peerId, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(call.state.uppercase(), modifier = Modifier.padding(top = 8.dp))
    Text("Duration: ${call.durationSeconds}s")
    Text("Safety: ${call.safetyCode}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = { vm.toggleMute() }) { Text(if (call.isMuted) "Unmute" else "Mute") }
      OutlinedButton(onClick = { vm.toggleSpeaker() }) { Text(if (call.isSpeaker) "Earpiece" else "Speaker") }
    }
    Spacer(Modifier.height(12.dp))
    Button(onClick = { vm.endCall(call.sessionId) }) { Text("End call") }
  }
}

@Composable
fun ActiveVideoCallScreen(vm: ChatWaveViewModel) {
  val call by vm.callState.collectAsState()
  val manager = AppGraph.webRtcCallManager
  Column(Modifier.fillMaxSize().background(Color.Black)) {
    Box(Modifier.weight(1f).fillMaxWidth()) {
      AndroidView(factory = { ctx -> SurfaceViewRenderer(ctx).also { manager.attachRemoteRenderer(it) } }, modifier = Modifier.fillMaxSize())
      AndroidView(factory = { ctx -> SurfaceViewRenderer(ctx).also { r -> r.setMirror(true); manager.attachLocalRenderer(r) } }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).height(160.dp).fillMaxWidth(0.33f))
    }
    Column(Modifier.fillMaxWidth().background(Color(0xAA000000)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(call.peerName, color = Color.White)
      Text(call.state, color = Color.White)
      Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { vm.toggleMute() }) { Text(if (call.isMuted) "Unmute" else "Mute") }
        OutlinedButton(onClick = { vm.toggleCamera() }) { Text(if (call.isCameraEnabled) "Camera off" else "Camera on") }
        OutlinedButton(onClick = { vm.switchCamera() }) { Text("Switch") }
      }
      Button(onClick = { vm.endCall(call.sessionId) }, modifier = Modifier.padding(top = 8.dp)) { Text("End") }
    }
  }
  DisposableEffect(Unit) { onDispose { manager.detachRenderers() } }
}

@Composable
fun CallSummaryScreen(vm: ChatWaveViewModel) {
  val call by vm.callState.collectAsState()
  Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Call summary", style = MaterialTheme.typography.headlineSmall)
    Text("${call.peerName} (${call.peerId})", modifier = Modifier.padding(top = 8.dp))
    Text("Status: ${call.state}")
    Text("Duration: ${call.durationSeconds}s")
    if (!call.error.isNullOrBlank()) Text(call.error!!, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(16.dp))
    Button(onClick = { vm.navigate(ScreenDestination.HOME) }) { Text("Back to Home") }
  }
}
