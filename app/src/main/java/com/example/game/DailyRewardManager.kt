package com.example.game

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class DailyRewardTier(
    val day: Int,
    val coins: Int,
    val title: String,
    val description: String,
    val isSpecial: Boolean = false
)

object DailyRewardManager {
    private const val PREFS_NAME = "backgammon_king_daily_rewards"
    private const val KEY_LAST_CLAIM_DAY = "last_claim_day"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_TOTAL_CLAIMS = "total_claims"

    val rewardTiers = listOf(
        DailyRewardTier(1, 500, "Day 1", "Beginner's Blessing"),
        DailyRewardTier(2, 800, "Day 2", "Royal Favor"),
        DailyRewardTier(3, 1200, "Day 3", "Knight's Fortune"),
        DailyRewardTier(4, 1800, "Day 4", "Crown Bounty"),
        DailyRewardTier(5, 2500, "Day 5", "Sultan's Treasure"),
        DailyRewardTier(6, 3500, "Day 6", "Grand Vault"),
        DailyRewardTier(7, 6000, "Day 7", "Royal King's Chest", isSpecial = true)
    )

    private val _currentStreak = MutableStateFlow(1)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _canClaimToday = MutableStateFlow(true)
    val canClaimToday: StateFlow<Boolean> = _canClaimToday.asStateFlow()

    fun checkStatus(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastClaimDay = prefs.getInt(KEY_LAST_CLAIM_DAY, -1)
        val streak = prefs.getInt(KEY_CURRENT_STREAK, 1)

        val today = getDayOfYear()
        val year = getYear()
        val todayKey = year * 1000 + today

        if (lastClaimDay == todayKey) {
            // Already claimed today
            _canClaimToday.value = false
            _currentStreak.value = streak
        } else if (lastClaimDay == todayKey - 1 || (today == 1 && lastClaimDay > 0)) {
            // Consecutive day
            _canClaimToday.value = true
            _currentStreak.value = streak
        } else if (lastClaimDay == -1) {
            // First time player
            _canClaimToday.value = true
            _currentStreak.value = 1
        } else {
            // Streak broken
            _canClaimToday.value = true
            _currentStreak.value = 1
        }
    }

    fun claimReward(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getDayOfYear()
        val year = getYear()
        val todayKey = year * 1000 + today

        val currentDay = _currentStreak.value
        val tier = rewardTiers.find { it.day == currentDay } ?: rewardTiers[0]
        val coinsWon = tier.coins

        // Advance streak (1..7 cycle)
        val nextStreak = if (currentDay >= 7) 1 else currentDay + 1
        val totalClaims = prefs.getInt(KEY_TOTAL_CLAIMS, 0) + 1

        prefs.edit()
            .putInt(KEY_LAST_CLAIM_DAY, todayKey)
            .putInt(KEY_CURRENT_STREAK, nextStreak)
            .putInt(KEY_TOTAL_CLAIMS, totalClaims)
            .apply()

        _canClaimToday.value = false
        _currentStreak.value = nextStreak
        return coinsWon
    }

    private fun getDayOfYear(): Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    private fun getYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
}
