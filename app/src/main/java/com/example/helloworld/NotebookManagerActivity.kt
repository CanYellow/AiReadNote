package com.example.helloworld

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
                    }
                }
            }
        }
    }
    class NbViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_nb_name)
        val category: TextView = view.findViewById(R.id.tv_nb_category)
        val activeSwitch: Switch = view.findViewById(R.id.switch_active)
    }
}
