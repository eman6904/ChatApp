package com.example.chatapp.ui.userInterface.localData.messages.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MessageViewModel(application: Application) : AndroidViewModel(application) {

    private val messageRepo: MessageRepository

    private val _selectedMessages = MutableLiveData<ArrayList<Int>>(ArrayList())
    val selectedMessages: LiveData<ArrayList<Int>> = _selectedMessages

    private val _editedMessage = MutableLiveData<MessageTable>()
    val editedMessage: LiveData<MessageTable> = _editedMessage

    private val _editMode = MutableLiveData<Boolean>(false)
    val editMode: LiveData<Boolean> = _editMode

    init {
        val dao = MessageDatabase.getDatabase(application).messageDao()
        messageRepo = MessageRepository(dao)

    }

    val messages: StateFlow<List<MessageTable>> = messageRepo.getMessages()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val networkMonitor = NetworkMonitor(application)

    val isConnected: LiveData<Boolean> get() = networkMonitor.isConnected

    fun startNetworkMonitoring() = networkMonitor.start()

    fun stopNetworkMonitoring() = networkMonitor.stop()

    private suspend fun canInsert(msg: MessageTable): Boolean {

        val existingMsg = messageRepo.getMessageById(msg.msgId)

        return existingMsg != msg
    }

    fun insertMessage(msg: MessageTable) = viewModelScope.launch {

        messageRepo.insertMessage(msg)
    }

    fun updateMessage(msg: MessageTable) = viewModelScope.launch {

        messageRepo.updateMessage(msg)
    }
    fun setMessageForEdit(msg:MessageTable){

        _editedMessage.value = msg
    }
    fun setEditMode(enable:Boolean){

        _editMode.value = enable
    }
    fun addPosition(pos: Int) {
        val currentList = _selectedMessages.value ?: ArrayList()
        if (currentList.contains(pos)) {
            removePosition(pos)
        } else {
            val updatedList = ArrayList(currentList)
            updatedList.add(pos)
            _selectedMessages.value = updatedList
        }
    }

    fun removePosition(pos: Int) {
        val currentList = _selectedMessages.value ?: ArrayList()
        val updatedList = ArrayList(currentList)
        updatedList.remove(pos)
        _selectedMessages.value = updatedList
    }

    fun clearSelectedMessages() {
        _selectedMessages.value = ArrayList()
    }

    fun uploadPendingMessages() = viewModelScope.launch {

        for (message in messages.value!!) {

            FirebaseDatabase.getInstance().getReference("Chat")
                .child(message.msgId)
                .setValue(message)
                .addOnSuccessListener {
                    viewModelScope.launch {

                    }
                }
        }
    }

    fun downloadMessagesFromFirebase() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Chat")
        val applicationContext = getApplication<Application>()
        val currentUserId = FirebaseAuth.getInstance()?.currentUser!!.uid

        dbRef.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    viewModelScope.launch {
                        val messagesToInsert = mutableListOf<MessageTable>()
                        val downloadJobs = mutableListOf<Deferred<Boolean>>()

                        for (snap in snapshot.children) {
                            val msg = snap.getValue(MessageTable::class.java)

                            if (msg != null&&(msg.receiverId==currentUserId||msg.senderId==currentUserId)) {
                                val canInsertResult = canInsert(msg)

                                if (canInsertResult) {
                                    val job = async {
                                        when (msg.msgType) {
                                            "image" -> {
                                                val localPath = downloadFileFromUrlAndSaveLocally(
                                                    context = applicationContext,
                                                    fileUrl = msg.imageMsg.imageRemoteUrl,
                                                    msg = msg
                                                )
                                                localPath?.let {
                                                    messagesToInsert.add(
                                                        msg.copy(
                                                            imageMsg = ImageModel(
                                                                imageLocalPath = localPath,
                                                                imageRemoteUrl = msg.imageMsg.imageRemoteUrl
                                                            )
                                                        )
                                                    )
                                                }
                                            }

                                            "record" -> {
                                                val localPath = downloadFileFromUrlAndSaveLocally(
                                                    context = applicationContext,
                                                    fileUrl = msg.recordMsg.recordRemoteUrl,
                                                    msg = msg
                                                )
                                                localPath?.let {
                                                    messagesToInsert.add(
                                                        msg.copy(
                                                            recordMsg = msg.recordMsg.copy(
                                                                recordLocalPath = localPath
                                                            )
                                                        )
                                                    )
                                                }
                                            }

                                            else -> {
                                                messagesToInsert.add(msg)
                                            }
                                        }
                                        true
                                    }
                                    downloadJobs.add(job)
                                }
                            }
                        }

                        downloadJobs.awaitAll()

                        if (messagesToInsert.isNotEmpty()) {
                            messageRepo.insertMessages(messagesToInsert)
                        }
                    }
                }


                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error: ${error.message}")
                }
            })
    }

    private suspend fun downloadFileFromUrlAndSaveLocally(
        context: Context,
        fileUrl: String,
        msg: MessageTable
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()

                val inputStream = connection.inputStream
                val fileName = when (msg.msgType) {
                    "image" -> "IMG_${msg.msgId}.jpg"
                    else -> "record_${msg.msgId}.mp3"
                }

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