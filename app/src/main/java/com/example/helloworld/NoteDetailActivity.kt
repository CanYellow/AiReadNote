package com.example.helloworld

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        val noteId = intent.getIntExtra("NOTE_ID", -1)
        if (noteId == -1) { finish(); return }

        val editSelected = findViewById<EditText>(R.id.edit_selected_text)
        val editThought = findViewById<EditText>(R.id.edit_thought)
        val editAi = findViewById<EditText>(R.id.edit_ai_response)
        val btnSave = findViewById<Button>(R.id.btn_save_changes)

        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            val note = db.noteDao().getNoteById(noteId)
            if (note != null) {
                editSelected.setText(note.selectedText)
                editThought.setText(note.thought)
                editAi.setText(note.aiResponse)

                btnSave.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.noteDao().update(note.copy(
                            selectedText = editSelected.text.toString(),
                            thought = editThought.text.toString(),
                            aiResponse = editAi.text.toString()
                        ))
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@NoteDetailActivity, "已保存", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
        }
    }
}
