package com.decompress.app.ui

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.work.WorkManager
import com.decompress.app.data.TaskHistoryCodec
import com.decompress.app.data.TaskHistoryStore
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import com.decompress.app.service.ArchiveTaskControl
import com.decompress.app.service.CompressionScheduler
import com.decompress.app.service.ExtractionScheduler
import com.decompress.app.service.ExtractionTaskRegistry
import com.decompress.app.service.TaskNavigation
import com.decompress.app.data.TaskHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.decompress.app.ui.theme.ZippestRadius

@Composable
internal fun TaskQueueDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf<List<WorkInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var pausedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        while (true) {
            tasks = runCatching {
                loadArchiveWorkInfos(context)
            }.getOrDefault(emptyList())
            pausedTaskIds = tasks
                .filter { ArchiveTaskControl.isPaused(context, it.id) }
                .mapTo(mutableSetOf()) { it.id.toString() }
            loading = false
            delay(1000)
        }
    }

    val sortedTasks = tasks.sortedWith(
        compareBy {
            when (it.state) {
                WorkInfo.State.RUNNING -> 0
                WorkInfo.State.ENQUEUED -> 1
                WorkInfo.State.BLOCKED -> 2
                else -> 3
            }
        }
    )
    val hasActiveTasks = sortedTasks.any {
        it.state == WorkInfo.State.RUNNING ||
            it.state == WorkInfo.State.ENQUEUED ||
            it.state == WorkInfo.State.BLOCKED
    }
    val hasFinishedTasks = sortedTasks.any {
        it.state != WorkInfo.State.RUNNING &&
            it.state != WorkInfo.State.ENQUEUED &&
            it.state != WorkInfo.State.BLOCKED
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("后台任务") },
        text = {
            when {
                loading -> LoadingSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    rows = 3,
                    delayMillis = 200
                )
                sortedTasks.isEmpty() -> EmptyStateView(
                    art = EmptyArt.TASKS,
                    title = "暂无后台任务",
                    description = "解压、压缩任务会出现在这里，熄屏也会继续",
                    actionLabel = "关闭",
                    onAction = onDismiss
                )
                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sortedTasks.forEach { task ->
                        TaskQueueRow(
                            task = task,
                            paused = task.id.toString() in pausedTaskIds,
                            onTogglePause = {
                                val paused = task.id.toString() !in pausedTaskIds
                                ArchiveTaskControl.setPaused(context, task.id, paused)
                                pausedTaskIds = if (paused) {
                                    pausedTaskIds + task.id.toString()
                                } else {
                                    pausedTaskIds - task.id.toString()
                                }
                            }
                        )
                    }
                    if (hasFinishedTasks) {
                        TextButton(
                            onClick = {
                                WorkManager.getInstance(context.applicationContext)
                                    .pruneWork()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("清理历史")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hasActiveTasks) {
                Button(
                    onClick = {
                        WorkManager.getInstance(context.applicationContext).apply {
                            cancelUniqueWork(ExtractionScheduler.QUEUE_NAME)
                            cancelUniqueWork(CompressionScheduler.QUEUE_NAME)
                        }
                    }
                ) {
                    Text("取消全部", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
internal fun TaskCenterContent(
    modifier: Modifier = Modifier,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf<List<WorkInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var pausedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var history by remember { mutableStateOf<List<TaskHistoryEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            tasks = runCatching { loadArchiveWorkInfos(context) }.getOrDefault(emptyList())
            pausedTaskIds = tasks
                .filter { ArchiveTaskControl.isPaused(context, it.id) }
                .mapTo(mutableSetOf()) { it.id.toString() }
            history = runCatching {
                TaskHistoryStore(context.applicationContext).list()
            }.getOrDefault(emptyList())
            loading = false
            delay(1000)
        }
    }

    val runningStates = setOf(
        WorkInfo.State.RUNNING,
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.BLOCKED
    )
    val activeTasks = tasks.filter { it.state in runningStates }
    val finishedTasks = tasks.filterNot { it.state in runningStates }

    if (loading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (tasks.isEmpty()) {
        Column(
            modifier = modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("还没有任务", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "压缩或解压后会出现在这里，可以暂停、继续或取消",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeTasks.isNotEmpty()) {
                item { TaskSectionLabel("进行中", "${activeTasks.size} 项") }
                items(activeTasks, key = { "active-${it.id}" }) { task ->
                    TaskQueueRow(
                        task = task,
                        paused = task.id.toString() in pausedTaskIds,
                        onTogglePause = {
                            val paused = task.id.toString() !in pausedTaskIds
                            ArchiveTaskControl.setPaused(context, task.id, paused)
                            pausedTaskIds = if (paused) {
                                pausedTaskIds + task.id.toString()
                            } else {
                                pausedTaskIds - task.id.toString()
                            }
                        },
                        onCancel = {
                            ArchiveTaskControl.clear(context, task.id)
                            WorkManager.getInstance(context.applicationContext)
                                .cancelWorkById(task.id)
                            onNotify("已取消任务")
                        }
                    )
                }
            }
            if (finishedTasks.isNotEmpty()) {
                item { TaskSectionLabel("已结束", "${finishedTasks.size} 项") }
                items(finishedTasks, key = { "done-${it.id}" }) { task ->
                    TaskQueueRow(
                        task = task,
                        paused = false,
                        onTogglePause = {},
                        onResumeTimedOut = {
                            val remembered = ExtractionTaskRegistry.find(task.id)
                            if (remembered != null) {
                                ExtractionScheduler.enqueue(context.applicationContext, remembered)
                                onNotify("已重新加入队列")
                            } else {
                                onNotify("任务参数已释放，请重新选择文件解压")
                            }
                        }
                    )
                }
            }
            if (history.isNotEmpty()) {
                item { TaskSectionLabel("历史", "${history.size} 条") }
                items(history, key = { "history-${it.id}" }) { entry ->
                    TaskHistoryRow(
                        entry = entry,
                        onOpen = {
                            entry.locationUri?.let { uri ->
                                val intent = android.content.Intent(
                                    context,
                                    com.decompress.app.MainActivity::class.java
                                ).apply {
                                    action = TaskNavigation.ACTION_OPEN_LOCATION
                                    putExtra(TaskNavigation.EXTRA_LOCATION_URI, uri)
                                    putExtra(TaskNavigation.EXTRA_LOCATION_NAME, entry.locationName)
                                    putExtra(TaskNavigation.EXTRA_LOCATION_TREE_URI, entry.treeUri)
                                    putExtra(TaskNavigation.EXTRA_LOCATION_PATH, entry.path)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        onRetry = {
                            val queued = entry.retryPayload?.let { payload ->
                                when (entry.kind) {
                                    "extract" -> TaskHistoryCodec.decodeExtraction(payload)
                                        ?.let {
                                            ExtractionScheduler.enqueue(
                                                context.applicationContext,
                                                it
                                            )
                                        }
                                    "compress" -> TaskHistoryCodec.decodeCompression(payload)
                                        ?.let {
                                            CompressionScheduler.enqueue(
                                                context.applicationContext,
                                                it
                                            )
                                        }
                                    else -> null
                                }
                            }
                            if (queued != null) {
                                onNotify("已重新加入队列")
                            } else {
                                onNotify("任务参数已失效，请重新发起")
                            }
                        },
                        onDelete = {
                            TaskHistoryStore(context.applicationContext).delete(entry.id)
                            history = history.filterNot { it.id == entry.id }
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (finishedTasks.isNotEmpty()) {
                TextButton(
                    onClick = {
                        WorkManager.getInstance(context.applicationContext).pruneWork()
                    }
                ) {
                    Text("清理历史")
                }
            }
            if (activeTasks.isNotEmpty()) {
                TextButton(
                    onClick = {
                        WorkManager.getInstance(context.applicationContext).apply {
                            cancelUniqueWork(ExtractionScheduler.QUEUE_NAME)
                            cancelUniqueWork(CompressionScheduler.QUEUE_NAME)
                        }
                        onNotify("已取消全部任务")
                    }
                ) {
                    Text("取消全部", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
internal fun RecentExtractionStrip(
    entries: List<TaskHistoryEntry>,
    onOpen: (TaskHistoryEntry) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            "最近解压",
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                Surface(
                    shape = RoundedCornerShape(ZippestRadius.sm),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .width(190.dp)
                        .clickable { onOpen(entry) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            entry.locationName.ifBlank { "结果文件夹" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}


internal suspend fun loadArchiveWorkInfos(context: Context): List<WorkInfo> =
    withContext(Dispatchers.IO) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        buildList {
            addAll(
                workManager
                    .getWorkInfosForUniqueWork(ExtractionScheduler.QUEUE_NAME)
                    .get()
            )
            addAll(
                workManager
                    .getWorkInfosForUniqueWork(CompressionScheduler.QUEUE_NAME)
                    .get()
            )
        }
    }

@Composable
internal fun ActiveArchiveTaskBar(
    task: WorkInfo,
    activeCount: Int,
    onClick: () -> Unit
) {
    val isCompression =
        task.tags.contains(CompressionScheduler.TAG_ALL_TASKS)
    val title = task.tags
        .firstOrNull { it.startsWith(ExtractionScheduler.TAG_NAME_PREFIX) }
        ?.removePrefix(ExtractionScheduler.TAG_NAME_PREFIX)
        ?: if (isCompression) "压缩任务" else "解压任务"
    val running = task.state == WorkInfo.State.RUNNING
    val paused = task.progress.getBoolean(ExtractionScheduler.KEY_PAUSED, false)
    val progress = task.progress.getInt(ExtractionScheduler.KEY_PROGRESS, 0)
        .coerceIn(0, 100)
    val message = when {
        paused -> "已暂停，点击任务队列继续"
        running -> task.progress.getString(ExtractionScheduler.KEY_MESSAGE)
            ?: if (isCompression) "正在压缩…" else "正在解压…"
        task.state == WorkInfo.State.BLOCKED -> "等待前序任务完成"
        else -> "等待后台任务开始"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(ZippestRadius.md),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isCompression) Icons.Outlined.Unarchive
                                else Icons.Outlined.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (activeCount > 1) "$title · 另有 ${activeCount - 1} 项"
                            else title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            message,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        when {
                            paused -> "暂停"
                            running -> "$progress%"
                            else -> "等待"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (running) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(ZippestRadius.pill)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(ZippestRadius.pill)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun TaskQueueRow(
    task: WorkInfo,
    paused: Boolean,
    onTogglePause: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onResumeTimedOut: (() -> Unit)? = null
) {
    val title = task.tags
        .firstOrNull { it.startsWith(ExtractionScheduler.TAG_NAME_PREFIX) }
        ?.removePrefix(ExtractionScheduler.TAG_NAME_PREFIX)
        ?: "后台任务"
    val foregroundTimeout =
        task.stopReason == WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT
    val message = when {
        foregroundTimeout -> "系统限制了后台运行时长，点「继续」接着解压"
        paused -> "已暂停"
        task.state == WorkInfo.State.SUCCEEDED ->
            task.outputData.getString(ExtractionScheduler.KEY_MESSAGE) ?: "任务完成"
        task.state == WorkInfo.State.FAILED ->
            task.outputData.getString(ExtractionScheduler.KEY_MESSAGE) ?: "任务失败"
        task.state == WorkInfo.State.CANCELLED -> "已取消"
        task.state == WorkInfo.State.BLOCKED -> "等待前序任务"
        task.state == WorkInfo.State.ENQUEUED -> "等待执行"
        else -> task.progress.getString(ExtractionScheduler.KEY_MESSAGE) ?: "正在处理"
    }
    val progress = when {
        task.state == WorkInfo.State.SUCCEEDED -> 100
        task.state == WorkInfo.State.RUNNING ->
            task.progress.getInt(ExtractionScheduler.KEY_PROGRESS, 0)
        else -> 0
    }
    var previousSample by remember(task.id) {
        mutableStateOf<Pair<Long, Int>?>(null)
    }
    var percentPerSecond by remember(task.id) { mutableStateOf<Float?>(null) }
    LaunchedEffect(task.id, task.state, progress, paused) {
        if (task.state == WorkInfo.State.RUNNING && !paused) {
            val now = SystemClock.elapsedRealtime()
            previousSample?.let { (previousTime, previousProgress) ->
                val elapsed = now - previousTime
                val changed = progress - previousProgress
                if (elapsed in 250..10_000 && changed > 0) {
                    percentPerSecond = changed * 1000f / elapsed
                }
            }
            previousSample = now to progress
        } else {
            previousSample = null
        }
    }
    val etaSeconds = percentPerSecond?.let { rate ->
        if (rate > 0.01f) ((100 - progress) / rate).toInt() else null
    }
    val active = task.state == WorkInfo.State.RUNNING ||
        task.state == WorkInfo.State.ENQUEUED ||
        task.state == WorkInfo.State.BLOCKED

    Surface(
        shape = RoundedCornerShape(ZippestRadius.sm),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (foregroundTimeout && onResumeTimedOut != null) {
                    TextButton(onClick = onResumeTimedOut) {
                        Text("继续")
                    }
                }
                if (active) {
                    if (onCancel != null) {
                        IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "取消任务",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    TextButton(onClick = onTogglePause) {
                        Icon(
                            if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (paused) "继续" else "暂停")
                    }
                }
            }
            if (task.state == WorkInfo.State.RUNNING && !paused) {
                Text(
                    buildString {
                        percentPerSecond?.let {
                            append("速度 ${"%.1f".format(it)}%/秒")
                        }
                        etaSeconds?.let {
                            if (isNotEmpty()) append(" · ")
                            append("约 ${formatEta(it)}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (task.state == WorkInfo.State.RUNNING) {
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(ZippestRadius.pill))
                )
            }
        }
    }
}

internal fun formatEta(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    return if (safeSeconds < 60) {
        "${safeSeconds} 秒"
    } else if (safeSeconds < 3600) {
        "${safeSeconds / 60} 分 ${safeSeconds % 60} 秒"
    } else {
        "${safeSeconds / 3600} 小时 ${safeSeconds % 3600 / 60} 分"
    }
}

