package com.example.helloworld

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.helloworld.data.AppDatabase
import com.example.helloworld.data.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CaptureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        val selectedText = extractTextFromIntent(intent) ?: "No text received."

        val textSelected = findViewById<TextView>(R.id.text_selected)
        val editThought = findViewById<EditText>(R.id.edit_thought)
        val spinnerNotebook = findViewById<Spinner>(R.id.spinner_notebook)

        textSelected.text = selectedText

        val notebooks = mutableListOf("默认笔记本", "《人类简史》笔记", "英语生词本", "+ 新增笔记本")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, notebooks)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotebook.adapter = adapter

        spinnerNotebook.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (notebooks[position] == "+ 新增笔记本") {
                    val input = EditText(this@CaptureActivity)
                    AlertDialog.Builder(this@CaptureActivity)
                        .setTitle("新增笔记本")
                        .setView(input)
                        .setPositiveButton("确定") { _, _ ->
                            val newName = input.text.toString()
                            if (newName.isNotBlank()) {
                                notebooks.add(notebooks.size - 1, newName)
                                adapter.notifyDataSetChanged()
                                spinnerNotebook.setSelection(notebooks.size - 2)
                            }
                        }
                        .setNegativeButton("取消") { _, _ -> spinnerNotebook.setSelection(0) }
                        .show()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnSendAi = findViewById<Button>(R.id.btn_send_ai)

        btnSave.setOnClickListener {
            val thought = editThought.text.toString()
            val selectedNotebook = spinnerNotebook.selectedItem.toString()
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(applicationContext)
                db.noteDao().insert(Note(selectedText = selectedText, thought = thought, notebook = selectedNotebook))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CaptureActivity, "已保存", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        btnSendAi.setOnClickListener {
            val thought = editThought.text.toString()
            val notebook = spinnerNotebook.selectedItem.toString()
            Toast.makeText(this, "正在发送给 AI...", Toast.LENGTH_SHORT).show()
            
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val note = Note(selectedText = selectedText, thought = thought, notebook = notebook)
                db.noteDao().insert(note)
                
                delay(2000) // 模拟 AI 网络请求延迟
                val mockAiResponse = "AI回复：关于“${thought}”的见解很深刻！"
                
                val latestNote = db.noteDao().getLatestNote()
                if (latestNote != null) {
                    db.noteDao().update(latestNote.copy(aiResponse = mockAiResponse))
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CaptureActivity, "AI 回复已更新", Toast.LENGTH_SHORT).show()
                }
            }
            finish() // 发送后直接关闭弹窗
        }
    }

    private fun extractTextFromIntent(intent: Intent): String? {
        // 保持原有的 extractTextFromIntent 逻辑不变
        return when (intent.action) {
            "colordict.intent.action.SEARCH" -> intent.getStringExtra("EXTRA_QUERY")
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
    }
}
