package com.example.chatapp.ui.userInterface.localData.messages.repository

import androidx.lifecycle.LiveData
import com.example.chatapp.ui.userInterface.localData.messages.dao.MessageDAO
import com.example.chatapp.ui.userInterface.localData.messages.table.MessageTable
import kotlinx.coroutines.flow.Flow

class MessageRepository(private val messageDao: MessageDAO) {

    fun getMessages(): Flow<List<MessageTable>> = messageDao.getMessages()

    suspend fun insertMessage(message: MessageTable) {
        messageDao.insertMessage(message)
    }
    suspend fun insertMessages(messages: List<MessageTable>) {
        messageDao.insertMessages(messages)
    }
    suspend fun updateMessage(message: MessageTable){
        messageDao.updateMessage(message)
    }
    suspend fun existsByMsgId(msgId: String):Boolean{
        return messageDao.existsByMsgId(msgId)
    }

    suspend fun getMessageById(id: String): MessageTable? {
        return messageDao.getMessageById(id)
    }


}