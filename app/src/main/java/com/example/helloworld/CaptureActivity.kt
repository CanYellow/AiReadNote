package com.example.helloworld

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class CaptureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        val selectedText = extractTextFromIntent(intent) ?: "No text received."

        val textSelected = findViewById<TextView>(R.id.text_selected)
        val editThought = findViewById<EditText>(R.id.edit_thought)
        val spinnerNotebook = findViewById<Spinner>(R.id.spinner_notebook)
        val btnSend = findViewById<Button>(R.id.btn_send)

        textSelected.text = selectedText

        // 模拟笔记本列表数据（后续从数据库获取）
        val notebooks = arrayOf("默认笔记本", "《人类简史》笔记", "英语生词本")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, notebooks)
        spinnerNotebook.adapter = adapter

        btnSend.setOnClickListener {
            val thought = editThought.text.toString()
            val selectedNotebook = spinnerNotebook.selectedItem.toString()
            
            Toast.makeText(this, "已保存至 [$selectedNotebook]: $thought", Toast.LENGTH_SHORT).show()
            finish()
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
