package com.xltool.quadrant.ui.home

import android.app.Activity
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xltool.quadrant.data.Quadrant
import com.xltool.quadrant.data.ReminderUnit
import com.xltool.quadrant.data.TaskDraft
import com.xltool.quadrant.data.TaskStatus
import com.xltool.quadrant.data.RepeatType
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import com.xltool.quadrant.data.ai.DeepSeekService
import kotlinx.coroutines.launch

// 象限颜色定义
private val QuadrantColors = mapOf(
    Quadrant.IMPORTANT_URGENT to Color(0xFFE53935),
    Quadrant.IMPORTANT_NOT_URGENT to Color(0xFF1E88E5),
    Quadrant.URGENT_NOT_IMPORTANT to Color(0xFFFB8C00),
    Quadrant.NOT_IMPORTANT_NOT_URGENT to Color(0xFF757575)
)

private val QuadrantLightColors = mapOf(
    Quadrant.IMPORTANT_URGENT to Color(0xFFFFEBEE),
    Quadrant.IMPORTANT_NOT_URGENT to Color(0xFFE3F2FD),
    Quadrant.URGENT_NOT_IMPORTANT to Color(0xFFFFF3E0),
    Quadrant.NOT_IMPORTANT_NOT_URGENT to Color(0xFFF5F5F5)
)

// 状态颜色定义（简化为3种状态）
private val StatusColors = mapOf(
    TaskStatus.IN_PROGRESS to Color(0xFF1E88E5),
    TaskStatus.COMPLETED to Color(0xFF43A047),
    TaskStatus.OVERDUE to Color(0xFFE53935)
)

// 用户可选择的状态（进行中和已完成）
private val SelectableStatuses = listOf(
    TaskStatus.IN_PROGRESS,
    TaskStatus.COMPLETED
)

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    state: EditorState,
    onDismiss: () -> Unit,
    onChange: ((TaskDraft) -> TaskDraft) -> Unit,
    onSave: () -> Unit,
    onGenerateTags: () -> Unit = {},
    onAnalyzeQuadrant: () -> Unit = {},
    onVoiceInput: (String) -> Unit = {}, // 兼容旧接口
    isGeneratingTags: Boolean = false,
    isAnalyzingQuadrant: Boolean = false,
    isVoiceAnalyzing: Boolean = false
) {
    val draft = state.draft
    val scroll = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // DeepSeek 服务实例
    val deepSeekService = remember { DeepSeekService() }
    var isTranscribing by remember { mutableStateOf(false) }

    var dateTimePickerVisible by remember { mutableStateOf(false) }
    var reminderDialogVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // 录音相关状态
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    
    // 录音文件管理
    fun getAudioFile(fileName: String): File {
        val audioDir = File(context.filesDir, "audio_notes")
        if (!audioDir.exists()) audioDir.mkdirs()
        return File(audioDir, fileName)
    }

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder.value?.release()
                player.value?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startRecording() {
        // 生成新的文件名
        val fileName = "audio_${UUID.randomUUID()}.m4a"
        val file = getAudioFile(fileName)
        
        val r = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setOutputFile(file.absolutePath)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.prepare()
            r.start()
            recorder.value = r
            isRecording = true
            // 更新 Draft 中的路径
            onChange { it.copy(audioPath = fileName) }
            Toast.makeText(context, "开始录音...", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "录音启动失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        try {
            recorder.value?.stop()
            recorder.value?.release()
            recorder.value = null
            isRecording = false
            Toast.makeText(context, "录音已保存，正在转文字...", Toast.LENGTH_SHORT).show()
            
            // 自动开始转录
            if (draft.audioPath != null) {
                isTranscribing = true
                scope.launch {
                    val file = getAudioFile(draft.audioPath!!)
                    val result = deepSeekService.transcribeAudio(file)
                    
                    if (result.isSuccess) {
                        val text = result.getOrNull() ?: ""
                        if (text.isNotBlank()) {
                            // 逻辑：<=15字放标题，>15字放描述
                            if (text.length <= 15) {
                                // 如果标题为空或者用户没改过，就覆盖标题
                                if (draft.title.isEmpty()) {
                                    onChange { it.copy(title = text) }
                                } else {
                                    // 否则追加到描述
                                    onChange { it.copy(description = (it.description + "\n" + text).trim()) }
                                }
                            } else {
                                // > 15字，放入描述
                                onChange { it.copy(description = (it.description + "\n" + text).trim()) }
                                // 如果标题是空的，截取前10个字作为标题
                                if (draft.title.isEmpty()) {
                                    onChange { it.copy(title = text.take(10) + "...") }
                                }
                            }
                            Toast.makeText(context, "转录成功", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "转录失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                    isTranscribing = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            isTranscribing = false
        }
    }

    fun playAudio(fileName: String) {
        val file = getAudioFile(fileName)
        if (!file.exists()) {
            Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val p = MediaPlayer()
            p.setDataSource(file.absolutePath)
            p.prepare()
            p.start()
            p.setOnCompletionListener { 
                isPlaying = false
                p.release()
                player.value = null
            }
            player.value = p
            isPlaying = true
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopPlaying() {
        try {
            player.value?.stop()
            player.value?.release()
            player.value = null
            isPlaying = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun deleteAudio() {
        if (draft.audioPath != null) {
            val file = getAudioFile(draft.audioPath)
            if (file.exists()) file.delete()
            onChange { it.copy(audioPath = null) }
            stopPlaying()
        }
    }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(context, "需要麦克风权限才能录音", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            // 检查权限
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            )
            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // 从 dueTimestamp 解析出日期和时间
    val dueDateTime = remember(draft.dueTimestamp) {
        draft.dueTimestamp?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }
    }

    fun dateTimeToMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 顶部操作栏 (Header)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "取消", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = if (draft.id == null) "新建任务" else "编辑任务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            }

            // 2. 标题输入与录音 (Compact Title with Recorder)
            TextField(
                value = draft.title,
                onValueChange = { v -> onChange { it.copy(title = v) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入任务标题...", style = MaterialTheme.typography.titleLarge) },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 录音按钮
                        IconButton(onClick = { toggleRecording() }) {
                            if (isRecording) {
                                // 录音中动画效果 (简单的变色)
                                Icon(Icons.Default.Stop, "停止录音", tint = Color.Red)
                            } else if (isTranscribing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Mic, "录音", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            )
            
            // 录音状态栏 (如果正在录音或有录音文件)
            if (isRecording) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("正在录音... 点击麦克风停止", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
            } else if (draft.audioPath != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("语音备注", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Row {
                         IconButton(onClick = { 
                            if (isPlaying) stopPlaying() else playAudio(draft.audioPath)
                        }) {
                            Icon(
                                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, 
                                "播放/停止",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { deleteAudio() }) {
                            Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 3. 属性栏 (Chips Row) - 日期、时间、提醒、状态
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 日期时间合并 Chip
                AssistChip(
                    onClick = { dateTimePickerVisible = true },
                    label = { 
                        Text(
                            if (dueDateTime != null) 
                                "${dueDateTime.format(dateFormatter)} ${dueDateTime.format(timeFormatter)}"
                            else "设置时间"
                        ) 
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (dueDateTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        leadingIconContentColor = if (dueDateTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                // 提醒 Chip
                AssistChip(
                    onClick = { 
                        if (!draft.reminderEnabled) {
                            onChange { it.copy(reminderEnabled = true, reminderIntervalValue = 30) }
                        }
                        reminderDialogVisible = true 
                    },
                    label = { 
                        Text(
                            if (draft.reminderEnabled && draft.reminderIntervalValue != null) 
                                "提前 ${draft.reminderIntervalValue}${draft.reminderIntervalUnit.displayName}"
                            else "提醒"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (draft.reminderEnabled) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                            null, 
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (draft.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        leadingIconContentColor = if (draft.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                // 状态 Chip (仅编辑时显示，或作为选项)
                AssistChip(
                    onClick = { /* Cycle status or open dialog? Simplified to cycle here for compactness */ 
                         val nextStatus = SelectableStatuses[(SelectableStatuses.indexOf(draft.status) + 1) % SelectableStatuses.size]
                         onChange { it.copy(status = nextStatus) }
                    },
                    label = { Text(draft.status.displayName) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(StatusColors[draft.status] ?: Color.Gray, CircleShape)
                        )
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }

            // 4. 任务类型选择（重复类型）
            Column {
                Text(
                    "任务类型", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatType.entries.forEach { type ->
                        FilterChip(
                            selected = draft.repeatType == type,
                            onClick = { onChange { it.copy(repeatType = type) } },
                            label = { Text(type.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. 象限选择 (Compact Grid)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "优先级", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onAnalyzeQuadrant, modifier = Modifier.size(24.dp)) {
                         if (isAnalyzingQuadrant) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, "AI 分析", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1
                    CompactQuadrantCard(
                        quadrant = Quadrant.IMPORTANT_URGENT,
                        isSelected = draft.quadrant == Quadrant.IMPORTANT_URGENT,
                        onClick = { onChange { it.copy(quadrant = Quadrant.IMPORTANT_URGENT) } },
                        modifier = Modifier.weight(1f)
                    )
                    CompactQuadrantCard(
                        quadrant = Quadrant.IMPORTANT_NOT_URGENT,
                        isSelected = draft.quadrant == Quadrant.IMPORTANT_NOT_URGENT,
                        onClick = { onChange { it.copy(quadrant = Quadrant.IMPORTANT_NOT_URGENT) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 2
                    CompactQuadrantCard(
                        quadrant = Quadrant.URGENT_NOT_IMPORTANT,
                        isSelected = draft.quadrant == Quadrant.URGENT_NOT_IMPORTANT,
                        onClick = { onChange { it.copy(quadrant = Quadrant.URGENT_NOT_IMPORTANT) } },
                        modifier = Modifier.weight(1f)
                    )
                    CompactQuadrantCard(
                        quadrant = Quadrant.NOT_IMPORTANT_NOT_URGENT,
                        isSelected = draft.quadrant == Quadrant.NOT_IMPORTANT_NOT_URGENT,
                        onClick = { onChange { it.copy(quadrant = Quadrant.NOT_IMPORTANT_NOT_URGENT) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. 描述与标签 (Merged visual style)
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp) // 遵循 8px 倍数
            ) {
                TextField(
                    value = draft.description,
                    onValueChange = { v -> onChange { it.copy(description = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("添加描述...") },
                    minLines = 3,
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                TextField(
                    value = draft.tags,
                    onValueChange = { v -> onChange { it.copy(tags = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("标签 (逗号分隔)") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        IconButton(onClick = onGenerateTags) {
                             if (isGeneratingTags) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, "AI 生成", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DateTime Picker Dialog (合并日期和时间选择)
    if (dateTimePickerVisible) {
        var selectedDate by remember(dueDateTime) { mutableStateOf(dueDateTime?.toLocalDate() ?: LocalDate.now()) }
        var selectedTime by remember(dueDateTime) { mutableStateOf(dueDateTime?.toLocalTime() ?: LocalTime.of(9, 0)) }
        var showDatePicker by remember { mutableStateOf(true) }
        
        AlertDialog(
            onDismissRequest = { dateTimePickerVisible = false },
            title = { Text(if (showDatePicker) "选择日期" else "选择时间") },
            text = {
                Column {
                    if (showDatePicker) {
                        val pickerState = rememberDatePickerState(
                            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        )
                        DatePicker(state = pickerState)
                        
                        LaunchedEffect(pickerState.selectedDateMillis) {
                            pickerState.selectedDateMillis?.let { millis ->
                                selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                        }
                    } else {
                        val timePickerState = rememberTimePickerState(
                            initialHour = selectedTime.hour,
                            initialMinute = selectedTime.minute,
                            is24Hour = true
                        )
                        TimePicker(state = timePickerState)
                        
                        LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                            selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (showDatePicker) {
                            showDatePicker = false
                        } else {
                            val newDateTime = LocalDateTime.of(selectedDate, selectedTime)
                            onChange {
                                it.copy(
                                    dueTimestamp = dateTimeToMillis(newDateTime),
                                    dueEpochDay = selectedDate.toEpochDay()
                                )
                            }
                            dateTimePickerVisible = false
                        }
                    }
                ) { Text(if (showDatePicker) "下一步" else "确定") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (showDatePicker) {
                            dateTimePickerVisible = false
                        } else {
                            showDatePicker = true
                        }
                    }
                ) { Text(if (showDatePicker) "取消" else "返回") }
            }
        )
    }
    
    // Reminder Setting Dialog
    if (reminderDialogVisible) {
        AlertDialog(
            onDismissRequest = { reminderDialogVisible = false },
            title = { Text("设置提醒") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("启用提醒")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = draft.reminderEnabled,
                            onCheckedChange = { enabled -> onChange { it.copy(reminderEnabled = enabled) } }
                        )
                    }
                    if (draft.reminderEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = draft.reminderIntervalValue?.toString() ?: "",
                                onValueChange = { v -> 
                                    val num = v.trim().toIntOrNull()?.coerceIn(1, 999)
                                    onChange { it.copy(reminderIntervalValue = num) }
                                },
                                modifier = Modifier.width(80.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Simple Unit Selector
                             Row {
                                ReminderUnit.entries.forEach { unit ->
                                    FilterChip(
                                        selected = draft.reminderIntervalUnit == unit,
                                        onClick = { onChange { it.copy(reminderIntervalUnit = unit) } },
                                        label = { Text(unit.displayName) },
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reminderDialogVisible = false }) { Text("确定") }
            }
        )
    }
}

@Composable
private fun CompactQuadrantCard(
    quadrant: Quadrant,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = QuadrantColors[quadrant] ?: Color.Gray
    val lightColor = QuadrantLightColors[quadrant] ?: Color.LightGray
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) lightColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(if (isSelected) 1.dp else 0.dp, if (isSelected) color else Color.Transparent),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(12.dp).background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = quadrant.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
