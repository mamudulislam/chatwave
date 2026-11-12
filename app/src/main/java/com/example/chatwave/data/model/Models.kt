package com.example.chatwave.data.model

enum class MessageStatus {
  SENDING,
  SENT,
  DELIVERED,
  READ
}

enum class MessageType {
  TEXT,
  IMAGE,
  VIDEO,
  AUDIO,
  DOCUMENT
}

data class User(
  val id: String,
  val name: String,
  val phoneNumber: String,
  val email: String = "",
  val avatarColorHex: String = "#00A884",
  val avatarInitials: String = "",
  val about: String = "Hey there! I am using ChatWave.",
  val isOnline: Boolean = false,
  val lastSeenText: String = "last seen recently",
  val isCurrent: Boolean = false
)

data class MessageReaction(
  val emoji: String,
  val userId: String,
  val userName: String
)

data class Message(
  val id: String,
  val chatId: String,
  val senderId: String,
  val senderName: String,
  val text: String,
  val timestamp: Long,
  val status: MessageStatus = MessageStatus.SENT,
  val type: MessageType = MessageType.TEXT,
  val mediaCaption: String = "",
  val audioDurationSec: Int = 0,
  val replyToMessageId: String? = null,
  val replyToText: String? = null,
  val replyToSenderName: String? = null,
  val isEdited: Boolean = false,
  val isDeletedForEveryone: Boolean = false,
  val reactions: List<MessageReaction> = emptyList(),
  val isEncrypted: Boolean = false
)

data class Chat(
  val id: String,
  val name: String,
  val isGroup: Boolean = false,
  val participantIds: List<String> = emptyList(),
  val avatarColorHex: String = "#075E54",
  val lastMessage: Message? = null,
  val unreadCount: Int = 0,
  val isPinned: Boolean = false,
  val isArchived: Boolean = false,
  val groupAdminIds: List<String> = emptyList(),
  val groupDescription: String = "",
  val isOnline: Boolean = false,
  val lastSeenText: String = ""
)

enum class CallStatus {
  INCOMING,
  OUTGOING,
  MISSED,
  COMPLETED,
  REJECTED
}

data class CallRecord(
  val id: String,
  val peerId: String,
  val peerName: String,
  val peerAvatarColorHex: String,
  val isVideo: Boolean,
  val isIncoming: Boolean,
  val timestamp: Long,
  val durationSeconds: Int,
  val status: CallStatus
)

data class StatusViewer(
  val userId: String,
  val userName: String,
  val viewedAt: Long
)

data class StatusItem(
  val id: String,
  val userId: String,
  val userName: String,
  val userAvatarColorHex: String,
  val textContent: String = "",
  val backgroundColorHex: String = "#075E54",
  val isImage: Boolean = false,
  val caption: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val viewers: List<StatusViewer> = emptyList(),
  val isViewedByMe: Boolean = false
)

data class UserStatusStory(
  val userId: String,
  val userName: String,
  val userAvatarColorHex: String,
  val isCurrentUser: Boolean = false,
  val stories: List<StatusItem> = emptyList()
) {
  val hasUnseen: Boolean get() = stories.any { !it.isViewedByMe }
  val latestTimestamp: Long get() = stories.maxOfOrNull { it.timestamp } ?: 0L
}

enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK
}

enum class PrivacyAudience {
  EVERYONE,
  MY_CONTACTS,
  NOBODY
}

data class SettingsState(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val lastSeenPrivacy: PrivacyAudience = PrivacyAudience.EVERYONE,
  val statusPrivacy: PrivacyAudience = PrivacyAudience.MY_CONTACTS,
  val readReceiptsEnabled: Boolean = true,
  val notificationsEnabled: Boolean = true,
  val chatWallpaperColorHex: String? = null, // null for classic WhatsApp doodle
  val blockedUserIds: Set<String> = emptySet(),
  val endToEndEncryptionEnabled: Boolean = false
)
