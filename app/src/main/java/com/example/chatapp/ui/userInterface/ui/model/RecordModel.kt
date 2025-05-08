package com.example.chatapp.ui.userInterface.ui.model

import android.health.connect.datatypes.units.Length

data class RecordModel(
    val recordLocalPath:String = "",
    val recordRemoteUrl:String = "",
    val recordLength:String = "",
    var listen:Boolean = false
)
