package com.example.chatapp.ui.userInterface.model

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.model.GlideUrl
import com.example.chatapp.R
import com.example.chatapp.databinding.UserItemBinding
import com.example.chatapp.ui.userInterface.ui.MainActivity.Companion.usersMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.HashMap

class UserAdapter(private val list:ArrayList<UserItems>):
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    var objChat: DatabaseReference? = null
    var currentUserId = FirebaseAuth.getInstance()?.currentUser!!.uid
    inner class ViewHolder(val binding:UserItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
            var profilrPhoto=binding.profileImage
            var msg=binding.msg
            var username=binding.username
            var currentTime=binding.time
            var msgCounter=binding.msgCounter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        var ctr:Int=0
        var senderId=list[position].id
        objChat = FirebaseDatabase.getInstance().getReference("Chat")
        objChat?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val chat = data.getValue(ChatModel::class.java)
                    if(chat!!.receiverid==currentUserId&&chat.senderId==senderId&&chat.seen==""){
                        ctr++
                    }
                }
                if(ctr!=0) {
                    holder.msgCounter.text = ctr.toString()
                    holder.msgCounter.isVisible = true
                }
            }
        })

        holder.username.text=list[position].username
        holder.currentTime.text=list[position].currentTime
        holder.msg.text=list[position].msg

        Glide.with(holder.binding.root).asBitmap().load(Uri.parse(list[position].profilePhoto))
            .placeholder(R.drawable.personalphoto).into(holder.profilrPhoto)

        holder.binding.root.setOnClickListener()
        {

            var bundle= bundleOf("id" to list[position].id)
           it.findNavController().navigate(R.id.action_users_to_chat,bundle)
        }

    }
    override fun getItemCount(): Int {
        return list.size
    }
}