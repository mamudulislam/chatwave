package com.example.chatwave.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatwave.data.model.ScreenDestination
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel

@Composable
fun HomeScreen(vm: ChatWaveViewModel) {
  val profile by vm.profile.collectAsState()
  val contacts by vm.contacts.collectAsState()
  val error by vm.error.collectAsState()
  val notice by vm.notice.collectAsState()
  Column(Modifier.fillMaxSize().padding(16.dp)) {
    Text("ChatWave", style = MaterialTheme.typography.headlineSmall)
    Text("${profile.displayName} • ${profile.chatwaveId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Fingerprint: ${profile.fingerprint}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = { vm.navigate(ScreenDestination.ADD_CONTACT) }, modifier = Modifier.weight(1f)) { Text("Add") }
      OutlinedButton(onClick = { vm.navigate(ScreenDestination.INVITE_CONTACT) }, modifier = Modifier.weight(1f)) { Text("Invite") }
      OutlinedButton(onClick = { vm.navigate(ScreenDestination.CALL_HISTORY) }, modifier = Modifier.weight(1f)) { Text("History") }
      OutlinedButton(onClick = { vm.navigate(ScreenDestination.SETTINGS) }, modifier = Modifier.weight(1f)) { Text("Settings") }
    }
    if (!error.isNullOrBlank()) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    if (!notice.isNullOrBlank()) Text(notice!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(12.dp))
    Text("Contacts", style = MaterialTheme.typography.titleMedium)
    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(contacts, key = { it.chatwaveId }) { c ->
        Card(Modifier.fillMaxWidth()) {
          Column(Modifier.padding(12.dp)) {
            Text("${c.displayName} (${c.chatwaveId})")
            Text(if (c.verified) "Verified" else "Unverified - verify safety code", color = if (c.verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Text(c.fingerprint, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(onClick = { vm.startCall(c, false) }) { Text("Audio") }
              Button(onClick = { vm.startCall(c, true) }) { Text("Video") }
              OutlinedButton(onClick = { vm.verifyContact(c.chatwaveId) }) { Text("Verify") }
            }
          }
        }
      }
      if (contacts.isEmpty()) item { Text("No contacts yet. Add by ChatWave ID or invite.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
  }
}
