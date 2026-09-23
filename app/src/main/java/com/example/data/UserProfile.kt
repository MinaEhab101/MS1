package com.example.data

data class UserProfile(
    val userId: String = "",
    val username: String = "Player",
    val email: String = "",
    val avatarIndex: Int = 0,
    val rating: Int = 1200,
    val level: Int = 1,
    val coins: Int = 1000,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalGames: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
) {
    val winRate: Int
        get() = if (totalGames > 0) ((wins * 100.0) / totalGames).toInt() else 0

    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "username" to username,
        "email" to email,
        "avatarIndex" to avatarIndex,
        "rating" to rating,
        "level" to level,
        "coins" to coins,
        "wins" to wins,
        "losses" to losses,
        "totalGames" to totalGames,
        "currentStreak" to currentStreak,
        "bestStreak" to bestStreak
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): UserProfile {
            return UserProfile(
                userId = (map["userId"] as? String) ?: "",
                username = (map["username"] as? String) ?: "Player",
                email = (map["email"] as? String) ?: "",
                avatarIndex = ((map["avatarIndex"] as? Number)?.toInt()) ?: 0,
                rating = ((map["rating"] as? Number)?.toInt()) ?: 1200,
                level = ((map["level"] as? Number)?.toInt()) ?: 1,
                coins = ((map["coins"] as? Number)?.toInt()) ?: 1000,
                wins = ((map["wins"] as? Number)?.toInt()) ?: 0,
                losses = ((map["losses"] as? Number)?.toInt()) ?: 0,
                totalGames = ((map["totalGames"] as? Number)?.toInt()) ?: 0,
                currentStreak = ((map["currentStreak"] as? Number)?.toInt()) ?: 0,
                bestStreak = ((map["bestStreak"] as? Number)?.toInt()) ?: 0
            )
        }
    }
}
