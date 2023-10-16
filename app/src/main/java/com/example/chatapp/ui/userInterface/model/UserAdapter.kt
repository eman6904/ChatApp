package com.example.chatapp.ui.userInterface.model

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.firebase.storage.FirebaseStorage

class UserAdapter(private val context: Context,private val list:ArrayList<UserItems>):
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    var currentUser = FirebaseAuth.getInstance()?.currentUser!!.uid

    inner class ViewHolder(val binding:UserItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
            var profilrPhoto=binding.profileImage
            var msg=binding.msg
            var username=binding.username
            var currentTime=binding.time
            var counterMsg=binding.counterMsg

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       holder.username.text=list[position].username
       holder.msg.text=list[position].msg
       holder.currentTime.text=list[position].currentTime
        Glide.with(context).asBitmap().load(Uri.parse(list[position].profilePhoto)).placeholder(R.drawable.personalphoto).into(holder.profilrPhoto)

//        if(usersMap.get(currentUser)?.get(list[position].id)!=null||usersMap.get(currentUser)?.get(list[position].id)!=0) {
//            holder.counterMsg.isVisible=true
//            holder.counterMsg.text = usersMap.get(currentUser)?.get(list[position].id).toString()
//        }
        holder.binding.root.setOnClickListener()
        {
//            usersMap.get(currentUser)?.put(list[position].id,0)
//            holder.counterMsg.isVisible=false
            var bundle= bundleOf("id" to list[position].id)
           it.findNavController().navigate(R.id.action_users_to_chat,bundle)
        }

    }
    override fun getItemCount(): Int {
        return list.size
    }
}