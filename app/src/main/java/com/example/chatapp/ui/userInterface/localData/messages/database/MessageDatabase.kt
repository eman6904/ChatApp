package com.example.chatapp.ui.userInterface.localData.messages.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chatapp.ui.userInterface.localData.messages.dao.MessageDAO
import com.example.chatapp.ui.userInterface.localData.messages.table.MessageTable

@Database(entities = [MessageTable::class], version = 2, exportSchema = false)
abstract class MessageDatabase : RoomDatabase()
{
    abstract fun messageDao(): MessageDAO
    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: MessageDatabase? = null

        fun getDatabase(context: Context): MessageDatabase
        {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MessageDatabase::class.java,
                    "WhatsAppMessages"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}