package com.example.chatapp.ui.userInterface.fragments

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PICK
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentChatBinding
import com.example.chatapp.ui.userInterface.adapter.ChatAdapter
import com.example.chatapp.ui.userInterface.model.ChatModel
import com.example.chatapp.ui.userInterface.model.UserItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class Chat : Fragment(R.layout.fragment_chat) {
    private lateinit var binding: FragmentChatBinding
    private lateinit var navController: NavController
    private lateinit var chatListener: ValueEventListener
    var msgModel: ChatModel? = null
    var imageMsg: String? = null
    var storage: StorageReference? = null
    var uriImage: Uri? = null
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
        storage = FirebaseStorage.getInstance().reference

        ///////////////////////////////////////////////////////////////////////////////

        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if(binding.messageInput.text.toString().isNotEmpty()){

                    binding.micIcon.isVisible = false
                    binding.cameraIcon.isVisible = false
                    binding.sendIcon.isVisible = true
                }else{

                    binding.micIcon.isVisible = true
                    binding.cameraIcon.isVisible = true
                    binding.sendIcon.isVisible = false
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
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
        //attachment bottom sheet
        binding.attachIcon.setOnClickListener {
            attachmentBottomSheetPopup(requireContext())
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
                if(isAdded)
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserItems::class.java)
                binding.username.text=user!!.username
                if(isAdded){
                    Glide.with(requireContext()).asBitmap().load(Uri.parse(user.profilePhoto))
                        .placeholder(R.drawable.personalphotojpg).into(binding.userImage)
                }
            }
        })
        ////////////////////////////////////////////////////////////////////////////////////
        senderId=FirebaseAuth.getInstance()?.currentUser!!.uid
        getMyImage()
        //for send message
        binding.send.setOnClickListener()
        {
            if(binding.messageInput.text.isEmpty())
                Toast.makeText(requireContext(),"Message is empty",Toast.LENGTH_LONG).show()
            else {
                prepareMsg()
                if(msgModel!=null){
                    msgModel!!.msg = binding.messageInput.text.toString()
                    objChat!!.child(idMsg).setValue(
                        msgModel
                    )
                    binding.messageInput.setText("")
                }
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
        objUsers?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
               if(isAdded)
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
        chatListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                if(isAdded)
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                senderId= FirebaseAuth.getInstance().currentUser!!.uid
                receiverId=arguments?.getString("id").toString()
                for (data in snapshot.children) {
                    val chat = data.getValue(ChatModel::class.java)
                    if (chat != null) {
                        if (chat.senderId.equals(receiverId) && chat.receiverid.equals(senderId)) {
                            idMsg=chat.idMsg
                            val hashMap: HashMap<String, Any> = HashMap()
                            hashMap.put("seen", "seen")
                            objChat?.child(idMsg)?.updateChildren(hashMap as Map<String, Any>)?.addOnFailureListener {
                                if(isAdded)
                                    Toast.makeText(view!!.context, it.message, Toast.LENGTH_LONG).show()
                            }

                        }
                        if (chat.senderId.equals(senderId) && chat.receiverid.equals(receiverId) ||
                            chat.senderId.equals(receiverId) && chat.receiverid.equals(senderId)) {
                            chatList.add(chat)
                        }
                    }
                }
                if(isAdded){
                    if(chatList.size>=1)
                        setLastMsg(chatList[chatList.size-1].msg,chatList[chatList.size-1].time)
                    else
                        setLastMsg("  ","  ")
                    binding.progressBar.isVisible = false
                    val adapter = ChatAdapter(requireContext(),chatList)
                    binding.listview.adapter = adapter
                    binding.listview.post {
                        binding.listview.setSelection(chatList.size - 1)
                    }
                }
            }
        }
        objChat?.addValueEventListener(chatListener)
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
    // to upload image from gallery to send it in chat
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == RESULT_OK) {

            uriImage = data!!.data
            storage?.child("image/" + UUID.randomUUID().toString())?.putFile(uriImage!!)?.addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    imageMsg = uri.toString()
                    if(imageMsg!=null) {
                        prepareMsg()
                        if (msgModel != null) {
                            msgModel!!.imageMsg = imageMsg!!
                            objChat!!.child(idMsg).setValue(
                                msgModel
                            )
                        }
                    }

                }
                Toast.makeText(requireContext(),"Image uploaded successfully",Toast.LENGTH_LONG).show()

            }?.addOnFailureListener(){
                Toast.makeText(requireContext(),it.message,Toast.LENGTH_LONG).show()
            }
        }
        Log.d("image",imageMsg.toString())

    }
    private fun attachmentBottomSheetPopup(
        context:Context
    ){

        val popupView = LayoutInflater.from(context).inflate(R.layout.attachment_bottom_sheet, null)
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val marginInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            16f,
            context.resources.displayMetrics
        ).toInt()

        val popupWidth = screenWidth - (marginInPx * 2)

        val popupWindow = PopupWindow(
            popupView,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        val location = IntArray(2)
        binding.messageInput.getLocationOnScreen(location)
        val editTextX = location[0]
        val editTextY = location[1]

        popupWindow.showAtLocation( binding.messageInput, Gravity.NO_GRAVITY, editTextX, editTextY - popupView.measuredHeight)

        val gallery = popupView.findViewById<CardView>(R.id.gallery)
        gallery.setOnClickListener {
            val intentImage = Intent(ACTION_PICK)
            intentImage.type = "image/*"
            startActivityForResult(intentImage, 2)
            popupWindow.dismiss()
        }
    }
    private fun prepareMsg(){
        var currentTime:String=""
        var calendar=Calendar.getInstance()
        val hour12hrs: Int = calendar.get(Calendar.HOUR)
        val minutes: Int = calendar.get(Calendar.MINUTE)
        if(calendar.get(Calendar.AM_PM) == Calendar.AM)
            currentTime="$hour12hrs : $minutes AM"
        else
            currentTime="$hour12hrs : $minutes PM"
        idMsg = objChat!!.push()?.key.toString()
        msgModel = ChatModel(
            idMsg,
            myImage,
            "",
            "",
            senderId,
            receiverId
            ,currentTime,"",""
        )
    }
    override fun onStop() {
        super.onStop()
        objChat?.removeEventListener(chatListener)
    }

}