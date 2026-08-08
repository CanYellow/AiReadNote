package com.example.helloworld

import android.app.AlertDialog
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

                // 新增：AI 回答按钮逻辑
                val btnAiReply = findViewById<Button>(R.id.btn_ai_reply)
                btnAiReply.setOnClickListener {
                    val currentAiText = editAi.text.toString().trim()
                    
                    val options = if (currentAiText.isNotEmpty()) {
                        arrayOf("追加回答 (保留现有AI内容)", "重新回答 (覆盖现有AI内容)")
                    } else {
                        arrayOf("直接回答")
                    }

                    AlertDialog.Builder(this@NoteDetailActivity)
                        .setTitle("选择 AI 回答模式")
                        .setItems(options) { _, which ->
                            val isAppend = currentAiText.isNotEmpty() && which == 0
                            
                            Toast.makeText(this@NoteDetailActivity, "正在保存并请求 AI...", Toast.LENGTH_SHORT).show()
                            
                            lifecycleScope.launch(Dispatchers.IO) {
                                // 1. 动作前自动保存当前笔记内容
                                val updatedNote = note.copy(
                                    selectedText = editSelected.text.toString(),
                                    thought = editThought.text.toString(),
                                    aiResponse = editAi.text.toString()
                                )
                                db.noteDao().update(updatedNote)

                                // 2. 获取 AI 配置和笔记本提示词
                                val activeConfig = db.aiConfigDao().getActiveConfig()
                                val notebookObj = db.notebookDao().getNotebookByName(updatedNote.notebook)
                                
                                if (activeConfig == null) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@NoteDetailActivity, "未配置或未激活 AI 模型", Toast.LENGTH_LONG).show()
                                    }
                                    return@launch
                                }

                                // 3. 构造 Prompt
                                val aiPrompt = activeConfig.systemPrompt.takeIf { it.isNotBlank() }?.let { "$it\n" } ?: ""
                                val nbPrompt = notebookObj?.systemPrompt?.takeIf { it.isNotBlank() }?.let { "$it\n" } ?: ""
                                
                                var prompt = "${aiPrompt}${nbPrompt}原文：${updatedNote.selectedText}\n我的感想：${updatedNote.thought}\n"
                                if (isAppend) {
                                    prompt += "之前的AI回复：${currentAiText}\n请在以上基础上继续补充或回答。"
                                } else {
                                    prompt += "请根据以上内容给出你的见解或补充。"
                                }

                                // 4. 请求网络
                                try {
                                    val response = AiNetworkManager.sendRequest(activeConfig, prompt)
                                    val finalAiText = if (isAppend) "$currentAiText\n\n[追加回复]:\n$response" else response
                                    
                                    // 5. 得到结果后再次自动保存
                                    db.noteDao().update(updatedNote.copy(aiResponse = finalAiText))
                                    
                                    withContext(Dispatchers.Main) {
                                        editAi.setText(finalAiText)
                                        Toast.makeText(this@NoteDetailActivity, "AI 回复已更新并保存", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@NoteDetailActivity, "AI 请求失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                        .show()
                }
            }
        }
    }
}
