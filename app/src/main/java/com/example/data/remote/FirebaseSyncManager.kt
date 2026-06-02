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
        val user = auth.currentUser ?: return
        try {
            db.collection("users")
                .document(user.uid)
                .collection("settings")
                .document("user_settings")
                .set(settings)
                .await()
            Log.d("FirebaseSync", "Synced UserSettings to cloud.")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync UserSettings", e)
        }
    }

    suspend fun fetchUserSettings(): UserSettings? {
        val user = auth.currentUser ?: return null
        return try {
            val snapshot = db.collection("users")
                .document(user.uid)
                .collection("settings")
                .document("user_settings")
                .get()
                .await()
            snapshot.toObject(UserSettings::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to fetch UserSettings", e)
            null
        }
    }

    suspend fun syncBlockEvent(event: BlockEvent) {
        val user = auth.currentUser ?: return
        try {
            // Document ID can be string timestamp
            db.collection("users")
                .document(user.uid)
                .collection("block_events")
                .document(event.timestamp.toString())
                .set(event)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync BlockEvent", e)
        }
    }

    suspend fun fetchAllBlockEvents(): List<BlockEvent> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            val snapshot = db.collection("users")
                .document(user.uid)
                .collection("block_events")
                .get()
                .await()
            snapshot.toObjects(BlockEvent::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to fetch BlockEvents", e)
            emptyList()
        }
    }
}
