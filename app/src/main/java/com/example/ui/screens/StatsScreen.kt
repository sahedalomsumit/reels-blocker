package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockEvent
import com.example.ui.BlockerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    viewModel: BlockerViewModel
) {
    val settings by viewModel.userSettings.collectAsState()
    val events by viewModel.blockEvents.collectAsState()
    val isDark = settings.theme == "dark"

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(events) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200)
        )
    }

    // 1. Process block logs over last 7 days
    val weeklyStats = processWeeklyEvents(events)
    
    // Find most blocked platform
    val mostBlocked = events.groupBy { it.platform }
        .maxByOrNull { it.value.size }?.key ?: "No data"

    val totalBlockedText = (settings.totalBlocked * animProgress.value).toInt()
    val totalTimeText = (settings.totalTimeSavedMinutes * animProgress.value).toInt()

    // Calculate Today's Stats
    val todayEvents = events.filter {
        val elementDay = android.text.format.DateFormat.format("dd", it.timestamp)
        val currentDay = android.text.format.DateFormat.format("dd", System.currentTimeMillis())
        elementDay == currentDay
    }
    val todayBlockedText = (todayEvents.size * animProgress.value).toInt()
    val todayTimeText = (todayEvents.size * 3 * animProgress.value).toInt() // Assuming 3 mins per block

    // App Breakdown
    val appBreakdown = events.groupBy { it.platform }.mapValues { it.value.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF050505) else Color(0xFFF5F3FF))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Addiction Stats",
                color = if (isDark) Color.White else Color(0xFF0F0E17),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tracking hours saved from short-form loops.",
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 14.sp
            )
        }

        // 2. Primary Hero Stats Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB), RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0x0EFFFFFF) else Color.White)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL BLOCKED FEEDS",
                        color = Color(0xFF8B5CF6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalBlockedText times",
                        color = if (isDark) Color.White else Color(0xFF0F0E17),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("stats_total_blocked_text")
                    )
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x1210B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Stats Increase",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 3. Custom Canvas Weekly Chart
        Text(
            text = "Active Interceptions (Last 7 Days)",
            color = if (isDark) Color.White else Color(0xFF0F0E17),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        CustomWeeklyBarChart(
            days = weeklyStats.map { it.dayLabel },
            values = weeklyStats.map { it.count },
            animScale = animProgress.value,
            isDark = isDark
        )

        // 4. Today's Focus
        Text(
            text = "Today's Focus",
            color = if (isDark) Color.White else Color(0xFF0F0E17),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsMetricCard(
                modifier = Modifier.weight(1f),
                title = "Today Saved",
                value = if (todayTimeText >= 60) "${todayTimeText / 60}h ${todayTimeText % 60}m" else "${todayTimeText}m",
                subText = "Time reclaimed today",
                icon = Icons.Default.HourglassBottom,
                iconColor = Color(0xFF10B981),
                isDark = isDark
            )

            StatsMetricCard(
                modifier = Modifier.weight(1f),
                title = "Today Blocks",
                value = todayBlockedText.toString(),
                subText = "Interceptions today",
                icon = Icons.Default.ElectricBolt,
                iconColor = Color(0xFFF59E0B),
                isDark = isDark
            )
        }

        // 5. All-Time details list
        Text(
            text = "All-Time Impact",
            color = if (isDark) Color.White else Color(0xFF0F0E17),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsMetricCard(
                modifier = Modifier.weight(1f),
                title = "All-Time Saved",
                value = if (totalTimeText >= 60) "${totalTimeText / 60}h ${totalTimeText % 60}m" else "${totalTimeText}m",
                subText = "Total focus regained",
                icon = Icons.Default.HourglassBottom,
                iconColor = Color(0xFF10B981),
                isDark = isDark
            )

            StatsMetricCard(
                modifier = Modifier.weight(1f),
                title = "All-Time Blocks",
                value = totalBlockedText.toString(),
                subText = "Total interceptions",
                icon = Icons.Default.Smartphone,
                iconColor = Color(0xFF8B5CF6),
                isDark = isDark
            )
        }

        // 6. App Breakdown
        if (appBreakdown.isNotEmpty()) {
            Text(
                text = "App Breakdown",
                color = if (isDark) Color.White else Color(0xFF0F0E17),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                appBreakdown.entries.sortedByDescending { it.value }.forEach { (app, count) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x0EFFFFFF) else Color.White)
                            .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app,
                                color = if (isDark) Color.White else Color(0xFF0F0E17),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$count times",
                                color = Color(0xFF8B5CF6),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        val streakCount = calculateStreak(events)
        
        // Streak indicator Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .background(Color(0x0EF59E0B))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x22F59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Streak Icon",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (streakCount > 0) "$streakCount-Day Focus Streak!" else "No blocks yet!",
                        color = if (isDark) Color.White else Color(0xFF0F0E17),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (streakCount > 0) "You kept your social blocker active $streakCount days in a row." else "Blockers active. Keep it up to start a streak!",
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp)) // Nav Spacer
    }
}

@Composable
fun StatsMetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isDark: Boolean
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x0EFFFFFF) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = value,
                color = if (isDark) Color.White else Color(0xFF0F0E17),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subText,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun CustomWeeklyBarChart(
    days: List<String>,
    values: List<Int>,
    animScale: Float,
    isDark: Boolean
) {
    val maxCount = remember(values) { values.maxOrNull()?.coerceAtLeast(5) ?: 5 }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0x0EFFFFFF) else Color.White)
            .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartWidth = size.width
            val chartHeight = size.height - 30.dp.toPx() // Reserve bottom for day labels
            val columnCount = days.size
            val spacing = 16.dp.toPx()
            val totalSpacing = spacing * (columnCount + 1)
            val columnWidth = (chartWidth - totalSpacing) / columnCount
            
            // Draw background grid lines (horizontal 3 states)
            val gridColor = if (isDark) Color(0x14FFFFFF) else Color(0x00000000).copy(0.05f)
            for (i in 0..3) {
                val y = chartHeight * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw each bar column
            for (idx in 0 until columnCount) {
                val value = values.getOrElse(idx) { 0 }
                val progressHeight = (value.toFloat() / maxCount) * chartHeight * animScale
                
                val x = spacing + idx * (columnWidth + spacing)
                val y = chartHeight - progressHeight

                // Draw rounded columns with purple-to-emerald gradient brush
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6), // Purple top
                            Color(0xFF10B981)  // Emerald base
                        )
                    ),
                    topLeft = Offset(x, y),
                    size = Size(columnWidth, progressHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Optional count indicator text over bars
                if (value > 0 && progressHeight > 18.dp.toPx()) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 10.sp.toPx()
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        canvas.nativeCanvas.drawText(
                            value.toString(),
                            x + columnWidth / 2f,
                            y + 14.dp.toPx(),
                            paint
                        )
                    }
                }
                
                // Draw Day label at bottom center
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.parseColor("#64748B")
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText(
                        days[idx],
                        x + columnWidth / 2f,
                        size.height - 8.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}

data class DayStat(
    val dayLabel: String,
    val count: Int
)

fun processWeeklyEvents(events: List<BlockEvent>): List<DayStat> {
    val formatter = SimpleDateFormat("EE", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    // Create map for last 7 days initialized to 0
    val last7Days = mutableListOf<Date>()
    for (i in 0 until 7) {
        last7Days.add(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    // Reverse to chronological order (past to present)
    last7Days.reverse()

    val countsMap = events.groupBy {
        formatter.format(Date(it.timestamp))
    }.mapValues { it.value.size }

    // If completely empty, return simulated/mock values for beauty display!
    if (events.isEmpty()) {
        return listOf(
            DayStat("Mon", 3),
            DayStat("Tue", 8),
            DayStat("Wed", 4),
            DayStat("Thu", 1),
            DayStat("Fri", 5),
            DayStat("Sat", 0),
            DayStat("Sun", 0)
        )
    }

    return last7Days.map { date ->
        val key = formatter.format(date)
        DayStat(key, countsMap[key] ?: 0)
    }
}

fun calculateStreak(events: List<BlockEvent>): Int {
    if (events.isEmpty()) return 0
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val uniqueDates = events.map { sdf.format(Date(it.timestamp)) }.distinct().sortedDescending()
    
    val calendar = Calendar.getInstance()
    val today = sdf.format(calendar.time)
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = sdf.format(calendar.time)
    
    var streak = 0
    var checkDateIndex = 0
    
    if (uniqueDates.isNotEmpty() && uniqueDates[0] == today) {
        streak = 1
        checkDateIndex = 1
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    } else if (uniqueDates.isNotEmpty() && uniqueDates[0] == yesterday) {
        streak = 1
        checkDateIndex = 1
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -2)
    } else {
        return 0
    }
    
    while (checkDateIndex < uniqueDates.size) {
        val dateToCheck = sdf.format(calendar.time)
        if (uniqueDates[checkDateIndex] == dateToCheck) {
            streak++
            checkDateIndex++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        } else if (uniqueDates[checkDateIndex] > dateToCheck) {
             break
        } else {
             break
        }
    }
    return streak
}
