package com.example.helloworld

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helloworld.data.AiConfig
import com.example.helloworld.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.system.exitProcess

class SettingsActivity : ComponentActivity() {

    // 新增：导出文件的启动器
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { exportDatabase(it) }
    }

    // 新增：导入文件的启动器
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importDatabase(it) }
    }

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

                        // 新增：点击整个条目弹出编辑框
                        holder.itemView.setOnClickListener {
                            showEditConfigDialog(config, db)
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_add_config).setOnClickListener {
            showAddConfigDialog(db)
        }

        // 新增：绑定导入导出按钮事件
        findViewById<Button>(R.id.btn_export_data).setOnClickListener {
            exportLauncher.launch("notes_backup.db")
        }

        findViewById<Button>(R.id.btn_import_data).setOnClickListener {
            importLauncher.launch(arrayOf("*/*"))
        }
    }

    // 新增：导出数据库逻辑
    private fun exportDatabase(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@SettingsActivity)
                // 强制将 WAL 日志写入主数据库文件，确保数据完整
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
                
                val dbFile = getDatabasePath("notes_database")
                contentResolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(dbFile).use { input ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "导出成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 新增：导入数据库逻辑
    private fun importDatabase(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dbFile = getDatabasePath("notes_database")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "导入成功，应用即将退出以应用更改", Toast.LENGTH_LONG).show()
                    // 延迟一下让用户看到提示，然后杀掉进程重启以重新加载数据库
                    Thread.sleep(1500)
                    exitProcess(0)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 新增：编辑已有 AI 配置的弹窗方法
    private fun showEditConfigDialog(config: AiConfig, db: AppDatabase) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val editName = EditText(this).apply { hint = "配置名称"; setText(config.name) }
        val spinnerProtocol = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, listOf("GEMINI", "OPENAI"))
            setSelection(if (config.protocol == "OPENAI") 1 else 0)
        }
        val editBaseUrl = EditText(this).apply { hint = "Base URL"; setText(config.baseUrl) }
        val editApiKey = EditText(this).apply { hint = "API Key"; setText(config.apiKey) }
        val editModel = EditText(this).apply { hint = "模型名称"; setText(config.modelName) }
        val editSystemPrompt = EditText(this).apply { hint = "系统提示词 (可选)"; setText(config.systemPrompt) }

        layout.addView(editName)
        layout.addView(spinnerProtocol)
        layout.addView(editBaseUrl)
        layout.addView(editApiKey)
        layout.addView(editModel)
        layout.addView(editSystemPrompt)

        AlertDialog.Builder(this)
            .setTitle("编辑 AI 配置")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val updatedConfig = config.copy(
                    name = editName.text.toString(),
                    protocol = spinnerProtocol.selectedItem.toString(),
                    baseUrl = editBaseUrl.text.toString(),
                    apiKey = editApiKey.text.toString(),
                    modelName = editModel.text.toString(),
                    systemPrompt = editSystemPrompt.text.toString()
                )
                lifecycleScope.launch(Dispatchers.IO) {
                    db.aiConfigDao().update(updatedConfig)
                }
            }
            .setNegativeButton("取消", null)
            .show()
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
