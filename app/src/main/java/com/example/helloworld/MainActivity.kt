package com.example.helloworld

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 使用 XML 布局文件
        setContentView(R.layout.activity_main)

        val selectedText = extractTextFromIntent(intent) ?: "No text received. Hello World!"

        // 获取布局中的视图组件
        val textSelected = findViewById<TextView>(R.id.text_selected)
        val editThought = findViewById<EditText>(R.id.edit_thought)
        val btnSend = findViewById<Button>(R.id.btn_send)

        // 设置选中的文本
        textSelected.text = selectedText

        // 设置发送按钮的点击事件
        btnSend.setOnClickListener {
            val thought = editThought.text.toString()
            
            // 阶段 2：目前只做简单的 Toast 提示，并关闭悬浮窗
            // 后续阶段这里将替换为保存数据库和发起网络请求的代码
            Toast.makeText(this, "笔记已记录: $thought", Toast.LENGTH_SHORT).show()
            
            // 关闭当前 Activity，返回阅读器
            finish()
        }
    }

    private fun extractTextFromIntent(intent: Intent): String? {
        return when (intent.action) {
            "colordict.intent.action.SEARCH" -> {
                intent.getStringExtra("EXTRA_QUERY")
            }
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            else -> null
        }
    }
}
