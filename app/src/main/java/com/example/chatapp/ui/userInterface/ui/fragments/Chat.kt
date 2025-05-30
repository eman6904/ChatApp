package com.example.chatapp.ui.userInterface.ui.fragments

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PICK
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentChatBinding
import com.example.chatapp.ui.userInterface.localData.messages.table.MessageTable
import com.example.chatapp.ui.userInterface.localData.messages.viewModel.MessageViewModel
import com.example.chatapp.ui.userInterface.ui.adapter.ChatAdapter
import com.example.chatapp.ui.userInterface.ui.adapter.ReactionsAdapter
import com.example.chatapp.ui.userInterface.ui.model.ChatModel
import com.example.chatapp.ui.userInterface.ui.model.ImageModel
import com.example.chatapp.ui.userInterface.ui.model.RecordModel
import com.example.chatapp.ui.userInterface.ui.model.UserItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList

class Chat : Fragment(R.layout.fragment_chat) {
    private lateinit var binding: FragmentChatBinding
    private lateinit var navController: NavController
    private lateinit var chatListener: ValueEventListener
    private lateinit var messagesList: ArrayList<MessageTable>
    private lateinit var chatAdapter: ChatAdapter
    var msgModel: MessageTable? = null
    var storage: StorageReference? = null
    var uriImage: Uri? = null
    var objUsers: DatabaseReference? = null
    var objChat: DatabaseReference? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    var senderId: String = ""
    var receiverId: String = ""
    var myImage: String = ""
    var isUserScrolling = false
    val viewModel: MessageViewModel by viewModels()
    companion object{
        var popupWindow: PopupWindow? = null
        fun dismissPopupIfVisible() {
            popupWindow?.dismiss()
            popupWindow = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)
        navController = Navigation.findNavController(view)

        objChat = FirebaseDatabase.getInstance().getReference("Chat")
        storage = FirebaseStorage.getInstance().reference
        val activity = activity as MainActivity

        viewModel.editMode.observe(viewLifecycleOwner){ enable->

            if(enable){
                binding.editingPopup.isVisible = true
                binding.chatInputContainer.setBackgroundColor(Color.parseColor("#cc000000"))
            }else{
                binding.editingPopup.isVisible = false
                binding.chatInputContainer.background = null
                binding.messageInput.text.clear()
            }
        }
        viewModel.selectedMessages.observe(viewLifecycleOwner){ selectedMessages->

            if(selectedMessages.size==1){

                setHasOptionsMenu(true)
                activity.supportActionBar?.show()
                activity?.invalidateOptionsMenu()
            }else if(selectedMessages.size>1){

                setHasOptionsMenu(false)
                activity.supportActionBar?.show()
            }else{
                activity.supportActionBar?.hide()
            }
        }

        ///////////////////////////////////////////////////////////////////////////////
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {

            if(viewModel.selectedMessages.value!!.isNotEmpty()) {

                viewModel.clearSelectedMessages()
                chatAdapter.notifyDataSetChanged()

            }else if(viewModel.editMode.value==true){

                viewModel.setEditMode(false)
            } else{
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }
        ///////////////////////////////////////////////////////////////////////////////
        //for record message
        binding.micIcon.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // المستخدم بدأ يضغط
                    binding.recordingContainer.isVisible = true
                    binding.msgContainer.isVisible = false
                    startRecording(requireContext())
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // المستخدم ساب الزر أو سحب صباعه بعيد
                    binding.recordingContainer.isVisible = false
                    binding.msgContainer.isVisible = true
                    stopRecording()
                    prepareMsg(
                        msgType = "record"
                    )
                    uploadVoiceMessage(
                        onSuccess = { remoteUrl ->
                            if (msgModel != null) {
                                msgModel!!.recordMsg = RecordModel(
                                    msgModel!!.recordMsg.recordLocalPath,
                                    remoteUrl,
                                    "",
                                    false
                                )
                                viewModel.insertMessage(
                                    msg = msgModel!!
                                )
                            }
                        },
                        onFailure = {
                            Toast.makeText(requireContext(), "فشل في الرفع", Toast.LENGTH_SHORT)
                                .show()
                        }
                    )

                    true
                }

                else -> false
            }
        }
        ///////////////////////////////////////////////////////////////////////////////
        //for handling mic icon and send icon when write msg
        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (binding.messageInput.text.toString().isNotEmpty()) {

                    binding.micIcon.isVisible = false
                    binding.cameraIcon.isVisible = false
                    binding.sendIcon.isVisible = true
                } else {

                    binding.micIcon.isVisible = true
                    binding.cameraIcon.isVisible = true
                    binding.sendIcon.isVisible = false
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        /////////////////////////////////////////////////////////////////////////////////

        //////////////////////////////////////////////////////////////////////////////////
        //attachment bottom sheet
        binding.attachIcon.setOnClickListener {
            attachmentBottomSheetPopup(requireContext())
        }
        //////////////////////////////////////////////////////////////////////////////////
//        //for delete or update msg
//        binding.listview.onItemLongClickListener=object :AdapterView.OnItemLongClickListener{
//            override fun onItemLongClick(
//                p0: AdapterView<*>?,
//                p1: View?,
//                position: Int,
//                p3: Long
//            ): Boolean {
//               if(messagesList[position].senderId==senderId)
//               {
//                   val alertbuilder = AlertDialog.Builder(requireContext())
//                   val view = layoutInflater.inflate(R.layout.about_message, null)
//                   alertbuilder.setView(view)
//                   val alertDialog = alertbuilder.create()
//                   alertDialog.show()
//
//                   val delete = view.findViewById<ImageView>(R.id.delete)
//                   val update = view.findViewById<ImageView>(R.id.update)
//                   val edMsg = view.findViewById<EditText>(R.id.edMsg)
//
//                   var message=messagesList.get(position)
//                   edMsg.setText(message.textMsg)
//
//                   update.setOnClickListener()
//                   {
//                       message.textMsg=edMsg.text.toString()
//                       objChat?.child(message.msgId)?.setValue(message)
//                       alertDialog.dismiss()
//                   }
//                   delete.setOnClickListener(){
//                       objChat?.child(message.msgId)?.removeValue()
//                       alertDialog.dismiss()
//                   }
//               }
//                return false
//            }
//
//        }
        ///////////////////////////////////////////////////////////////////////////////////
        binding.arrowBack.setOnClickListener() {
            navController.navigate(R.id.action_chat_to_users)
        }
        receiverId = arguments?.getString("id").toString()
        objUsers = FirebaseDatabase.getInstance().getReference("User").child(receiverId)
        objUsers?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                if (isAdded)
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserItems::class.java)
                binding.username.text = user!!.username
                if (isAdded) {
                    Glide.with(requireContext()).asBitmap().load(Uri.parse(user.profilePhoto))
                        .placeholder(R.drawable.personalphotojpg).into(binding.userImage)
                }
            }
        })
        ////////////////////////////////////////////////////////////////////////////////////
        senderId = FirebaseAuth.getInstance()?.currentUser!!.uid
        getMyImage()
        //for send message
        binding.send.setOnClickListener()
        {
            if (binding.messageInput.text.isEmpty())
                Toast.makeText(requireContext(), "Message is empty", Toast.LENGTH_LONG).show()
            else {
                prepareMsg(
                    msgType = "text"
                )
                if (msgModel != null) {
                    msgModel!!.textMsg = binding.messageInput.text.toString()
                    viewModel.insertMessage(msgModel!!)
                    if (viewModel.isConnected.value == true) {

                        viewModel.uploadPendingMessages()
                        viewModel.downloadMessagesFromFirebase()
                    }
                    binding.messageInput.setText("")
                }
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)

        dismissPopupIfVisible()

        val pos = viewModel.selectedMessages.value?.getOrNull(0)
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid

        if (pos != null && currentUser != null) {
            val selectedMessage = viewModel.messages.value?.getOrNull(pos)

            if (selectedMessage != null && selectedMessage.senderId == currentUser) {

                menu.findItem(R.id.action_report)?.isVisible = false

            }else if(selectedMessage != null && selectedMessage.receiverId == currentUser){

                menu.findItem(R.id.action_inf)?.isVisible = false
                menu.findItem(R.id.action_edit)?.isVisible = false
                
            }
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit ->{

                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                viewModel.setEditMode(true)

                viewModel.selectedMessages.value?.let { it->
                    val pos = it.get(0)
                    viewModel.setMessageForEdit(viewModel.messages.value[pos])
                    viewModel.editedMessage.value?.let { binding.messageInput.setText(it.textMsg) }

                    viewModel.clearSelectedMessages()
                    chatAdapter.notifyItemChanged(pos)

                    binding.messageInput.setSelection(binding.messageInput.text.length)
                    binding.messageInput.requestFocus()
                    binding.messageInput.post {
                        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                    }

               }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onStart() {
        super.onStart()

        readMessage()
        viewModel.startNetworkMonitoring()
        messagesList = ArrayList()
        binding.listview.layoutManager = LinearLayoutManager(requireContext())

        chatAdapter = ChatAdapter(requireContext(), messagesList, viewModel)
        chatAdapter.setOnClickListener(object : ChatAdapter.OnClickListener {
            override fun onClick(position: Int, model: MessageTable) {
                // هنا تعملي اللي عايزاه عند الضغط العادي
                Log.d("press","onclick")
                dismissPopupIfVisible()
            }

            override fun onLongClick(view: View,position: Int, model: MessageTable) {
                dismissPopupIfVisible()
                if(isAdded){
                    showReactionPopup(
                        anchorView = view,
                        context = requireContext(),
                        onReactionSelected = { selectedReaction->

                            var action = if(selectedReaction==viewModel.messages.value[position].action) "" else selectedReaction

                            viewModel.updateMessage(viewModel.messages.value[position].copy(
                                action = action
                            ))
                            viewModel.removePosition(position)

                        }
                    )
                }
            }
        })
        binding.listview.adapter = chatAdapter

        viewModel.downloadMessagesFromFirebase()

        lifecycleScope.launchWhenStarted {
            viewModel.messages.collect { messages ->
                viewModel.uploadPendingMessages()
                val filteredMessages = messages.filter { msg ->
                    (msg.senderId == senderId && msg.receiverId == receiverId) ||
                            (msg.senderId == receiverId && msg.receiverId == senderId)
                }
                if (isAdded) {
                    val oldMessages = messagesList
                    val newMessagesList = filteredMessages

                    newMessagesList.forEachIndexed { index, newMsg ->
                        if (index < oldMessages.size) {
                            if (oldMessages[index] != newMsg) {
                                oldMessages[index] = newMsg
                                chatAdapter.notifyItemChanged(index)
                            }
                        } else {
                            oldMessages.add(newMsg)
                            chatAdapter.notifyItemInserted(index)
                            binding.listview.post {
                                if (!isUserScrolling) {
                                    binding.listview.scrollToPosition(messagesList.size - 1)
                                }
                            }
                        }
                    }

                    if (messagesList.isNotEmpty()) {
                        val lastMsg = messagesList.last()
                        setLastMsg(lastMsg.textMsg, lastMsg.time)
                    }

                }
            }
        }
    }

    private fun getMyImage() {
        objUsers = FirebaseDatabase.getInstance().getReference("User").child(senderId)
        objUsers?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                if (isAdded)
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserItems::class.java)
                myImage = user!!.profilePhoto
            }
        })
    }

    private fun readMessage() {

        chatListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                if (isAdded)
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                senderId = FirebaseAuth.getInstance().currentUser!!.uid
                receiverId = arguments?.getString("id").toString()
                for (data in snapshot.children) {
                    val chat = data.getValue(ChatModel::class.java)
                    if (chat != null) {
                        if (chat.senderId.equals(receiverId) && chat.receiverId.equals(senderId)) {
                            val hashMap: HashMap<String, Any> = HashMap()
                            hashMap.put("status", "seen")
                            objChat?.child(chat.msgId)?.updateChildren(hashMap as Map<String, Any>)
                                ?.addOnFailureListener {
                                    if (isAdded)
                                        Toast.makeText(
                                            view!!.context,
                                            it.message,
                                            Toast.LENGTH_LONG
                                        ).show()
                                }

                        }
                    }
                }
            }
        }
        objChat?.addValueEventListener(chatListener)
    }

    private fun setLastMsg(lastMsg: String, currentTime: String) {
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

            uriImage = data?.data
            if (uriImage != null && isAdded) {
                prepareMsg(
                    msgType = "image"
                )
                if (msgModel != null) {
                    msgModel!!.imageMsg.imageLocalPath =
                        copyImageToAppStorage(requireContext(), uriImage!!).toString()
                    viewModel.insertMessage(msgModel!!)
                }
            }
            storage?.child("image/" + UUID.randomUUID().toString())?.putFile(uriImage!!)
                ?.addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                        if (msgModel != null) {
                            msgModel!!.imageMsg.imageRemoteUrl = uri.toString()
                            viewModel.insertMessage(msgModel!!)
                        }

                    }
                    // Toast.makeText(requireContext(),"Image uploaded successfully",Toast.LENGTH_LONG).show()

                }?.addOnFailureListener() {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                }
        }

    }

    private fun attachmentBottomSheetPopup(
        context: Context
    ) {

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

        popupWindow.showAtLocation(
            binding.messageInput,
            Gravity.NO_GRAVITY,
            editTextX,
            editTextY - popupView.measuredHeight
        )

        val gallery = popupView.findViewById<CardView>(R.id.gallery)
        gallery.setOnClickListener {
            val intentImage = Intent(ACTION_PICK)
            intentImage.type = "image/*"
            startActivityForResult(intentImage, 2)
            popupWindow.dismiss()
        }
    }

    private fun prepareMsg(msgType: String) {
        var currentTime: String = ""
        var calendar = Calendar.getInstance()
        val hour12hrs: Int = calendar.get(Calendar.HOUR)
        val minutes: Int = calendar.get(Calendar.MINUTE)
        if (calendar.get(Calendar.AM_PM) == Calendar.AM)
            currentTime = "$hour12hrs : $minutes AM"
        else
            currentTime = "$hour12hrs : $minutes PM"
        val msgId = objChat!!.push()?.key.toString()
        msgModel = MessageTable(
            msgId = msgId,
            msgType = msgType,
            profileImage = myImage,
            textMsg = "",
            imageMsg = ImageModel("", ""),
            recordMsg = RecordModel("", "", "", false),
            senderId = senderId,
            receiverId = receiverId, time = currentTime, action = "", status = ""
        )
    }

    override fun onStop() {
        super.onStop()
        objChat?.removeEventListener(chatListener)
        viewModel.stopNetworkMonitoring()
    }

    private fun startRecording(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
            Toast.makeText(context, "Please allow microphone permission first", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val fileName = "record_${System.currentTimeMillis()}.mp3"
        audioFile = File(context.cacheDir, fileName)

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile!!.absolutePath)
            prepare()
            start()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uploadVoiceMessage(
        onSuccess: (remoteUrl: String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val voiceRef = storageRef.child("voiceMessages/${audioFile!!.name}")
        val uri = Uri.fromFile(audioFile)
        msgModel?.let {
            msgModel = msgModel!!.copy(
                recordMsg = msgModel!!.recordMsg.copy(recordLocalPath = uri.toString())
            )
            viewModel.insertMessage(
                msgModel!!
            )
        }
        voiceRef.putFile(uri)
            .addOnSuccessListener {
                voiceRef.downloadUrl.addOnSuccessListener { remoteUrl ->
                    onSuccess(remoteUrl.toString())
                }
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    private fun copyImageToAppStorage(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    fun showReactionPopup(anchorView: View, context: Context, onReactionSelected: (String) -> Unit) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.dialog_reaction, null)
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.reactionsRecyclerView)
        popupWindow = PopupWindow(popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            60.dpToPx(context),
            false)

        popupWindow?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow?.isOutsideTouchable = false
        popupWindow?.elevation = 10f

        val reactions = listOf("👍","❤️", "😂", "😍", "😢", "😮", "👎")

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = ReactionsAdapter(reactions) {
            onReactionSelected(it)
            popupWindow?.dismiss()
        }

        // قياس الـ popup عشان نعرف أبعاده
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        // تحديد مكان العنصر اللي ضغط عليه المستخدم
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]
        val anchorWidth = anchorView.width

        // نعرض البوب أب فوق الرسالة وفي نصها
        popupWindow?.showAtLocation(
            anchorView,
            Gravity.NO_GRAVITY,
            anchorX + (anchorWidth / 2) - (popupWidth / 2),
            anchorY - popupHeight
        )
    }

}


