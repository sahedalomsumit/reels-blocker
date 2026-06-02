package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserSettings
import com.example.ui.components.UserProfileAvatar
import com.example.ui.components.DisableBlockerDialog
import com.example.ui.BlockerViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: BlockerViewModel,
    onNavigateToPlatforms: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.userSettings.collectAsState()
    val events by viewModel.blockEvents.collectAsState()

    // Animation for gradient border glow on active Master Card
    val infiniteTransition = rememberInfiniteTransition(label = "BorderGlow")
    val borderOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlowOffset"
    )

    var showDisableDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var platformToDisable by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    if (showDisableDialog) {
        DisableBlockerDialog(
            isDark = settings.theme == "dark",
            platformName = "Master Shield",
            onDismiss = { showDisableDialog = false },
            onConfirmDisable = {
                viewModel.setBlockerEnabled(false)
            }
        )
    }

    platformToDisable?.let { platformName ->
        DisableBlockerDialog(
            isDark = settings.theme == "dark",
            platformName = platformName,
            onDismiss = { platformToDisable = null },
            onConfirmDisable = {
                viewModel.togglePlatformBlock(platformName)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (settings.theme == "dark") Color(0xFF050505) else Color(0xFFF5F3FF))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Top Bar Navigation
        HomeHeader(
            settings = settings,
            onThemeToggle = {
                viewModel.toggleTheme(settings.theme)
            }
        )

        // 2. Master Toggle Card
        MasterToggleCard(
            settings = settings,
            onToggleEnabled = { isEnabled ->
                if (!isEnabled) {
                    showDisableDialog = true
                } else {
                    viewModel.setBlockerEnabled(true)
                }
            },
            offsetAnim = borderOffset
        )

        // 3. Quick Stats Row
        val totalBlockedToday = events.filter {
            val elementDay = android.text.format.DateFormat.format("dd", it.timestamp)
            val currentDay = android.text.format.DateFormat.format("dd", System.currentTimeMillis())
            elementDay == currentDay
        }.size
        val todayTimeSaved = totalBlockedToday * 3

        QuickStatsRow(
            todayBlocked = totalBlockedToday,
            timeSavedMinutes = todayTimeSaved,
            streakDays = calculateStreak(events)
        )

        // 4. Horizontal Platforms Quick Control Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blocked Reels/Shorts",
                color = if (settings.theme == "dark") Color.White else Color(0xFF0F0E17),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.clickable { onNavigateToPlatforms() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View All",
                    color = Color(0xFF8B5CF6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate to platforms page",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HorizontalPlatformsList(settings = settings, onToggle = { platform, isEnabled ->
            if (isEnabled) {
                platformToDisable = platform
            } else {
                viewModel.togglePlatformBlock(platform)
            }
        }, onSimulateBlock = { platform ->
            if (settings.blockerEnabled) {
                viewModel.triggerSimulatedBlock(platform)
                Toast.makeText(context, "$platform: Simulated dome-scrolling filter intercepted!", Toast.LENGTH_SHORT).show()
                
                // Also trigger our Overlay block screen to demo!
                val intent = android.content.Intent(context, com.example.OverlayBlockerActivity::class.java).apply {
                    putExtra("platform_name", platform)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Please activate Master Blocker first!", Toast.LENGTH_SHORT).show()
            }
        })

        // 5. Schedule Banner
        if (settings.scheduleEnabled) {
            ScheduleStatusBanner(
                start = settings.scheduleStart,
                end = settings.scheduleEnd,
                onEditClick = onNavigateToSettings,
                isDark = settings.theme == "dark"
            )
        }

        Spacer(modifier = Modifier.height(60.dp)) // Floating nav spacer padding
    }
}

@Composable
fun HomeHeader(
    settings: UserSettings,
    onThemeToggle: () -> Unit
) {
    val isDark = settings.theme == "dark"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // User Avatar image with fallbacks
            UserProfileAvatar(
                photoUrl = settings.photoUrl,
                displayName = settings.displayName,
                size = 44.dp,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Welcome back,",
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 13.sp
                )
                Text(
                    text = if (settings.displayName.isNotEmpty()) settings.displayName else "Stay Focused",
                    color = if (isDark) Color.White else Color(0xFF0F0E17),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Theme Toggle Switch Node Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0x0EFFFFFF) else Color(0x08000000))
                .clickable { onThemeToggle() }
                .testTag("theme_toggle_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle color scheme",
                tint = if (isDark) Color.White else Color(0xFF0F0E17),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MasterToggleCard(
    settings: UserSettings,
    onToggleEnabled: (Boolean) -> Unit,
    offsetAnim: Float
) {
    val isDark = settings.theme == "dark"
    val isEnabled = settings.blockerEnabled
    
    // Dynamic brush borders mapping purple to emerald green flow based on active state
    val activeBorderBrush = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF8B5CF6), // Purple
            Color(0xFF10B981), // Emerald
            Color(0xFF8B5CF6)
        )
    )
    val inactiveBorderBrush = Brush.linearGradient(
        colors = listOf(
            if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB),
            if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 2.dp,
                brush = if (isEnabled) activeBorderBrush else inactiveBorderBrush,
                shape = RoundedCornerShape(24.dp)
            )
            .background(
                color = if (isDark) Color(0x0EFFFFFF) else Color.White,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) "Shield Enabled" else "Shield Paused",
                    color = if (isEnabled) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isEnabled) "Reels Blocker Active" else "Blocker Resting",
                    color = if (isDark) Color.White else Color(0xFF0F0E17),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Total intercepted: ${settings.totalBlocked} times",
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggleEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981), // Emerald green toggle
                    uncheckedTrackColor = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.testTag("master_blocker_switch")
            )
        }
    }
}

@Composable
fun QuickStatsRow(
    todayBlocked: Int,
    timeSavedMinutes: Int,
    streakDays: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatItemChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Shield,
            value = todayBlocked.toString(),
            label = "Blocks Today",
            color = Color(0xFF8B5CF6)
        )
        StatItemChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.HourglassTop,
            value = if (timeSavedMinutes >= 60) "${timeSavedMinutes / 60}h ${timeSavedMinutes % 60}m" else "${timeSavedMinutes}m",
            label = "Saved Today",
            color = Color(0xFF10B981)
        )
        StatItemChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ElectricBolt,
            value = "$streakDays Days",
            label = "Streak Count",
            color = Color(0xFFF59E0B)
        )
    }
}

@Composable
fun StatItemChip(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF050505)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0x0EFFFFFF) else Color.White)
            .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = if (isDark) Color.White else Color(0xFF0F0E17),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun HorizontalPlatformsList(
    settings: UserSettings,
    onToggle: (String, Boolean) -> Unit,
    onSimulateBlock: (String) -> Unit
) {
    val isDark = settings.theme == "dark"
    val scrollState = rememberScrollState()
    
    val list = listOf(
        PlatformItem("Facebook", "Reels", settings.blockFacebook, Color(0xFF1877F2)),
        PlatformItem("Instagram", "Reels", settings.blockInstagram, Color(0xFFE1306C))
    ).sortedByDescending { it.enabled }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (item in list) {
            Card(
                modifier = Modifier
                    .width(135.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (item.enabled) item.color.copy(alpha = 0.4f) else (if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB)),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSimulateBlock(item.name) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0x0EFFFFFF) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                    		.size(40.dp)
                            .clip(CircleShape)
                            .background(item.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = item.name,
                            tint = item.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = item.name,
                        color = if (isDark) Color.White else Color(0xFF0F0E17),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.subText,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (item.enabled) item.color.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { onToggle(item.name, item.enabled) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (item.enabled) "Blocking" else "Bypassed",
                            color = if (item.enabled) item.color else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleStatusBanner(
    start: String,
    end: String,
    onEditClick: () -> Unit,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x198B5CF6), RoundedCornerShape(16.dp))
            .background(Color(0x0B8B5CF6))
            .clickable { onEditClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Active Schedule",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Schedule Enabled",
                        color = if (isDark) Color.White else Color(0xFF0F0E17),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active Daily $start – $end",
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = "Edit",
                color = Color(0xFF8B5CF6),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class PlatformItem(
    val name: String,
    val subText: String,
    val enabled: Boolean,
    val color: Color
)
