package com.example.chatapp.ui.userInterface.ui.adapter

import android.app.Activity
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.ui.userInterface.localData.messages.table.MessageTable
import com.example.chatapp.ui.userInterface.localData.messages.viewModel.MessageViewModel
import com.example.chatapp.ui.userInterface.ui.model.RecordModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.util.HashMap


class ChatAdapter(private val context: Context, private val chatList: List<MessageTable>,private val viewModel:MessageViewModel) : BaseAdapter() {
    private val RIGHT=0
    private val LEFT=1
    private var isPlaying = false
    private var firebaseUser: FirebaseUser? = null
    private var mediaPlayer: MediaPlayer? = null
    private var objChat = FirebaseDatabase.getInstance().getReference("Chat")
    var currentUserId = FirebaseAuth.getInstance()?.currentUser!!.uid
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
            as LayoutInflater

    override fun getCount(): Int {
        return chatList.size
    }

    override fun getItem(p0: Int): Any {
        return  chatList[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }
    override fun getView(position: Int, converterView: View?, p2: ViewGroup?): View {
        var view:View
        val item: MessageTable = getItem(position) as MessageTable
            val type = getItemViewType(position)
            if (type == RIGHT) {

                view = LayoutInflater.from(context).inflate(R.layout.item_right,p2,false)
            } else {
                view = LayoutInflater.from(context).inflate(R.layout.item_left,p2,false)
            }
        var action=view.findViewById<TextView>(R.id.action)
        var tail=view.findViewById<ImageView>(R.id.tail)
        val text_msg = view.findViewById<LinearLayout>(R.id.text_msg)
        val image_msg = view.findViewById<LinearLayout>(R.id.image_msg)
        val record_msg = view.findViewById<LinearLayout>(R.id.record_msg)
        var actionBackground=view.findViewById<CardView>(R.id.actionBackground)
        if(item.action.isNotEmpty()){
            actionBackground.isVisible = true
            action.text = item.action
        }else{
            actionBackground.isVisible = false
        }
        if(position>0&&getItemViewType(position)==getItemViewType(position-1))
            tail.isVisible = false
        else
            tail.isVisible = true

        if(item.msgType =="text"){

            image_msg.isVisible = false
            record_msg.isVisible = false
            text_msg.isVisible = true

            var msg=view.findViewById<TextView>(R.id.text_message_body)
            var time=view.findViewById<TextView>(R.id.text_message_time)
            var seen=view.findViewById<TextView>(R.id.seen)
            msg.text=item.textMsg
            time.text=item.time
            if(type==RIGHT)
            {
                if(item.status=="seen")
                    seen.isVisible=true
                else
                    seen.isVisible=false
            }
            return view
        }else if(item.msgType == "image"){

            image_msg.isVisible = true
            text_msg.isVisible = false
            record_msg.isVisible = false

            var time=view.findViewById<TextView>(R.id.image_msg_time)
            var image=view.findViewById<ImageView>(R.id.image)
            var seen=view.findViewById<TextView>(R.id.image_seen)
            if (context is Activity && !context.isDestroyed)
            {
                if(viewModel.isConnected.value == true) {
                    Glide.with(context).asBitmap()
                        .load(Uri.parse(item.imageMsg.imageRemoteUrl))
                        .placeholder(R.drawable.progress_animation)
                        .error(R.drawable.progress_animation)
                        .into(image)
                }else {
                    Glide.with(context)
                        .asBitmap()
                        .load(File(item.imageMsg.imageLocalPath))
                        .placeholder(R.drawable.progress_animation)
                        .error(R.drawable.progress_animation)
                        .into(image)
                }

            }
            time.text=item.time
            if(type==RIGHT)
            {
                if(item.status=="seen")
                    seen.isVisible=true
                else
                    seen.isVisible=false
            }
            return view
        }else{
            image_msg.isVisible = false
            text_msg.isVisible = false
            record_msg.isVisible = true

            var time=view.findViewById<TextView>(R.id.record_msg_time)
            var play_icon=view.findViewById<ImageView>(R.id.play_icon)
            var pause_icon=view.findViewById<ImageView>(R.id.pause_icon)
            var mic_icon = view.findViewById<ImageView>(R.id.mic_icon)
            var thumb_dot = view.findViewById<View>(R.id.thumbDot)
            var seen=view.findViewById<TextView>(R.id.record_seen)
            val background = thumb_dot.background as GradientDrawable

            if(item.recordMsg.listen){
                mic_icon.setColorFilter(ContextCompat.getColor(context, R.color.listenRecordColor), PorterDuff.Mode.SRC_IN)
                pause_icon.setColorFilter(ContextCompat.getColor(context, R.color.listenRecordColor), PorterDuff.Mode.SRC_IN)
                play_icon.setColorFilter(ContextCompat.getColor(context, R.color.listenRecordColor), PorterDuff.Mode.SRC_IN)
                background.setColor(ContextCompat.getColor(context, R.color.listenRecordColor))
            }else{
                mic_icon.setColorFilter(ContextCompat.getColor(context, R.color.iconsColor), PorterDuff.Mode.SRC_IN)
                pause_icon.setColorFilter(ContextCompat.getColor(context, R.color.iconsColor), PorterDuff.Mode.SRC_IN)
                play_icon.setColorFilter(ContextCompat.getColor(context, R.color.iconsColor), PorterDuff.Mode.SRC_IN)
                background.setColor(ContextCompat.getColor(context, R.color.iconsColor))
            }
            record_msg.setOnClickListener {
                if(!isPlaying){
                    playAudioFromUrl(
                        url = item.recordMsg.recordRemoteUrl,
                        setPlayIcon = {
                                play,pause ->
                            pause_icon.isVisible = pause
                            play_icon.isVisible = play
                        }
                    )
                    pause_icon.isVisible = true
                    play_icon.isVisible = false
                }else{
                    stopAudio()
                    pause_icon.isVisible = false
                    play_icon.isVisible = true
                }
                if(currentUserId==item.receiverId && item.recordMsg.listen==false) {
                    setListen(
                        item = item,
                        view = view
                    )
                }
                isPlaying=!isPlaying
            }
            time.text=item.time
            if(type==RIGHT)
            {
                if(item.status=="seen")
                    seen.isVisible=true
                else
                    seen.isVisible=false
            }
            return view
        }

    }
    override fun getItemViewType(position: Int): Int {
        firebaseUser = FirebaseAuth.getInstance().currentUser
        if (chatList[position].senderId == firebaseUser!!.uid) {
            return RIGHT
        } else {
            return LEFT
        }
    }
    private fun playAudioFromUrl(url: String, setPlayIcon:(play:Boolean, Pause:Boolean)->Unit) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener {
                it.start()
            }
            setOnCompletionListener {
                setPlayIcon(true,false)
            }
        }
    }
    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }
    private fun setListen(item: MessageTable, view: View){
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap.put(
            "recordMsg",
            RecordModel(item.recordMsg.recordRemoteUrl, item.recordMsg.recordLength, "",true)
        )
        objChat.child(item.msgId).updateChildren(hashMap as Map<String, Any>)
            ?.addOnFailureListener {
                Toast.makeText(view!!.context, it.message, Toast.LENGTH_LONG).show()
            }
    }

}
