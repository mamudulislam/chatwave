package com.example.chatwave.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
  @PrimaryKey val id: String = "local",
  val chatwaveId: String,
  val displayName: String,
  val publicKey: String,
  val publicKeyVersion: Int,
  val fingerprint: String,
  val recoveryHash: String,
  val deviceId: String,
  val createdAt: Long,
  val updatedAt: Long,
  val registeredRemotely: Boolean = false,
  val biometricEnabled: Boolean = false
)

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
  @PrimaryKey val chatwaveId: String,
  val displayName: String,
  val publicKey: String,
  val publicKeyVersion: Int,
  val fingerprint: String,
  val safetyCode: String,
  val verified: Boolean = false,
  val keyChangeDetected: Boolean = false,
  val createdAt: Long,
  val updatedAt: Long
)

@Entity(tableName = "invitations")
data class InvitationEntity(
  @PrimaryKey val codeHash: String,
  val maskedCode: String,
  val inviterId: String,
  val expiresAt: Long,
  val createdAt: Long,
  val status: String,
  val redeemedBy: String? = null
)

@Entity(tableName = "call_history")
data class CallRecordEntity(
  @PrimaryKey val id: String,
  val peerId: String,
  val peerName: String,
  val direction: String,
  val state: String,
  val startedAt: Long,
  val endedAt: Long?,
  val durationSeconds: Long,
  val video: Boolean,
  val safetyCode: String?,
  val relayOnly: Boolean
)

@Entity(tableName = "devices")
data class DeviceEntity(
  @PrimaryKey val deviceId: String,
  val displayName: String,
  val platform: String,
  val createdAt: Long,
  val lastSeen: Long,
  val revoked: Boolean = false,
  val isCurrent: Boolean = false
)

@Entity(tableName = "security_settings")
data class SecuritySettingsEntity(
  @PrimaryKey val id: Int = 1,
  val relayOnly: Boolean = false,
  val lockOnBackground: Boolean = true,
  val autoLockSeconds: Int = 300,
  val notificationsEnabled: Boolean = true,
  val showSafetyCode: Boolean = true
)
