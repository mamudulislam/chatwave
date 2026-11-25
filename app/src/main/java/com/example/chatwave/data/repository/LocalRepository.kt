package com.example.chatwave.data.repository

import com.example.chatwave.data.local.AppDatabase
import com.example.chatwave.data.local.CallRecordEntity
import com.example.chatwave.data.local.DeviceEntity
import com.example.chatwave.data.local.InvitationEntity
import com.example.chatwave.data.local.SecuritySettingsEntity
import com.example.chatwave.data.local.TrustedContactEntity
import com.example.chatwave.data.local.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class LocalRepository(private val database: AppDatabase) {
  val profile: Flow<UserProfileEntity?> = database.userProfileDao().observe()
  val contacts: Flow<List<TrustedContactEntity>> = database.trustedContactDao().observeAll()
  val invitations: Flow<List<InvitationEntity>> = database.invitationDao().observeAll()
  val callHistory: Flow<List<CallRecordEntity>> = database.callRecordDao().observeAll()
  val devices: Flow<List<DeviceEntity>> = database.deviceDao().observeAll()
  val settings: Flow<SecuritySettingsEntity?> = database.securitySettingsDao().observe()

  suspend fun getProfile(): UserProfileEntity? = database.userProfileDao().get()
  suspend fun saveProfile(profile: UserProfileEntity) = database.userProfileDao().upsert(profile)
  suspend fun markRegistered(registered: Boolean) = database.userProfileDao().setRegistered(registered, System.currentTimeMillis())
  suspend fun setBiometricEnabled(enabled: Boolean) = database.userProfileDao().setBiometricEnabled(enabled)
  suspend fun getContact(id: String): TrustedContactEntity? = database.trustedContactDao().get(id)
  suspend fun saveContact(contact: TrustedContactEntity) = database.trustedContactDao().upsert(contact)
  suspend fun verifyContact(id: String) = database.trustedContactDao().markVerified(id, System.currentTimeMillis())
  suspend fun markKeyChange(id: String) = database.trustedContactDao().markKeyChange(id, System.currentTimeMillis())
  suspend fun deleteContact(id: String) = database.trustedContactDao().delete(id)
  suspend fun saveInvitation(invitation: InvitationEntity) = database.invitationDao().upsert(invitation)
  suspend fun updateInvitationStatus(hash: String, status: String, redeemedBy: String? = null) = database.invitationDao().updateStatus(hash, status, redeemedBy)
  suspend fun deleteExpiredInvitations() = database.invitationDao().deleteExpired(System.currentTimeMillis())
  suspend fun addCallRecord(record: CallRecordEntity) = database.callRecordDao().insert(record)
  suspend fun finishCallRecord(id: String, state: String, durationSeconds: Long) = database.callRecordDao().finish(id, state, System.currentTimeMillis(), durationSeconds)
  suspend fun addDevice(device: DeviceEntity) = database.deviceDao().upsert(device)
  suspend fun revokeDevice(deviceId: String) = database.deviceDao().revoke(deviceId)
  suspend fun deleteDevice(deviceId: String) = database.deviceDao().delete(deviceId)
  suspend fun getSettings(): SecuritySettingsEntity = database.securitySettingsDao().get() ?: SecuritySettingsEntity().also { database.securitySettingsDao().insert(it) }
  suspend fun saveSettings(value: SecuritySettingsEntity) {
    if (database.securitySettingsDao().get() == null) database.securitySettingsDao().insert(value)
    else database.securitySettingsDao().update(value)
  }
  suspend fun clearAll() {
    database.clearAllTables()
  }
}
