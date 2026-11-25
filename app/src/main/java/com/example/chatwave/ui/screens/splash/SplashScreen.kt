package com.example.chatwave.ui.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen() {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    Text("ChatWave", style = MaterialTheme.typography.headlineLarge)
    Text("Secure private calls", modifier = Modifier.padding(top = 8.dp, bottom = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    CircularProgressIndicator()
  }
}
