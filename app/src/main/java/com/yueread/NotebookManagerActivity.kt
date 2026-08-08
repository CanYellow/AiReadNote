package com.yueread

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.yueread.data.AppDatabase
import com.yueread.data.Notebook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotebookManagerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_notebook_manager)
        
        loadNotebooks()
    }

    private fun loadNotebooks() {
        // 重新加载数据的逻辑
    }

    inner class NotebookAdapter(private val notebooks: List<Notebook>) : RecyclerView.Adapter<NotebookAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // 返回实际的 ViewHolder
            return ViewHolder(View(parent.context))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val notebook = notebooks[position]
            
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(this@NotebookManagerActivity)
                    .setTitle("删除笔记本")
                    .setMessage("确定要删除该笔记本吗？删除后，该笔记本下的所有笔记将永久消失！")
                    .setPositiveButton("确定") { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val db = AppDatabase.getDatabase(this@NotebookManagerActivity)
                            // 1. 删除该笔记本下的所有笔记
                            db.noteDao().deleteNotesByNotebookId(notebook.id)
                            // 2. 删除笔记本本身
                            db.notebookDao().delete(notebook)
                            
                            // 3. 刷新列表
                            withContext(Dispatchers.Main) {
                                loadNotebooks()
                            }
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
                true
            }
        }

        override fun getItemCount(): Int = notebooks.size
    }
}
