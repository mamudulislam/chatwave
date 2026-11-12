package com.example.chatwave.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class RealAudioRecorder(private val context: Context) {

  private var mediaRecorder: MediaRecorder? = null
  private var mediaPlayer: MediaPlayer? = null
  private var startTime: Long = 0L

  var isRecording: Boolean = false
    private set

  var isPlaying: Boolean = false
    private set

  fun startRecording(file: File): Boolean {
    return try {
      stopPlayback()
      if (file.exists()) file.delete()

      mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setOutputFile(file.absolutePath)
        prepare()
        start()
      }
      startTime = System.currentTimeMillis()
      isRecording = true
      true
    } catch (e: Exception) {
      e.printStackTrace()
      isRecording = false
      mediaRecorder?.release()
      mediaRecorder = null
      false
    }
  }

  fun stopRecording(): Int {
    if (!isRecording) return 0
    return try {
      val durationSec = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(1)
      mediaRecorder?.apply {
        stop()
        release()
      }
      mediaRecorder = null
      isRecording = false
      durationSec
    } catch (e: Exception) {
      mediaRecorder?.release()
      mediaRecorder = null
      isRecording = false
      1
    }
  }

  fun playAudio(file: File, onCompletion: () -> Unit) {
    try {
      stopPlayback()
      if (!file.exists()) {
        onCompletion()
        return
      }
      mediaPlayer = MediaPlayer().apply {
        setDataSource(file.absolutePath)
        prepare()
        setOnCompletionListener {
          this@RealAudioRecorder.isPlaying = false
          onCompletion()
        }
        start()
      }
      this@RealAudioRecorder.isPlaying = true
    } catch (e: Exception) {
      e.printStackTrace()
      this@RealAudioRecorder.isPlaying = false
      onCompletion()
    }
  }

  fun stopPlayback() {
    try {
      mediaPlayer?.apply {
        if (isPlaying) stop()
        release()
      }
    } catch (e: Exception) {
      // ignore
    } finally {
      mediaPlayer = null
      isPlaying = false
    }
  }
}
