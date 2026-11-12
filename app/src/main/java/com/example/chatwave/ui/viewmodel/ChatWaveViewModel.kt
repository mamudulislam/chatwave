package com.example.chatwave.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatwave.data.model.*
import com.example.chatwave.data.security.ActiveCallState
import com.example.chatwave.data.security.EncryptedDataRepository
import com.example.chatwave.data.security.IncomingCallState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenDestination {
  SPLASH,
  AUTH_LOGIN,
  AUTH_REGISTER,
  AUTH_OTP,
  AUTH_FORGOT_PASSWORD,
  HOME,
  CHAT_DETAIL,
  GROUP_INFO,
  CREATE_GROUP,
  CREATE_STATUS,
  STATUS_VIEWER,
  PROFILE,
  SETTINGS,
  SETTINGS_PRIVACY,
  SETTINGS_WALLPAPER,
  ADD_CONTACT
}

enum class HomeTab {
  CHATS,
  STATUS,
  CALLS,
  CONTACTS
}

class ChatWaveViewModel(application: Application) : AndroidViewModel(application) {

  val repository = EncryptedDataRepository(application.applicationContext)

  // Navigation State
  private val _currentScreen = MutableStateFlow(ScreenDestination.SPLASH)
  val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

  private val _homeTab = MutableStateFlow(HomeTab.CHATS)
  val homeTab: StateFlow<HomeTab> = _homeTab.asStateFlow()

  private val _selectedChat = MutableStateFlow<Chat?>(null)
  val selectedChat: StateFlow<Chat?> = _selectedChat.asStateFlow()

  private val _selectedStory = MutableStateFlow<UserStatusStory?>(null)
  val selectedStory: StateFlow<UserStatusStory?> = _selectedStory.asStateFlow()

  // Search & Filter
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

  // Selected Message for contextual action bar (reply, react, edit, delete)
  private val _selectedMessage = MutableStateFlow<Message?>(null)
  val selectedMessage: StateFlow<Message?> = _selectedMessage.asStateFlow()

  // Pending reply message
  private val _replyingToMessage = MutableStateFlow<Message?>(null)
  val replyingToMessage: StateFlow<Message?> = _replyingToMessage.asStateFlow()

  // Editing message text
  private val _editingMessage = MutableStateFlow<Message?>(null)
  val editingMessage: StateFlow<Message?> = _editingMessage.asStateFlow()

  // Auth Inputs
  var pendingAuthPhone = "+1 555 019 2834"

  // Repositories StateFlows
  val currentUser = repository.currentUser
  val isAuthenticated = repository.isAuthenticated
  val contacts = repository.contacts
  val chats = repository.chats
  val messagesMap = repository.messagesMap
  val typingStatus = repository.typingStatus
  val callHistory = repository.callHistory
  val activeCall = repository.activeCall
  val incomingCall = repository.incomingCall
  val userStories = repository.userStories
  val settings = repository.settings
  val persistenceError = repository.persistenceError

  init {
    viewModelScope.launch {
      // Show splash for 1.2s then route to HOME or AUTH
      kotlinx.coroutines.delay(1200)
      if (repository.isAuthenticated.value) {
        _currentScreen.value = ScreenDestination.HOME
      } else {
        _currentScreen.value = ScreenDestination.AUTH_LOGIN
      }
    }
  }

  // --- Navigation Controls ---
  fun navigateTo(screen: ScreenDestination) {
    _currentScreen.value = screen
    if (screen == ScreenDestination.HOME) {
      _selectedChat.value = null
      _selectedStory.value = null
      _selectedMessage.value = null
      _replyingToMessage.value = null
      _editingMessage.value = null
    }
  }

  fun setHomeTab(tab: HomeTab) {
    _homeTab.value = tab
    _isSearching.value = false
    _searchQuery.value = ""
  }

  fun toggleSearch() {
    _isSearching.value = !_isSearching.value
    if (!_isSearching.value) _searchQuery.value = ""
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun openChat(chat: Chat) {
    _selectedChat.value = chat
    _currentScreen.value = ScreenDestination.CHAT_DETAIL
  }

  fun openStory(story: UserStatusStory) {
    _selectedStory.value = story
    _currentScreen.value = ScreenDestination.STATUS_VIEWER
  }

  // --- Auth Actions ---
  fun login(emailOrPhone: String, pass: String): Boolean {
    val success = repository.login(emailOrPhone, pass)
    if (success) {
      _currentScreen.value = ScreenDestination.HOME
    }
    return success
  }

  fun register(name: String, email: String, phone: String, pass: String): Boolean {
    pendingAuthPhone = phone
    val success = repository.register(name, email, phone, pass)
    if (success) {
      _currentScreen.value = ScreenDestination.AUTH_OTP
    }
    return success
  }

  fun verifyOtp(otp: String): Boolean {
    if (otp.length == 4 || otp.length == 6) {
      _currentScreen.value = ScreenDestination.HOME
      return true
    }
    return false
  }

  fun logout() {
    repository.logout()
    _currentScreen.value = ScreenDestination.AUTH_LOGIN
  }

  // --- Messaging Actions ---
  fun sendMessage(text: String, type: MessageType = MessageType.TEXT, mediaCaption: String = "", audioDurationSec: Int = 0) {
    val chat = _selectedChat.value ?: return

    val editing = _editingMessage.value
    if (editing != null) {
      repository.editMessage(chat.id, editing.id, text)
      _editingMessage.value = null
      return
    }

    repository.sendMessage(
      chatId = chat.id,
      text = text,
      type = type,
      mediaCaption = mediaCaption,
      audioDurationSec = audioDurationSec,
      replyTo = _replyingToMessage.value
    )
    _replyingToMessage.value = null
  }

  fun setSelectedMessage(message: Message?) {
    _selectedMessage.value = message
  }

  fun setReplyingTo(message: Message?) {
    _replyingToMessage.value = message
    _selectedMessage.value = null
  }

  fun setEditingMessage(message: Message?) {
    _editingMessage.value = message
    _selectedMessage.value = null
  }

  fun cancelEditing() {
    _editingMessage.value = null
  }

  fun cancelReply() {
    _replyingToMessage.value = null
  }

  fun deleteSelectedMessage(forEveryone: Boolean) {
    val chat = _selectedChat.value ?: return
    val msg = _selectedMessage.value ?: return
    if (forEveryone) {
      repository.deleteMessageForEveryone(chat.id, msg.id)
    } else {
      repository.deleteMessageForMe(chat.id, msg.id)
    }
    _selectedMessage.value = null
  }

  fun reactToSelectedMessage(emoji: String) {
    val chat = _selectedChat.value ?: return
    val msg = _selectedMessage.value ?: return
    repository.addReaction(chat.id, msg.id, emoji)
    _selectedMessage.value = null
  }

  // --- Calls ---
  fun startCallWithUser(user: User, isVideo: Boolean) {
    repository.startCall(
      peerId = user.id,
      peerName = user.name,
      peerAvatarColor = user.avatarColorHex,
      isVideo = isVideo
    )
  }

  fun startCallFromCurrentChat(isVideo: Boolean) {
    val chat = _selectedChat.value ?: return
    repository.startCall(
      peerId = chat.id,
      peerName = chat.name,
      peerAvatarColor = chat.avatarColorHex,
      isVideo = isVideo
    )
  }

  fun toggleMute() = repository.toggleMute()
  fun toggleCamera() = repository.toggleCamera()
  fun toggleSpeaker() = repository.toggleSpeaker()
  fun endCall() = repository.endCall()
  fun acceptIncomingCall() = repository.acceptIncomingCall()
  fun rejectIncomingCall() = repository.rejectIncomingCall()

  fun triggerIncomingCallSimulation() {
    val randomContact = repository.contacts.value.firstOrNull() ?: return
    repository.simulateIncomingCall(randomContact, isVideo = true)
  }

  // --- Status Stories ---
  fun postStatus(textContent: String, colorHex: String, caption: String = "") {
    repository.postStatus(textContent, colorHex, caption)
    _currentScreen.value = ScreenDestination.HOME
    _homeTab.value = HomeTab.STATUS
  }

  fun markStatusViewed(statusId: String) {
    repository.markStatusViewed(statusId)
  }

  // --- Contacts & Groups ---
  fun addContact(name: String, phone: String, about: String) {
    val newContact = repository.addContact(name, phone, about)
    val chat = repository.startOrGetChatWithContact(newContact)
    openChat(chat)
  }

  fun createGroup(name: String, selectedContactIds: List<String>, description: String) {
    val newGroupChat = repository.createGroup(name, selectedContactIds, description)
    openChat(newGroupChat)
  }

  fun startChatWithContact(contact: User) {
    val chat = repository.startOrGetChatWithContact(contact)
    openChat(chat)
  }

  // --- Settings ---
  fun updateSettings(newSettings: SettingsState) {
    repository.updateSettings(newSettings)
  }

  fun updateProfile(name: String, about: String, phone: String) {
    repository.updateCurrentUserProfile(name, about, phone)
  }

  fun toggleBlockUser(userId: String) {
    repository.toggleBlockUser(userId)
  }
}
