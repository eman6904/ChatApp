package com.example.chatapp.ui.userInterface.localData.messages.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chatapp.ui.userInterface.localData.messages.table.MessageTable
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageTable)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageTable>)

    @Query("SELECT * FROM MessageTable ORDER BY timestamp ASC")
    fun getMessages(): Flow<List<MessageTable>>

    @Query("SELECT * FROM MessageTable WHERE uploaded = 0")
    suspend fun getUnuploadedMessages(): List<MessageTable>

    @Update
    suspend fun updateMessage(message: MessageTable)

    @Query("SELECT EXISTS(SELECT 1 FROM MessageTable WHERE msgId = :msgId)")
    suspend fun existsByMsgId(msgId: String): Boolean

    @Query("SELECT * FROM MessageTable WHERE msgId = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageTable?



}