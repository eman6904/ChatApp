package com.example.chatapp.ui.userInterface.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R

class ReactionsAdapter(
    private val reactions: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ReactionsAdapter.ReactionViewHolder>() {

    inner class ReactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reactionText: TextView = itemView.findViewById(R.id.reactionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reaction, parent, false)
        return ReactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReactionViewHolder, position: Int) {
        val reaction = reactions[position]
        holder.reactionText.text = reaction
        holder.reactionText.setOnClickListener {
            onItemClick(reaction)
        }
    }

    override fun getItemCount() = reactions.size
}
