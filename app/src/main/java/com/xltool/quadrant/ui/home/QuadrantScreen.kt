package com.xltool.quadrant.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xltool.quadrant.R

@Composable
fun QuadrantScreen(
    vm: HomeViewModel,
    onOpenSearch: () -> Unit = {}
) {
    val editor = vm.editor.collectAsStateWithLifecycle()
    val todayDecisionState = vm.todayDecisionState.collectAsStateWithLifecycle()
    val tasksWithReminder = vm.tasksWithReminder.collectAsStateWithLifecycle()
    val birthDate = vm.birthDate.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarText = vm.snackbar.collectAsState(initial = null)
    
    // ... (lines 32-56 omitted for brevity in thought, but I must be careful with context)
    // I can't skip lines in replacement content unless I match them.
    // I will target the collection block and then the usage block separately or together.
    // It's safer to target the top block to add `birthDate`.
    // Then target `TodayDecisionPanel` call.
    // I'll use multi_replace.



    val taskDeletedMsg = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(snackbarText.value) {
        val msg = snackbarText.value ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg)
        vm.consumeSnackbar()
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is HomeEvent.UndoDeleteRequested -> {
                    val result = snackbarHostState.showSnackbar(
                        message = taskDeletedMsg,
                        actionLabel = undoLabel
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.undoDelete(event.taskId)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 今日决策区域 - 可滚动
        TodayDecisionPanel(
            state = todayDecisionState.value,
            tasksWithReminder = tasksWithReminder.value,
            onSetMIT = vm::setMIT,
            onClearMIT = vm::clearMIT,
            onEditTask = vm::openEdit,
            onDeleteTask = vm::deleteTask,
            onSetTaskStatus = vm::setStatus,
            onCompleteTask = vm::completeTask,
            birthDate = birthDate.value,
            onSetBirthDate = vm::setBirthDate,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        )

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    val showWelcomeGuide = vm.showWelcomeGuide.collectAsStateWithLifecycle()

    // 欢迎指引
    if (showWelcomeGuide.value) {
        WelcomeGuideDialog(
            onConfirm = vm::completeWelcomeGuide,
            onDismiss = vm::dismissWelcomeGuide
        )
    }

    // 任务编辑弹窗
    if (editor.value.visible) {
        TaskEditorSheet(
            state = editor.value,
            onDismiss = vm::closeEditor,
            onChange = vm::updateDraft,
            onSave = vm::saveDraft,
            onGenerateTags = vm::generateTags,
            onAnalyzeQuadrant = vm::analyzeQuadrant,
            onVoiceInput = vm::handleVoiceInput,
            isGeneratingTags = editor.value.isGeneratingTags,
            isAnalyzingQuadrant = editor.value.isAnalyzingQuadrant,
            isVoiceAnalyzing = editor.value.isVoiceAnalyzing
        )
    }
}
