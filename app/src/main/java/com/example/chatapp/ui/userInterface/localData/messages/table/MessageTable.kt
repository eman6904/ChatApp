package com.example.chatapp.ui.userInterface.localData.messages.table

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.chatapp.ui.userInterface.ui.model.DeletedMessageModel
import com.example.chatapp.ui.userInterface.ui.model.ImageModel
import com.example.chatapp.ui.userInterface.ui.model.RecordModel

@Entity
data class MessageTable(
    @PrimaryKey
    var msgId: String = "",
    var msgType:String = "",
    var profileImage: String = "",
    var textMsg: String = "",
    @Embedded
    var imageMsg:ImageModel = ImageModel("",""),
    @Embedded
    var recordMsg: RecordModel =
        RecordModel("","","",false),
    var senderId: String = "",
    var receiverId: String = "",
    var time: String = "",
    var action: String = "",
    var status: String ="",
    var edited:Boolean = false,
    @Embedded
    var deleted: DeletedMessageModel = DeletedMessageModel(0,""),
    val timestamp: Long = System.currentTimeMillis()
)
