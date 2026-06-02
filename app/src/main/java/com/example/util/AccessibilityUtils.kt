package com.example.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.ReelsBlockerAccessibilityService

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return isAccessibilityServiceEnabledViaSettings(context)

    val serviceClass = ReelsBlockerAccessibilityService::class.java
    val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    )
    val enabledViaManager = enabledServices.any { info ->
        val serviceInfo = info.resolveInfo?.serviceInfo ?: return@any false
        serviceInfo.packageName == context.packageName &&
            (serviceInfo.name == serviceClass.name ||
                serviceInfo.name == serviceClass.canonicalName)
    }
    if (enabledViaManager) return true

    return isAccessibilityServiceEnabledViaSettings(context)
}

private fun isAccessibilityServiceEnabledViaSettings(context: Context): Boolean {
    val serviceClass = ReelsBlockerAccessibilityService::class.java
    val candidates = listOf(
        "${context.packageName}/${serviceClass.name}",
        "${context.packageName}/${serviceClass.canonicalName}",
    )
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return candidates.any { candidate ->
        enabledServicesSetting.contains(candidate, ignoreCase = true)
    }
}
