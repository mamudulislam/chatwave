package com.example.chatwave.data.security

import android.content.Context
import android.util.Log
import com.example.chatwave.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Local repository with encrypted persistence and reactive state streams.
 */
class EncryptedDataRepository(private val context: Context) {

  private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val encryptedStore = LocalEncryptedStore(context)
  private val snapshotAdapter = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
    .adapter(RepositorySnapshot::class.java)

  // Current Logged-in User
  private val _currentUser = MutableStateFlow<User?>(
    User(
      id = "me",
      name = "You",
      phoneNumber = "+1 555 019 2834",
      email = "user@chatwave.app",
      avatarColorHex = "#00A884",
      avatarInitials = "ME",
      about = "Living life one wave at a time 🌊",
      isOnline = true,
      isCurrent = true
    )
  )
  val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

  // Authentication State
  private val _isAuthenticated = MutableStateFlow(true)
  val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

  // Contacts
  private val _contacts = MutableStateFlow<List<User>>(emptyList())
  val contacts: StateFlow<List<User>> = _contacts.asStateFlow()

  // Chats
  private val _chats = MutableStateFlow<List<Chat>>(emptyList())
  val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

  // Messages map: ChatId -> List<Message>
  private val _messagesMap = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
  val messagesMap: StateFlow<Map<String, List<Message>>> = _messagesMap.asStateFlow()

  // Typing indicators: ChatId -> Name of person typing or null
  private val _typingStatus = MutableStateFlow<Map<String, String?>>(emptyMap())
  val typingStatus: StateFlow<Map<String, String?>> = _typingStatus.asStateFlow()

  // Call History
  private val _callHistory = MutableStateFlow<List<CallRecord>>(emptyList())
  val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

  // Active Call State
  private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
  val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()

  // Incoming Call State
  private val _incomingCall = MutableStateFlow<IncomingCallState?>(null)
  val incomingCall: StateFlow<IncomingCallState?> = _incomingCall.asStateFlow()

  // Status / Stories list
  private val _userStories = MutableStateFlow<List<UserStatusStory>>(emptyList())
  val userStories: StateFlow<List<UserStatusStory>> = _userStories.asStateFlow()

  // App Settings
  private val _settings = MutableStateFlow(SettingsState())
  val settings: StateFlow<SettingsState> = _settings.asStateFlow()

  private val _persistenceError = MutableStateFlow<String?>(null)
  val persistenceError: StateFlow<String?> = _persistenceError.asStateFlow()
  private var canPersist = true

  init {
    try {
      if (!restoreSnapshot()) seedInitialData()
    } catch (error: IOException) {
      handleUnreadableStore(error)
    } catch (error: GeneralSecurityException) {
      handleUnreadableStore(error)
    } catch (error: JsonDataException) {
      handleUnreadableStore(error)
    }

    if (canPersist) {
      coroutineScope.launch {
        merge(
          _currentUser.map { Unit },
          _isAuthenticated.map { Unit },
          _contacts.map { Unit },
          _chats.map { Unit },
          _messagesMap.map { Unit },
          _callHistory.map { Unit },
          _userStories.map { Unit },
          _settings.map { Unit }
        ).collectLatest {
          delay(150)
          try {
            persistSnapshot()
            _persistenceError.value = null
          } catch (error: IOException) {
            reportPersistenceError(error)
          } catch (error: GeneralSecurityException) {
            reportPersistenceError(error)
          } catch (error: JsonDataException) {
            reportPersistenceError(error)
          }
        }
      }
    }
  }

  fun encryptString(plainText: String): String =
    encryptedStore.encryptToBase64(plainText)

  fun decryptString(cipherText: String): String =
    encryptedStore.decryptFromBase64(cipherText)

  private fun restoreSnapshot(): Boolean {
    val serialized = encryptedStore.readText() ?: return false
    val snapshot = snapshotAdapter.fromJson(serialized)
      ?: throw JsonDataException("Encrypted local snapshot is empty")

    _currentUser.value = snapshot.currentUser
    _isAuthenticated.value = snapshot.isAuthenticated
    _contacts.value = snapshot.contacts
    _chats.value = snapshot.chats
    _messagesMap.value = snapshot.messagesMap
    _callHistory.value = snapshot.callHistory
    _userStories.value = snapshot.userStories
    _settings.value = snapshot.settings
    return true
  }

  private fun persistSnapshot() {
    val snapshot = RepositorySnapshot(
      currentUser = _currentUser.value,
      isAuthenticated = _isAuthenticated.value,
      contacts = _contacts.value,
      chats = _chats.value,
      messagesMap = _messagesMap.value,
      callHistory = _callHistory.value,
      userStories = _userStories.value,
      settings = _settings.value
    )
    encryptedStore.writeText(snapshotAdapter.toJson(snapshot))
  }

  private fun reportPersistenceError(error: Exception) {
    Log.e(TAG, "Unable to persist encrypted ChatWave data", error)
    _persistenceError.value = "Encrypted local data could not be saved: ${error.message ?: "unknown storage error"}"
  }

  private fun handleUnreadableStore(error: Exception) {
    canPersist = false
    Log.e(TAG, "Unable to read encrypted ChatWave data; preserving the unreadable snapshot", error)
    _persistenceError.value = "Encrypted local data could not be opened: ${error.message ?: "unknown storage error"}"
    seedInitialData()
  }

  // --- Seed Data ---
  private fun seedInitialData() {
    val initialContacts = listOf(
      User(
        id = "c1",
        name = "Elena Vance",
        phoneNumber = "+1 555 839 2011",
        email = "elena.vance@example.com",
        avatarColorHex = "#00A884",
        avatarInitials = "EV",
        about = "Exploring new horizons 🏔️ | Design lead",
        isOnline = true,
        lastSeenText = "Online"
      ),
      User(
        id = "c2",
        name = "Alex Miller",
        phoneNumber = "+1 555 249 9871",
        email = "alex.m@example.com",
        avatarColorHex = "#2196F3",
        avatarInitials = "AM",
        about = "Building Android apps with Jetpack Compose 🚀",
        isOnline = false,
        lastSeenText = "last seen today at 10:45 AM"
      ),
      User(
        id = "c3",
        name = "Sarah Connor",
        phoneNumber = "+1 555 918 3320",
        email = "sarah.c@example.com",
        avatarColorHex = "#E91E63",
        avatarInitials = "SC",
        about = "Coffee enthusiast ☕ | Always on time",
        isOnline = true,
        lastSeenText = "Online"
      ),
      User(
        id = "c4",
        name = "David Kim",
        phoneNumber = "+1 555 776 1204",
        email = "david.kim@example.com",
        avatarColorHex = "#9C27B0",
        avatarInitials = "DK",
        about = "Product Management & Photography 📸",
        isOnline = false,
        lastSeenText = "last seen yesterday at 8:20 PM"
      ),
      User(
        id = "c5",
        name = "Marcus Aurelius",
        phoneNumber = "+1 555 432 1099",
        email = "marcus@example.com",
        avatarColorHex = "#FF9800",
        avatarInitials = "MA",
        about = "The happiness of your life depends upon the quality of your thoughts.",
        isOnline = false,
        lastSeenText = "last seen Monday at 14:10"
      )
    )
    _contacts.value = initialContacts

    val now = System.currentTimeMillis()
    val minute = 60_000L
    val hour = 3_600_000L

    val msgElena1 = Message(
      id = "m101",
      chatId = "chat_elena",
      senderId = "c1",
      senderName = "Elena Vance",
      text = "Hey! Have you checked out the new ChatWave UI design?",
      timestamp = now - 25 * minute,
      status = MessageStatus.READ
    )
    val msgElena2 = Message(
      id = "m102",
      chatId = "chat_elena",
      senderId = "me",
      senderName = "You",
      text = "Yes, it looks incredible! The WhatsApp green and animations are super slick.",
      timestamp = now - 18 * minute,
      status = MessageStatus.READ
    )
    val msgElena3 = Message(
      id = "m103",
      chatId = "chat_elena",
      senderId = "c1",
      senderName = "Elena Vance",
      text = "We should do a quick video call later to review the sprint tasks.",
      timestamp = now - 10 * minute,
      status = MessageStatus.READ
    )

    val msgAlex1 = Message(
      id = "m201",
      chatId = "chat_alex",
      senderId = "c2",
      senderName = "Alex Miller",
      text = "PR is merged! All unit tests passed cleanly.",
      timestamp = now - 2 * hour,
      status = MessageStatus.READ
    )
    val msgAlex2 = Message(
      id = "m202",
      chatId = "chat_alex",
      senderId = "me",
      senderName = "You",
      text = "Awesome work Alex! Let's ship it today.",
      timestamp = now - 1 * hour,
      status = MessageStatus.DELIVERED
    )

    val msgSarah1 = Message(
      id = "m301",
      chatId = "chat_sarah",
      senderId = "c3",
      senderName = "Sarah Connor",
      text = "Are we still grabbing lunch at 12:30?",
      timestamp = now - 45 * minute,
      status = MessageStatus.READ
    )

    val msgGroup1 = Message(
      id = "m401",
      chatId = "chat_group_tech",
      senderId = "c2",
      senderName = "Alex Miller",
      text = "Team, remember our sync meeting at 3 PM UTC.",
      timestamp = now - 4 * hour,
      status = MessageStatus.READ
    )
    val msgGroup2 = Message(
      id = "m402",
      chatId = "chat_group_tech",
      senderId = "c1",
      senderName = "Elena Vance",
      text = "Got it! I will present the new user flow deck 🎨",
      timestamp = now - 3 * hour,
      status = MessageStatus.READ
    )

    _messagesMap.value = mapOf(
      "chat_elena" to listOf(msgElena1, msgElena2, msgElena3),
      "chat_alex" to listOf(msgAlex1, msgAlex2),
      "chat_sarah" to listOf(msgSarah1),
      "chat_group_tech" to listOf(msgGroup1, msgGroup2)
    )

    _chats.value = listOf(
      Chat(
        id = "chat_elena",
        name = "Elena Vance",
        isGroup = false,
        participantIds = listOf("me", "c1"),
        avatarColorHex = "#00A884",
        lastMessage = msgElena3,
        unreadCount = 1,
        isPinned = true,
        isOnline = true,
        lastSeenText = "Online"
      ),
      Chat(
        id = "chat_group_tech",
        name = "ChatWave Core Team 🚀",
        isGroup = true,
        participantIds = listOf("me", "c1", "c2", "c3"),
        avatarColorHex = "#075E54",
        lastMessage = msgGroup2,
        unreadCount = 2,
        groupAdminIds = listOf("me", "c1"),
        groupDescription = "Official collaboration space for ChatWave architecture and release notes.",
        isOnline = true,
        lastSeenText = "4 members"
      ),
      Chat(
        id = "chat_alex",
        name = "Alex Miller",
        isGroup = false,
        participantIds = listOf("me", "c2"),
        avatarColorHex = "#2196F3",
        lastMessage = msgAlex2,
        unreadCount = 0,
        isOnline = false,
        lastSeenText = "last seen today at 10:45 AM"
      ),
      Chat(
        id = "chat_sarah",
        name = "Sarah Connor",
        isGroup = false,
        participantIds = listOf("me", "c3"),
        avatarColorHex = "#E91E63",
        lastMessage = msgSarah1,
        unreadCount = 0,
        isOnline = true,
        lastSeenText = "Online"
      )
    )

    // Seed Call History
    _callHistory.value = listOf(
      CallRecord(
        id = "call_1",
        peerId = "c1",
        peerName = "Elena Vance",
        peerAvatarColorHex = "#00A884",
        isVideo = true,
        isIncoming = false,
        timestamp = now - 1 * hour,
        durationSeconds = 485,
        status = CallStatus.COMPLETED
      ),
      CallRecord(
        id = "call_2",
        peerId = "c3",
        peerName = "Sarah Connor",
        peerAvatarColorHex = "#E91E63",
        isVideo = false,
        isIncoming = true,
        timestamp = now - 5 * hour,
        durationSeconds = 0,
        status = CallStatus.MISSED
      ),
      CallRecord(
        id = "call_3",
        peerId = "c2",
        peerName = "Alex Miller",
        peerAvatarColorHex = "#2196F3",
        isVideo = true,
        isIncoming = true,
        timestamp = now - 24 * hour,
        durationSeconds = 1240,
        status = CallStatus.COMPLETED
      )
    )

    // Seed Status Stories
    _userStories.value = listOf(
      UserStatusStory(
        userId = "me",
        userName = "My Status",
        userAvatarColorHex = "#00A884",
        isCurrentUser = true,
        stories = listOf(
          StatusItem(
            id = "my_story_1",
            userId = "me",
            userName = "You",
            userAvatarColorHex = "#00A884",
            textContent = "Building modern Android apps with Jetpack Compose 🚀 #ChatWave",
            backgroundColorHex = "#075E54",
            timestamp = now - 2 * hour,
            viewers = listOf(
              StatusViewer("c1", "Elena Vance", now - 1 * hour),
              StatusViewer("c2", "Alex Miller", now - 30 * minute)
            )
          )
        )
      ),
      UserStatusStory(
        userId = "c1",
        userName = "Elena Vance",
        userAvatarColorHex = "#00A884",
        isCurrentUser = false,
        stories = listOf(
          StatusItem(
            id = "elena_story_1",
            userId = "c1",
            userName = "Elena Vance",
            userAvatarColorHex = "#00A884",
            textContent = "Morning run along the bay! 🌊✨ The view is spectacular today.",
            backgroundColorHex = "#2E7D32",
            timestamp = now - 4 * hour,
            isViewedByMe = false
          ),
          StatusItem(
            id = "elena_story_2",
            userId = "c1",
            userName = "Elena Vance",
            userAvatarColorHex = "#00A884",
            textContent = "Working on the new design system components 🎨 Keep waving!",
            backgroundColorHex = "#6A1B9A",
            timestamp = now - 1 * hour,
            isViewedByMe = false
          )
        )
      ),
      UserStatusStory(
        userId = "c2",
        userName = "Alex Miller",
        userAvatarColorHex = "#2196F3",
        isCurrentUser = false,
        stories = listOf(
          StatusItem(
            id = "alex_story_1",
            userId = "c2",
            userName = "Alex Miller",
            userAvatarColorHex = "#2196F3",
            textContent = "Late night debugging session conquered! Zero bugs remaining 💻🎉",
            backgroundColorHex = "#1565C0",
            timestamp = now - 8 * hour,
            isViewedByMe = true
          )
        )
      )
    )
  }

  // --- Auth Operations ---
  fun login(emailOrPhone: String, pass: String): Boolean {
    if (emailOrPhone.isNotBlank() && pass.length >= 4) {
      _currentUser.value = User(
        id = "me",
        name = if (emailOrPhone.contains("@")) emailOrPhone.substringBefore("@").replaceFirstChar { it.uppercase() } else "User",
        phoneNumber = if (emailOrPhone.contains("@")) "+1 555 019 2834" else emailOrPhone,
        email = if (emailOrPhone.contains("@")) emailOrPhone else "user@chatwave.app",
        avatarColorHex = "#00A884",
        avatarInitials = "CW",
        about = "Living life one wave at a time 🌊",
        isOnline = true,
        isCurrent = true
      )
      _isAuthenticated.value = true
      return true
    }
    return false
  }

  fun register(name: String, email: String, phone: String, pass: String): Boolean {
    if (name.isNotBlank() && phone.isNotBlank()) {
      _currentUser.value = User(
        id = "me",
        name = name,
        phoneNumber = phone,
        email = email,
        avatarColorHex = "#00A884",
        avatarInitials = name.take(2).uppercase(),
        about = "Hey there! I am using ChatWave.",
        isOnline = true,
        isCurrent = true
      )
      _isAuthenticated.value = true
      return true
    }
    return false
  }

  fun logout() {
    _isAuthenticated.value = false
  }

  fun updateCurrentUserProfile(name: String, about: String, phone: String) {
    _currentUser.value = _currentUser.value?.copy(
      name = name,
      about = about,
      phoneNumber = phone,
      avatarInitials = name.take(2).uppercase()
    )
  }

  // --- Real-Time Messaging ---
  fun sendMessage(
    chatId: String,
    text: String,
    type: MessageType = MessageType.TEXT,
    mediaCaption: String = "",
    audioDurationSec: Int = 0,
    replyTo: Message? = null
  ) {
    if (text.isBlank() && type == MessageType.TEXT) return

    val currentList = _messagesMap.value[chatId] ?: emptyList()
    val newId = "m_" + System.currentTimeMillis()
    val msg = Message(
      id = newId,
      chatId = chatId,
      senderId = "me",
      senderName = _currentUser.value?.name ?: "You",
      text = text,
      timestamp = System.currentTimeMillis(),
      status = MessageStatus.SENDING,
      type = type,
      mediaCaption = mediaCaption,
      audioDurationSec = audioDurationSec,
      replyToMessageId = replyTo?.id,
      replyToText = replyTo?.text,
      replyToSenderName = replyTo?.senderName
    )

    // Append message immediately
    val updatedList = currentList + msg
    _messagesMap.value = _messagesMap.value + (chatId to updatedList)

    // Update Chat's lastMessage
    updateChatLastMessage(chatId, msg)

    // Coroutine to simulate instant delivery receipt progression (Sending -> Sent -> Delivered -> Read)
    coroutineScope.launch {
      delay(350)
      updateMessageStatus(chatId, newId, MessageStatus.SENT)
      delay(550)
      updateMessageStatus(chatId, newId, MessageStatus.DELIVERED)
      delay(900)
      updateMessageStatus(chatId, newId, MessageStatus.READ)

      // Auto reply simulation if not group or if chatting with contact
      simulatePeerResponse(chatId, text)
    }
  }

  private fun updateMessageStatus(chatId: String, messageId: String, newStatus: MessageStatus) {
    val list = _messagesMap.value[chatId] ?: return
    val updated = list.map {
      if (it.id == messageId) it.copy(status = newStatus) else it
    }
    _messagesMap.value = _messagesMap.value + (chatId to updated)
    val last = updated.lastOrNull()
    if (last != null && last.id == messageId) {
      updateChatLastMessage(chatId, last)
    }
  }

  private fun updateChatLastMessage(chatId: String, msg: Message) {
    _chats.value = _chats.value.map {
      if (it.id == chatId) it.copy(lastMessage = msg) else it
    }
  }

  private suspend fun simulatePeerResponse(chatId: String, promptText: String) {
    val chat = _chats.value.find { it.id == chatId } ?: return
    val contact = _contacts.value.find { it.name == chat.name }
    val peerName = contact?.name ?: if (chat.isGroup) "Elena Vance" else chat.name

    delay(1200)
    // Set typing indicator
    _typingStatus.value = _typingStatus.value + (chatId to "$peerName is typing...")

    delay(2000)
    // Clear typing indicator
    _typingStatus.value = _typingStatus.value - chatId

    // Dynamic smart replies based on what was said
    val replyText = when {
      promptText.contains("call", ignoreCase = true) ->
        "Sounds good! Let's connect on video call in a few minutes 📞"
      promptText.contains("hello", ignoreCase = true) || promptText.contains("hi", ignoreCase = true) ->
        "Hey! Great to hear from you. How is your day going? 😊"
      promptText.contains("meeting", ignoreCase = true) || promptText.contains("sync", ignoreCase = true) ->
        "I've got the agenda ready. Joining the room shortly!"
      promptText.contains("lunch", ignoreCase = true) || promptText.contains("coffee", ignoreCase = true) ->
        "Count me in! Meet you at the lobby downstairs ☕"
      promptText.contains("?", ignoreCase = true) ->
        "Definitely! Let me check the details and ping you back right away."
      else ->
        "Got it! Thanks for keeping me updated 👍"
    }

    val replyMsg = Message(
      id = "m_reply_" + System.currentTimeMillis(),
      chatId = chatId,
      senderId = contact?.id ?: "c1",
      senderName = peerName,
      text = replyText,
      timestamp = System.currentTimeMillis(),
      status = MessageStatus.READ
    )

    val currentList = _messagesMap.value[chatId] ?: emptyList()
    _messagesMap.value = _messagesMap.value + (chatId to (currentList + replyMsg))
    updateChatLastMessage(chatId, replyMsg)
  }

  fun editMessage(chatId: String, messageId: String, newText: String) {
    val list = _messagesMap.value[chatId] ?: return
    val updated = list.map {
      if (it.id == messageId) it.copy(text = newText, isEdited = true) else it
    }
    _messagesMap.value = _messagesMap.value + (chatId to updated)
  }

  fun deleteMessageForEveryone(chatId: String, messageId: String) {
    val list = _messagesMap.value[chatId] ?: return
    val updated = list.map {
      if (it.id == messageId) {
        it.copy(
          text = "🚫 This message was deleted",
          isDeletedForEveryone = true,
          reactions = emptyList()
        )
      } else it
    }
    _messagesMap.value = _messagesMap.value + (chatId to updated)
  }

  fun deleteMessageForMe(chatId: String, messageId: String) {
    val list = _messagesMap.value[chatId] ?: return
    val updated = list.filterNot { it.id == messageId }
    _messagesMap.value = _messagesMap.value + (chatId to updated)
  }

  fun addReaction(chatId: String, messageId: String, emoji: String) {
    val list = _messagesMap.value[chatId] ?: return
    val user = _currentUser.value ?: return
    val updated = list.map { msg ->
      if (msg.id == messageId) {
        val existing = msg.reactions.filterNot { it.userId == user.id }
        val newReactions = existing + MessageReaction(emoji, user.id, user.name)
        msg.copy(reactions = newReactions)
      } else msg
    }
    _messagesMap.value = _messagesMap.value + (chatId to updated)
  }

  // --- Contacts & Groups ---
  fun addContact(name: String, phone: String, about: String = "Hey there! I am using ChatWave."): User {
    val colors = listOf("#00A884", "#2196F3", "#E91E63", "#9C27B0", "#FF9800", "#00BCD4")
    val newContact = User(
      id = "c_" + System.currentTimeMillis(),
      name = name,
      phoneNumber = phone,
      avatarColorHex = colors.random(),
      avatarInitials = name.take(2).uppercase(),
      about = about,
      isOnline = true,
      lastSeenText = "Online"
    )
    _contacts.value = _contacts.value + newContact
    return newContact
  }

  fun startOrGetChatWithContact(contact: User): Chat {
    val existing = _chats.value.find { !it.isGroup && (it.participantIds.contains(contact.id) || it.name == contact.name) }
    if (existing != null) return existing

    val newChatId = "chat_" + contact.id
    val newChat = Chat(
      id = newChatId,
      name = contact.name,
      isGroup = false,
      participantIds = listOf("me", contact.id),
      avatarColorHex = contact.avatarColorHex,
      unreadCount = 0,
      isOnline = contact.isOnline,
      lastSeenText = contact.lastSeenText
    )
    _chats.value = listOf(newChat) + _chats.value
    return newChat
  }

  fun createGroup(name: String, selectedContactIds: List<String>, description: String = ""): Chat {
    val newGroupId = "group_" + System.currentTimeMillis()
    val groupChat = Chat(
      id = newGroupId,
      name = name,
      isGroup = true,
      participantIds = listOf("me") + selectedContactIds,
      avatarColorHex = "#075E54",
      groupAdminIds = listOf("me"),
      groupDescription = description,
      unreadCount = 0,
      lastSeenText = "${selectedContactIds.size + 1} members"
    )
    _chats.value = listOf(groupChat) + _chats.value

    val welcomeMsg = Message(
      id = "m_group_init",
      chatId = newGroupId,
      senderId = "me",
      senderName = "You",
      text = "Created group \"$name\"",
      timestamp = System.currentTimeMillis(),
      status = MessageStatus.SENT
    )
    _messagesMap.value = _messagesMap.value + (newGroupId to listOf(welcomeMsg))
    updateChatLastMessage(newGroupId, welcomeMsg)
    return groupChat
  }

  // --- Voice & Video Calls ---
  fun startCall(peerId: String, peerName: String, peerAvatarColor: String, isVideo: Boolean) {
    _activeCall.value = ActiveCallState(
      callId = "call_" + System.currentTimeMillis(),
      peerId = peerId,
      peerName = peerName,
      peerAvatarColorHex = peerAvatarColor,
      isVideo = isVideo,
      isMuted = false,
      isSpeakerOn = true,
      isCameraOn = isVideo,
      durationSeconds = 0,
      isConnected = false
    )

    // Simulate connection after 1.8s
    coroutineScope.launch {
      delay(1800)
      _activeCall.value = _activeCall.value?.copy(isConnected = true)
      // Timer ticker
      while (_activeCall.value != null && _activeCall.value?.isConnected == true) {
        delay(1000)
        _activeCall.value = _activeCall.value?.let {
          it.copy(durationSeconds = it.durationSeconds + 1)
        }
      }
    }
  }

  fun toggleMute() {
    _activeCall.value = _activeCall.value?.let { it.copy(isMuted = !it.isMuted) }
  }

  fun toggleCamera() {
    _activeCall.value = _activeCall.value?.let { it.copy(isCameraOn = !it.isCameraOn) }
  }

  fun toggleSpeaker() {
    _activeCall.value = _activeCall.value?.let { it.copy(isSpeakerOn = !it.isSpeakerOn) }
  }

  fun endCall() {
    val call = _activeCall.value
    if (call != null) {
      val record = CallRecord(
        id = call.callId,
        peerId = call.peerId,
        peerName = call.peerName,
        peerAvatarColorHex = call.peerAvatarColorHex,
        isVideo = call.isVideo,
        isIncoming = false,
        timestamp = System.currentTimeMillis(),
        durationSeconds = call.durationSeconds,
        status = if (call.durationSeconds > 0) CallStatus.COMPLETED else CallStatus.REJECTED
      )
      _callHistory.value = listOf(record) + _callHistory.value
    }
    _activeCall.value = null
  }

  fun simulateIncomingCall(peer: User, isVideo: Boolean) {
    _incomingCall.value = IncomingCallState(
      peerId = peer.id,
      peerName = peer.name,
      peerAvatarColorHex = peer.avatarColorHex,
      isVideo = isVideo
    )
  }

  fun acceptIncomingCall() {
    val incoming = _incomingCall.value ?: return
    _incomingCall.value = null
    startCall(
      peerId = incoming.peerId,
      peerName = incoming.peerName,
      peerAvatarColor = incoming.peerAvatarColorHex,
      isVideo = incoming.isVideo
    )
  }

  fun rejectIncomingCall() {
    val incoming = _incomingCall.value ?: return
    val record = CallRecord(
      id = "call_missed_" + System.currentTimeMillis(),
      peerId = incoming.peerId,
      peerName = incoming.peerName,
      peerAvatarColorHex = incoming.peerAvatarColorHex,
      isVideo = incoming.isVideo,
      isIncoming = true,
      timestamp = System.currentTimeMillis(),
      durationSeconds = 0,
      status = CallStatus.MISSED
    )
    _callHistory.value = listOf(record) + _callHistory.value
    _incomingCall.value = null
  }

  // --- Status / Stories ---
  fun postStatus(textContent: String, colorHex: String, caption: String = "") {
    val user = _currentUser.value ?: return
    val newItem = StatusItem(
      id = "status_" + System.currentTimeMillis(),
      userId = user.id,
      userName = user.name,
      userAvatarColorHex = user.avatarColorHex,
      textContent = textContent,
      backgroundColorHex = colorHex,
      caption = caption,
      timestamp = System.currentTimeMillis()
    )

    val currentStories = _userStories.value
    val myStoryIndex = currentStories.indexOfFirst { it.userId == user.id }
    if (myStoryIndex >= 0) {
      val existing = currentStories[myStoryIndex]
      val updated = existing.copy(stories = listOf(newItem) + existing.stories)
      _userStories.value = currentStories.toMutableList().also { it[myStoryIndex] = updated }
    } else {
      val newStory = UserStatusStory(
        userId = user.id,
        userName = "My Status",
        userAvatarColorHex = user.avatarColorHex,
        isCurrentUser = true,
        stories = listOf(newItem)
      )
      _userStories.value = listOf(newStory) + currentStories
    }
  }

  fun markStatusViewed(statusId: String) {
    val user = _currentUser.value ?: return
    _userStories.value = _userStories.value.map { story ->
      val updatedStories = story.stories.map { item ->
        if (item.id == statusId) {
          val alreadyViewer = item.viewers.any { it.userId == user.id }
          val newViewers = if (alreadyViewer) item.viewers else item.viewers + StatusViewer(user.id, user.name, System.currentTimeMillis())
          item.copy(isViewedByMe = true, viewers = newViewers)
        } else item
      }
      story.copy(stories = updatedStories)
    }
  }

  // --- Settings ---
  fun updateSettings(newSettings: SettingsState) {
    _settings.value = newSettings
  }

  fun toggleBlockUser(userId: String) {
    val currentBlocked = _settings.value.blockedUserIds
    val newBlocked = if (currentBlocked.contains(userId)) {
      currentBlocked - userId
    } else {
      currentBlocked + userId
    }
    _settings.value = _settings.value.copy(blockedUserIds = newBlocked)
  }
}

private data class RepositorySnapshot(
  val currentUser: User?,
  val isAuthenticated: Boolean,
  val contacts: List<User>,
  val chats: List<Chat>,
  val messagesMap: Map<String, List<Message>>,
  val callHistory: List<CallRecord>,
  val userStories: List<UserStatusStory>,
  val settings: SettingsState
)

data class ActiveCallState(
  val callId: String,
  val peerId: String,
  val peerName: String,
  val peerAvatarColorHex: String,
  val isVideo: Boolean,
  val isMuted: Boolean,
  val isSpeakerOn: Boolean,
  val isCameraOn: Boolean,
  val durationSeconds: Int,
  val isConnected: Boolean
)

data class IncomingCallState(
  val peerId: String,
  val peerName: String,
  val peerAvatarColorHex: String,
  val isVideo: Boolean
)

private const val TAG = "ChatWaveRepository"
