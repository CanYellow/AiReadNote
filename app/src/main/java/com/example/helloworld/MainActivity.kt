package com.example.helloworld

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.example.helloworld.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSettings = findViewById<Button>(R.id.btn_settings)
        btnSettings.setOnClickListener {
            Toast.makeText(this, "即将打开设置页面", Toast.LENGTH_SHORT).show()
        }
        
        // 针对你的第4个需求：处理从通知栏点击进来的跳转
        val noteIdToOpen = intent.getStringExtra("OPEN_NOTE_ID")
        if (noteIdToOpen != null) {
            Toast.makeText(this, "从通知跳转，准备打开笔记详情: $noteIdToOpen", Toast.LENGTH_LONG).show()
            // 后续这里会编写跳转到笔记详情页面的代码
        }
        
        // 监听数据库中的笔记数据
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.noteDao().getAllNotes().collect { notes ->
                // 当数据库数据变化时，这里会收到最新的笔记列表
                Log.d("MainActivity", "当前共有 ${notes.size} 条笔记")
                notes.forEach { note ->
                    Log.d("MainActivity", "笔记: ${note.thought} (来自: ${note.notebook})")
                }
            }
        }
    }
}
