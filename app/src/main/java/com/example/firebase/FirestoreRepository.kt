package com.example.firebase

import android.content.Context
import com.example.data.Friend
import com.example.data.OnlineMatch
import com.example.data.PrivateRoom
import com.example.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class FirestoreRepository(private val context: Context) {

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    // Default global leaderboard data for initial launch / offline
    private val defaultLeaderboard = listOf(
        UserProfile("top_1", "Sultan_Tawla", "", 1, 2350, 48, 50000, 310, 45, 355, 12, 28),
        UserProfile("top_2", "Royal_King", "", 2, 2210, 42, 42000, 260, 52, 312, 8, 22),
        UserProfile("top_3", "GrandDice_99", "", 0, 2140, 39, 38000, 240, 60, 300, 5, 19),
        UserProfile("top_4", "Cairo_King", "", 3, 2080, 36, 31000, 210, 55, 265, 7, 16),
        UserProfile("top_5", "Alexandria_Ace", "", 4, 1990, 33, 27000, 190, 62, 252, 4, 14),
        UserProfile("top_6", "Sheik_SheshBesh", "", 1, 1920, 30, 23000, 175, 70, 245, 3, 12),
        UserProfile("top_7", "Desert_Falcon", "", 2, 1850, 27, 19000, 150, 65, 215, 6, 11),
        UserProfile("top_8", "Nile_Strategist", "", 0, 1790, 24, 16000, 135, 60, 195, 2, 9),
        UserProfile("top_9", "Pyramid_Roller", "", 3, 1720, 21, 14000, 120, 58, 178, 4, 8),
        UserProfile("top_10", "Lucky_Doubles", "", 4, 1650, 18, 11000, 105, 50, 155, 1, 7)
    )

    suspend fun syncUserProfile(profile: UserProfile) {
        try {
            db?.collection("users")?.document(profile.userId)?.set(profile.toMap())?.await()
        } catch (_: Exception) {}
    }

    suspend fun updateUserCoins(userId: String, newCoins: Int) {
        if (userId.isEmpty()) return
        try {
            db?.collection("users")?.document(userId)?.update("coins", newCoins)?.await()
        } catch (_: Exception) {}
    }

    suspend fun getTopLeaderboard(): List<UserProfile> {
        return try {
            val firestore = db
            if (firestore != null) {
                val snapshot = firestore.collection("users")
                    .orderBy("rating", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()
                if (!snapshot.isEmpty) {
                    snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { UserProfile.fromMap(it) }
                    }
                } else {
                    defaultLeaderboard
                }
            } else {
                defaultLeaderboard
            }
        } catch (_: Exception) {
            defaultLeaderboard
        }
    }

    // --- Private Rooms ---
    suspend fun createPrivateRoom(host: UserProfile): String {
        val code = generateRoomCode()
        val room = PrivateRoom(
            roomCode = code,
            hostId = host.userId,
            hostName = host.username,
            status = "WAITING"
        )
        try {
            db?.collection("rooms")?.document(code)?.set(room)?.await()
        } catch (_: Exception) {}
        return code
    }

    suspend fun joinPrivateRoom(code: String, guest: UserProfile): Boolean {
        return try {
            val firestore = db
            if (firestore != null) {
                val doc = firestore.collection("rooms").document(code).get().await()
                if (doc.exists()) {
                    firestore.collection("rooms").document(code).update(
                        mapOf(
                            "guestId" to guest.userId,
                            "guestName" to guest.username,
                            "status" to "READY"
                        )
                    ).await()
                    true
                } else {
                    false
                }
            } else {
                true // Allow local room testing
            }
        } catch (_: Exception) {
            true
        }
    }

    fun observeRoom(code: String): Flow<PrivateRoom?> = callbackFlow {
        val firestore = db
        var registration: ListenerRegistration? = null
        if (firestore != null) {
            registration = firestore.collection("rooms").document(code)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val room = snapshot.toObject(PrivateRoom::class.java)
                        trySend(room)
                    }
                }
        } else {
            // Simulated room flow for offline testing
            trySend(PrivateRoom(roomCode = code, hostId = "host", hostName = "Host", status = "WAITING"))
        }
        awaitClose { registration?.remove() }
    }

    // --- Matchmaking & Online Match ---
    suspend fun requestMatchmaking(player: UserProfile): String {
        return try {
            val firestore = db
            if (firestore != null) {
                val queue = firestore.collection("matchmaking_queue").limit(1).get().await()
                if (!queue.isEmpty && queue.documents.first().id != player.userId) {
                    val opponentDoc = queue.documents.first()
                    val opponentId = opponentDoc.id
                    val opponentName = opponentDoc.getString("username") ?: "Opponent"

                    // Remove from queue
                    firestore.collection("matchmaking_queue").document(opponentId).delete().await()

                    // Create Match
                    val matchId = "match_${System.currentTimeMillis()}"
                    val match = OnlineMatch(
                        matchId = matchId,
                        hostId = opponentId,
                        hostName = opponentName,
                        guestId = player.userId,
                        guestName = player.username,
                        status = "PLAYING"
                    )
                    firestore.collection("matches").document(matchId).set(match).await()
                    matchId
                } else {
                    // Put in queue
                    firestore.collection("matchmaking_queue").document(player.userId).set(
                        mapOf(
                            "userId" to player.userId,
                            "username" to player.username,
                            "timestamp" to System.currentTimeMillis()
                        )
                    ).await()
                    "queued"
                }
            } else {
                "simulated_online_match"
            }
        } catch (_: Exception) {
            "simulated_online_match"
        }
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    // --- Friends List ---
    fun getInitialFriends(): List<Friend> = listOf(
        Friend("f1", "Karim_Tawla", 1840, true, 1),
        Friend("f2", "Omar_Backgammon", 1520, true, 2),
        Friend("f3", "Sara_Dice", 1690, false, 3),
        Friend("f4", "Nader_Alex", 1430, true, 4)
    )
}
