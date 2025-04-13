package com.example.chatapp.ui.userInterface.adapter

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.ui.userInterface.model.ChatModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class ChatAdapter(private val context: Context, private val chatList: ArrayList<ChatModel>) : BaseAdapter() {
    private val RIGHT=0
    private val LEFT=1
    var firebaseUser: FirebaseUser? = null
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
        val item: ChatModel = getItem(position) as ChatModel
        if(item.imageMsg.isEmpty()){
            val type = getItemViewType(position)
            if (type == RIGHT) {

                view = LayoutInflater.from(context).inflate(R.layout.item_right,p2,false)
            } else {
                view = LayoutInflater.from(context).inflate(R.layout.item_left,p2,false)
            }
            var msg=view.findViewById<TextView>(R.id.text_message_body)
            var time=view.findViewById<TextView>(R.id.text_message_time)
            var action=view.findViewById<TextView>(R.id.action)
            var seen=view.findViewById<TextView>(R.id.seen)
            var tail=view.findViewById<ImageView>(R.id.tail)
            var actionBackground=view.findViewById<CardView>(R.id.actionBackground)
            msg.text=item.msg
            time.text=item.time
            if(item.action.isNotEmpty()){
                actionBackground.isVisible = true
                action.text = item.action
            }else{
                actionBackground.isVisible = false
            }
            if(type==RIGHT)
            {
                if(item.seen=="seen")
                    seen.isVisible=true
                else
                    seen.isVisible=false
            }
            if(position>0&&getItemViewType(position)==getItemViewType(position-1))
                tail.isVisible = false
            else
                tail.isVisible = true
            return view
        }else{
            view = LayoutInflater.from(context).inflate(R.layout.image_msg_model,p2,false)
            var time=view.findViewById<TextView>(R.id.time)
            var image=view.findViewById<ImageView>(R.id.image)
            if (context is Activity && !context.isDestroyed)
            {
                Glide.with(context).asBitmap()
                    .load(Uri.parse(item.imageMsg))
                    .placeholder(R.drawable.personalphoto)
                    .error(R.drawable.personalphoto)
                    .into(image)
            }
            time.setText(item.time)

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
}
