package com.example.helloworld

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helloworld.data.AiConfig
import com.example.helloworld.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_ai_configs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            db.aiConfigDao().getAllConfigs().collect { configs ->
                recyclerView.adapter = object : RecyclerView.Adapter<ConfigViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                        ConfigViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_ai_config, parent, false))
                    override fun getItemCount() = configs.size
                    override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
                        val config = configs[position]
                        holder.name.text = config.name
                        holder.details.text = "${config.protocol} | ${config.modelName}"
                        
                        holder.activeSwitch.setOnCheckedChangeListener(null)
                        holder.activeSwitch.isChecked = config.isActive
                        holder.activeSwitch.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    db.aiConfigDao().deactivateAll()
                                    db.aiConfigDao().update(config.copy(isActive = true))
                                }
                            }
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_add_config).setOnClickListener {
            showAddConfigDialog(db)
        }
    }

    private fun showAddConfigDialog(db: AppDatabase) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val editName = EditText(this).apply { hint = "配置名称 (如: 我的Gemini)" }
        val spinnerProtocol = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, listOf("GEMINI", "OPENAI"))
        }
        val editBaseUrl = EditText(this).apply { hint = "Base URL" }
        val editApiKey = EditText(this).apply { hint = "API Key" }
        val editModel = EditText(this).apply { hint = "模型名称 (如: gemini-1.5-pro)" }
        val editSystemPrompt = EditText(this).apply { hint = "系统提示词 (可选)" }

        layout.addView(editName)
        layout.addView(spinnerProtocol)
        layout.addView(editBaseUrl)
        layout.addView(editApiKey)
        layout.addView(editModel)
        layout.addView(editSystemPrompt)

        AlertDialog.Builder(this)
            .setTitle("新增 AI 配置")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val newConfig = AiConfig(
                    name = editName.text.toString(),
                    protocol = spinnerProtocol.selectedItem.toString(),
                    baseUrl = editBaseUrl.text.toString(),
                    apiKey = editApiKey.text.toString(),
                    modelName = editModel.text.toString(),
                    systemPrompt = editSystemPrompt.text.toString()
                )
                lifecycleScope.launch(Dispatchers.IO) {
                    db.aiConfigDao().insert(newConfig)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    class ConfigViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_config_name)
        val details: TextView = view.findViewById(R.id.tv_config_details)
        val activeSwitch: Switch = view.findViewById(R.id.switch_config_active)
    }
}
