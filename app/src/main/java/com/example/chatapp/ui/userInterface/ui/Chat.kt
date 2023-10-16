package com.example.chatapp.ui.userInterface.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentChatBinding
import com.example.chatapp.ui.userInterface.model.ChatAdapter
import com.example.chatapp.ui.userInterface.model.ChatModel
import com.example.chatapp.ui.userInterface.model.UserItems
import com.example.chatapp.ui.userInterface.ui.MainActivity.Companion.usersMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class Chat : Fragment(R.layout.fragment_chat) {
    private lateinit var binding: FragmentChatBinding
    private lateinit var navController: NavController
    var objUsers: DatabaseReference? = null
    var objChat: DatabaseReference? = null
    var senderId:String=""
    var receiverId:String=""
    var myImage:String=""
    var chatList=ArrayList<ChatModel>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)
        navController = Navigation.findNavController(view)

        val activity = activity as MainActivity
        activity.supportActionBar?.hide()


        binding.arrowBack.setOnClickListener(){
            navController.navigate(R.id.action_chat_to_users)
        }
        receiverId=arguments?.getString("id").toString()
        objUsers = FirebaseDatabase.getInstance().getReference("User").child(receiverId)
        objUsers?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserItems::class.java)
                binding.username.text=user!!.username
                Glide.with(activity.applicationContext).asBitmap().load(Uri.parse(user!!.profilePhoto))
                    .placeholder(R.drawable.personalphotojpg).into(binding.userImage)
            }
        })
        ////////////////////////////////////////////////////////////////////////////////////
        objChat=FirebaseDatabase.getInstance().getReference("Chat")
        senderId=FirebaseAuth.getInstance()?.currentUser!!.uid
        getMyImage()
        binding.progressBar.isVisible=true
        readMessage()
        binding.sendmMsg.setOnClickListener()
        {
            if(binding.edtext.text.isEmpty())
                Toast.makeText(requireContext(),"Message is empty",Toast.LENGTH_LONG).show()
            else {

                //this way is not correct
//                if(usersMap.get(receiverId)==null)
//                    usersMap.put(receiverId,mutableMapOf<String, Int>())
//
//                if(usersMap.get(receiverId)?.get(senderId)==null)
//                     usersMap.get(receiverId)?.put(senderId,1)
//                else
//                      usersMap.get(receiverId)?.put(senderId,(usersMap.get(receiverId)?.get(senderId))!!+1)

                var currentTime:String=""
                var calendar=Calendar.getInstance()
                val hour12hrs: Int = calendar.get(Calendar.HOUR)
                val minutes: Int = calendar.get(Calendar.MINUTE)
                if(calendar.get(Calendar.AM_PM) == Calendar.AM)
                    currentTime="$hour12hrs : $minutes AM"
                else
                    currentTime="$hour12hrs : $minutes PM"
                objChat!!.push().setValue(
                    ChatModel(
                        myImage,
                        binding.edtext.text.toString(),
                        senderId,
                        receiverId
                        ,currentTime
                    )
                )
                setLastMsg(binding.edtext.text.toString(),currentTime)
                binding.edtext.setText("")
                readMessage()
            }
        }
    }
    private fun getMyImage()
    {
        objUsers = FirebaseDatabase.getInstance().getReference("User").child(senderId)
        objUsers?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserItems::class.java)
                myImage=user!!.profilePhoto
            }
        })
    }
    private fun readMessage()
    {
        objChat = FirebaseDatabase.getInstance().getReference("Chat")
        objChat?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (data in snapshot.children) {
                    val chat = data.getValue(ChatModel::class.java)
                    if (chat!!.senderId.equals(senderId) && chat!!.receiverid.equals(receiverId) ||
                        chat!!.senderId.equals(receiverId) && chat!!.receiverid.equals(senderId)) {
                        chatList.add(chat)
                    }
                }
            }
        })
        binding.progressBar.isVisible=false
        val adapter = ChatAdapter(requireContext(),chatList)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
    }
    private fun setLastMsg(lastMsg:String,currentTime:String)
    {
        objUsers = FirebaseDatabase.getInstance().getReference("User").child(senderId)
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap.put("msg", lastMsg)
        hashMap.put("currentTime", currentTime)
        objUsers?.updateChildren(hashMap as Map<String, Any>)?.addOnFailureListener {
            Toast.makeText(view!!.context, it.message, Toast.LENGTH_LONG).show()
        }
        objUsers = FirebaseDatabase.getInstance().getReference("User").child(receiverId)
        val hashMap2: HashMap<String, Any> = HashMap()
        hashMap2.put("msg", lastMsg)
        hashMap2.put("currentTime", currentTime)
        objUsers?.updateChildren(hashMap2 as Map<String, Any>)?.addOnFailureListener {
            Toast.makeText(view!!.context, it.message, Toast.LENGTH_LONG).show()
        }
    }
}