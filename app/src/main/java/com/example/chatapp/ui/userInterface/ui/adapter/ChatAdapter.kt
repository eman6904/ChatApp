package com.example.chatapp.ui.userInterface.ui.adapter

import android.app.Activity
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.compose.animation.core.animateDpAsState
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.chatapp.R
import com.example.chatapp.ui.userInterface.localData.messages.table.MessageTable
import com.example.chatapp.ui.userInterface.localData.messages.viewModel.MessageViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.File

class ChatAdapter(
    private val context: Context,
    private val chatList: List<MessageTable>,
    private val viewModel: MessageViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object com {
        private const val RIGHT = 0
        private const val LEFT = 1
    }
    private var _onClickListener: OnClickListener? = null

    override fun getItemViewType(position: Int): Int {
        return if (chatList[position].senderId == FirebaseAuth.getInstance().currentUser!!.uid) RIGHT else LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutId = if (viewType == RIGHT) R.layout.item_right else R.layout.item_left
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)
        return ChatViewHolder(view)
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is ChatViewHolder) {
            holder.bind(chatList[position], position)
        }


        holder.itemView.setOnClickListener {

            val adapterPosition = holder.bindingAdapterPosition

            if (adapterPosition != RecyclerView.NO_POSITION) {

                if (viewModel.selectedMessages.value!!.isNotEmpty()&&viewModel.messages.value[adapterPosition].deleted.sides==0) {

                    viewModel.addPosition(adapterPosition)
                    notifyItemChanged(adapterPosition)
                }
                _onClickListener?.onClick(adapterPosition, chatList[adapterPosition])
            }
        }


        holder.itemView.setOnLongClickListener {

            val adapterPosition = holder.bindingAdapterPosition

            if (adapterPosition != RecyclerView.NO_POSITION && viewModel.selectedMessages.value!!.isEmpty()&&
                viewModel.messages.value[adapterPosition].deleted.sides==0) {

                viewModel.addPosition(adapterPosition)

                notifyItemChanged(adapterPosition)

                _onClickListener?.onLongClick(it, adapterPosition, chatList[adapterPosition])
            }
            true
        }


        if (viewModel.selectedMessages.value!!.contains(position)) {
            holder.itemView.findViewById<View>(R.id.selectionOverlay).isVisible = true
        } else {
            holder.itemView.findViewById<View>(R.id.selectionOverlay).isVisible = false
        }

    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        _onClickListener = onClickListener
    }

    interface OnClickListener {
        fun onClick(position: Int, model: MessageTable)
        fun onLongClick(view: View, position: Int, model: MessageTable)
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: MessageTable, position: Int) {
            val action = itemView.findViewById<TextView>(R.id.action)
            val tail = itemView.findViewById<ImageView>(R.id.tail)
            val msgLayout = itemView.findViewById<RelativeLayout>(R.id.msg)
            val actionBackground = itemView.findViewById<CardView>(R.id.actionBackground)

            actionBackground.isVisible = item.action.isNotEmpty()
            action.text = item.action
            if(item.action.isNotEmpty()){

                val scale = context.resources.displayMetrics.density
                val paddingInPx = (20 * scale + 0.5f).toInt()

                msgLayout.setPadding(
                    msgLayout.paddingLeft,
                    msgLayout.paddingTop,
                    msgLayout.paddingRight,
                    paddingInPx
                )
            }else{

                val scale = context.resources.displayMetrics.density
                val paddingInPx = (0 * scale + 0.5f).toInt()

                msgLayout.setPadding(
                    msgLayout.paddingLeft,
                    msgLayout.paddingTop,
                    msgLayout.paddingRight,
                    paddingInPx
                )
            }

            tail.isVisible =
                !(position > 0 && getItemViewType(position) == getItemViewType(position - 1))

            if (item.deleted.sides == 2) {
                bindDeletedMessage(item)
                actionBackground.isVisible = false
                action.text = ""
            }else if( (item.deleted.sides == 1 && item.deleted.userId == FirebaseAuth.getInstance().currentUser!!.uid)) {
                bindDeletedMessage(item)
                actionBackground.isVisible = false
                action.text = ""
            }else {
                when (item.msgType) {
                    "text" -> bindTextMessage(item)
                    "image" -> bindImageMessage(item)
                    else -> bindRecordMessage(item)
                }
            }
        }

        private fun bindTextMessage(item: MessageTable) {
            itemView.findViewById<LinearLayout>(R.id.text_msg).isVisible = true
            itemView.findViewById<LinearLayout>(R.id.image_msg).isVisible = false
            itemView.findViewById<LinearLayout>(R.id.deleted_msg).isVisible = false
            itemView.findViewById<LinearLayout>(R.id.record_msg).isVisible = false

            itemView.findViewById<TextView>(R.id.text_message_body).text = item.textMsg
            itemView.findViewById<TextView>(R.id.text_message_time).text = item.time

            val seen = itemView.findViewById<TextView>(R.id.seen)
            seen.isVisible = getItemViewType(adapterPosition) == RIGHT && item.status == "seen"

            val edited = itemView.findViewById<TextView>(R.id.edited)
            edited.isVisible = item.edited
        }

        private fun bindImageMessage(item: MessageTable) {
            itemView.findViewById<LinearLayout>(R.id.image_msg).isVisible = true
            itemView.findViewById<LinearLayout>(R.id.text_msg).isVisible = false
            itemView.findViewById<LinearLayout>(R.id.deleted_msg).isVisible = false
            itemView.findViewById<LinearLayout>(R.id.record_msg).isVisible = false

            if (context is Activity && !context.isDestroyed) {
                Glide.with(context)
                    .load(File(item.imageMsg.imageLocalPath))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.findViewById(R.id.image))
            }

            itemView.findViewById<TextView>(R.id.image_msg_time).text = item.time
            val seen = itemView.findViewById<TextView>(R.id.image_seen)
            seen.isVisible = getItemViewType(adapterPosition) == RIGHT && item.status == "seen"

        }

        private fun bindRecordMessage(item: MessageTable) {

            itemView.findViewById<LinearLayout>(R.id.text_msg).isVisible = false
            itemView.findViewById<LinearLayout>(R.id.image_msg).isVisible = false
            itemView.findViewById<LinearLayout>(R.id.deleted_msg).isVisible = false
            itemView.findViewById<LinearLayout>(R.id.record_msg).isVisible = true

            val playIcon = itemView.findViewById<ImageView>(R.id.play_icon)
            val pauseIcon = itemView.findViewById<ImageView>(R.id.pause_icon)
            val micIcon = itemView.findViewById<ImageView>(R.id.mic_icon)
            val timer = itemView.findViewById<TextView>(R.id.timerTextView)
            val thumbDot = itemView.findViewById<View>(R.id.thumbDot)
            val listenProgressView = itemView.findViewById<View>(R.id.listenedProgressView)
            val fullProgressView = itemView.findViewById<View>(R.id.fullProgressView)
            val thumbDotBackground = thumbDot.background as GradientDrawable


            timer.text =if(item.recordMsg.recordLength.isNotEmpty())
                formatDuration(item.recordMsg.recordLength.toLong()) else ""

            val color = if (item.recordMsg.listen) R.color.listenRecordColor else R.color.iconsColor
            listOf(playIcon, pauseIcon, micIcon).forEach {
                it.setColorFilter(ContextCompat.getColor(context, color), PorterDuff.Mode.SRC_IN)
            }
            thumbDotBackground.setColor(ContextCompat.getColor(context, color))

            listenProgressView.setBackgroundColor(ContextCompat.getColor(context,color))

            if (AudioPlayerManager.currentPath == item.recordMsg.recordLocalPath) {

                pauseIcon.isVisible = true
                playIcon.isVisible = false

            } else {
                thumbDot.translationX = 0f
                val layoutParams = listenProgressView.layoutParams
                layoutParams.width = 0
                listenProgressView.layoutParams = layoutParams

                pauseIcon.isVisible = false
                playIcon.isVisible = true
            }

            itemView.findViewById<FrameLayout>(R.id.play_record).setOnClickListener {

                if (AudioPlayerManager.currentPath != item.recordMsg.recordLocalPath) {

                    Log.d("DEBUG",AudioPlayerManager.previusPosition.toString())
                    AudioPlayerManager.play(
                        path = item.recordMsg.recordLocalPath,
                        clearPath = {
                            notifyItemChanged(AudioPlayerManager.previusPosition!!)
                        },
                        onTick = { remainingMillis, progress ->
                            (itemView.context as Activity).runOnUiThread {
                                val remainingSecs = remainingMillis / 1000
                                val mins = remainingSecs / 60
                                val secs = remainingSecs % 60
                                timer.text = String.format("%02d:%02d", mins, secs)

                                val maxTranslate = fullProgressView.width - thumbDot.width
                                val currentX = progress * maxTranslate
                                thumbDot.translationX = currentX

                                val layoutParams = listenProgressView.layoutParams
                                layoutParams.width = currentX.toInt() + thumbDot.width / 2
                                listenProgressView.layoutParams = layoutParams
                            }
                        }
                    )

                    if (FirebaseAuth.getInstance().currentUser?.uid == item.receiverId && !item.recordMsg.listen) {
                        viewModel.updateMessages(
                            listOf(item.copy(
                                recordMsg = item.recordMsg.copy(
                                    listen = true
                                )
                            ))
                        )
                        if(viewModel.isConnected.value==true){
                            viewModel.uploadMessage(item.copy(
                                recordMsg = item.recordMsg.copy(
                                    listen = true
                                )
                            ))
                        }
                    }
                    AudioPlayerManager.previusPosition?.let {
                        if(AudioPlayerManager.previusPosition!= adapterPosition)
                          notifyItemChanged(AudioPlayerManager.previusPosition!!)
                    }
                    AudioPlayerManager.previusPosition = position
                    pauseIcon.isVisible = true
                    playIcon.isVisible = false

                } else {
                    pauseIcon.isVisible = !pauseIcon.isVisible
                    playIcon.isVisible = !playIcon.isVisible
                    if(AudioPlayerManager.isPaused())
                        AudioPlayerManager.resume(
                            clearPath = {
                                notifyItemChanged(AudioPlayerManager.previusPosition!!)
                            },
                            onTick = { remainingMillis, progress ->

                                val remainingSecs = remainingMillis / 1000
                                val mins = remainingSecs / 60
                                val secs = remainingSecs % 60
                                timer.text = String.format("%02d:%02d", mins, secs)

                                val maxTranslate = fullProgressView.width - thumbDot.width
                                val currentX = progress * maxTranslate

                                thumbDot.translationX = currentX

                                val layoutParams = listenProgressView.layoutParams
                                layoutParams.width = currentX.toInt() + thumbDot.width / 2
                                listenProgressView.layoutParams = layoutParams
                            }

                        )
                    else
                        AudioPlayerManager.pause()
                }
            }

            itemView.findViewById<TextView>(R.id.record_msg_time).text = item.time

            val seen = itemView.findViewById<TextView>(R.id.record_seen)
            seen.isVisible = getItemViewType(adapterPosition) == RIGHT && item.status == "seen"

        }

        private fun bindDeletedMessage(item: MessageTable) {

            itemView.findViewById<LinearLayout>(R.id.text_msg).isVisible = false
            itemView.findViewById<LinearLayout>(R.id.image_msg).isVisible = false
            itemView.findViewById<LinearLayout>(R.id.deleted_msg).isVisible = true
            itemView.findViewById<LinearLayout>(R.id.record_msg).isVisible = false
            val deletedMessage = itemView.findViewById<TextView>(R.id.deleted_message_body)
            if (item.deleted.userId == FirebaseAuth.getInstance().currentUser!!.uid)
                deletedMessage.text = context.getString(R.string.you_deleted_this_message)
            else
                deletedMessage.text = context.getString(R.string.this_message_is_deleted)
            itemView.findViewById<TextView>(R.id.deleted_message_time).text = item.time
        }
    }


    object AudioPlayerManager {
        private var mediaPlayer: MediaPlayer? = null
        var currentPath: String? = null
        var previusPosition: Int? = null
        private var timer: CountDownTimer? = null
        private var remainingTimeMillis: Long = 0L

        fun play(
            path: String,
            clearPath: () -> Unit,
            onTick: (millisUntilFinished: Long, progress: Float) -> Unit
        ) {
             stop()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
            }
            currentPath = path
            val duration = mediaPlayer!!.duration.toLong()
            timer = object : CountDownTimer(duration, 50) {
                override fun onTick(millisUntilFinished: Long) {
                    val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val progress = current.toFloat() / duration
                    onTick(duration - current, progress)
                }

                override fun onFinish() {
                    stop()
                    clearPath()
                }

            }.start()
        }
        fun resume(
            onTick: (millisUntilFinished: Long, progress: Float) -> Unit,
            clearPath: () -> Unit
        ) {
            mediaPlayer?.start()

            val duration = mediaPlayer?.duration?.toLong() ?: return

            timer = object : CountDownTimer(remainingTimeMillis, 50) {
                override fun onTick(millisUntilFinished: Long) {
                    val currentPos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val progress = currentPos.toFloat() / duration
                    onTick(duration - currentPos, progress)
                }

                override fun onFinish() {
                    stop()
                    clearPath()
                }

            }.start()
        }

        fun stop() {
            mediaPlayer?.release()
            mediaPlayer = null
            timer?.cancel()
            timer = null
            currentPath = null
        }
        fun pause() {
            mediaPlayer?.pause()
            timer?.cancel()
            val duration = mediaPlayer?.duration?.toLong() ?: 0L
            val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
            remainingTimeMillis = duration - current
        }
        fun isPaused(): Boolean {
            return mediaPlayer?.isPlaying == false && currentPath != null
        }
    }

    fun formatDuration(seconds: Long): String {

        val minutes = (seconds % 3600) / 60

        val secs = seconds % 60

        return String.format("%02d:%02d",minutes, secs)
    }

}
