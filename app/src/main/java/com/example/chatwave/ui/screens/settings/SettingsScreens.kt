package com.example.chatwave.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatwave.data.model.ScreenDestination
import com.example.chatwave.di.AppGraph
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel

@Composable
fun SettingsScreen(vm: ChatWaveViewModel) {
  val settings by vm.settings.collectAsState()
  var backendUrl by remember(AppGraph.backendApi.baseUrl) { mutableStateOf(AppGraph.backendApi.baseUrl) }
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Settings", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(backendUrl, { backendUrl = it }, label = { Text("Backend URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Button(onClick = { vm.saveBackendUrl(backendUrl) }, modifier = Modifier.fillMaxWidth()) { Text("Save backend URL") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Relay-only mode"); Switch(checked = settings.relayOnly, onCheckedChange = vm::setRelayOnly)
    }
    OutlinedButton(onClick = { vm.navigate(ScreenDestination.DEVICE_SECURITY) }, modifier = Modifier.fillMaxWidth()) { Text("Device & Security") }
    OutlinedButton(onClick = { vm.navigate(ScreenDestination.DIAGNOSTICS) }, modifier = Modifier.fillMaxWidth()) { Text("Diagnostics") }
    OutlinedButton(onClick = { vm.navigate(ScreenDestination.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
  }
}

@Composable
fun DeviceSecurityScreen(vm: ChatWaveViewModel) {
  val profile by vm.profile.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Device & Security", style = MaterialTheme.typography.headlineSmall)
    Text("ChatWave ID: ${profile.chatwaveId}")
    Text("Device ID: ${profile.deviceId}")
    Text("Fingerprint: ${profile.fingerprint}")
    Text("Key version: ${profile.publicKeyVersion}")
    Text("Biometric: ${if (profile.biometricEnabled) "enabled" else "not enabled"}")
    Button(onClick = { vm.lock() }, modifier = Modifier.fillMaxWidth()) { Text("Lock now") }
    OutlinedButton(onClick = { vm.wipeLocalData() }, modifier = Modifier.fillMaxWidth()) { Text("Wipe local data") }
    OutlinedButton(onClick = { vm.navigate(ScreenDestination.SETTINGS) }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
  }
}

@Composable
fun DiagnosticsScreen(vm: ChatWaveViewModel) {
  val diag by vm.diagnostics.collectAsState()
  Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Diagnostics", style = MaterialTheme.typography.headlineSmall)
    Text("Backend: ${diag.backendUrl}")
    Text("Signaling connected: ${diag.signalingConnected}")
    Text("TURN configured: ${diag.turnConfigured}")
    Text("Relay-only: ${diag.relayOnly}")
    Text("Key version: ${diag.keyVersion}")
    Text("Local fingerprint: ${diag.localFingerprint}")
    Text("Last error: ${diag.lastError ?: "none"}")
    OutlinedButton(onClick = { vm.navigate(ScreenDestination.SETTINGS) }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
  }
}
