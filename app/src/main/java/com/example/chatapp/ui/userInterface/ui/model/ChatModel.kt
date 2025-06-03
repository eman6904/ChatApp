package com.example.chatapp.ui.userInterface.ui.model

data class ChatModel(
    var msgId: String = "",
    var msgType:String = "",
    var profileImage: String = "",
    var textMsg: String = "",
    var imageMsg:ImageModel = ImageModel("",""),
    var recordMsg: RecordModel =
        RecordModel("","","",false),
    var senderId: String = "",
    var receiverId: String = "",
    var time: String = "",
    var action: String = "",
    var status: String ="",
    var edited:Boolean = false,
    var deleted:DeletedMessageModel = DeletedMessageModel(0,""),
    val timestamp: Long = System.currentTimeMillis()
)