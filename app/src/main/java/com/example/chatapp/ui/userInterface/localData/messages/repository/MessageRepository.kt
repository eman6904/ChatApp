package com.example.chatapp.ui.userInterface.localData.messages.repository

import androidx.lifecycle.LiveData
import com.example.chatapp.ui.userInterface.localData.messages.dao.MessageDAO
import com.example.chatapp.ui.userInterface.localData.messages.table.MessageTable

class MessageRepository(private val messageDao: MessageDAO) {

    val messages: LiveData<List<MessageTable>> = messageDao.getMessages()

    suspend fun insertMessage(message: MessageTable) {
        messageDao.insertMessage(message)
    }
    suspend fun updateMessage(message: MessageTable){
        messageDao.updateMessage(message)
    }
    suspend fun existsByMsgId(msgId: String):Boolean{
        return messageDao.existsByMsgId(msgId)
    }
    suspend fun getUnuploadedMessages():List<MessageTable>{

        return messageDao.getUnuploadedMessages()
    }

}