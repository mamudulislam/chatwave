package com.example.chatwave.ui.screens.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel

@Composable
fun AddContactScreen(vm: ChatWaveViewModel) {
  var id by remember { mutableStateOf("") }
  val loading by vm.loading.collectAsState()
  val error by vm.error.collectAsState()
  val notice by vm.notice.collectAsState()
  Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Add Contact", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(id, { id = it.uppercase() }, label = { Text("ChatWave ID (CW-XXXXXXX)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Button(onClick = { vm.addContact(id) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Looking up..." else "Lookup & Add") }
    OutlinedButton(onClick = { vm.navigate(ScreenDestination.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    if (!error.isNullOrBlank()) Text(error!!, color = MaterialTheme.colorScheme.error)
    if (!notice.isNullOrBlank()) Text(notice!!, color = MaterialTheme.colorScheme.primary)
  }
}

@Composable
fun InviteContactScreen(vm: ChatWaveViewModel) {
  var code by remember { mutableStateOf("") }
  val loading by vm.loading.collectAsState()
  val error by vm.error.collectAsState()
  val notice by vm.notice.collectAsState()
  Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Invite Contact", style = MaterialTheme.typography.headlineSmall)
    Button(onClick = { vm.createInvitation() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Create invitation code") }
    OutlinedTextField(code, { code = it }, label = { Text("Redeem invitation code") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Button(onClick = { vm.redeemInvitation(code) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Redeem code") }
    OutlinedButton(onClick = { vm.navigate(ScreenDestination.INCOMING_INVITES) }, modifier = Modifier.fillMaxWidth()) { Text("View invites") }
    OutlinedButton(onClick = { vm.navigate(ScreenDestination.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    if (!error.isNullOrBlank()) Text(error!!, color = MaterialTheme.colorScheme.error)
    if (!notice.isNullOrBlank()) Text(notice!!, color = MaterialTheme.colorScheme.primary)
  }
}

@Composable
fun IncomingInvitesScreen(vm: ChatWaveViewModel) {
  val invites by vm.invites.collectAsState()
  Column(Modifier.fillMaxSize().padding(16.dp)) {
    Text("Incoming/Local Invites", style = MaterialTheme.typography.headlineSmall)
    LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(invites, key = { it.invitationId }) { inv ->
        Card(Modifier.fillMaxWidth()) {
          Column(Modifier.padding(12.dp)) {
            Text("Status: ${inv.status}")
            Text("Masked: ${inv.senderFingerprint}")
            Text("Created: ${inv.createdAt}", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
      if (invites.isEmpty()) item { Text("No invitations stored locally.") }
      item { OutlinedButton(onClick = { vm.navigate(ScreenDestination.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Back") } }
    }
  }
}
