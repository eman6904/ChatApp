package com.example.chatapp.ui.userInterface.model

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.chatapp.R
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
        val type = getItemViewType(position)
            if (type == RIGHT) {

                view = LayoutInflater.from(context).inflate(R.layout.item_right,p2,false)
            } else {
                view = LayoutInflater.from(context).inflate(R.layout.item_left,p2,false)
            }
        var msg=view.findViewById<TextView>(R.id.msg)
        var time=view.findViewById<TextView>(R.id.time)
        var action=view.findViewById<TextView>(R.id.action)
        var seen=view.findViewById<ImageView>(R.id.seen)
        val item: ChatModel = getItem(position) as ChatModel
        msg.text=item.msg
        time.text=item.time
        action.text=item.action
        if(type==RIGHT)
        {
            if(item.seen=="seen")
                seen.isVisible=true
            else
                seen.isVisible=false
        }
        return view
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
