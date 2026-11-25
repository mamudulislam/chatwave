package com.example.chatwave.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    UserProfileEntity::class,
    TrustedContactEntity::class,
    InvitationEntity::class,
    CallRecordEntity::class,
    DeviceEntity::class,
    SecuritySettingsEntity::class
  ],
  version = 2,
  exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userProfileDao(): UserProfileDao
  abstract fun trustedContactDao(): TrustedContactDao
  abstract fun invitationDao(): InvitationDao
  abstract fun callRecordDao(): CallRecordDao
  abstract fun deviceDao(): DeviceDao
  abstract fun securitySettingsDao(): SecuritySettingsDao

  companion object {
    @Volatile
    private var instance: AppDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN publicKeyVersion INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE trusted_contacts ADD COLUMN keyChangeDetected INTEGER NOT NULL DEFAULT 0")
      }
    }

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
      instance ?: Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "chatwave.db"
      )
        .addMigrations(MIGRATION_1_2)
        .build()
        .also { instance = it }
    }

    fun closeAndReset() {
      instance?.close()
      instance = null
    }
  }
}
