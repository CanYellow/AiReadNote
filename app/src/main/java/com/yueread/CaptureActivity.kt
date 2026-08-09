package com.yueread

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
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.yueread.data.AppDatabase
import com.yueread.data.Note
import com.yueread.data.Notebook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        val selectedText = extractTextFromIntent(intent) ?: "No text received."

        val textSelected = findViewById<TextView>(R.id.text_selected)
        val editThought = findViewById<EditText>(R.id.edit_thought)
        val spinnerNotebook = findViewById<Spinner>(R.id.spinner_notebook)

        textSelected.text = selectedText

        val displayList = mutableListOf<String>()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotebook.adapter = adapter

        val db = AppDatabase.getDatabase(applicationContext)

        // 动态监听激活的笔记本
        lifecycleScope.launch {
            db.notebookDao().getActiveNotebooks().collect { notebooks ->
                displayList.clear()
                displayList.addAll(notebooks.map { it.name })
                if (displayList.isEmpty()) {
                    // 如果为空，插入一个默认的
                    db.notebookDao().insert(Notebook("默认笔记本"))
                }
                displayList.add("+ 新增笔记本")
                adapter.notifyDataSetChanged()
                // 默认选中第一个（即最近使用的）
                spinnerNotebook.setSelection(0)
            }
        }

        spinnerNotebook.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (displayList[position] == "+ 新增笔记本") {
                    val input = EditText(this@CaptureActivity)
                    AlertDialog.Builder(this@CaptureActivity)
                        .setTitle("新增笔记本")
                        .setView(input)
                        .setPositiveButton("确定") { _, _ ->
                            val newName = input.text.toString()
                            if (newName.isNotBlank()) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    db.notebookDao().insert(Notebook(name = newName))
                                }
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
                db.notebookDao().update(Notebook(name = selectedNotebook, isActive = true, lastUsed = System.currentTimeMillis()))
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
            
            // 禁用按钮防止重复点击导致多次请求
            btnSendAi.isEnabled = false
            
            CoroutineScope(Dispatchers.IO).launch {
                db.notebookDao().update(Notebook(name = notebook, isActive = true, lastUsed = System.currentTimeMillis()))
                val note = Note(selectedText = selectedText, thought = thought, notebook = notebook)
                db.noteDao().insert(note)
                
                val activeConfig = db.aiConfigDao().getActiveConfig()
                val notebookObj = db.notebookDao().getNotebookByName(notebook)
                
                val aiResponseText = if (activeConfig == null) {
                    "错误：未配置或未激活任何 AI 模型，请前往设置页面配置。"
                } else {
                    try {
                        // 修复：采用与 NoteDetailActivity 完全一致的提示词拼接逻辑
                        val aiPrompt = activeConfig.systemPrompt.takeIf { it.isNotBlank() }?.let { "$it\n" } ?: ""
                        val nbPrompt = notebookObj?.systemPrompt?.takeIf { it.isNotBlank() }?.let { "$it\n" } ?: ""
                        
                        val prompt = "${aiPrompt}${nbPrompt}原文：$selectedText\n我的感想：$thought\n请根据以上内容给出你的见解或补充。"
                        AiNetworkManager.sendRequest(activeConfig, prompt)
                    } catch (e: Exception) {
                        "网络请求异常: ${e.message}"
                    }
                }
                
                val latestNote = db.noteDao().getLatestNote()
                if (latestNote != null) {
                    db.noteDao().update(latestNote.copy(aiResponse = aiResponseText))
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CaptureActivity, "AI 回复已更新", Toast.LENGTH_SHORT).show()
                    // 修复：将 finish() 移到协程内部，确保网络请求和数据库操作完成后再关闭页面，防止后台被杀或 Context 失效崩溃
                    finish()
                }
            }
        }
    }

    private fun extractTextFromIntent(intent: Intent): String? {
        return when (intent.action) {
            "colordict.intent.action.SEARCH" -> intent.getStringExtra("EXTRA_QUERY")
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
    }
}
