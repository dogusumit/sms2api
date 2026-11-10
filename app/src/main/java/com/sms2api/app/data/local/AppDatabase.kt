package com.sms2api.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sms2api.app.data.model.SmsRecord

@Database(entities = [SmsRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smsDao(): SmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms2api.db"
                ).fallbackToDestructiveMigration(true)
                 .build()
                 .also { INSTANCE = it }
            }
        }
    }
}
