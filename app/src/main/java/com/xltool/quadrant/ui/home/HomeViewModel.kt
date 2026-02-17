package com.xltool.quadrant.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xltool.quadrant.data.Quadrant
import com.xltool.quadrant.data.TaskEntity
import com.xltool.quadrant.data.TaskDraft
import com.xltool.quadrant.data.TaskRepository
import com.xltool.quadrant.data.TaskStatus
import com.xltool.quadrant.data.TaskUiModel
import com.xltool.quadrant.data.ai.DeepSeekService
import com.xltool.quadrant.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditorState(
    val visible: Boolean = false,
    val draft: TaskDraft = TaskDraft(),
    val titleError: String? = null,
    val isGeneratingTags: Boolean = false,
    val isAnalyzingQuadrant: Boolean = false,
    val isVoiceAnalyzing: Boolean = false
)

sealed interface HomeEvent {
    data class UndoDeleteRequested(val taskId: Long) : HomeEvent
}

class HomeViewModel(
    private val repo: TaskRepository,
    private val userPrefs: UserPreferences
) : ViewModel() {

    private val deepSeekService = DeepSeekService()

    private val _showWelcomeGuide = MutableStateFlow(false)
    val showWelcomeGuide: StateFlow<Boolean> = _showWelcomeGuide.asStateFlow()

    private val _birthDate = MutableStateFlow(userPrefs.birthDate)
    val birthDate: StateFlow<Long> = _birthDate.asStateFlow()

    // 提前声明，以便在 init 中使用
    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    init {
        // 检查是否首次运行
        if (userPrefs.isFirstRun) {
            _showWelcomeGuide.value = true
        }

        // 启动时自动检测并更新已超过截止日期的任务状态
        viewModelScope.launch {
            val updatedCount = repo.updateOverdueTasks()
            if (updatedCount > 0) {
                _snackbar.value = "已自动将 $updatedCount 个超期任务标记为已过期"
            }
        }
    }

    // 今日任务（只显示截止日期在今天或之前的任务）
    val q1: StateFlow<List<TaskUiModel>> =
        repo.observeTodayQuadrant(Quadrant.IMPORTANT_URGENT)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val q2: StateFlow<List<TaskUiModel>> =
        repo.observeTodayQuadrant(Quadrant.IMPORTANT_NOT_URGENT)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val q3: StateFlow<List<TaskUiModel>> =
        repo.observeTodayQuadrant(Quadrant.URGENT_NOT_IMPORTANT)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val q4: StateFlow<List<TaskUiModel>> =
        repo.observeTodayQuadrant(Quadrant.NOT_IMPORTANT_NOT_URGENT)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ========== 今日决策相关 ==========
    
    /**
     * 当前 MIT 任务
     */
    val currentMIT: StateFlow<TaskUiModel?> =
        repo.observeMIT()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 所有活跃任务（用于统计）
     */
    val allActiveTasks: StateFlow<List<TaskUiModel>> =
        repo.observeAllActiveTasks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 需要提醒的任务
     */
    val tasksWithReminder: StateFlow<List<TaskUiModel>> =
        repo.observeTasksWithReminder()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 今日决策状态
     */
    val todayDecisionState: StateFlow<TodayDecisionState> = 
        kotlinx.coroutines.flow.combine(currentMIT, q1, q2, q3, q4) { mit, q1Tasks, q2Tasks, q3Tasks, q4Tasks ->
            val suggestedMIT = if (mit == null) {
                q2Tasks.firstOrNull { it.status != TaskStatus.COMPLETED }
            } else null
            
            val counts = QuadrantCounts(
                urgentImportant = q1Tasks.size,
                importantNotUrgent = q2Tasks.size,
                urgentNotImportant = q3Tasks.size,
                notUrgentNotImportant = q4Tasks.size
            )
            
            // 获取所有重要任务用于 MIT 选择
            val importantTasks = q1Tasks + q2Tasks
            
            TodayDecisionState(
                currentMIT = mit,
                suggestedMIT = suggestedMIT,
                quadrantCounts = counts,
                importantTasks = importantTasks,
                q1Tasks = q1Tasks,
                q2Tasks = q2Tasks,
                q3Tasks = q3Tasks,
                q4Tasks = q4Tasks
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayDecisionState())

    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val recentlyDeleted: LinkedHashMap<Long, TaskEntity> = object : LinkedHashMap<Long, TaskEntity>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, TaskEntity>?): Boolean {
            return size > 8
        }
    }

    fun consumeSnackbar() {
        _snackbar.value = null
    }

    fun openCreate(quadrant: Quadrant) {
        _editor.value = EditorState(
            visible = true,
            draft = TaskDraft(quadrant = quadrant),
            titleError = null
        )
    }

    fun openEdit(id: Long) {
        viewModelScope.launch {
            val draft = repo.getTaskDraft(id)
            if (draft == null) {
                _snackbar.value = "任务不存在或已被删除"
                return@launch
            }
            _editor.value = EditorState(visible = true, draft = draft, titleError = null)
        }
    }

    fun closeEditor() {
        _editor.value = EditorState()
    }

    fun updateDraft(transform: (TaskDraft) -> TaskDraft) {
        val current = _editor.value
        _editor.value = current.copy(draft = transform(current.draft), titleError = null)
    }

    fun saveDraft() {
        val current = _editor.value
        val title = current.draft.title.trim()
        if (title.isEmpty()) {
            _editor.value = current.copy(titleError = "标题不能为空")
            return
        }
        viewModelScope.launch {
            try {
                repo.upsert(current.draft.copy(title = title))
                closeEditor()
            } catch (e: Exception) {
                _snackbar.value = "保存失败：${e.message ?: "未知错误"}"
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            val deleted = runCatching { repo.deleteAndReturn(id) }.getOrNull()
            if (deleted == null) {
                _snackbar.value = "任务不存在或已被删除"
                return@launch
            }
            recentlyDeleted[deleted.id] = deleted
            _events.tryEmit(HomeEvent.UndoDeleteRequested(taskId = deleted.id))
        }
    }

    fun undoDelete(id: Long) {
        viewModelScope.launch {
            val entity = recentlyDeleted.remove(id) ?: return@launch
            runCatching { repo.restore(entity) }
                .onFailure { e -> _snackbar.value = "撤销失败：${e.message ?: "未知错误"}" }
        }
    }

    fun togglePinned(id: Long) {
        viewModelScope.launch { repo.togglePinned(id) }
    }

    fun moveToQuadrant(id: Long, target: Quadrant) {
        viewModelScope.launch { repo.moveToQuadrant(id, target) }
    }

    fun moveUp(id: Long) {
        viewModelScope.launch { repo.moveUp(id) }
    }

    fun moveDown(id: Long) {
        viewModelScope.launch { repo.moveDown(id) }
    }

    fun setStatus(id: Long, status: TaskStatus) {
        viewModelScope.launch { repo.setStatus(id, status) }
    }

    fun completeTask(id: Long) {
        viewModelScope.launch {
            repo.setStatus(id, TaskStatus.COMPLETED)
            _snackbar.value = "已标记为完成"
        }
    }

    fun deferOneDay(id: Long) {
        viewModelScope.launch {
            repo.deferDueByDays(id, 1)
            _snackbar.value = "已推迟一天"
        }
    }

    // ========== MIT 操作 ==========

    fun setMIT(id: Long) {
        viewModelScope.launch {
            repo.setMIT(id)
            _snackbar.value = "已设为今日最重要任务"
        }
    }

    fun clearMIT() {
        viewModelScope.launch {
            repo.clearMIT()
            _snackbar.value = "已清除 MIT"
        }
    }

    // ========== AI 生成标签 ==========

    fun generateTags() {
        val current = _editor.value
        val title = current.draft.title.trim()
        val description = current.draft.description.trim()

        if (title.isEmpty() && description.isEmpty()) {
            _snackbar.value = "请先输入任务标题或描述"
            return
        }

        _editor.value = current.copy(isGeneratingTags = true)

        viewModelScope.launch {
            deepSeekService.generateTags(title, description)
                .onSuccess { tags ->
                    val updatedDraft = _editor.value.draft.copy(tags = tags)
                    _editor.value = _editor.value.copy(
                        draft = updatedDraft,
                        isGeneratingTags = false
                    )
                    _snackbar.value = "标签生成成功"
                }
                .onFailure { e ->
                    _editor.value = _editor.value.copy(isGeneratingTags = false)
                    _snackbar.value = "生成失败：${e.message ?: "网络错误"}"
                }
        }
    }

    fun analyzeQuadrant() {
        val current = _editor.value
        val title = current.draft.title.trim()
        val description = current.draft.description.trim()

        if (title.isEmpty() && description.isEmpty()) {
            _snackbar.value = "请先输入任务标题或描述"
            return
        }

        _editor.value = current.copy(isAnalyzingQuadrant = true)

        viewModelScope.launch {
            deepSeekService.analyzeQuadrant(title, description)
                .onSuccess { quadrantId ->
                    val quadrant = Quadrant.fromValue(quadrantId)
                    val updatedDraft = _editor.value.draft.copy(quadrant = quadrant)
                    _editor.value = _editor.value.copy(
                        draft = updatedDraft,
                        isAnalyzingQuadrant = false
                    )
                    _snackbar.value = "AI 判定为：${quadrant.displayName}"
                }
                .onFailure { e ->
                    _editor.value = _editor.value.copy(isAnalyzingQuadrant = false)
                    _snackbar.value = "分析失败：${e.message ?: "网络错误"}"
                }
        }
    }

    fun handleVoiceInput(text: String) {
        if (text.isBlank()) return
        
        _editor.value = _editor.value.copy(isVoiceAnalyzing = true)
        
        viewModelScope.launch {
            deepSeekService.parseVoiceInput(text)
                .onSuccess { parsed ->
                    val quadrant = Quadrant.fromValue(parsed.quadrant)
                    val updatedDraft = _editor.value.draft.copy(
                        title = parsed.title,
                        description = parsed.description,
                        quadrant = quadrant
                    )
                    _editor.value = _editor.value.copy(
                        draft = updatedDraft,
                        isVoiceAnalyzing = false
                    )
                    _snackbar.value = "语音分析完成：${quadrant.displayName}"
                }
                .onFailure { e ->
                    // 即使分析失败，也将原始文本填入标题，方便用户修改
                    val updatedDraft = _editor.value.draft.copy(title = text)
                    _editor.value = _editor.value.copy(
                        draft = updatedDraft,
                        isVoiceAnalyzing = false
                    )
                    _snackbar.value = "智能分析失败，已填入原始文本"
                }
        }
    }

    fun completeWelcomeGuide(mitTitle: String) {
        if (mitTitle.isBlank()) return
        
        viewModelScope.launch {
            // 创建 MIT 任务 (默认为重要不紧急，并设为 MIT)
            val draft = TaskDraft(
                title = mitTitle,
                quadrant = Quadrant.IMPORTANT_NOT_URGENT, // 引导用户关注重要不紧急
                isMIT = true,
                status = TaskStatus.IN_PROGRESS
            )
            repo.upsert(draft)
            
            // 标记非首次运行
            userPrefs.isFirstRun = false
            _showWelcomeGuide.value = false
            
            _snackbar.value = "今日目标已锁定！开始行动吧！"
        }
    }

    fun dismissWelcomeGuide() {
        userPrefs.isFirstRun = false
        _showWelcomeGuide.value = false
    }

    fun setBirthDate(timestamp: Long) {
        userPrefs.birthDate = timestamp
        _birthDate.value = timestamp
        _snackbar.value = "生命倒计时已更新"
    }

    companion object {
        fun factory(repo: TaskRepository, userPrefs: UserPreferences): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repo, userPrefs) as T
                }
            }
        }
    }
}


