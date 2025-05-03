package com.example.chatapp.ui.userInterface.localData.messages.viewModel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chatapp.ui.userInterface.localData.messages.database.MessageDatabase
import com.example.chatapp.ui.userInterface.localData.messages.repository.MessageRepository
import com.example.chatapp.ui.userInterface.localData.messages.table.MessageTable
import com.example.chatapp.ui.userInterface.localData.networkMenitor.NetworkMonitor
import com.example.chatapp.ui.userInterface.ui.model.ImageModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap

class MessageViewModel(application: Application) : AndroidViewModel(application)  {

   private val messageRepo:MessageRepository
    val messages: LiveData<List<MessageTable>> get() = messageRepo.messages
    private val _pendingMessages = MutableLiveData<List<MessageTable>>()
    val pendingMessages: LiveData<List<MessageTable>> get() = _pendingMessages

    init {
        val dao = MessageDatabase.getDatabase(application).messageDao()
        messageRepo = MessageRepository(dao)
    }

    val networkMonitor = NetworkMonitor(application)
    val isConnected: LiveData<Boolean> get() = networkMonitor.isConnected

    fun startNetworkMonitoring() = networkMonitor.start()
    fun stopNetworkMonitoring() = networkMonitor.stop()

     fun insertMessage(msg:MessageTable) = viewModelScope.launch {
         messageRepo.insertMessage(msg)
     }
     fun uploadPendingMessages() = viewModelScope.launch{
        _pendingMessages.value = messageRepo.getUnuploadedMessages()
        if(pendingMessages.value!=null){
            for (message in pendingMessages.value!!) {
                    FirebaseDatabase.getInstance().getReference("Chat")
                        .child(message.msgId)
                        .setValue(message)
                        .addOnSuccessListener {
                            viewModelScope.launch {
                                messageRepo.updateMessage(message.copy(uploaded = true))
                            }
                        }
            }
        }
    }
    fun downloadMessagesFromFirebase() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Chat")
        val applicationContext = getApplication<Application>()
        dbRef.orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var currentUserId = FirebaseAuth.getInstance()?.currentUser!!.uid
                    for (snap in snapshot.children) {
                        val msg = snap.getValue(MessageTable::class.java)
                        if(msg!=null&&msg.receiverId==currentUserId&&msg.downloaded == false){
                            viewModelScope.launch {
                                when(msg.msgType){
                                    "text"->{ messageRepo.insertMessage(msg)}
                                    "image"->{
                                        val localPath = downloadImageFromUrlAndSaveLocally(applicationContext, msg.imageMsg.imageRemoteUrl)
                                        if (localPath != null) {
                                            messageRepo.insertMessage(msg.copy(imageMsg = ImageModel(
                                                imageLocalPath =localPath,
                                                imageRemoteUrl = msg.imageMsg.imageRemoteUrl
                                            )))
                                        }
                                     }
                                }
                            }
                            val hashMap: HashMap<String, Any> = HashMap()
                            hashMap.put("downloaded", true)
                            FirebaseDatabase.getInstance().getReference("Chat")
                                ?.child(msg.msgId)?.updateChildren(hashMap as Map<String, Any>)?.addOnFailureListener {
                                }?.addOnSuccessListener {}
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error: ${error.message}")
                }
            })
    }
    private suspend fun downloadImageFromUrlAndSaveLocally(context: Context, imageUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()

                val inputStream = connection.inputStream
                val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, fileName)

                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)

                inputStream.close()
                outputStream.close()
                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }



}