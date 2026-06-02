package com.example.data.remote

import android.util.Log
import com.example.data.model.BlockEvent
import com.example.data.model.UserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun syncUserSettings(settings: UserSettings) {
        val user = auth.currentUser
        if (user == null) {
            Log.w("FirebaseSync", "Cannot sync UserSettings: No user signed in.")
            return
        }
        try {
            db.collection("users")
                .document(user.uid)
                .collection("settings")
                .document("user_settings")
                .set(settings)
                .await()
            Log.d("FirebaseSync", "Successfully synced UserSettings for user ${user.uid}")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error syncing UserSettings for ${user.uid}", e)
        }
    }

    suspend fun fetchUserSettings(): UserSettings? {
        val user = auth.currentUser
        if (user == null) {
            Log.w("FirebaseSync", "Cannot fetch UserSettings: No user signed in.")
            return null
        }
        return try {
            val snapshot = db.collection("users")
                .document(user.uid)
                .collection("settings")
                .document("user_settings")
                .get()
                .await()
            
            if (snapshot.exists()) {
                val settings = snapshot.toObject(UserSettings::class.java)
                Log.d("FirebaseSync", "Fetched UserSettings for ${user.uid}")
                settings
            } else {
                Log.d("FirebaseSync", "No cloud UserSettings found for ${user.uid}")
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error fetching UserSettings for ${user.uid}", e)
            null
        }
    }

    suspend fun syncBlockEvent(event: BlockEvent) {
        val user = auth.currentUser
        if (user == null) {
            Log.w("FirebaseSync", "Cannot sync BlockEvent: No user signed in.")
            return
        }
        try {
            val timestamp = event.timestamp
            db.collection("users")
                .document(user.uid)
                .collection("block_events")
                .document(timestamp.toString())
                .set(event)
                .await()
            Log.d("FirebaseSync", "Synced BlockEvent ($timestamp) for user ${user.uid}")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error syncing BlockEvent for ${user.uid}", e)
        }
    }

    suspend fun fetchAllBlockEvents(): List<BlockEvent> {
        val user = auth.currentUser
        if (user == null) {
            Log.w("FirebaseSync", "Cannot fetch BlockEvents: No user signed in.")
            return emptyList()
        }
        return try {
            val snapshot = db.collection("users")
                .document(user.uid)
                .collection("block_events")
                .get()
                .await()
            val events = snapshot.toObjects(BlockEvent::class.java)
            Log.d("FirebaseSync", "Fetched ${events.size} BlockEvents for ${user.uid}")
            events
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error fetching BlockEvents for ${user.uid}", e)
            emptyList()
        }
    }
}
