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
import android.util.Log

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
        // Sync to cloud in background
        scope.launch { 
            syncManager.syncUserSettings(settings) 
        }
    }

    suspend fun addBlockEvent(platform: String) {
        // 1. Record block event locally and get the generated ID
        val event = BlockEvent(platform = platform)
        val generatedId = dao.insertBlockEvent(event)
        val eventWithId = event.copy(id = generatedId)
        
        // 2. Fetch current settings directly & increment
        val currentSettings = getSettingsDirect()
        val updatedSettings = currentSettings.copy(
            totalBlocked = currentSettings.totalBlocked + 1,
            totalTimeSavedMinutes = currentSettings.totalTimeSavedMinutes + 3
        )
        dao.insertUserSettings(updatedSettings)
        
        // 3. Sync both to cloud
        scope.launch {
            syncManager.syncBlockEvent(eventWithId)
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
            Log.d("BlockerRepository", "User logged in, starting cloud sync...")
            // Upon login, pull cloud data
            val cloudSettings = syncManager.fetchUserSettings()
            if (cloudSettings != null) {
                Log.d("BlockerRepository", "Found cloud settings, merging...")
                dao.insertUserSettings(cloudSettings.copy(isLoggedIn = true))
            } else {
                Log.d("BlockerRepository", "No cloud settings found, uploading local settings...")
                syncManager.syncUserSettings(updated)
            }
            
            val cloudEvents = syncManager.fetchAllBlockEvents()
            Log.d("BlockerRepository", "Fetched ${cloudEvents.size} cloud events.")
            cloudEvents.forEach { 
                // We use insertBlockEvent which will auto-generate new IDs locally 
                // to avoid conflicts, or we could preserve cloud IDs.
                // Since cloud ID in Firestore is the timestamp document name,
                // and BlockEvent.id is just for local Room, this is safe.
                dao.insertBlockEvent(it.copy(id = 0)) 
            }
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

}
