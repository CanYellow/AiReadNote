package com.example.helloworld

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helloworld.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotebookManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recyclerView = RecyclerView(this)
        setContentView(recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val db = AppDatabase.getDatabase(this)
        
        lifecycleScope.launch {
            db.notebookDao().getAllNotebooks().collect { notebooks ->
                recyclerView.adapter = object : RecyclerView.Adapter<NbViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = 
                        NbViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_notebook, parent, false))
                    override fun getItemCount() = notebooks.size
                    override fun onBindViewHolder(holder: NbViewHolder, position: Int) {
                        val nb = notebooks[position]
                        holder.name.text = nb.name
                        holder.category.text = "分类: ${nb.category}"
                        holder.activeSwitch.setOnCheckedChangeListener(null)
                        holder.activeSwitch.isChecked = nb.isActive
                        holder.activeSwitch.setOnCheckedChangeListener { _, isChecked ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                db.notebookDao().update(nb.copy(isActive = isChecked))
                            }
                        }
                        
                        // 新增：点击整个条目弹出编辑框
                        holder.itemView.setOnClickListener {
                            showEditDialog(nb, db)
                        }
                    }
                }
            }
        }
    }

    // 新增：编辑笔记本的弹窗方法
    private fun showEditDialog(notebook: com.example.helloworld.data.Notebook, db: AppDatabase) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val editName = EditText(this).apply {
            hint = "笔记本名称"
            setText(notebook.name)
        }
        val editCategory = EditText(this).apply { 
            hint = "分类 (如: 默认分类)"
            setText(notebook.category)
        }
        val editPrompt = EditText(this).apply { 
            hint = "笔记本专属 AI 提示词 (可选)"
            setText(notebook.systemPrompt)
        }

        layout.addView(editName)
        layout.addView(editCategory)
        layout.addView(editPrompt)

        AlertDialog.Builder(this)
            .setTitle("编辑笔记本: ${notebook.name}")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val newName = editName.text.toString()
                val newCategory = editCategory.text.toString()
                val newPrompt = editPrompt.text.toString()
                
                lifecycleScope.launch(Dispatchers.IO) {
                    if (newName != notebook.name) {
                        db.notebookDao().updateNotebookDetails(notebook.name, newName, newCategory, newPrompt)
                        db.noteDao().updateNotebookName(notebook.name, newName)
                    } else {
                        val updatedNb = notebook.copy(
                            category = newCategory,
                            systemPrompt = newPrompt
                        )
                        db.notebookDao().update(updatedNb)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    class NbViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_nb_name)
        val category: TextView = view.findViewById(R.id.tv_nb_category)
        val activeSwitch: Switch = view.findViewById(R.id.switch_active)
    }
}
