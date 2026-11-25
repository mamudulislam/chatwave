package com.example.chatwave.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.chatwave.data.model.ScreenDestination
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel

@Composable
private fun ErrorNotice(vm: ChatWaveViewModel) {
  val error by vm.error.collectAsState()
  val notice by vm.notice.collectAsState()
  if (!error.isNullOrBlank()) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
  if (!notice.isNullOrBlank()) Text(notice!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun FirstLaunchScreen(vm: ChatWaveViewModel) {
  Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
    Text("ChatWave", style = MaterialTheme.typography.headlineLarge)
    Text("Private 1:1 audio/video calls. No cloud identity database.", modifier = Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(24.dp))
    Button(onClick = { vm.navigate(ScreenDestination.CREATE_ACCOUNT) }, modifier = Modifier.fillMaxWidth()) { Text("Create secure account") }
    ErrorNotice(vm)
  }
}

@Composable
fun CreateAccountScreen(vm: ChatWaveViewModel) {
  var displayName by remember { mutableStateOf("") }
  var pin by remember { mutableStateOf("") }
  var backendUrl by remember { mutableStateOf("http://10.0.2.2:8080") }
  val loading by vm.loading.collectAsState()
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
    Text("Create account", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(displayName, { displayName = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), singleLine = true)
    OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(12) }, label = { Text("Local PIN (4-12 digits)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true)
    OutlinedTextField(backendUrl, { backendUrl = it }, label = { Text("Self-hosted backend URL") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true)
    Spacer(Modifier.height(20.dp))
    Button(onClick = { vm.createAccount(displayName, pin, backendUrl) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Creating..." else "Create account") }
    OutlinedButton(onClick = { vm.navigate(ScreenDestination.FIRST_LAUNCH) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Back") }
    ErrorNotice(vm)
  }
}

@Composable
fun UnlockScreen(vm: ChatWaveViewModel) {
  var pin by remember { mutableStateOf("") }
  val loading by vm.loading.collectAsState()
  Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
    Text("Unlock ChatWave", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(12) }, label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 16.dp), singleLine = true)
    Button(onClick = { vm.unlock(pin) }, enabled = !loading, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text(if (loading) "Unlocking..." else "Unlock") }
    ErrorNotice(vm)
  }
}
