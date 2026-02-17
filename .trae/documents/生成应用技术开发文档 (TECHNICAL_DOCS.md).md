我已完成对 `quadrant` 项目的全面技术调研，并准备好了技术文档的撰写计划。

### 文档生成计划：TECHNICAL_DOCS.md

我将创建一个名为 `TECHNICAL_DOCS.md` 的详细技术文档，包含以下核心章节：

1.  **项目概览 (Project Overview)**
    *   应用定位：基于四象限法则（Eisenhower Matrix）的时间管理工具。
    *   核心价值：帮助用户区分轻重缓急，聚焦每日最重要任务 (MIT)。

2.  **技术栈 (Tech Stack)**
    *   **语言**: Kotlin (100%)
    *   **UI 框架**: Jetpack Compose (Material3)
    *   **架构模式**: MVVM (Model-View-ViewModel) + Repository Pattern
    *   **数据存储**: Room Database (SQLite)
    *   **异步处理**: Kotlin Coroutines & Flow
    *   **网络通信**: OkHttp (用于 AI 服务)
    *   **构建工具**: Gradle Kotlin DSL

3.  **架构设计 (Architecture Design)**
    *   **数据层 (Data Layer)**: 
        *   `TaskEntity`: 核心数据模型设计（包含象限、状态、录音路径等）。
        *   `TaskRepository`: 业务逻辑中心（MIT 互斥逻辑、过期检测、排序算法）。
    *   **UI 层 (UI Layer)**:
        *   `HomeViewModel`: 状态管理（StateFlow, 组合状态流）。
        *   `TaskEditorSheet`: 复杂的交互逻辑（录音、AI 调用、表单验证）。

4.  **核心业务逻辑梳理 (Core Business Logic)**
    *   **四象限管理**: 任务分类、拖拽排序、状态流转。
    *   **MIT (Most Important Task)**: 每日唯一的“最重要任务”机制与自动建议逻辑。
    *   **智能辅助 (AI Integration)**:
        *   语音转文字 (Whisper-1 via OpenAI API)。
        *   智能标签与象限分析 (DeepSeek Chat API)。
        *   智能填充策略（根据文本长度自动分配标题/描述）。
    *   **录音功能**: 原生 `MediaRecorder` 实现与文件管理策略。
    *   **提醒机制**: 基于 UI 轮询的前台提醒实现（及其局限性说明）。

5.  **数据模型详解 (Data Models)**
    *   数据库表结构 (`tasks` 表) 详细字段说明。
    *   关键枚举：`Quadrant` (象限), `TaskStatus` (状态), `ReminderUnit` (提醒单位)。

6.  **项目结构说明 (Project Structure)**
    *   关键目录 (`data`, `ui`, `util`) 的职责划分。

该文档将以 Markdown 格式呈现，清晰、专业，适合开发人员阅读和维护。
