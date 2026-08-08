新建文件：README.md


# 悦读 (YueRead) - AI 辅助阅读笔记工具

## 📖 项目介绍
“悦读”是一款基于 Android 平台的 AI 辅助阅读与笔记管理工具。它的核心理念是**“无缝捕获，AI 赋能”**。
用户可以在任何阅读软件（如 Moon+ Reader 等）中选中文本，通过系统分享、文本处理菜单或 ColorDict
协议直接将文本发送到“悦读”。应用会根据用户选择的“笔记本”及其预设的“系统提示词”，自动调用 AI（OpenAI /
Gemini）生成感想、翻译或总结，并将其结构化地保存到本地数据库中。

## 🎯 项目目标
- **极简的捕获体验**：不打断用户的阅读心流，一键摘录并触发 AI 思考。
- **高度定制化**：通过“笔记本”分类，为不同类型的阅读材料（如小说、技术文档、外语原著）设定不同的 AI 角色和提示词。
- **数据本地化与安全**：笔记数据完全存储在本地 SQLite 数据库中，支持导出备份。

## 🛠 技术栈                                                                                                             
- **语言**: Kotlin
- **高度定制化**：通过“笔记本”分类，为不同类型的阅读材料（如小说、技术文档、外语原著）设定不同的 AI 角色和提示词。
- **UI 体系**: Android 原生 View (XML 布局)
- **异步处理**: Kotlin Coroutines (协程) & Flow (响应式数据流)
- **本地存储**: Room Database (SQLite 抽象层)
- **网络请求**: OkHttp3 (用于与大模型 API 通信)
- **JSON 解析**: org.json (Android 原生)

---

## 🚀 新手入门 (Getting Started)

### 1. 环境准备
- 安装最新版 **Android Studio**。
- 确保已安装 **JDK 17** 或以上版本。
- Android SDK 编译版本 (compileSdk) 设为 34。

### 2. 导入项目
1. 克隆或下载本项目到本地。
2. 打开 Android Studio，选择 `File -> Open`，选中项目根目录。
3. 等待 Gradle 同步完成（首次同步可能需要下载依赖，请保持网络畅通）。

---

## 💻 构建、调试与部署命令 (Cheat Sheet)

在项目根目录的终端（Terminal）中执行以下命令：

### 编译与构建
- **清理项目**:
  ```bash
  ./gradlew clean


 • 编译 Debug 版本 APK:

   ./gradlew assembleDebug

   (输出路径: app/build/outputs/apk/debug/app-debug.apk)
 • 编译 Release 版本 APK:

   ./gradlew assembleRelease

   (输出路径: app/build/outputs/apk/release/app-release.apk)

安装与调试

 • 安装 Debug 包到连接的设备/模拟器:

   adb install -r app/build/outputs/apk/debug/app-debug.apk

 • 查看应用崩溃日志 (极速定位闪退):

   adb logcat -d -s AndroidRuntime

 • 实时查看应用的所有日志:

   adb logcat | grep "com.yueread"


------------------------------------------------------------------------------------------------------------------------


📂 项目核心结构指南 (老手速查)

当你时隔多月重新打开项目时，请看这里快速找回记忆：

 • com.yueread.MainActivity: 主界面。负责展示笔记列表、按笔记本过滤笔记，以及跳转到其他管理页面。
 • com.yueread.CaptureActivity: 核心业务入口。处理来自其他 App 的文本分享 Intent，展示 AI 交互界面，保存笔记。
 • com.yueread.NotebookManagerActivity: 笔记本管理页面。负责笔记本的增删改查（CRUD）。
 • com.yueread.SettingsActivity: 设置页面。负责 AI 配置（API Key、Base URL、模型名称）的管理，以及数据库的导入导出。
 • com.yueread.AiNetworkManager: AI 网络请求核心类。如果你要新增 AI 协议（比如 Claude、文心一言），就在这里加代码。
 • com.yueread.data.*: Room 数据库层。包含 Note、Notebook、AiConfig 的实体类(Entity)和数据访问对象(DAO)。

------------------------------------------------------------------------------------------------------------------------


🔧 后期维护与升级指南

1. 如何修改或新增数据库字段？(Room 数据库升级)

如果你需要在 Note 或 Notebook 表中增加新字段，千万不要直接改完就运行，会闪退！ 请严格按照以下步骤：

 1 打开 app/src/main/java/com/yueread/data/Note.kt (或对应实体类)，新增字段。
 2 打开 app/src/main/java/com/yueread/data/AppDatabase.kt。
 3 将 @Database(version = X) 中的版本号 +1。
 4 在 AppDatabase 的 companion object 中编写 Migration 逻辑。例如从版本 6 升到 7：

   val MIGRATION_6_7 = object : Migration(6, 7) {
       override fun migrate(database: SupportSQLiteDatabase) {
           database.execSQL("ALTER TABLE `notes` ADD COLUMN `new_field` TEXT NOT NULL DEFAULT ''")
       }
   }

 5 在 Room.databaseBuilder 中添加 .addMigrations(MIGRATION_6_7)。

2. 如何接入新的 AI 大模型？

 1 打开 AiNetworkManager.kt。
 2 在 sendRequest 方法的 when 分支中新增协议类型（如 "CLAUDE"）。
 3 仿照 requestOpenAI 方法，新建一个 requestClaude 方法，根据该大模型的官方 API 文档拼接 JSON Body 和 URL。
 4 打开 SettingsActivity.kt，在 showAddConfigDialog 和 showEditConfigDialog 的
   Spinner（下拉菜单）数据源中，加上新协议的名称。

3. 外部文本捕获失效怎么办？

如果发现其他阅读软件无法将文本分享到“悦读”，请检查 AndroidManifest.xml 中的 CaptureActivity 配置。
目前支持三种捕获方式：

 • colordict.intent.action.SEARCH: 适配 Moon+ Reader 等支持 ColorDict 词典协议的阅读器。
 • android.intent.action.PROCESS_TEXT: 适配 Android 原生长按文本弹出的“自定义操作”菜单。
 • android.intent.action.SEND: 适配标准的系统“分享”菜单。

4. 数据库导出与导入逻辑

 • 数据库文件默认位于 /data/data/com.yueread/databases/notes_database。
 • 导出时，代码会执行 PRAGMA wal_checkpoint(TRUNCATE) 强制将 WAL（预写日志）合并到主库中，确保导出的单文件是完整的。
 • 导入时，必须先 db.close() 关闭当前数据库连接，再覆盖文件，否则会导致数据库损坏或数据不更新。





