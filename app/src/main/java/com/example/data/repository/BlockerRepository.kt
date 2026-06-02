package com.example.data.repository

import com.example.data.local.BlockerDao
import com.example.data.model.BlockEvent
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.example.data.remote.FirebaseSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlockerRepository(private val dao: BlockerDao) {
    private val syncManager = FirebaseSyncManager()
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun ensureSettingsExist() {
        if (dao.getUserSettingsDirect() == null) {
            dao.insertUserSettings(UserSettings())
        }
    }

    val userSettings: Flow<UserSettings?> = dao.getUserSettings()
    val blockEvents: Flow<List<BlockEvent>> = dao.getAllBlockEvents()

    suspend fun getSettingsDirect(): UserSettings {
        return dao.getUserSettingsDirect() ?: createDefaultSettings()
    }

    private suspend fun createDefaultSettings(): UserSettings {
        val default = UserSettings()
        dao.insertUserSettings(default)
        return default
    }

    suspend fun updateSettings(settings: UserSettings) {
        dao.insertUserSettings(settings)
        scope.launch { syncManager.syncUserSettings(settings) }
    }

    suspend fun addBlockEvent(platform: String) {
        // Record block event
        dao.insertBlockEvent(BlockEvent(platform = platform))
        
        // Fetch current settings directly & increment
        val currentSettings = getSettingsDirect()
        val totalBlocked = currentSettings.totalBlocked + 1
        // Assuming each block saves about 3 minutes of doomscrolling
        val timeSaved = currentSettings.totalTimeSavedMinutes + 3
        
        val updatedSettings = currentSettings.copy(
            totalBlocked = totalBlocked,
            totalTimeSavedMinutes = timeSaved
        )
        dao.insertUserSettings(updatedSettings)
        
        scope.launch {
            syncManager.syncBlockEvent(BlockEvent(platform = platform, timestamp = System.currentTimeMillis()))
            syncManager.syncUserSettings(updatedSettings)
        }
    }

    suspend fun loginUser(name: String, email: String, profilePicUrl: String) {
        val current = dao.getUserSettingsDirect() ?: UserSettings()
        val updated = current.copy(
            displayName = name,
            email = email,
            photoUrl = profilePicUrl,
            isLoggedIn = true,
            blockerEnabled = true,
            loginTime = System.currentTimeMillis()
        )
        dao.insertUserSettings(updated)
        
        scope.launch {
            // Upon login, pull cloud data
            val cloudSettings = syncManager.fetchUserSettings()
            if (cloudSettings != null) {
                dao.insertUserSettings(cloudSettings.copy(isLoggedIn = true))
            } else {
                syncManager.syncUserSettings(updated)
            }
            
            val cloudEvents = syncManager.fetchAllBlockEvents()
            cloudEvents.forEach { dao.insertBlockEvent(it) }
        }
    }

    suspend fun logoutUser() {
        val current = dao.getUserSettingsDirect() ?: UserSettings()
        dao.insertUserSettings(
            current.copy(
                isLoggedIn = false,
                displayName = "",
                email = "",
                photoUrl = ""
            )
        )
    }

    suspend fun resetStats() {
        val current = dao.getUserSettingsDirect() ?: UserSettings()
        dao.clearAllBlockEvents()
        dao.insertUserSettings(
            current.copy(
                totalBlocked = 0,
                totalTimeSavedMinutes = 0
            )
        )
    }
}
