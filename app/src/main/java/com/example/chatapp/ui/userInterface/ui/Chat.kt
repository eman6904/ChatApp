package com.example.chatapp.ui.userInterface.ui

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentChatBinding
import com.example.chatapp.ui.userInterface.model.ChatAdapter
import com.example.chatapp.ui.userInterface.model.ChatModel
import com.example.chatapp.ui.userInterface.model.UserItems
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
    var idMsg:String=""
    var chatList=ArrayList<ChatModel>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)
        navController = Navigation.findNavController(view)
        val activity = activity as MainActivity
        activity.supportActionBar?.hide()
        objChat = FirebaseDatabase.getInstance().getReference("Chat")
       /////////////////////////////////////////////////////////////////////////////////
        //for react with message
        binding.listview.onItemClickListener=object :AdapterView.OnItemClickListener{
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {

                val alertbuilder2 = AlertDialog.Builder(requireContext())
                val view2 = layoutInflater.inflate(R.layout.interaction, null)
                alertbuilder2.setView(view2)
                val alertDialog2 = alertbuilder2.create()
                alertDialog2.show()

                var like=view2.findViewById<TextView>(R.id.like)
                var love=view2.findViewById<TextView>(R.id.love)
                var waw=view2.findViewById<TextView>(R.id.waw)
                var haha=view2.findViewById<TextView>(R.id.haha)
                var sad=view2.findViewById<TextView>(R.id.sad)

                var message=chatList.get(position)
                var react=""
                like.setOnClickListener()
                {
                    react=like.text.toString()
                    if(message.action==react)
                        message.action=""
                    else
                        message.action=react
                    objChat?.child(message.idMsg)?.setValue(message)
                    alertDialog2.dismiss()
                }
                love.setOnClickListener()
                {
                    react=love.text.toString()
                    if(message.action==react)
                        message.action=""
                    else
                        message.action=react
                    objChat?.child(message.idMsg)?.setValue(message)
                    alertDialog2.dismiss()
                }
                waw.setOnClickListener()
                {
                    react=waw.text.toString()
                    if(message.action==react)
                        message.action=""
                    else
                        message.action=react
                    objChat?.child(message.idMsg)?.setValue(message)
                    alertDialog2.dismiss()
                }
                haha.setOnClickListener()
                {
                    react=haha.text.toString()
                    if(message.action==react)
                        message.action=""
                    else
                        message.action=react
                    objChat?.child(message.idMsg)?.setValue(message)
                    alertDialog2.dismiss()
                }
                sad.setOnClickListener()
                {
                    react=sad.text.toString()
                    if(message.action==react)
                        message.action=""
                    else
                        message.action=react
                    Log.d("message",message.action)
                    objChat?.child(message.idMsg)?.setValue(message)
                    alertDialog2.dismiss()
                }
            }

        }
        //////////////////////////////////////////////////////////////////////////////////
        //for delete or update msg
        binding.listview.onItemLongClickListener=object :AdapterView.OnItemLongClickListener{
            override fun onItemLongClick(
                p0: AdapterView<*>?,
                p1: View?,
                position: Int,
                p3: Long
            ): Boolean {
               if(chatList[position].senderId==senderId)
               {
                   val alertbuilder = AlertDialog.Builder(requireContext())
                   val view = layoutInflater.inflate(R.layout.about_message, null)
                   alertbuilder.setView(view)
                   val alertDialog = alertbuilder.create()
                   alertDialog.show()

                   val delete = view.findViewById<ImageView>(R.id.delete)
                   val update = view.findViewById<ImageView>(R.id.update)
                   val edMsg = view.findViewById<EditText>(R.id.edMsg)

                   var message=chatList.get(position)
                   edMsg.setText(message.msg)

                   update.setOnClickListener()
                   {
                       message.msg=edMsg.text.toString()
                       objChat?.child(message.idMsg)?.setValue(message)
                       alertDialog.dismiss()
                   }
                   delete.setOnClickListener(){
                       objChat?.child(message.idMsg)?.removeValue()
                       alertDialog.dismiss()
                   }
               }
                return false
            }

        }
       ///////////////////////////////////////////////////////////////////////////////////
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
        senderId=FirebaseAuth.getInstance()?.currentUser!!.uid
        getMyImage()
        binding.sendmMsg.setOnClickListener()
        {
            if(binding.edtext.text.isEmpty())
                Toast.makeText(requireContext(),"Message is empty",Toast.LENGTH_LONG).show()
            else {
                var currentTime:String=""
                var calendar=Calendar.getInstance()
                val hour12hrs: Int = calendar.get(Calendar.HOUR)
                val minutes: Int = calendar.get(Calendar.MINUTE)
                if(calendar.get(Calendar.AM_PM) == Calendar.AM)
                    currentTime="$hour12hrs : $minutes AM"
                else
                    currentTime="$hour12hrs : $minutes PM"
                 idMsg=objChat!!.push()?.key.toString()
                objChat!!.child(idMsg).setValue(
                    ChatModel(
                        idMsg,
                        myImage,
                        binding.edtext.text.toString(),
                        senderId,
                        receiverId
                        ,currentTime,"",""
                    )
                )
                binding.edtext.setText("")
            }
        }
    }
    override fun onStart() {
        super.onStart()
        readMessage()
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
        objChat?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (data in snapshot.children) {
                    val chat = data.getValue(ChatModel::class.java)
                    if (chat!!.senderId.equals(receiverId) && chat!!.receiverid.equals(senderId))
                    {
                        idMsg=chat!!.idMsg
                        val hashMap: HashMap<String, Any> = HashMap()
                        hashMap.put("seen", "seen")
                        objChat?.child(idMsg)?.updateChildren(hashMap as Map<String, Any>)?.addOnFailureListener {
                            Toast.makeText(view!!.context, it.message, Toast.LENGTH_LONG).show()
                        }

                    }
                    if (chat!!.senderId.equals(senderId) && chat!!.receiverid.equals(receiverId) ||
                        chat!!.senderId.equals(receiverId) && chat!!.receiverid.equals(senderId)) {
                        chatList.add(chat)
                    }
                }
                if(chatList.size>=1)
                    setLastMsg(chatList[chatList.size-1].msg,chatList[chatList.size-1].time)
                else
                    setLastMsg("  ","  ")
                val adapter = ChatAdapter(requireContext(),chatList)
                binding.listview.adapter = adapter
            }
        })
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