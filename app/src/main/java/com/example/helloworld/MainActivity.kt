package com.example.helloworld

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // 针对你的第4个需求：处理从通知栏点击进来的跳转
        val noteIdToOpen = intent.getStringExtra("OPEN_NOTE_ID")
        if (noteIdToOpen != null) {
            Toast.makeText(this, "从通知跳转，准备打开笔记详情: $noteIdToOpen", Toast.LENGTH_LONG).show()
            // 后续这里会编写跳转到笔记详情页面的代码
        }
        
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = NoteAdapter(emptyList())
        recyclerView.adapter = adapter

        // 监听数据库中的笔记数据
        CoroutineScope(Dispatchers.Main).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.noteDao().getAllNotes().collect { notes ->
                adapter.updateData(notes)
            }
        }
    }
}
