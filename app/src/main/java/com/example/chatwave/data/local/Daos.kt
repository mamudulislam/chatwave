package com.example.chatwave.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
  @Query("SELECT * FROM user_profile WHERE id = 'local'")
  fun observe(): Flow<UserProfileEntity?>

  @Query("SELECT * FROM user_profile WHERE id = 'local'")
  suspend fun get(): UserProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(value: UserProfileEntity)

  @Query("UPDATE user_profile SET registeredRemotely = :registered, updatedAt = :updatedAt WHERE id = 'local'")
  suspend fun setRegistered(registered: Boolean, updatedAt: Long)

  @Query("UPDATE user_profile SET biometricEnabled = :enabled WHERE id = 'local'")
  suspend fun setBiometricEnabled(enabled: Boolean)

  @Query("DELETE FROM user_profile")
  suspend fun deleteAll()
}

@Dao
interface TrustedContactDao {
  @Query("SELECT * FROM trusted_contacts ORDER BY displayName COLLATE NOCASE ASC")
  fun observeAll(): Flow<List<TrustedContactEntity>>

  @Query("SELECT * FROM trusted_contacts WHERE chatwaveId = :id")
  suspend fun get(id: String): TrustedContactEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(value: TrustedContactEntity)

  @Query("UPDATE trusted_contacts SET verified = 1, keyChangeDetected = 0, updatedAt = :updatedAt WHERE chatwaveId = :id")
  suspend fun markVerified(id: String, updatedAt: Long)

  @Query("UPDATE trusted_contacts SET keyChangeDetected = 1, updatedAt = :updatedAt WHERE chatwaveId = :id")
  suspend fun markKeyChange(id: String, updatedAt: Long)

  @Query("DELETE FROM trusted_contacts WHERE chatwaveId = :id")
  suspend fun delete(id: String)
}

@Dao
interface InvitationDao {
  @Query("SELECT * FROM invitations ORDER BY createdAt DESC")
  fun observeAll(): Flow<List<InvitationEntity>>

  @Query("SELECT * FROM invitations WHERE codeHash = :codeHash")
  suspend fun get(codeHash: String): InvitationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(value: InvitationEntity)

  @Query("UPDATE invitations SET status = :status, redeemedBy = :redeemedBy WHERE codeHash = :codeHash")
  suspend fun updateStatus(codeHash: String, status: String, redeemedBy: String?)

  @Query("DELETE FROM invitations WHERE expiresAt < :now AND status = 'CREATED'")
  suspend fun deleteExpired(now: Long)

  @Query("DELETE FROM invitations")
  suspend fun deleteAll()
}

@Dao
interface CallRecordDao {
  @Query("SELECT * FROM call_history ORDER BY startedAt DESC")
  fun observeAll(): Flow<List<CallRecordEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(value: CallRecordEntity)

  @Query("UPDATE call_history SET state = :state, endedAt = :endedAt, durationSeconds = :durationSeconds WHERE id = :id")
  suspend fun finish(id: String, state: String, endedAt: Long, durationSeconds: Long)

  @Query("DELETE FROM call_history")
  suspend fun deleteAll()
}

@Dao
interface DeviceDao {
  @Query("SELECT * FROM devices ORDER BY isCurrent DESC, lastSeen DESC")
  fun observeAll(): Flow<List<DeviceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(value: DeviceEntity)

  @Query("UPDATE devices SET revoked = 1 WHERE deviceId = :deviceId")
  suspend fun revoke(deviceId: String)

  @Query("DELETE FROM devices WHERE deviceId = :deviceId")
  suspend fun delete(deviceId: String)
}

@Dao
interface SecuritySettingsDao {
  @Query("SELECT * FROM security_settings WHERE id = 1")
  fun observe(): Flow<SecuritySettingsEntity?>

  @Query("SELECT * FROM security_settings WHERE id = 1")
  suspend fun get(): SecuritySettingsEntity?

  @Update(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(value: SecuritySettingsEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(value: SecuritySettingsEntity)
}
