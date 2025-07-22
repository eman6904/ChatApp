package com.example.chatapp.ui.userInterface.ui.fragments

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PICK
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.view.Window
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.example.chatapp.ui.userInterface.ui.model.DeletedMessageModel
import com.example.chatapp.ui.userInterface.ui.model.ImageModel
import com.example.chatapp.ui.userInterface.ui.model.RecordModel
import com.example.chatapp.ui.userInterface.ui.model.UserItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
    private var startTime: Long = 0L
    private var recordedDuration: Long = 0L
    private var startX = 0f
    private val cancelThreshold = 150f
    private lateinit var blinkAnimation:android.view.animation.Animation
    private var timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
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
        blinkAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.blink)


        //////////////////////////////////////////////////////////////////////////////////
        //to make RecyclerView scroll to the last item when the keyboard opens
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight =  binding.root.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15 && chatAdapter.itemCount>0) {
                binding.listview.postDelayed({
                    binding.listview.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }, 100)
            }
        }

       ////////////////////////////////////////////////////////////////////////////
        viewModel.editMode.observe(viewLifecycleOwner){ enable->

            if(enable){
                binding.editingPopup.isVisible = true
                binding.chatInputContainer.setBackgroundColor(Color.parseColor("#cc000000"))
                binding.micIcon.isVisible = false
                binding.doneIcon.isVisible = true
                binding.cameraIcon.isVisible = false
                binding.attachIcon.isVisible = false
            }else{
                binding.editingPopup.isVisible = false
                binding.micIcon.isVisible = true
                binding.doneIcon.isVisible = false
                binding.cameraIcon.isVisible = true
                binding.attachIcon.isVisible = true
                binding.send.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
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
        /////////////////////////////////////////////////////////////////////////////
        requireActivity().findViewById<ImageView>(R.id.delete_icon).setOnClickListener {

            showDeleteConfirmationDialog(requireContext())
            dismissPopupIfVisible()
        }
        ///////////////////////////////////////////////////////////////////////////////
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {

            dismissPopupIfVisible()
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
                    startX = event.rawX
                    binding.recordingContainer.isVisible = true
                    binding.msgContainer.isVisible = false
                    startRecording(requireContext())
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val currentX = event.rawX
                    val deltaX = currentX - startX

                    if (deltaX < 0) {
                        binding.micIcon.translationX = deltaX
                    }

                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.recordingContainer.isVisible = false
                    binding.msgContainer.isVisible = true

                    val deltaX = event.rawX - startX

                    if (deltaX < -cancelThreshold) {
                        stopRecording()
                        audioFile?.delete()
                        Toast.makeText(requireContext(), "record stopped", Toast.LENGTH_SHORT).show()
                    } else {
                        stopRecording()
                        prepareMsg(msgType = "record")

                        uploadVoiceMessage(
                            onSuccess = { remoteUrl ->
                                msgModel?.let {
                                    it.recordMsg = RecordModel(
                                        it.recordMsg.recordLocalPath,
                                        remoteUrl,
                                        recordedDuration.toString(),
                                        false
                                    )

                                    viewModel.insertMessage(msg = it)

                                    if (viewModel.isConnected.value == true) {

                                        viewModel.uploadMessage(msgModel!!)

                                    }
                                }
                            },
                            onFailure = {
                                Toast.makeText(requireContext(), "failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    binding.micIcon.animate()
                        .translationX(0f)
                        .setDuration(200)
                        .start()

                    startX = 0f
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
                    //editText.paintFlags = editText.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
                    if(viewModel.editMode.value==false){

                        binding.micIcon.isVisible = false
                        binding.cameraIcon.isVisible = false
                        binding.sendIcon.isVisible = true

                    }else{

                        binding.doneIcon.isVisible = true
                        binding.micIcon.isVisible = false
                        binding.cameraIcon.isVisible = false
                        binding.attachIcon.isVisible = false
                        binding.send.backgroundTintList =
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                        binding.doneIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
                    }

                } else {

                    if(viewModel.editMode.value == false){

                        binding.micIcon.isVisible = true
                        binding.cameraIcon.isVisible = true
                        binding.sendIcon.isVisible = false

                    }else{

                        binding.doneIcon.isVisible = true
                        binding.micIcon.isVisible = false
                        binding.cameraIcon.isVisible = false
                        binding.attachIcon.isVisible = false
                        binding.send.backgroundTintList =
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.disabledIconColor))
                        binding.doneIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.iconsColor))
                    }
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
                        .placeholder(R.drawable.profile_ic).into(binding.userImage)
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
            else if(viewModel.editMode.value== false) {
                prepareMsg(
                    msgType = "text"
                )
                if (msgModel != null) {
                    msgModel!!.textMsg = binding.messageInput.text.toString()
                    viewModel.insertMessage(msgModel!!)
                    if (viewModel.isConnected.value == true) {

                        viewModel.uploadMessage(msgModel!!)

                    }
                    binding.messageInput.setText("")
                }
            }else{

                val updatedMessage = viewModel.editedMessage.value?.copy(
                    textMsg = binding.messageInput.text.toString(),
                    edited = true
                )
                val position = viewModel.selectedMessages.value?.getOrNull(0)

                Log.d("edit",position.toString()+updatedMessage.toString())

                if (updatedMessage != null && position != null) {

                    viewModel.updateMessages(listOf(updatedMessage))

                    if (viewModel.isConnected.value == true) {

                        viewModel.uploadMessage(updatedMessage)

                    }
                    chatAdapter.notifyItemChanged(position)

                    binding.messageInput.setText("")
                    viewModel.setEditMode(false)
                    viewModel.clearSelectedMessages()
                }

            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)

        val pos = viewModel.selectedMessages.value?.getOrNull(0)
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid

        if (pos != null && currentUser != null && viewModel.messages.value?.getOrNull(pos)?.deleted?.sides == 0) {
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

        dismissPopupIfVisible()

        return when (item.itemId) {

            R.id.action_edit ->{

                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                viewModel.setEditMode(true)

                viewModel.selectedMessages.value?.let { it->

                    val pos = it.get(0)

                    viewModel.setMessageForEdit(viewModel.messages.value[pos])
                    viewModel.editedMessage.value?.let { binding.messageInput.setText(it.textMsg) }

                    binding.textMessageBody.text =   viewModel.editedMessage.value?.textMsg

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

                dismissPopupIfVisible()
            }

            override fun onLongClick(view: View,position: Int, model: MessageTable) {
                dismissPopupIfVisible()
                if(isAdded&&viewModel.messages.value[position].deleted.sides==0){
                    showReactionPopup(
                        anchorView = view,
                        context = requireContext(),
                        onReactionSelected = { selectedReaction->

                            var action = if(selectedReaction==viewModel.messages.value[position].action) "" else selectedReaction

                            viewModel.updateMessages(
                                listOf( viewModel.messages.value[position].copy(
                                action = action
                            )))
                            if (viewModel.isConnected.value == true) {

                                viewModel.uploadMessage(viewModel.messages.value[position].copy(
                                    action = action
                                ))

                            }
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
                        if(lastMsg.msgType=="record")
                           setLastMsg("record", lastMsg.time)
                        else
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
                            if (viewModel.isConnected.value == true) {

                                viewModel.uploadMessage(msgModel!!)

                            }
                        }

                    }

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

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }
            binding.mic3Icon.startAnimation(blinkAnimation)
            startTime = System.currentTimeMillis()
            startTimer()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to prepare the recorder", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            Toast.makeText(context, "Recorder is in illegal state", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                val mins = elapsed / 60
                val secs = elapsed % 60
                binding.timer.text = String.format("%02d:%02d", mins, secs)
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)
    }


    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            recordedDuration = (System.currentTimeMillis() - startTime) / 1000
            timerHandler.removeCallbacks(timerRunnable)
            binding.mic3Icon.clearAnimation()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Recorder is in illegal state", Toast.LENGTH_SHORT).show()
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

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]
        val anchorWidth = anchorView.width

        popupWindow?.showAtLocation(
            anchorView,
            Gravity.NO_GRAVITY,
            anchorX + (anchorWidth / 2) - (popupWidth / 2),
            anchorY - popupHeight
        )
    }
    private fun showDeleteConfirmationDialog(
        context: Context
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.delete_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val width = (context.resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
        var title: TextView? = null
        var deleteForEveryOne: TextView? = null
        var deleteForMe: TextView? = null
        var cancel: TextView? = null

        val list = viewModel.selectedMessages.value?.filter { pos ->
            viewModel.messages.value?.get(pos)?.senderId != FirebaseAuth.getInstance()?.currentUser!!.uid
        }

        if (list != null && list.isNotEmpty()) {

            dialog.findViewById<LinearLayout>(R.id.dialog2).isVisible = true
            dialog.findViewById<LinearLayout>(R.id.dialog1).isVisible = false
            title = dialog.findViewById(R.id.title2)
            deleteForMe = dialog.findViewById(R.id.delet_for_me2)
            cancel = dialog.findViewById(R.id.cancel2)
        } else {
            dialog.findViewById<LinearLayout>(R.id.dialog2).isVisible = false
            dialog.findViewById<LinearLayout>(R.id.dialog1).isVisible = true
            title = dialog.findViewById<TextView?>(R.id.title1)
            deleteForMe = dialog.findViewById(R.id.delete_for_me1)
            deleteForEveryOne = dialog.findViewById(R.id.delete_everyone1)
            cancel = dialog.findViewById(R.id.cancel1)

        }
        if(viewModel.selectedMessages.value?.size==1)
           title.setText("Delete message ?")
        else
           title.setText("Delete ${viewModel.selectedMessages.value?.size} messages ?")
        cancel?.setOnClickListener {
            dialog.dismiss()
        }
        deleteForMe?.setOnClickListener {
            deleteMessages(
                sides = 1
            )
            dialog.dismiss()
        }
        deleteForEveryOne?.setOnClickListener {
            deleteMessages(
                sides = 2
            )
            dialog.dismiss()
        }
    }

    private fun deleteMessages(sides: Int) {
        val selectedPositions = viewModel.selectedMessages.value
        val messagesList = viewModel.messages.value?.toMutableList() ?: return
        lifecycleScope.launch {
            val updatedMessages = mutableListOf<MessageTable>()
            if (selectedPositions != null) {
                for (pos in selectedPositions) {

                    val message = messagesList[pos]
                    val updatedMessage = message.copy(
                        deleted = DeletedMessageModel(
                            sides = sides,
                            userId = FirebaseAuth.getInstance().currentUser?.uid!!
                        )
                    )
                    updatedMessages.add(updatedMessage)

                    viewModel.updateMessages(listOf(updatedMessage))

                    if (viewModel.isConnected.value == true) {

                        viewModel.uploadMessage(updatedMessage)

                    }
                    viewModel.removePosition(pos)

                    chatAdapter.notifyItemChanged(pos)
                }

            }
        }
    }


}


