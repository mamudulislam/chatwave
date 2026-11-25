package com.example.chatwave.calling

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface CallCommand {
  data class Accept(val sessionId: String) : CallCommand
  data class Decline(val sessionId: String) : CallCommand
  data class End(val sessionId: String) : CallCommand
}

object CallCommandBus {
  private val _commands = MutableSharedFlow<CallCommand>(extraBufferCapacity = 8)
  val commands = _commands.asSharedFlow()
  fun emit(command: CallCommand) { _commands.tryEmit(command) }
}
