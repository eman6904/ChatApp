package com.example.chatapp.ui.userInterface.model

data class ChatModel(
    var idMsg: String = "",
    var pr_image: String = "",
    var msg: String = "",
    var senderId: String = "",
    var receiverid: String = "",
    var time: String = "",
    var action: String = "",
    var seen: String =""
)