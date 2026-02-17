# XLTool Quadrant - 技术开发文档

本文档详细梳理了 XLTool Quadrant 应用的业务逻辑、架构设计及核心技术实现。

## 1. 项目概览 (Project Overview)

**XLTool Quadrant** 是一款基于**四象限法则（Eisenhower Matrix）**的高效时间管理工具。它旨在帮助用户将任务按“重要”和“紧急”程度分类，从而聚焦于真正重要的事情。

### 核心价值
*   **可视化分类**：通过四个象限直观展示任务优先级。
*   **MIT 聚焦**：每日锁定唯一的“最重要任务”（Most Important Task），防止精力分散。
*   **AI 赋能**：集成语音转文字、智能标签生成和象限自动分析功能，降低录入成本。
*   **隐私优先**：数据完全本地化存储（Room Database），录音文件本地保存。

---

## 2. 技术栈 (Tech Stack)

| 类别 | 技术/库 | 说明 |
| :--- | :--- | :--- |
| **编程语言** | Kotlin | 100% 纯 Kotlin 开发 |
| **UI 框架** | Jetpack Compose | Material3 设计风格，声明式 UI |
| **架构模式** | MVVM | Model-View-ViewModel + Repository Pattern |
| **数据存储** | Room Database | Android 官方 SQLite 封装库 |
| **异步处理** | Coroutines & Flow | 协程处理异步任务，StateFlow 管理状态 |
| **依赖注入** | Manual DI | 目前采用手动依赖注入（在 App 类中初始化） |
| **网络通信** | OkHttp | 用于与 DeepSeek/OpenAI API 通信 |
| **构建工具** | Gradle (Kotlin DSL) | 现代化构建配置 |

---

## 3. 架构设计 (Architecture Design)

本项目采用经典的 **MVVM (Model-View-ViewModel)** 架构，遵循关注点分离原则。

### 3.1 数据层 (Data Layer)
负责数据的持久化、业务逻辑处理及远程数据获取。

*   **`TaskEntity`**: 数据库实体类，定义了任务的数据结构。
*   **`TaskDao`**: 数据访问对象，提供对 SQLite 数据库的 CRUD 操作。
*   **`TaskRepository`**: **核心业务逻辑中心**。
    *   它是 UI 层与数据库之间的唯一桥梁。
    *   负责处理复杂的业务规则（如 MIT 的互斥性、任务状态流转、过期检测）。
*   **`DeepSeekService`**: AI 服务封装，负责处理录音转写和智能分析请求。

### 3.2 UI 层 (UI Layer)
负责界面展示和用户交互。

*   **`HomeViewModel`**: 
    *   持有 UI 状态 (`StateFlow`)。
    *   将 Repository 的数据流转换为 UI 可用的状态（如 `todayDecisionState`）。
    *   处理 UI 事件（如点击、拖拽）并调用 Repository。
*   **UI Components**:
    *   `HomeScreen`: 主界面容器。
    *   `QuadrantPanel`: 四象限面板，负责渲染四个列表。
    *   `TaskCard`: 单个任务卡片。
    *   `TaskEditorSheet`: 任务创建/编辑页，包含复杂的交互逻辑（录音、AI）。

---

## 4. 核心业务逻辑 (Core Business Logic)

### 4.1 四象限管理 (Quadrant Management)
系统将任务分为四个象限，由 `Quadrant` 枚举定义：
1.  **Q1 (重要且紧急)**: 立即去做（Do First）。
2.  **Q2 (重要不紧急)**: 制定计划（Schedule）。**这是高效能人士关注的重点**。
3.  **Q3 (紧急不重要)**: 授权他人/快速处理（Delegate）。
4.  **Q4 (不重要不紧急)**: 尽量不做（Don't Do）。

**实现逻辑**:
*   `TaskRepository` 提供了 `getTasksByQuadrant(quadrant)` 方法，实时筛选各象限任务。
*   任务支持在象限间拖拽（修改 `quadrant` 字段）和象限内排序（修改 `sortOrder` 字段）。

### 4.2 MIT (Most Important Task) 机制
*   **定义**: 每天只能有一个“最重要任务”。
*   **互斥逻辑**: 当用户将某个任务设为 MIT 时，`TaskRepository.setMIT()` 会自动取消其他所有任务的 MIT 标记（事务操作）。
*   **建议**: 系统提供 `getSuggestedMIT()`，优先推荐 Q1 或 Q2 中未完成且截止时间最近的任务。

### 4.3 智能录入与 AI 分析 (Smart Entry & AI)
位于 `TaskEditorSheet` 中的核心功能。

1.  **录音 (Audio Recording)**:
    *   使用 Android 原生 `MediaRecorder`。
    *   录音文件保存为 `.m4a` (AAC编码)，存储在应用私有目录 `filesDir/audio_notes/`。
    *   数据库 `audioPath` 字段仅存储文件名。

2.  **语音转文字 (Transcription)**:
    *   录音结束后，自动调用 `DeepSeekService.transcribeAudio`。
    *   接口：目前兼容 OpenAI Whisper 格式 API。
    *   **智能填充策略**:
        *   若转录文本 **≤ 15 字** -> 自动填充为**任务标题**。
        *   若转录文本 **> 15 字** -> 自动填充为**任务描述**（若标题为空，截取前10字作为标题）。

3.  **智能分析 (AI Analysis)**:
    *   **生成标签**: AI 根据标题/描述生成 3-5 个关键词标签。
    *   **象限分析**: AI 根据任务紧迫性和重要性，自动建议所属象限。

### 4.4 提醒机制 (Reminders)
*   **当前实现**: **前台轮询机制**。
*   **逻辑**:
    *   `TodayDecisionPanel` 中包含一个 `LaunchedEffect` 循环（每秒检测）。
    *   对比当前时间与任务的 `dueTimestamp` - `reminderInterval`。
    *   若满足条件且应用在前台，播放系统提示音。
*   **注意**: 目前未集成 WorkManager 或 AlarmManager，因此应用退到后台时提醒不会触发。

---

## 5. 数据模型详解 (Data Models)

### 5.1 数据库表结构 (`tasks`)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long (PK) | 自增主键 |
| `title` | String | 任务标题 |
| `description` | String | 任务描述 |
| `quadrant` | Int | 所属象限 (1-4) |
| `status` | Int | 任务状态 (0:未开始, 1:进行中, 2:已完成, 3:推迟, 4:过期) |
| `isMIT` | Boolean | 是否为今日最重要任务 (0/1) |
| `dueTimestamp` | Long? | 截止时间戳 (毫秒) |
| `tags` | String | 标签（逗号分隔的字符串） |
| `audioPath` | String? | 录音文件名 (如 `audio_uuid.m4a`) |
| `reminderEnabled` | Boolean | 是否开启提醒 |
| `reminderIntervalValue` | Int? | 提醒提前量数值 |
| `reminderIntervalUnit` | Int | 提醒提前量单位 (分钟/小时/天) |
| `createdAt` | Long | 创建时间 |
| `updatedAt` | Long | 更新时间 |

### 5.2 关键枚举

*   **`TaskStatus`**:
    *   `NOT_STARTED`: 未开始
    *   `IN_PROGRESS`: 进行中
    *   `COMPLETED`: 已完成
    *   `DEFERRED`: 已推迟
    *   `OVERDUE`: 已过期 (由 Repository 自动计算，非持久化状态)

---

## 6. 项目目录结构 (Project Structure)

```text
com.xltool.quadrant
├── data/                  # 数据层
│   ├── ai/                # AI 服务 (DeepSeekService)
│   ├── AppDatabase.kt     # Room 数据库定义
│   ├── TaskDao.kt         # 数据库访问接口
│   ├── TaskEntity.kt      # 实体类
│   └── TaskRepository.kt  # 仓库层 (业务逻辑)
├── ui/                    # 界面层
│   ├── components/        # 通用组件
│   ├── home/              # 主页模块
│   │   ├── HomeScreen.kt  # 主屏幕布局
│   │   ├── HomeViewModel.kt # 主页状态管理
│   │   ├── TaskCard.kt    # 任务卡片组件
│   │   ├── TaskEditorSheet.kt # 任务编辑/新建页
│   │   └── QuadrantPanel.kt # 四象限面板
│   ├── navigation/        # 导航配置
│   └── theme/             # 主题与样式
├── update/                # 应用更新模块
└── util/                  # 工具类
```

## 7. 总结
XLTool Quadrant 展示了一个完整的 Android 本地化应用架构。它通过合理的架构分层，保证了代码的可维护性；通过引入 AI 和语音技术，极大地提升了用户录入任务的效率。未来可进一步通过引入 WorkManager 增强后台提醒能力，以及通过云同步功能实现多端数据互通。
