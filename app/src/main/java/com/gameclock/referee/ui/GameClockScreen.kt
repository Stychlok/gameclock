package com.gameclock.referee.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.gameclock.referee.GameClockPrefs
import java.time.LocalTime
import kotlinx.coroutines.delay

private const val VIBRATE_LAST_SECONDS = 10
private const val VIBRATE_URGENT_SECONDS = 5

private const val QUARTER_EDIT_MIN_SEC = 10
private const val QUARTER_EDIT_MAX_SEC = 99 * 60 + 59

@Composable
fun GameClockScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext

    var savedQuarterSec by remember {
        mutableIntStateOf(GameClockPrefs.getQuarterDurationSec(appContext))
    }
    var showSettings by remember { mutableStateOf(false) }

    var sessionTotalSec by remember { mutableStateOf<Int?>(null) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var showAdjustRemaining by remember { mutableStateOf(false) }
    var adjustEntrySnapshotSec by remember { mutableIntStateOf(0) }

    DisposableEffect(sessionTotalSec) {
        if (sessionTotalSec == null) {
            onDispose { }
        } else {
            val activity = context as? Activity
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GameClock::Countdown"
            ).apply { setReferenceCounted(false) }
            wakeLock.acquire()
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                if (wakeLock.isHeld) wakeLock.release()
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    LaunchedEffect(isRunning, sessionTotalSec) {
        if (!isRunning || sessionTotalSec == null) return@LaunchedEffect
        while (isRunning && secondsLeft > 0) {
            delay(1000)
            if (secondsLeft > 0) {
                secondsLeft -= 1
                if (secondsLeft in 1..VIBRATE_URGENT_SECONDS) {
                    vibrateUrgent(context)
                } else if (secondsLeft in (VIBRATE_URGENT_SECONDS + 1)..VIBRATE_LAST_SECONDS) {
                    vibrateShort(context)
                }
            }
        }
        isRunning = false
        if (secondsLeft <= 0) {
            sessionTotalSec = null
            secondsLeft = 0
        }
    }

    when {
        showSettings -> QuarterSettingsScreen(
            initialTotalSec = savedQuarterSec,
            onSave = { totalSec ->
                GameClockPrefs.setQuarterDurationSec(appContext, totalSec)
                savedQuarterSec = GameClockPrefs.getQuarterDurationSec(appContext)
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )

        sessionTotalSec != null && showAdjustRemaining -> RemainingTimeAdjustScreen(
            snapshotSec = adjustEntrySnapshotSec,
            secondsLeft = secondsLeft,
            maxTotalSec = sessionTotalSec!!,
            onSecondsLeftChange = { secondsLeft = it },
            onDismiss = { showAdjustRemaining = false },
            onResetSession = {
                showAdjustRemaining = false
                sessionTotalSec = null
                secondsLeft = 0
                isRunning = false
            }
        )

        sessionTotalSec != null -> RunningQuarterView(
            secondsLeft = secondsLeft,
            isRunning = isRunning,
            onTogglePause = {
                val starting = !isRunning
                isRunning = starting
                if (starting) vibrateStart(context) else vibrateStop(context)
            },
            onOpenAdjustRemaining = {
                if (!isRunning && secondsLeft > 0) {
                    adjustEntrySnapshotSec = secondsLeft
                    showAdjustRemaining = true
                }
            }
        )

        else -> IdleHomeView(
            savedQuarterSec = savedQuarterSec,
            onStartQuarter = {
                sessionTotalSec = savedQuarterSec
                secondsLeft = savedQuarterSec
                isRunning = true
                vibrateStart(context)
            },
            onOpenSettings = { showSettings = true }
        )
    }
}

@Composable
private fun IdleHomeView(
    savedQuarterSec: Int,
    onStartQuarter: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onStartQuarter)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatMmSs(savedQuarterSec),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFBB86FC),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tap to start",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            SetLengthSlidersIcon(color = Color(0xFFBB86FC))
        }
    }
}

@Composable
private fun QuarterSettingsScreen(
    initialTotalSec: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    DurationEditScreen(
        title = "Quarter",
        initialTotalSec = initialTotalSec,
        minTotalSec = QUARTER_EDIT_MIN_SEC,
        maxTotalSec = QUARTER_EDIT_MAX_SEC,
        onSave = onSave,
        onDismiss = onDismiss
    )
}

@Composable
private fun DurationEditScreen(
    title: String,
    initialTotalSec: Int,
    minTotalSec: Int,
    maxTotalSec: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val clamped = initialTotalSec.coerceIn(minTotalSec, maxTotalSec)
    var draftMin by remember(initialTotalSec, minTotalSec, maxTotalSec, title) {
        mutableIntStateOf(clamped / 60)
    }
    var draftSec by remember(initialTotalSec, minTotalSec, maxTotalSec, title) {
        mutableIntStateOf(clamped % 60)
    }

    val maxMinute = maxTotalSec / 60
    val secUpperInclusive = if (draftMin >= maxMinute) maxTotalSec % 60 else 59
    val secLowerInclusive = if (draftMin == 0) minTotalSec else 0

    fun setDraftMin(newMin: Int) {
        val m = newMin.coerceIn(0, maxMinute)
        draftMin = m
        val su = if (m >= maxMinute) maxTotalSec % 60 else 59
        val sl = if (m == 0) minTotalSec else 0
        draftSec = draftSec.coerceIn(sl, su)
    }

    fun setDraftSec(newSec: Int) {
        val su = if (draftMin >= maxMinute) maxTotalSec % 60 else 59
        val sl = if (draftMin == 0) minTotalSec else 0
        draftSec = newSec.coerceIn(sl, su)
    }

    val draftTotal = draftMin * 60 + draftSec

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = formatMmSs(draftTotal),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFBB86FC),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Length",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TimeStepperRow(
                label = "min",
                value = draftMin,
                range = 0..maxMinute,
                onChange = { setDraftMin(it) },
                compact = true
            )
            TimeStepperRow(
                label = "sec",
                value = draftSec,
                range = secLowerInclusive..secUpperInclusive,
                onChange = { setDraftSec(it) },
                compact = true
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                ChevronBackIcon(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onSave(draftTotal) },
                contentAlignment = Alignment.Center
            ) {
                DoneCheckIcon(color = Color(0xFFBB86FC))
            }
        }
    }
}

@Composable
private fun RemainingTimeAdjustScreen(
    snapshotSec: Int,
    secondsLeft: Int,
    maxTotalSec: Int,
    onSecondsLeftChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onResetSession: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    val maxMinute = maxTotalSec / 60
    val draftMin = secondsLeft / 60
    val draftSec = secondsLeft % 60
    val secUpperInclusive = if (draftMin >= maxMinute) maxTotalSec % 60 else 59

    fun applyMinute(newMin: Int) {
        onSecondsLeftChange(remainingAfterMinuteChange(secondsLeft, maxTotalSec, newMin))
    }

    fun applySec(newSec: Int) {
        onSecondsLeftChange(remainingAfterSecChange(secondsLeft, maxTotalSec, newSec))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Time left",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatMmSs(snapshotSec),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFBB86FC),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Before change",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimeStepperRow(
                    label = "min",
                    value = draftMin,
                    range = 0..maxMinute,
                    onChange = { applyMinute(it) },
                    compact = true
                )
                TimeStepperRow(
                    label = "sec",
                    value = draftSec,
                    range = 0..secUpperInclusive,
                    onChange = { applySec(it) },
                    compact = true
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { showResetConfirm = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↺",
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    DoneCheckIcon(color = Color(0xFFBB86FC))
                }
            }
        }

        if (showResetConfirm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable { showResetConfirm = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { /* consume; avoid closing when tapping dialog body */ },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Reset quarter?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Timer stops and you return home.",
                        fontSize = 11.sp,
                        color = Color(0xFFB3B3B3),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            color = Color(0xFFB3B3B3),
                            modifier = Modifier
                                .clickable { showResetConfirm = false }
                                .padding(8.dp)
                        )
                        Text(
                            text = "Reset",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800),
                            modifier = Modifier
                                .clickable {
                                    showResetConfirm = false
                                    onResetSession()
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetLengthSlidersIcon(color: Color = Color(0xFFBB86FC)) {
    Canvas(modifier = Modifier.size(28.dp)) {
        val sw = 2.5.dp.toPx()
        val knobR = 2.8.dp.toPx()
        val w = size.width
        val h = size.height
        val tracks = listOf(
            Triple(0.22f, 0.78f, 0.34f),
            Triple(0.12f, 0.88f, 0.58f),
            Triple(0.28f, 0.72f, 0.46f)
        )
        tracks.forEachIndexed { i, (x0f, x1f, kf) ->
            val y = h * (0.30f + i * 0.20f)
            drawLine(
                color = color,
                start = Offset(w * x0f, y),
                end = Offset(w * x1f, y),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = color,
                radius = knobR,
                center = Offset(w * kf, y)
            )
        }
    }
}

@Composable
private fun AdjustTimeClockIcon(color: Color = Color(0xFFBB86FC)) {
    Canvas(modifier = Modifier.size(28.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.36f
        val stroke = 2.dp.toPx()
        drawCircle(
            color = color,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = color,
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round,
            start = Offset(cx, cy),
            end = Offset(cx - r * 0.32f, cy - r * 0.22f)
        )
        drawLine(
            color = color,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
            start = Offset(cx, cy),
            end = Offset(cx + r * 0.34f, cy - r * 0.26f)
        )
    }
}

@Composable
private fun ChevronBackIcon(color: Color) {
    Canvas(modifier = Modifier.size(26.dp)) {
        val stroke = 2.8.dp.toPx()
        val cap = StrokeCap.Round
        val w = size.width
        val h = size.height
        drawLine(
            color = color,
            strokeWidth = stroke,
            cap = cap,
            start = Offset(w * 0.62f, h * 0.22f),
            end = Offset(w * 0.32f, h * 0.50f)
        )
        drawLine(
            color = color,
            strokeWidth = stroke,
            cap = cap,
            start = Offset(w * 0.32f, h * 0.50f),
            end = Offset(w * 0.62f, h * 0.78f)
        )
    }
}

@Composable
private fun DoneCheckIcon(color: Color) {
    Canvas(modifier = Modifier.size(26.dp)) {
        val stroke = 3.dp.toPx()
        drawLine(
            color = color,
            strokeWidth = stroke,
            cap = StrokeCap.Round,
            start = Offset(size.width * 0.14f, size.height * 0.52f),
            end = Offset(size.width * 0.42f, size.height * 0.72f)
        )
        drawLine(
            color = color,
            strokeWidth = stroke,
            cap = StrokeCap.Round,
            start = Offset(size.width * 0.42f, size.height * 0.72f),
            end = Offset(size.width * 0.88f, size.height * 0.28f)
        )
    }
}

private fun remainingAfterMinuteChange(
    currentSec: Int,
    maxTotalSec: Int,
    newMin: Int
): Int {
    val maxM = maxTotalSec / 60
    val m = newMin.coerceIn(0, maxM)
    val secPart = currentSec % 60
    val maxS = if (m >= maxM) maxTotalSec % 60 else 59
    val s = secPart.coerceAtMost(maxS)
    return (m * 60 + s).coerceIn(1, maxTotalSec)
}

private fun remainingAfterSecChange(
    currentSec: Int,
    maxTotalSec: Int,
    newSec: Int
): Int {
    val maxM = maxTotalSec / 60
    val m = currentSec / 60
    val maxS = if (m >= maxM) maxTotalSec % 60 else 59
    return (m * 60 + newSec.coerceIn(0, maxS)).coerceIn(1, maxTotalSec)
}

@Composable
private fun WallClockHeader() {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = LocalTime.now()
        }
    }
    Text(
        text = String.format("%02d:%02d", now.hour, now.minute),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun TimeStepperRow(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    compact: Boolean = false
) {
    val btnSize = if (compact) 34.dp else 40.dp
    val btnFont = if (compact) 22.sp else 28.sp
    val digitFont = if (compact) 20.sp else 28.sp
    val labelFont = if (compact) 9.sp else 11.sp
    val gap = if (compact) 8.dp else 12.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        Box(
            modifier = Modifier
                .size(btnSize)
                .clickable {
                    if (value > range.first) onChange(value - 1)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                fontSize = btnFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value.toString().padStart(2, '0'),
                fontSize = digitFont,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFBB86FC),
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = labelFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(btnSize)
                .clickable {
                    if (value < range.last) onChange(value + 1)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontSize = btnFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RunningQuarterView(
    secondsLeft: Int,
    isRunning: Boolean,
    onTogglePause: () -> Unit,
    onOpenAdjustRemaining: () -> Unit
) {
    val displayColor = when {
        secondsLeft <= 0 -> Color.Red
        secondsLeft <= 5 -> Color(0xFFFF9800)
        else -> Color(0xFFBB86FC)
    }
    val paused = !isRunning && secondsLeft > 0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = secondsLeft > 0, onClick = onTogglePause)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        WallClockHeader()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatMmSs(secondsLeft),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = displayColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "quarter",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (paused) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Paused",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (paused) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clickable(onClick = onOpenAdjustRemaining),
                contentAlignment = Alignment.Center
            ) {
                AdjustTimeClockIcon(color = Color(0xFFBB86FC))
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatMmSs(totalSec: Int): String {
    val t = totalSec.coerceAtLeast(0)
    val m = t / 60
    val s = t % 60
    return String.format("%d:%02d", m, s)
}

private fun vibrateShort(context: Context) {
    val vibrator = getVibrator(context) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasVibrator()) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
}

private fun vibrateUrgent(context: Context) {
    val vibrator = getVibrator(context) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasVibrator()) {
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 120, 80, 120),
                intArrayOf(0, 255, 0, 255),
                -1
            )
        )
    }
}

private fun vibrateStart(context: Context) {
    val vibrator = getVibrator(context) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasVibrator()) {
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 60, 50, 60),
                intArrayOf(0, 180, 0, 255),
                -1
            )
        )
    }
}

private fun vibrateStop(context: Context) {
    val vibrator = getVibrator(context) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasVibrator()) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(220, 255)
        )
    }
}

private fun getVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
