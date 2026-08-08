package com.yueread

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yueread.R
import com.yueread.data.AppDatabase
import com.yueread.data.Notebook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotebookManagerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notebook_manager)
        
        val btnAdd = findViewById<android.widget.Button>(R.id.btn_add_notebook)
        btnAdd.setOnClickListener {
            showAddDialog()
        }

        recyclerView = findViewById(R.id.recyclerViewNotebooks)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        loadNotebooks()
    }

    private fun loadNotebooks() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@NotebookManagerActivity)
            db.notebookDao().getAllNotebooks().collect { notebooks ->
                recyclerView.adapter = NotebookAdapter(notebooks)
            }
        }
    }

    inner class NotebookAdapter(private val notebooks: List<Notebook>) : RecyclerView.Adapter<NotebookAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text1: TextView = view.findViewById(android.R.id.text1)
            val text2: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val notebook = notebooks[position]
            holder.text1.text = notebook.name
            holder.text2.text = "分类: ${notebook.category}"
            
            // 点击编辑笔记本
            holder.itemView.setOnClickListener {
                showEditDialog(notebook)
            }

            // 长按删除笔记本
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(this@NotebookManagerActivity)
                    .setTitle("删除笔记本")
                    .setMessage("确定要删除该笔记本吗？删除后，该笔记本下的所有笔记将永久消失！")
                    .setPositiveButton("确定") { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val db = AppDatabase.getDatabase(this@NotebookManagerActivity)
                            db.noteDao().deleteNotesByNotebook(notebook.name)
                            db.notebookDao().delete(notebook)
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
                true
            }
        }

        override fun getItemCount(): Int = notebooks.size
    }

    private fun showEditDialog(notebook: Notebook) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val nameInput = EditText(this).apply {
            hint = "笔记本名称"
            setText(notebook.name)
        }
        val categoryInput = EditText(this).apply {
            hint = "分类"
            setText(notebook.category)
        }
        val promptInput = EditText(this).apply {
            hint = "系统提示词"
            setText(notebook.systemPrompt)
        }

        layout.addView(nameInput)
        layout.addView(categoryInput)
        layout.addView(promptInput)

        AlertDialog.Builder(this)
            .setTitle("编辑笔记本")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val newName = nameInput.text.toString().trim()
                val newCategory = categoryInput.text.toString().trim()
                val newPrompt = promptInput.text.toString().trim()

                if (newName.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(this@NotebookManagerActivity)
                        // 更新笔记本详情
                        db.notebookDao().updateNotebookDetails(notebook.name, newName, newCategory, newPrompt)
                        // 如果修改了名称，同步更新该笔记本下所有笔记的归属
                        if (newName != notebook.name) {
                            db.noteDao().updateNotebookName(notebook.name, newName)
                        }
                    }
                } else {
                    Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val nameInput = EditText(this).apply { hint = "笔记本名称" }
        val categoryInput = EditText(this).apply { hint = "分类" }
        val promptInput = EditText(this).apply { hint = "系统提示词" }

        layout.addView(nameInput)
        layout.addView(categoryInput)
        layout.addView(promptInput)

        AlertDialog.Builder(this)
            .setTitle("新建笔记本")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val name = nameInput.text.toString().trim()
                val category = categoryInput.text.toString().trim()
                val prompt = promptInput.text.toString().trim()

                if (name.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(this@NotebookManagerActivity)
                        db.notebookDao().insert(
                            Notebook(name = name, category = category, systemPrompt = prompt, lastUsed = System.currentTimeMillis())
                        )
                    }
                } else {
                    Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
