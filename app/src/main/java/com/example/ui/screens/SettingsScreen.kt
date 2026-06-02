package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserSettings
import com.example.ui.components.UserProfileAvatar
import com.example.ui.BlockerViewModel
import com.example.util.isAccessibilityServiceEnabled
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: BlockerViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.userSettings.collectAsState()
    val isDark = settings.theme == "dark"

    var notificationEnabled by remember { mutableStateOf(true) }

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
                text = "Settings",
                color = if (isDark) Color.White else Color(0xFF0F0E17),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage account, theme, schedule and notifications.",
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 14.sp
            )
        }

        // 1. Profile section card & Logout button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                .background(if (isDark) Color(0x0EFFFFFF) else Color.White)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserProfileAvatar(
                        photoUrl = settings.photoUrl,
                        displayName = settings.displayName,
                        size = 50.dp,
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (settings.displayName.isNotEmpty()) settings.displayName else "Guest User",
                            color = if (isDark) Color.White else Color(0xFF0F0E17),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (settings.email.isNotEmpty()) settings.email else "focusModeEnabled@local.app",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                }

                // Explicit Logout trigger button
                Button(
                    onClick = {
                        viewModel.signOut()
                        onLogout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("logout_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign Out",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Theme Setting Card
        SettingToggleCard(
            title = "Dark Theme",
            description = "Toggle light and dark theme",
            icon = Icons.Default.DarkMode,
            checked = isDark,
            isDark = isDark
        ) { checked ->
            viewModel.toggleTheme(settings.theme)
            Toast.makeText(context, if (checked) "Dark theme enabled" else "Light theme enabled", Toast.LENGTH_SHORT).show()
        }

        // 3. Scheduling Picker Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                .background(if (isDark) Color(0x0EFFFFFF) else Color.White)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Active Timing",
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Blocking Schedule",
                                color = if (isDark) Color.White else Color(0xFF0F0E17),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Restrict doomscrolls during set hours",
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.scheduleEnabled,
                        onCheckedChange = { checked -> 
                            viewModel.setScheduleEnabled(checked)
                            Toast.makeText(context, if (checked) "Schedule turned on" else "Schedule turned off", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981)
                        ),
                        modifier = Modifier.testTag("schedule_toggle_switch")
                    )
                }

                if (settings.scheduleEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Inline Time picker increment components (start & end)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ScheduleTimeAdjuster(
                            modifier = Modifier.weight(1f),
                            label = "Starts At",
                            timeString = settings.scheduleStart,
                            isDark = isDark
                        ) { newTime ->
                            viewModel.updateSchedule(newTime, settings.scheduleEnd)
                        }

                        ScheduleTimeAdjuster(
                            modifier = Modifier.weight(1f),
                            label = "Ends At",
                            timeString = settings.scheduleEnd,
                            isDark = isDark
                        ) { newTime ->
                            viewModel.updateSchedule(settings.scheduleStart, newTime)
                        }
                    }
                }
            }
        }



        // 5. Notification Setting Card
        SettingToggleCard(
            title = "Notifications",
            description = "Get daily reminders of hours saved",
            icon = Icons.Default.Notifications,
            checked = notificationEnabled,
            isDark = isDark
        ) { checked ->
            notificationEnabled = checked
            Toast.makeText(context, if (checked) "Notifications turned on" else "Notifications turned off", Toast.LENGTH_SHORT).show()
        }



        // Footer version details
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val versionName = com.example.BuildConfig.VERSION_NAME

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Version $versionName \n© $year Sahed Alom Sumit",
                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://sahedalomsumit.com"))
                    context.startActivity(intent)
                }
            ) {
                Text(
                    text = "Built with ",
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                
                val infiniteTransition = rememberInfiniteTransition()
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "heart_pulse"
                )
                
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Love",
                    tint = Color.Red,
                    modifier = Modifier.size(14.dp).scale(scale)
                )
                
                Text(
                    text = " by ",
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                
                Text(
                    text = "Sahed",
                    color = Color(0xFF8B5CF6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun SettingToggleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    isDark: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0x0EFFFFFF) else Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = if (isDark) Color.White else Color(0xFF0F0E17),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981)
                )
            )
        }
    }
}

@Composable
fun ScheduleTimeAdjuster(
    modifier: Modifier,
    label: String,
    timeString: String,
    isDark: Boolean,
    onTimeChanged: (String) -> Unit
) {
    val times = timeString.split(":")
    val hour = times.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = times.getOrNull(1)?.toIntOrNull() ?: 0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0x05FFFFFF) else Color(0x04000000))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Hour Column Adjuster
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Add Hour",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            val newHour = (hour + 1) % 24
                            val time = String.format("%02d:%02d", newHour, minute)
                            onTimeChanged(time)
                        }
                )
                Text(
                    text = String.format("%02d", hour),
                    color = if (isDark) Color.White else Color(0xFF0F0E17),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Lower Hour",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            val newHour = if (hour - 1 < 0) 23 else hour - 1
                            val time = String.format("%02d:%02d", newHour, minute)
                            onTimeChanged(time)
                        }
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            Text(text = ":", color = if (isDark) Color.White else Color(0xFF0F0E17), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))

            // Minute Column Adjuster
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Add Minute",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            val newMinute = (minute + 5) % 60
                            val time = String.format("%02d:%02d", hour, newMinute)
                            onTimeChanged(time)
                        }
                )
                Text(
                    text = String.format("%02d", minute),
                    color = if (isDark) Color.White else Color(0xFF0F0E17),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Lower Minute",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            val newMinute = if (minute - 5 < 0) 55 else minute - 5
                            val time = String.format("%02d:%02d", hour, newMinute)
                            onTimeChanged(time)
                        }
                )
            }
        }
    }
}

