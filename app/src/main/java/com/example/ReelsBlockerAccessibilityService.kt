package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher",
        "com.android.launcher3",
        "com.miui.home",
        "com.sec.android.app.launcher",
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val selfPackage = applicationContext.packageName
        if (packageName == selfPackage || packageName in ignoredPackages) return

        val eventType = event.eventType

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val currentTime = System.currentTimeMillis()
            // Throttle checks slightly to prevent performance hits
            if (currentTime - lastCheckTime < 250) return
            lastCheckTime = currentTime

            serviceScope.launch {
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
                        evaluateReelNodes(event, "Instagram", listOf(
                            "reel", "Reels", "reels_tab", "clip", "Clips", "Suggested Reels", "reels_view"
                        ), settings.blockInstagramInbox)
                    }
                    packageName.contains("facebook") && settings.blockFacebook -> {
                        evaluateReelNodes(event, "Facebook", listOf(
                            "watch", "Reels", "reel", "Short videos", "facebook_reels"
                        ), settings.blockFacebookInbox)
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

    private fun evaluateReelNodes(event: AccessibilityEvent, platform: String, keywords: List<String>, isInboxBlocked: Boolean) {
        val rootNode = rootInActiveWindow ?: return
        try {
            val rootRect = android.graphics.Rect()
            rootNode.getBoundsInScreen(rootRect)
            val screenHeight = rootRect.height()
            val screenWidth = rootRect.width()

            if (checkNodeKeywordsRecursive(rootNode, keywords, screenHeight, screenWidth)) {
                val inInbox = isInboxContext(rootNode)
                if (inInbox && !isInboxBlocked) {
                    Log.d("ReelsBlocker", "Inbox reel detected and inbox blocking is off. Allowing.")
                    return
                }
                blockAndOverlay(platform)
            }
        } catch (e: Exception) {
            Log.e("ReelsBlocker", "Error evaluating reel nodes", e)
        } finally {
            try {
                rootNode.recycle()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun checkNodeKeywordsRecursive(node: AccessibilityNodeInfo?, keywords: List<String>, screenHeight: Int, screenWidth: Int): Boolean {
        if (node == null) return false

        // Check text content or content descriptions
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""

        val isSelected = node.isSelected || desc.contains("selected", ignoreCase = true) || text.contains("selected", ignoreCase = true)
        
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        val isAlmostFullScreen = rect.height() >= screenHeight * 0.7 && rect.width() >= screenWidth * 0.8

        for (keyword in keywords) {
            if (text.contains(keyword, ignoreCase = true) || 
                desc.contains(keyword, ignoreCase = true) ||
                viewId.contains(keyword, ignoreCase = true)) {
                
                if (isSelected || isAlmostFullScreen) {
                    Log.d("ReelsBlocker", "Matched blocker keyword: '$keyword' on node: text=$text desc=$desc viewId=$viewId, selected=$isSelected, fullScreen=$isAlmostFullScreen")
                    return true
                }
            }
        }

        // Recursively search child nodes
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (e: Exception) {
                null
            }
            if (child != null) {
                val found = checkNodeKeywordsRecursive(child, keywords, screenHeight, screenWidth)
                try {
                    child.recycle()
                } catch (e: Exception) {
                    // Ignore
                }
                if (found) {
                    return true
                }
            }
        }
        return false
    }

    private fun isInboxContext(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (text.contains("message") || text.contains("direct") || text.contains("chat") ||
            desc.contains("message") || desc.contains("direct") || desc.contains("chat") ||
            viewId.contains("message") || viewId.contains("direct") || viewId.contains("chat") || viewId.contains("thread")) {
            return true
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (e: Exception) {
                null
            }
            if (child != null) {
                val found = isInboxContext(child)
                try {
                    child.recycle()
                } catch (e: Exception) {
                    // Ignore
                }
                if (found) {
                    return true
                }
            }
        }
        return false
    }

    private fun blockAndOverlay(platformName: String) {
        val now = System.currentTimeMillis()
        if (now - lastBlockTime < 2000) return
        lastBlockTime = now

        Log.d("ReelsBlocker", "LAUNCHING BLOCKER OVERLAY FOR PLATFORM: $platformName")

        performGlobalAction(GLOBAL_ACTION_BACK)
        
        // Option B: Hard Block overlay launch
        val intent = Intent(this, OverlayBlockerActivity::class.java).apply {
            putExtra("platform_name", platformName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
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
