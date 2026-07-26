package com.example.helloworld

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = extractTextFromIntent(intent) ?: "No text received. Hello World!"

        // 用代码直接创建一个文本视图，不使用 xml 布局文件以保持极简
        val textView = TextView(this)
        textView.text = selectedText
        textView.textSize = 20f
        textView.setPadding(50, 50, 50, 50)

        setContentView(textView)
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
