package com.yueread

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yueread.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    private var notesJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnManage = findViewById<Button>(R.id.btn_manage_notebooks)
        btnManage.setOnClickListener {
            startActivity(Intent(this, NotebookManagerActivity::class.java))
        }

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
        
        // 更新了 NoteAdapter 的初始化以匹配新的构造函数
        val adapter = NoteAdapter(
            emptyList(),
            onClick = { note ->
                val intent = Intent(this, NoteDetailActivity::class.java)
                intent.putExtra("NOTE_ID", note.id)
                startActivity(intent)
            },
            onLongClick = { note ->
                AlertDialog.Builder(this)
                    .setTitle("删除笔记")
                    .setMessage("确定要删除这条笔记吗？")
                    .setPositiveButton("删除") { _, _ ->
                        CoroutineScope(Dispatchers.IO).launch {
                            AppDatabase.getDatabase(applicationContext).noteDao().delete(note)
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )
        recyclerView.adapter = adapter

        val spinnerFilter = findViewById<Spinner>(R.id.spinner_filter_notebook)
        val displayList = mutableListOf<String>()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = spinnerAdapter

        val db = AppDatabase.getDatabase(applicationContext)

        CoroutineScope(Dispatchers.Main).launch {
            db.notebookDao().getAllNotebooks().collect { notebooks ->
                displayList.clear()
                displayList.add("全部笔记本")
                displayList.addAll(notebooks.map { it.name })
                spinnerAdapter.notifyDataSetChanged()
            }
        }

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = displayList[position]
                notesJob?.cancel()
                notesJob = CoroutineScope(Dispatchers.Main).launch {
                    if (selected == "全部笔记本") {
                        db.noteDao().getAllNotes().collect { notes ->
                            adapter.updateData(notes)
                        }
                    } else {
                        db.noteDao().getNotesByNotebook(selected).collect { notes ->
                            adapter.updateData(notes)
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
