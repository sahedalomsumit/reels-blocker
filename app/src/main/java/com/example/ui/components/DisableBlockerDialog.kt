package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DisableBlockerDialog(
    isDark: Boolean,
    platformName: String,
    onDismiss: () -> Unit,
    onConfirmDisable: () -> Unit
) {
    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val totalTime = 30000L // 30 seconds
            val interval = 50L
            val steps = (totalTime / interval).toInt()
            for (i in 1..steps) {
                delay(interval)
                holdProgress = i.toFloat() / steps
            }
            onConfirmDisable()
            onDismiss()
        } else {
            holdProgress = 0f
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Disable Blocker",
                color = if (isDark) Color.White else Color(0xFF0F0E17),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "To disable the blocker for $platformName, you must hold the button below for 30 seconds. Are you sure you want to give up your focus?",
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(if (isDark) Color(0xFF333333) else Color(0xFFE2E8F0))
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(holdProgress)
                            .fillMaxHeight()
                            .background(Color(0xFF8B5CF6))
                            .align(Alignment.CenterStart)
                    )
                    Text(
                        text = if (isHolding) "Holding... (${(holdProgress * 30).toInt()}s)" else "Hold 30s to Disable",
                        color = if (isHolding || isDark) Color.White else Color(0xFF0F0E17),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8B5CF6))
            }
        }
    )
}
