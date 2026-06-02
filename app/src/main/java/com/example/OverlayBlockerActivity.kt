package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import com.example.data.local.AppDatabase
import com.example.data.repository.BlockerRepository
import kotlinx.coroutines.launch

class OverlayBlockerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val platformName = intent.getStringExtra("platform_name") ?: "Social Feed"

        setContent {
            OverlayBlockerScreen(platformName = platformName) {
                finish()
            }
        }
    }
}

@Composable
fun OverlayBlockerScreen(platformName: String, onGoBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Prevent gesture back
    BackHandler { }
    
    // Auto-record the block event on launch
    LaunchedEffect(Unit) {
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val repository = BlockerRepository(db.blockerDao())
            repository.addBlockEvent(platformName)
        }
    }

    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val totalTime = 3000L
            val interval = 50L
            val steps = (totalTime / interval).toInt()
            for (i in 1..steps) {
                delay(interval)
                holdProgress = i.toFloat() / steps
            }
            onGoBack()
        } else {
            holdProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505)), // BG Dark from rules
        contentAlignment = Alignment.Center
    ) {
        // Glowing Background Radial Circle Decoration
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x228B5CF6), // Subtle Accent Purple Glow
                            Color.Transparent
                        )
                    )
                )
        )

        // Glassmorphic Central Shield Card
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x33FFFFFF), // Highlight border
                            Color(0x05FFFFFF)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .background(
                    color = Color(0x0EFFFFFF), // Glass card bg
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Icon inside Glowing circle
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Warning Headings
            Text(
                text = "Reel Blocked",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Same content. Same loop. You've seen it all before on $platformName.",
                color = Color(0xFF94A3B8), // Muted dark text
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline quote box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x07FFFFFF), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "“Take back your focus.”",
                    color = Color(0xFF10B981), // Emerald green
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color(0xFF333333)) // Dark background for the button track
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isHolding = true
                                tryAwaitRelease()
                                isHolding = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Progress Fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(holdProgress)
                        .fillMaxHeight()
                        .background(Color(0xFF8B5CF6))
                        .align(Alignment.CenterStart)
                )

                Text(
                    text = if (isHolding) "Hold to unlock... (${(holdProgress * 3).toInt()}s)" else "Hold 3s to Go Back",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
