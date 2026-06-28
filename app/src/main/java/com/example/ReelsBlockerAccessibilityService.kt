package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReelsBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var cachedSettings: UserSettings? = null
    private var lastCheckTime = 0L
    private var lastBlockTime = 0L
    // True while the overlay activity is on screen; suppresses re-detection
    @Volatile private var isOverlayActive = false

    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher",
        "com.android.launcher3",
        "com.miui.home",
        "com.sec.android.app.launcher",
        // YouTube is not a supported platform; explicitly ignore it to prevent accidental blocking
        "com.google.android.youtube",
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("ReelsBlocker", "Accessibility Service Connected")
        // Load settings initially
        observeSettings()
    }

    private fun observeSettings() {
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.blockerDao().getUserSettings().collect { settings ->
                cachedSettings = settings ?: UserSettings()
                Log.d("ReelsBlocker", "Settings updated: isEnabled=${cachedSettings?.blockerEnabled}")
            }
        }
    }

    private var analysisJob: kotlinx.coroutines.Job? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val selfPackage = applicationContext.packageName
        // Ignore our own overlay activity and known launchers/system UI
        if (packageName == selfPackage || packageName in ignoredPackages) return

        // While the overlay is shown, suppress all re-detection to prevent flicker
        if (isOverlayActive) return

        val eventType = event.eventType

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            
            val currentTime = System.currentTimeMillis()
            // Throttle checks slightly to prevent performance hits
            if (currentTime - lastCheckTime < 100) return
            lastCheckTime = currentTime

            // Cancel any ongoing analysis to prioritize the latest event
            analysisJob?.cancel()
            analysisJob = serviceScope.launch {
                val settings = cachedSettings ?: fetchSettingsDirectly()
                
                // 1. Is master blocker turned on?
                if (!settings.blockerEnabled) return@launch

                // 2. Is schedule active?
                if (settings.scheduleEnabled && !isCurrentTimeInSchedule(settings.scheduleStart, settings.scheduleEnd)) {
                    return@launch
                }

                // 3. Inspect and match package names and content
                when {
                    packageName.contains("instagram") && settings.blockInstagram -> {
                        evaluateReelNodes("Instagram", listOf(
                            "Reels", "reel", "instagram_reels"
                        ))
                    }
                    packageName.contains("facebook") && settings.blockFacebook -> {
                        evaluateReelNodes("Facebook", listOf(
                            "watch", "Reels", "reel", "Short videos", "facebook_reels"
                        ))
                    }
                }
            }
        }
    }

    private suspend fun fetchSettingsDirectly(): UserSettings {
        val db = AppDatabase.getDatabase(applicationContext)
        val settings = db.blockerDao().getUserSettingsDirect() ?: UserSettings()
        cachedSettings = settings
        return settings
    }

    @Suppress("DEPRECATION")
    private fun recycleNode(node: AccessibilityNodeInfo?) {
        if (node != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            try {
                node.recycle()
            } catch (e: Exception) {
                // Ignore recycle errors
            }
        }
    }

    private fun evaluateReelNodes(platform: String, keywords: List<String>) {
        val rootNode = rootInActiveWindow ?: return
        try {
            val rootRect = android.graphics.Rect()
            rootNode.getBoundsInScreen(rootRect)
            val screenHeight = rootRect.height()
            val screenWidth = rootRect.width()

            if (checkNodeKeywordsIterative(rootNode, keywords, screenHeight, screenWidth)) {
                blockAndOverlay(platform)
            }
        } catch (e: Exception) {
            Log.e("ReelsBlocker", "Error evaluating reel nodes", e)
        } finally {
            recycleNode(rootNode)
        }
    }

    private fun checkNodeKeywordsIterative(rootNode: AccessibilityNodeInfo, keywords: List<String>, screenHeight: Int, screenWidth: Int): Boolean {
        val stack = mutableListOf<AccessibilityNodeInfo>()
        stack.add(rootNode)
        val rect = android.graphics.Rect()

        while (stack.isNotEmpty()) {
            val node = stack.removeAt(stack.size - 1)

            // Check text content or content descriptions
            val text = node.text
            val desc = node.contentDescription
            val viewId = node.viewIdResourceName

            node.getBoundsInScreen(rect)
            val isAlmostFullScreen = rect.height() >= screenHeight * 0.9 && rect.width() >= screenWidth * 0.9
            val isSelected = node.isSelected || 
                    (desc != null && desc.contains("selected", ignoreCase = true)) || 
                    (text != null && text.contains("selected", ignoreCase = true))

            var matched = false
            for (keyword in keywords) {
                if ((text != null && text.contains(keyword, ignoreCase = true)) || 
                    (desc != null && desc.contains(keyword, ignoreCase = true)) ||
                    (viewId != null && viewId.contains(keyword, ignoreCase = true))) {
                    
                    if (isSelected || isAlmostFullScreen) {
                        Log.d("ReelsBlocker", "Matched blocker keyword: '$keyword' on node: text=$text desc=$desc viewId=$viewId, selected=$isSelected, fullScreen=$isAlmostFullScreen")
                        matched = true
                        break
                    }
                }
            }

            if (matched) {
                // Recycle remaining nodes in stack
                for (n in stack) {
                    if (n != rootNode) recycleNode(n)
                }
                return true
            }

            // Add children to stack
            val childCount = node.childCount
            for (i in 0 until childCount) {
                val child = try { node.getChild(i) } catch (e: Exception) { null }
                if (child != null) {
                    stack.add(child)
                }
            }
            
            // Recycle node if it's not the root (root is recycled in evaluateReelNodes)
            if (node != rootNode) {
                recycleNode(node)
            }
        }
        return false
    }



    private fun blockAndOverlay(platformName: String) {
        val now = System.currentTimeMillis()
        // Cooldown: 6s (3s hold + 3s buffer) to prevent rapid re-triggering
        if (now - lastBlockTime < 6000) return
        lastBlockTime = now

        Log.d("ReelsBlocker", "LAUNCHING BLOCKER OVERLAY FOR PLATFORM: $platformName")

        // Mark overlay as active BEFORE launching to suppress concurrent re-detection
        isOverlayActive = true

        serviceScope.launch {
            performGlobalAction(GLOBAL_ACTION_BACK)
            
            // Wait briefly to allow the back action to execute before our overlay takes focus
            kotlinx.coroutines.delay(300L)
            
            val intent = Intent(this@ReelsBlockerAccessibilityService, OverlayBlockerActivity::class.java).apply {
                putExtra("platform_name", platformName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)

            // Clear the flag after a safe window (3s hold + 3s buffer = 6s)
            kotlinx.coroutines.delay(6000L)
            isOverlayActive = false
            Log.d("ReelsBlocker", "Overlay guard lifted, detection resumed")
        }
    }

    private fun isCurrentTimeInSchedule(start: String, end: String): Boolean {
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val currentStr = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
            val currentTime = format.parse(currentStr) ?: return true
            val startTime = format.parse(start) ?: return true
            val endTime = format.parse(end) ?: return true

            if (startTime.before(endTime)) {
                currentTime in startTime..endTime
            } else {
                // Overnight schedule (e.g. 22:00 to 06:00)
                currentTime.after(startTime) || currentTime.before(endTime)
            }
        } catch (e: Exception) {
            Log.e("ReelsBlocker", "Error parsing times: start=$start, end=$end", e)
            true
        }
    }

    override fun onInterrupt() {
        Log.d("ReelsBlocker", "Accessibility service interrupted")
    }
}
