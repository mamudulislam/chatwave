package com.example.chatwave.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.R
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val scale by infiniteTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "logoScale"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(WhatsAppTeal)
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Image(
        painter = painterResource(id = R.drawable.chatwave_logo_1794064134643),
        contentDescription = "ChatWave Logo",
        modifier = Modifier
          .size(110.dp)
          .scale(scale)
          .clip(RoundedCornerShape(26.dp))
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "ChatWave",
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 36.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "Local data encrypted",
          tint = WhatsAppGreen,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Encrypted on this device",
          color = Color.White.copy(alpha = 0.85f),
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Real-time • Voice • Video • Stories",
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 11.sp
      )
    }
  }
}
