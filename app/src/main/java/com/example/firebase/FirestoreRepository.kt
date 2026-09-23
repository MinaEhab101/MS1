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

    // Helper to build leaderboard dummy profiles
    private fun createLeaderboardProfile(
        id: String,
        name: String,
        avatarIdx: Int,
        overallRating: Int,
        lvl: Int,
        gold: Int,
        w: Int,
        l: Int,
        total: Int,
        cStreak: Int,
        bStreak: Int,
        chessR: Int,
        dominoR: Int,
        backgammonR: Int
    ): UserProfile = UserProfile(
        userId = id,
        username = name,
        email = "",
        avatarIndex = avatarIdx,
        rating = overallRating,
        chessRating = chessR,
        dominoRating = dominoR,
        backgammonRating = backgammonR,
        level = lvl,
        coins = gold,
        wins = w,
        losses = l,
        draws = 0,
        totalGames = total,
        currentStreak = cStreak,
        bestStreak = bStreak
    )

    // Default global leaderboards for initial launch / offline
    private val defaultBackgammonLeaderboard = listOf(
        createLeaderboardProfile("bg_1", "Sultan_Tawla", 1, 2350, 48, 50000, 310, 45, 355, 12, 28, 1750, 1620, 2350),
        createLeaderboardProfile("bg_2", "Royal_King", 2, 2210, 42, 42000, 260, 52, 312, 8, 22, 1820, 1710, 2210),
        createLeaderboardProfile("bg_3", "GrandDice_99", 0, 2140, 39, 38000, 240, 60, 300, 5, 19, 1600, 1890, 2140),
        createLeaderboardProfile("bg_4", "Cairo_King", 3, 2080, 36, 31000, 210, 55, 265, 7, 16, 1550, 1950, 2080),
        createLeaderboardProfile("bg_5", "Alexandria_Ace", 4, 1990, 33, 27000, 190, 62, 252, 4, 14, 1690, 1800, 1990),
        createLeaderboardProfile("bg_6", "Sheik_SheshBesh", 1, 1920, 30, 23000, 175, 70, 245, 3, 12, 1420, 1750, 1920),
        createLeaderboardProfile("bg_7", "Desert_Falcon", 2, 1850, 27, 19000, 150, 65, 215, 6, 11, 1510, 1680, 1850),
        createLeaderboardProfile("bg_8", "Nile_Strategist", 0, 1790, 24, 16000, 135, 60, 195, 2, 9, 1480, 1600, 1790),
        createLeaderboardProfile("bg_9", "Pyramid_Roller", 3, 1720, 21, 14000, 120, 58, 178, 4, 8, 1390, 1530, 1720),
        createLeaderboardProfile("bg_10", "Lucky_Doubles", 4, 1650, 18, 11000, 105, 50, 155, 1, 7, 1300, 1480, 1650)
    )

    private val defaultChessLeaderboard = listOf(
        createLeaderboardProfile("ch_1", "Grandmaster_Fischer", 2, 2680, 56, 75000, 480, 32, 512, 18, 42, 2680, 1420, 1500),
        createLeaderboardProfile("ch_2", "Kasparov_Tactics", 0, 2590, 52, 68000, 420, 41, 461, 15, 36, 2590, 1500, 1620),
        createLeaderboardProfile("ch_3", "Queen_Gambit_SA", 1, 2510, 49, 61000, 395, 48, 443, 14, 31, 2510, 1610, 1740),
        createLeaderboardProfile("ch_4", "Knight_Rider_EG", 4, 2440, 46, 54000, 360, 50, 410, 10, 26, 2440, 1700, 1810),
        createLeaderboardProfile("ch_5", "Carlsen_Vibes", 3, 2380, 43, 49000, 330, 55, 385, 9, 23, 2380, 1550, 1890),
        createLeaderboardProfile("ch_6", "Checkmate_Pro", 2, 2310, 40, 44000, 305, 62, 367, 8, 20, 2310, 1620, 1700),
        createLeaderboardProfile("ch_7", "Rook_Master", 1, 2240, 37, 39000, 280, 68, 348, 7, 18, 2240, 1750, 1650),
        createLeaderboardProfile("ch_8", "Bishop_Sniper", 0, 2180, 34, 34000, 255, 71, 326, 6, 15, 2180, 1680, 1580),
        createLeaderboardProfile("ch_9", "Castle_Defender", 4, 2110, 31, 30000, 230, 75, 305, 5, 13, 2110, 1590, 1520),
        createLeaderboardProfile("ch_10", "Pawn_Storm", 3, 2050, 28, 26000, 210, 80, 290, 4, 11, 2050, 1510, 1450)
    )

    private val defaultDominoLeaderboard = listOf(
        createLeaderboardProfile("dm_1", "Domino_Emperor", 4, 2420, 50, 55000, 380, 38, 418, 16, 32, 1650, 2420, 1720),
        createLeaderboardProfile("dm_2", "Double_Six_King", 1, 2340, 47, 48000, 345, 42, 387, 13, 27, 1720, 2340, 1800),
        createLeaderboardProfile("dm_3", "Bishr_Master", 3, 2270, 44, 43000, 315, 49, 364, 11, 24, 1580, 2270, 1910),
        createLeaderboardProfile("dm_4", "Ivory_Tactician", 0, 2200, 41, 37000, 285, 53, 338, 9, 21, 1690, 2200, 1840),
        createLeaderboardProfile("dm_5", "Block_King_99", 2, 2130, 38, 33000, 260, 58, 318, 8, 19, 1490, 2130, 1770),
        createLeaderboardProfile("dm_6", "Matador_Pro", 4, 2060, 35, 29000, 235, 62, 297, 6, 16, 1530, 2060, 1700),
        createLeaderboardProfile("dm_7", "Capicua_Champion", 1, 1990, 32, 25000, 210, 66, 276, 5, 14, 1450, 1990, 1630),
        createLeaderboardProfile("dm_8", "Domino_Sheikh", 3, 1920, 29, 21000, 190, 70, 260, 4, 12, 1380, 1920, 1590),
        createLeaderboardProfile("dm_9", "Tile_Crusher", 0, 1850, 26, 18000, 170, 73, 243, 3, 10, 1410, 1850, 1530),
        createLeaderboardProfile("dm_10", "Pip_Counter", 2, 1780, 23, 15000, 150, 76, 226, 2, 8, 1340, 1780, 1480)
    )

    suspend fun syncUserProfile(profile: UserProfile) {
        try {
            db?.collection("users")?.document(profile.userId)?.set(profile.toMap())?.await()
        } catch (_: Exception) {}
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        if (userId.isBlank()) return null
        return try {
            val doc = db?.collection("users")?.document(userId)?.get()?.await()
            if (doc != null && doc.exists()) {
                doc.data?.let { UserProfile.fromMap(it) }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateUserCoins(userId: String, newCoins: Int) {
        if (userId.isEmpty()) return
        try {
            db?.collection("users")?.document(userId)?.update("coins", newCoins)?.await()
        } catch (_: Exception) {}
    }

    suspend fun getTopLeaderboard(gameType: String = "backgammon"): List<UserProfile> {
        val defaultList = when (gameType.lowercase()) {
            "chess" -> defaultChessLeaderboard
            "domino", "dominoes" -> defaultDominoLeaderboard
            else -> defaultBackgammonLeaderboard
        }

        val ratingField = when (gameType.lowercase()) {
            "chess" -> "chessRating"
            "domino", "dominoes" -> "dominoRating"
            else -> "backgammonRating"
        }

        return try {
            val firestore = db
            if (firestore != null) {
                val snapshot = firestore.collection("users")
                    .orderBy(ratingField, Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()
                if (!snapshot.isEmpty) {
                    snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { UserProfile.fromMap(it) }
                    }
                } else {
                    defaultList
                }
            } else {
                defaultList
            }
        } catch (_: Exception) {
            defaultList
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
