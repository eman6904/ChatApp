package com.example.chatapp.ui.userInterface.localData

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class MessageSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        syncMessages()
        return Result.success()
    }

    private fun syncMessages() {
        Log.d("syncMessages","connect")
    }
}
