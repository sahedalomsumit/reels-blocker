package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1, // Singleton settings
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val isLoggedIn: Boolean = false,
    val theme: String = "dark",
    val blockerEnabled: Boolean = true,
    val scheduleEnabled: Boolean = false,
    val scheduleStart: String = "09:00",
    val scheduleEnd: String = "22:00",
    // Platform block toggles
    val blockInstagram: Boolean = true,
    val blockInstagramInbox: Boolean = false,
    val blockFacebook: Boolean = true,
    val blockFacebookInbox: Boolean = false,
    // Aggregated cache stats
    val totalBlocked: Int = 0,
    val totalTimeSavedMinutes: Int = 0,
    val loginTime: Long = 0L
)
