package com.example.data

data class UserProfile(
    val userId: String = "",
    val username: String = "Player",
    val email: String = "",
    val avatarIndex: Int = 0,
    val authProvider: String = "guest", // "guest", "email", "google"
    val rating: Int = 1200,
    val chessRating: Int = 1200,
    val dominoRating: Int = 1200,
    val backgammonRating: Int = 1200,
    val level: Int = 1,
    val xp: Int = 250,
    val coins: Int = 1000,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val totalGames: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
) {
    val winRate: Int
        get() = if (totalGames > 0) ((wins * 100.0) / totalGames).toInt() else 0

    val isGuest: Boolean
        get() = authProvider == "guest" || userId.startsWith("guest_")

    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "username" to username,
        "email" to email,
        "avatarIndex" to avatarIndex,
        "authProvider" to authProvider,
        "rating" to rating,
        "chessRating" to chessRating,
        "dominoRating" to dominoRating,
        "backgammonRating" to backgammonRating,
        "level" to level,
        "xp" to xp,
        "coins" to coins,
        "wins" to wins,
        "losses" to losses,
        "draws" to draws,
        "totalGames" to totalGames,
        "currentStreak" to currentStreak,
        "bestStreak" to bestStreak
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): UserProfile {
            val baseRating = ((map["rating"] as? Number)?.toInt()) ?: 1200
            return UserProfile(
                userId = (map["userId"] as? String) ?: "",
                username = (map["username"] as? String) ?: "Player",
                email = (map["email"] as? String) ?: "",
                avatarIndex = ((map["avatarIndex"] as? Number)?.toInt()) ?: 0,
                authProvider = (map["authProvider"] as? String) ?: "guest",
                rating = baseRating,
                chessRating = ((map["chessRating"] as? Number)?.toInt()) ?: baseRating,
                dominoRating = ((map["dominoRating"] as? Number)?.toInt()) ?: baseRating,
                backgammonRating = ((map["backgammonRating"] as? Number)?.toInt()) ?: baseRating,
                level = ((map["level"] as? Number)?.toInt()) ?: 1,
                xp = ((map["xp"] as? Number)?.toInt()) ?: 250,
                coins = ((map["coins"] as? Number)?.toInt()) ?: 1000,
                wins = ((map["wins"] as? Number)?.toInt()) ?: 0,
                losses = ((map["losses"] as? Number)?.toInt()) ?: 0,
                draws = ((map["draws"] as? Number)?.toInt()) ?: 0,
                totalGames = ((map["totalGames"] as? Number)?.toInt()) ?: 0,
                currentStreak = ((map["currentStreak"] as? Number)?.toInt()) ?: 0,
                bestStreak = ((map["bestStreak"] as? Number)?.toInt()) ?: 0
            )
        }
    }
}
