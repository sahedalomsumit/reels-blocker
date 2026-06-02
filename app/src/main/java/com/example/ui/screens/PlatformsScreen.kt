package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BlockerViewModel
import com.example.ui.components.DisableBlockerDialog

@Composable
fun PlatformsScreen(
    viewModel: BlockerViewModel
) {
    val settings by viewModel.userSettings.collectAsState()
    val isDark = settings.theme == "dark"

    val platforms = listOf(
        PlatformDetails("Facebook", "Reels", "com.facebook.katana", settings.blockFacebook, Color(0xFF1877F2)),
        PlatformDetails("Instagram", "Reels", "com.instagram.android", settings.blockInstagram, Color(0xFFE1306C))
    )

    val upcomingPlatforms = listOf(
        PlatformDetails("YouTube", "Shorts", "com.google.android.youtube", false, Color(0xFFFF0000)),
        PlatformDetails("Snapchat", "Spotlight", "com.snapchat.android", false, Color(0xFFFFFC00))
    )

    var platformToDisable by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<PlatformDetails?>(null) }

    platformToDisable?.let { platform ->
        DisableBlockerDialog(
            isDark = isDark,
            platformName = platform.name,
            onDismiss = { platformToDisable = null },
            onConfirmDisable = {
                viewModel.togglePlatformBlock(platform.name.lowercase())
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF050505) else Color(0xFFF5F3FF))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title block
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Blocked Platforms",
                color = if (isDark) Color.White else Color(0xFF0F0E17),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Toggle filters to restrict specific short-form sources.",
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 14.sp
            )
        }

        // Platform item rows
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (p in platforms) {
                PlatformConfigRow(
                    p = p,
                    isDark = isDark,
                    onToggle = { 
                        if (p.enabled) {
                            platformToDisable = p
                        } else {
                            viewModel.togglePlatformBlock(p.name.lowercase())
                        }
                    }
                )
            }
        }

        // Feature notice card - "More coming soon"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0x0EFFFFFF) else Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Coming Soon",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "More social platforms coming soon!",
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Upcoming Platform item rows
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (p in upcomingPlatforms) {
                PlatformConfigRow(p = p, isDark = isDark) {
                    // Do nothing for coming soon platforms
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp)) // padding for bottom menu
    }
}

@Composable
fun PlatformConfigRow(
    p: PlatformDetails,
    isDark: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (p.enabled) p.color.copy(alpha = 0.2f) else (if (isDark) Color(0x14FFFFFF) else Color(0xFFE5E7EB)),
                RoundedCornerShape(20.dp)
            )
            .background(if (isDark) Color(0x0EFFFFFF) else Color.White)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored Circle and Icon container
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(p.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = p.name,
                        tint = p.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = p.name,
                            color = if (isDark) Color.White else Color(0xFF0F0E17),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Mini Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (p.enabled) Color(0x2210B981) else Color(0x1A64748B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (p.enabled) "Blocking" else "Off",
                                color = if (p.enabled) Color(0xFF10B981) else Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = p.blockedContent,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Switch(
                checked = p.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981),
                    uncheckedTrackColor = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.testTag("switch_${p.name.lowercase().replace(" ", "_")}")
            )
        }
    }
}
}

data class PlatformDetails(
    val name: String,
    val blockedContent: String,
    val packageName: String,
    val enabled: Boolean,
    val color: Color
)
