package com.example.chatapp.ui.userInterface.adapter
import android.app.Activity
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.UserItemBinding
import com.example.chatapp.ui.userInterface.model.ChatModel
import com.example.chatapp.ui.userInterface.model.UserItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

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
        val context = holder.binding.root.context
        if (context is Activity && !context.isDestroyed)
            {
                Glide.with(context).asBitmap()
                    .load(Uri.parse(list[position].profilePhoto))
                    .placeholder(R.drawable.personalphoto)
                    .error(R.drawable.personalphoto)
                    .into(holder.profilrPhoto)
            }

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