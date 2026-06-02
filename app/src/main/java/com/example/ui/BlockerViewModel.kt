package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BlockEvent
import com.example.data.model.UserSettings
import com.example.data.repository.BlockerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlockerViewModel(private val repository: BlockerRepository) : ViewModel() {

    // Observe settings and map null value to active state
    val userSettings: StateFlow<UserSettings> = repository.userSettings
        .map { it ?: UserSettings() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    val blockEvents: StateFlow<List<BlockEvent>> = repository.blockEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun signIn(name: String, email: String, avatarUrl: String) {
        viewModelScope.launch {
            repository.loginUser(name, email, avatarUrl)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.logoutUser()
        }
    }

    fun setBlockerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            repository.updateSettings(current.copy(blockerEnabled = enabled))
        }
    }

    fun toggleTheme(currentTheme: String) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            val newTheme = if (currentTheme == "dark") "light" else "dark"
            repository.updateSettings(current.copy(theme = newTheme))
        }
    }

    fun setScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            repository.updateSettings(current.copy(scheduleEnabled = enabled))
        }
    }

    fun updateSchedule(start: String, end: String) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            repository.updateSettings(current.copy(scheduleStart = start, scheduleEnd = end))
        }
    }

    fun togglePlatformBlock(platform: String) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            val plat = platform.lowercase()
            val updated = when {
                plat == "instagram" -> current.copy(blockInstagram = !current.blockInstagram)
                plat == "facebook" -> current.copy(blockFacebook = !current.blockFacebook)
                else -> current
            }
            repository.updateSettings(updated)
        }
    }

    fun triggerSimulatedBlock(platform: String) {
        viewModelScope.launch {
            repository.addBlockEvent(platform)
        }
    }

}

class BlockerViewModelFactory(private val repository: BlockerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlockerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BlockerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
