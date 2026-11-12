package com.example.chatwave.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatwave.ui.components.ChatWaveAvatar
import com.example.chatwave.ui.viewmodel.ChatWaveViewModel
import com.example.chatwave.ui.viewmodel.ScreenDestination
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal

@Composable
fun LoginScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  var phoneOrEmail by remember { mutableStateOf("+1 555 019 2834") }
  var password by remember { mutableStateOf("secret123") }
  var passwordVisible by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(WhatsAppTeal)
          .statusBarsPadding()
          .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Enter your phone number",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 24.dp)
        .verticalScroll(rememberScrollState())
        .testTag("login_screen"),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "ChatWave will verify your account. Carrier SMS charges may apply.",
        fontSize = 13.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
      )

      Spacer(modifier = Modifier.height(32.dp))

      OutlinedTextField(
        value = phoneOrEmail,
        onValueChange = {
          phoneOrEmail = it
          errorMessage = null
        },
        label = { Text("Phone number or email") },
        leadingIcon = {
          Icon(Icons.Default.Phone, contentDescription = null, tint = WhatsAppGreen)
        },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_phone_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = password,
        onValueChange = {
          password = it
          errorMessage = null
        },
        label = { Text("Password") },
        leadingIcon = {
          Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreen)
        },
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = if (passwordVisible) "Hide password" else "Show password"
            )
          }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_password_input")
      )

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = errorMessage ?: "",
          color = MaterialTheme.colorScheme.error,
          fontSize = 12.5.sp
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(onClick = { viewModel.navigateTo(ScreenDestination.AUTH_FORGOT_PASSWORD) }) {
          Text("Forgot password?", color = WhatsAppGreen, fontSize = 13.sp)
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Button(
        onClick = {
          val ok = viewModel.login(phoneOrEmail, password)
          if (!ok) {
            errorMessage = "Please enter a valid phone/email and password (min 4 chars)"
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("login_submit_button")
      ) {
        Text("Next", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = "Need an account? ",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 14.sp
        )
        Text(
          text = "Sign Up",
          color = WhatsAppGreen,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          modifier = Modifier
            .clickable { viewModel.navigateTo(ScreenDestination.AUTH_REGISTER) }
            .testTag("nav_to_register_button")
        )
      }

      Spacer(modifier = Modifier.height(36.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          Icons.Default.Security,
          contentDescription = null,
          modifier = Modifier.size(15.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Your local app data is encrypted on this device",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun RegisterScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  var name by remember { mutableStateOf("Taylor Morgan") }
  var email by remember { mutableStateOf("taylor@example.com") }
  var phone by remember { mutableStateOf("+1 555 942 1827") }
  var password by remember { mutableStateOf("securePass12") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(WhatsAppTeal)
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 14.dp)
      ) {
        IconButton(
          onClick = { viewModel.navigateTo(ScreenDestination.AUTH_LOGIN) },
          modifier = Modifier.align(Alignment.CenterStart)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
          text = "Profile Info",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Center)
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 24.dp)
        .verticalScroll(rememberScrollState())
        .testTag("register_screen"),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Please provide your name and an optional profile photo",
        fontSize = 13.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(24.dp))

      Box(contentAlignment = Alignment.BottomEnd) {
        ChatWaveAvatar(
          name = name.ifBlank { "User" },
          colorHex = "#00A884",
          size = 88
        )
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(WhatsAppGreen)
            .padding(6.dp),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = Color.White)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Your full name") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = WhatsAppGreen) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("register_name_input")
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Phone number") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WhatsAppGreen) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("register_phone_input")
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email address") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = WhatsAppGreen) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("register_email_input")
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreen) },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("register_password_input")
      )

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = errorMessage ?: "",
          color = MaterialTheme.colorScheme.error,
          fontSize = 12.5.sp
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      Button(
        onClick = {
          val ok = viewModel.register(name, email, phone, password)
          if (!ok) {
            errorMessage = "Please fill in all fields with valid information"
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("register_submit_button")
      ) {
        Text("Continue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = "Already have an account? ",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 14.sp
        )
        Text(
          text = "Log In",
          color = WhatsAppGreen,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          modifier = Modifier
            .clickable { viewModel.navigateTo(ScreenDestination.AUTH_LOGIN) }
            .testTag("nav_to_login_button")
        )
      }
    }
  }
}

@Composable
fun OtpVerificationScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  var otp by remember { mutableStateOf("7294") }
  var countdown by remember { mutableIntStateOf(45) }
  var error by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(WhatsAppTeal)
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 14.dp)
      ) {
        IconButton(
          onClick = { viewModel.navigateTo(ScreenDestination.AUTH_LOGIN) },
          modifier = Modifier.align(Alignment.CenterStart)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
          text = "Verify Code",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Center)
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 24.dp)
        .testTag("otp_screen"),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(28.dp))

      Text(
        text = "Enter 4-digit code sent to ${viewModel.pendingAuthPhone}",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(36.dp))

      OutlinedTextField(
        value = otp,
        onValueChange = {
          if (it.length <= 6) {
            otp = it
            error = null
          }
        },
        label = { Text("Verification Code") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
          .width(220.dp)
          .testTag("otp_input")
      )

      if (error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.5.sp)
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Resend SMS in 0:${if (countdown < 10) "0$countdown" else countdown}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp
      )

      Spacer(modifier = Modifier.height(36.dp))

      Button(
        onClick = {
          val success = viewModel.verifyOtp(otp)
          if (!success) error = "Invalid code. Please enter 4 or 6 digits."
        },
        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("otp_verify_button")
      ) {
        Text("Verify & Start Chatting", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun ForgotPasswordScreen(
  viewModel: ChatWaveViewModel,
  modifier: Modifier = Modifier
) {
  var contactInput by remember { mutableStateOf("user@chatwave.app") }
  var isSent by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(WhatsAppTeal)
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 14.dp)
      ) {
        IconButton(
          onClick = { viewModel.navigateTo(ScreenDestination.AUTH_LOGIN) },
          modifier = Modifier.align(Alignment.CenterStart)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
          text = "Reset Password",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Center)
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 24.dp)
        .testTag("forgot_password_screen"),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Enter your registered email or phone number. We'll send you a secure password reset link.",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(32.dp))

      OutlinedTextField(
        value = contactInput,
        onValueChange = { contactInput = it },
        label = { Text("Email or Phone") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = WhatsAppGreen) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(24.dp))

      if (isSent) {
        Surface(
          color = WhatsAppGreen.copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WhatsAppGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Reset link sent! Please check your messages.",
              color = WhatsAppGreen,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
        Spacer(modifier = Modifier.height(20.dp))
      }

      Button(
        onClick = { isSent = true },
        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
      ) {
        Text("Send Reset Link", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
