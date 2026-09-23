package com.example.firebase

import android.content.Context
import com.example.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AuthRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("mina_backgammon_auth", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    init {
        loadPersistedUser()
    }

    private fun loadPersistedUser() {
        val savedUserId = prefs.getString("user_id", null)
        if (savedUserId != null) {
            val username = prefs.getString("username", "GrandMaster") ?: "GrandMaster"
            val email = prefs.getString("email", "") ?: ""
            val avatarIndex = prefs.getInt("avatar_index", 0)
            val rating = prefs.getInt("rating", 1200)
            val level = prefs.getInt("level", 1)
            val coins = prefs.getInt("coins", 1000)
            val wins = prefs.getInt("wins", 0)
            val losses = prefs.getInt("losses", 0)
            val totalGames = prefs.getInt("total_games", 0)
            val currentStreak = prefs.getInt("current_streak", 0)
            val bestStreak = prefs.getInt("best_streak", 0)

            _currentUser.value = UserProfile(
                userId = savedUserId,
                username = username,
                email = email,
                avatarIndex = avatarIndex,
                rating = rating,
                level = level,
                coins = coins,
                wins = wins,
                losses = losses,
                totalGames = totalGames,
                currentStreak = currentStreak,
                bestStreak = bestStreak
            )
        } else {
            // Check Firebase Auth
            val fbUser = try { firebaseAuth?.currentUser } catch (_: Exception) { null }
            if (fbUser != null) {
                saveUser(
                    UserProfile(
                        userId = fbUser.uid,
                        username = fbUser.displayName ?: "Player",
                        email = fbUser.email ?: "",
                        avatarIndex = 0
                    )
                )
            }
        }
    }

    fun loginAsGuest(customName: String? = null, avatarIndex: Int = 0): UserProfile {
        val randomDigits = (1000..9999).random()
        val defaultName = customName?.takeIf { it.isNotBlank() } ?: "Player_$randomDigits"
        val userId = "guest_" + UUID.randomUUID().toString().take(8)

        val profile = UserProfile(
            userId = userId,
            username = defaultName,
            email = "$userId@minabackgammon.local",
            avatarIndex = avatarIndex,
            rating = 1200,
            level = 1,
            coins = 1000
        )
        saveUser(profile)
        return profile
    }

    fun signInWithGoogleCredentials(displayName: String, email: String, googleId: String): UserProfile {
        val current = _currentUser.value
        val profile = (current ?: UserProfile()).copy(
            userId = googleId,
            username = displayName.ifBlank { "Player" },
            email = email
        )
        saveUser(profile)
        return profile
    }

    fun saveUser(profile: UserProfile) {
        _currentUser.value = profile
        prefs.edit()
            .putString("user_id", profile.userId)
            .putString("username", profile.username)
            .putString("email", profile.email)
            .putInt("avatar_index", profile.avatarIndex)
            .putInt("rating", profile.rating)
            .putInt("level", profile.level)
            .putInt("coins", profile.coins)
            .putInt("wins", profile.wins)
            .putInt("losses", profile.losses)
            .putInt("total_games", profile.totalGames)
            .putInt("current_streak", profile.currentStreak)
            .putInt("best_streak", profile.bestStreak)
            .apply()
    }

    fun updateStats(won: Boolean, ratingChange: Int) {
        val curr = _currentUser.value ?: return
        val newWins = if (won) curr.wins + 1 else curr.wins
        val newLosses = if (!won) curr.losses + 1 else curr.losses
        val newTotal = curr.totalGames + 1
        val newRating = (curr.rating + ratingChange).coerceAtLeast(400)
        val newStreak = if (won) curr.currentStreak + 1 else 0
        val newBestStreak = maxOf(curr.bestStreak, newStreak)
        val newLevel = 1 + (newWins / 5)
        val coinsEarned = if (won) 150 else 25

        val updated = curr.copy(
            wins = newWins,
            losses = newLosses,
            totalGames = newTotal,
            rating = newRating,
            currentStreak = newStreak,
            bestStreak = newBestStreak,
            level = newLevel,
            coins = curr.coins + coinsEarned
        )
        saveUser(updated)
    }

    fun addCoins(amount: Int) {
        val curr = _currentUser.value ?: return
        val updated = curr.copy(coins = curr.coins + amount)
        saveUser(updated)
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        prefs.edit().clear().apply()
        _currentUser.value = null
    }
}
