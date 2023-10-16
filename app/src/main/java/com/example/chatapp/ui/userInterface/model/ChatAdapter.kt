package com.example.chatapp.ui.userInterface.model

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.ItemLeftBinding
import com.example.chatapp.databinding.ItemRightBinding
import com.example.chatapp.databinding.UserItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ChatAdapter(private val context: Context, private val chatList:ArrayList<ChatModel>):
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    private val RIGHT=0
    private val LEFT=1
    var firebaseUser: FirebaseUser? = null
    class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
            var profilephoto=view.findViewById<ImageView>(R.id.pr_imge)
            var msg=view.findViewById<TextView>(R.id.msg)
            var time=view.findViewById<TextView>(R.id.time)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if(viewType==RIGHT)
           return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_right,parent,false))
        else
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_left,parent,false))
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.msg.text=chatList[position].msg
        holder.time.text=chatList[position].time
        Glide.with(context).asBitmap().load(Uri.parse(chatList[position].pr_image))
            .placeholder(R.drawable.personalphoto).into(holder.profilephoto)
    }
    override fun getItemCount(): Int {
        return chatList.size
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