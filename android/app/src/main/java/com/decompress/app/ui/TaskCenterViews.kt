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
import com.decompress.app.R
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
        title = { Text(UiStrings.get(R.string.task_title)) },
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
                    title = UiStrings.get(R.string.task_empty),
                    description = UiStrings.get(R.string.task_empty_hint),
                    actionLabel = UiStrings.get(R.string.close),
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
                            Text(UiStrings.get(R.string.clear_history))
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
                    Text(UiStrings.get(R.string.cancel_all), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = onDismiss) { Text(UiStrings.get(R.string.close)) }
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
            Text(UiStrings.get(R.string.no_tasks), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                UiStrings.get(R.string.no_tasks_hint),
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
                item { TaskSectionLabel(UiStrings.get(R.string.in_progress), UiStrings.get(R.string.active_count, activeTasks.size)) }
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
                            onNotify(UiStrings.get(R.string.cancelled_tasks))
                        }
                    )
                }
            }
            if (finishedTasks.isNotEmpty()) {
                item { TaskSectionLabel(UiStrings.get(R.string.finished), UiStrings.get(R.string.finished_count, finishedTasks.size)) }
                items(finishedTasks, key = { "done-${it.id}" }) { task ->
                    TaskQueueRow(
                        task = task,
                        paused = false,
                        onTogglePause = {},
                        onResumeTimedOut = {
                            val remembered = ExtractionTaskRegistry.find(task.id)
                            if (remembered != null) {
                                ExtractionScheduler.enqueue(context.applicationContext, remembered)
                                onNotify(UiStrings.get(R.string.requeued))
                            } else {
                                onNotify(UiStrings.get(R.string.params_released))
                            }
                        }
                    )
                }
            }
            if (history.isNotEmpty()) {
                item { TaskSectionLabel(UiStrings.get(R.string.history), UiStrings.get(R.string.history_count, history.size)) }
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
                                onNotify(UiStrings.get(R.string.requeued))
                            } else {
                                onNotify(UiStrings.get(R.string.params_expired))
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
                    Text(UiStrings.get(R.string.clear_history))
                }
            }
            if (activeTasks.isNotEmpty()) {
                TextButton(
                    onClick = {
                        WorkManager.getInstance(context.applicationContext).apply {
                            cancelUniqueWork(ExtractionScheduler.QUEUE_NAME)
                            cancelUniqueWork(CompressionScheduler.QUEUE_NAME)
                        }
                        onNotify(UiStrings.get(R.string.all_cancelled))
                    }
                ) {
                    Text(UiStrings.get(R.string.cancel_all), color = MaterialTheme.colorScheme.error)
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
            UiStrings.get(R.string.recent),
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
                            entry.locationName.ifBlank { UiStrings.get(R.string.result_folder) },
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
        ?: if (isCompression) UiStrings.get(R.string.compression) else UiStrings.get(R.string.extraction)
    val running = task.state == WorkInfo.State.RUNNING
    val paused = task.progress.getBoolean(ExtractionScheduler.KEY_PAUSED, false)
    val progress = task.progress.getInt(ExtractionScheduler.KEY_PROGRESS, 0)
        .coerceIn(0, 100)
    val message = when {
        paused -> UiStrings.get(R.string.paused_hint)
        running -> task.progress.getString(ExtractionScheduler.KEY_MESSAGE)
            ?: if (isCompression) UiStrings.get(R.string.compressing) else UiStrings.get(R.string.extracting)
        task.state == WorkInfo.State.BLOCKED -> UiStrings.get(R.string.waiting_earlier)
        else -> UiStrings.get(R.string.waiting_start)
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
                            if (activeCount > 1) UiStrings.get(R.string.title_more, title, activeCount - 1)
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
                            paused -> UiStrings.get(R.string.pause)
                            running -> "$progress%"
                            else -> UiStrings.get(R.string.waiting)
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
        ?: UiStrings.get(R.string.task_title)
    val foregroundTimeout =
        task.stopReason == WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT
    val message = when {
        foregroundTimeout -> UiStrings.get(R.string.limited)
        paused -> UiStrings.get(R.string.paused)
        task.state == WorkInfo.State.SUCCEEDED ->
            task.outputData.getString(ExtractionScheduler.KEY_MESSAGE) ?: UiStrings.get(R.string.completed)
        task.state == WorkInfo.State.FAILED ->
            task.outputData.getString(ExtractionScheduler.KEY_MESSAGE) ?: UiStrings.get(R.string.failed)
        task.state == WorkInfo.State.CANCELLED -> UiStrings.get(R.string.cancelled)
        task.state == WorkInfo.State.BLOCKED -> UiStrings.get(R.string.waiting_earlier)
        task.state == WorkInfo.State.ENQUEUED -> UiStrings.get(R.string.queued)
        else -> task.progress.getString(ExtractionScheduler.KEY_MESSAGE) ?: UiStrings.get(R.string.processing)
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
                        Text(UiStrings.get(R.string.resume))
                    }
                }
                if (active) {
                    if (onCancel != null) {
                        IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = UiStrings.get(R.string.cancel_task),
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
                        Text(if (paused) UiStrings.get(R.string.resume) else UiStrings.get(R.string.pause))
                    }
                }
            }
            if (task.state == WorkInfo.State.RUNNING && !paused) {
                Text(
                    buildString {
                        percentPerSecond?.let {
                            append(
                                UiStrings.get(
                                    R.string.speed_prefix
                                ) + String.format(java.util.Locale.ROOT, "%.1f", it) + "%/s"
                            )
                        }
                        etaSeconds?.let {
                            if (isNotEmpty()) append(" · ")
                            append(UiStrings.get(R.string.about_eta, formatEta(it)))
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
        UiStrings.get(R.string.eta_seconds, safeSeconds)
    } else if (safeSeconds < 3600) {
        UiStrings.get(R.string.eta_minutes, safeSeconds / 60, safeSeconds % 60)
    } else {
        UiStrings.get(R.string.eta_hours, safeSeconds / 3600, safeSeconds % 3600 / 60)
    }
}

