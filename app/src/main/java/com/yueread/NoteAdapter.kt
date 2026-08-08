package com.yueread

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yueread.data.Note

class NoteAdapter(
    private var notes: List<Note>,
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {
    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNotebook: TextView = view.findViewById(R.id.tv_notebook)
        val tvSelectedText: TextView = view.findViewById(R.id.tv_selected_text)
        val tvThought: TextView = view.findViewById(R.id.tv_thought)
        val tvAiResponse: TextView = view.findViewById(R.id.tv_ai_response)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.tvNotebook.text = "笔记本: ${note.notebook}"
        holder.tvSelectedText.text = "原文: ${note.selectedText}"
        holder.tvThought.text = "感想: ${note.thought}"
        holder.tvAiResponse.text = if (note.aiResponse != null) "AI: ${note.aiResponse}" else ""
        holder.tvAiResponse.visibility = if (note.aiResponse != null) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onClick(note) }
        holder.itemView.setOnLongClickListener {
            onLongClick(note)
            true
        }
    }
    override fun getItemCount() = notes.size
    fun updateData(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}
