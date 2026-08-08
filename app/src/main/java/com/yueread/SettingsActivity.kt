package com.yueread

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
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
import com.yueread.data.AiConfig
import com.yueread.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : ComponentActivity() {

    // 新增：记录当前是否为增量导入
    private var isIncrementalImport = false

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

                        // 新增：长按删除配置
                        holder.itemView.setOnLongClickListener {
                            AlertDialog.Builder(this@SettingsActivity)
                                .setTitle("删除配置")
                                .setMessage("确定要删除配置 ${config.name} 吗？")
                                .setPositiveButton("删除") { _, _ ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        db.aiConfigDao().delete(config)
                                    }
                                }
                                .setNegativeButton("取消", null)
                                .show()
                            true
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_add_config).setOnClickListener {
            showAddConfigDialog(db)
        }

        // 新增：调试信息按钮逻辑
        findViewById<Button>(R.id.btn_debug_info).setOnClickListener {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val debugInfo = """
                App Version: ${packageInfo.versionName} (${packageInfo.versionCode})
                OS Version: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                Brand: ${Build.BRAND}
                Board: ${Build.BOARD}
                Hardware: ${Build.HARDWARE}
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("设备调试信息")
                .setMessage(debugInfo)
                .setPositiveButton("复制到剪贴板") { _, _ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Debug Info", debugInfo)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("关闭", null)
                .show()
        }

        // 修改：绑定导出按钮事件，增加时间戳
        findViewById<Button>(R.id.btn_export_data).setOnClickListener {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "notes_backup_$timestamp.db"
            exportLauncher.launch(filename)
        }

        // 修改：绑定导入按钮事件，增加风险提示弹窗
        findViewById<Button>(R.id.btn_import_data).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("风险提示")
                .setMessage("导入数据可能会覆盖现有数据或产生冲突。强烈建议您在导入前先【导出备份】当前数据。\n\n您确定要继续导入吗？")
                .setPositiveButton("继续导入") { _, _ ->
                    // 用户确认后，再弹出导入方式选择框
                    AlertDialog.Builder(this)
                        .setTitle("选择导入方式")
                        .setItems(arrayOf("覆盖导入 (清空现有数据)", "增量导入 (合并到现有数据)")) { _, which ->
                            isIncrementalImport = (which == 1)
                            importLauncher.launch(arrayOf("*/*"))
                        }
                        .show()
                }
                .setNegativeButton("取消并去备份", null)
                .show()
        }
    }

    // 修改：导出数据库逻辑
    private fun exportDatabase(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@SettingsActivity)
                // 强制将 WAL 日志完全合并到主数据库文件中
                db.query("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                    cursor.moveToFirst()
                }
                
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

    // 修改：支持覆盖导入和增量导入
    private fun importDatabase(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!isIncrementalImport) {
                    // 覆盖导入逻辑
                    val db = AppDatabase.getDatabase(this@SettingsActivity)
                    db.close() // 必须先关闭当前数据库连接
                    
                    val dbFile = getDatabasePath("notes_database")
                    val walFile = getDatabasePath("notes_database-wal")
                    val shmFile = getDatabasePath("notes_database-shm")
                    
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(dbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // 必须删除缓存文件，否则 SQLite 会用旧缓存覆盖刚导入的数据
                    if (walFile.exists()) walFile.delete()
                    if (shmFile.exists()) shmFile.delete()
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity, "覆盖导入成功，即将重启应用", Toast.LENGTH_LONG).show()
                        Thread.sleep(1500)
                        exitProcess(0)
                    }
                } else {
                    // 增量导入逻辑
                    val tempDbFile = getDatabasePath("notes_database_temp")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempDbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // 打开临时数据库，必须加上迁移脚本，防止旧版本备份被清空
                    val tempDb = androidx.room.Room.databaseBuilder(
                        this@SettingsActivity,
                        AppDatabase::class.java,
                        "notes_database_temp"
                    )
                    .addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()

                    val mainDb = AppDatabase.getDatabase(this@SettingsActivity)

                    // 增量导入去重逻辑
                    val existingNotebooks = mainDb.notebookDao().getAllNotebooks().first().map { it.name }.toSet()
                    val notebooks = tempDb.notebookDao().getAllNotebooks().first()
                    notebooks.forEach { 
                        if (it.name !in existingNotebooks) {
                            mainDb.notebookDao().insert(it) 
                        }
                    }

                    val existingNotes = mainDb.noteDao().getAllNotes().first().map { "${it.timestamp}_${it.selectedText}" }.toSet()
                    val notes = tempDb.noteDao().getAllNotes().first()
                    notes.forEach { 
                        if ("${it.timestamp}_${it.selectedText}" !in existingNotes) {
                            mainDb.noteDao().insert(it.copy(id = 0)) 
                        }
                    }

                    val existingConfigs = mainDb.aiConfigDao().getAllConfigs().first().map { it.name }.toSet()
                    val configs = tempDb.aiConfigDao().getAllConfigs().first()
                    configs.forEach { 
                        if (it.name !in existingConfigs) {
                            mainDb.aiConfigDao().insert(it.copy(id = 0, isActive = false)) 
                        }
                    }

                    tempDb.close()
                    tempDbFile.delete()
                    getDatabasePath("notes_database_temp-wal").delete()
                    getDatabasePath("notes_database_temp-shm").delete()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity, "增量导入成功！", Toast.LENGTH_SHORT).show()
                    }
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
