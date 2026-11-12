package com.example.chatwave.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreviewView(
  isFrontCamera: Boolean = true,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasCameraPermission = isGranted
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  if (hasCameraPermission) {
    AndroidView(
      factory = { ctx ->
        val previewView = PreviewView(ctx).apply {
          scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
          try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
              it.surfaceProvider = previewView.surfaceProvider
            }
            val cameraSelector = if (isFrontCamera) {
              CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
              CameraSelector.DEFAULT_BACK_CAMERA
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }, ContextCompat.getMainExecutor(ctx))
        previewView
      },
      update = { previewView ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
        cameraProviderFuture.addListener({
          try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
              it.surfaceProvider = previewView.surfaceProvider
            }
            val cameraSelector = if (isFrontCamera) {
              CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
              CameraSelector.DEFAULT_BACK_CAMERA
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }, ContextCompat.getMainExecutor(previewView.context))
      },
      modifier = modifier
    )
  } else {
    Box(
      modifier = modifier
        .background(Color.Black.copy(alpha = 0.85f)),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
      ) {
        Icon(
          Icons.Default.CameraAlt,
          contentDescription = null,
          tint = Color.White.copy(alpha = 0.8f),
          modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Camera access required",
          color = Color.White,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
          shape = RoundedCornerShape(16.dp)
        ) {
          Text("Allow Camera", fontSize = 12.sp)
        }
      }
    }
  }
}
