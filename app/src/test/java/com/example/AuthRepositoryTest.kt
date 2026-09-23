package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.firebase.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("mina_backgammon_auth", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        authRepository = AuthRepository(context)
    }

    @Test
    fun `loginAsGuest creates valid guest profile with bonus coins`() {
        val user = authRepository.loginAsGuest(customName = "GrandSultan", avatarIndex = 3)
        assertEquals("GrandSultan", user.username)
        assertEquals(3, user.avatarIndex)
        assertEquals(1000, user.coins)
        assertEquals("guest", user.authProvider)
        assertTrue(user.isGuest)
        assertTrue(user.userId.startsWith("guest_"))

        // StateFlow check
        val current = authRepository.currentUser.value
        assertNotNull(current)
        assertEquals(user.userId, current?.userId)
    }

    @Test
    fun `registerWithEmail rejects empty or invalid email and short password`() = runTest {
        // Blank display name
        val blankNameResult = authRepository.registerWithEmail(
            email = "player@royalboard.com",
            password = "password123",
            displayName = ""
        )
        assertTrue(blankNameResult.isFailure)

        // Invalid email format
        val invalidEmailResult = authRepository.registerWithEmail(
            email = "notanemail",
            password = "password123",
            displayName = "Knight"
        )
        assertTrue(invalidEmailResult.isFailure)

        // Password too short (< 6 characters)
        val shortPasswordResult = authRepository.registerWithEmail(
            email = "knight@royalboard.com",
            password = "123",
            displayName = "Knight"
        )
        assertTrue(shortPasswordResult.isFailure)
    }

    @Test
    fun `registerWithEmail creates user with welcome bonus and email provider`() = runTest {
        val result = authRepository.registerWithEmail(
            email = "ehab@royalboard.com",
            password = "strongpassword123",
            displayName = "Ehab Mina",
            avatarIndex = 1
        )
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("Ehab Mina", user?.username)
        assertEquals("ehab@royalboard.com", user?.email)
        assertEquals("email", user?.authProvider)
        assertFalse(user!!.isGuest)
        assertEquals(1500, user.coins) // Welcome bonus
    }

    @Test
    fun `logout clears session and resets currentUser to null`() {
        authRepository.loginAsGuest("TestPlayer", 0)
        assertNotNull(authRepository.currentUser.value)

        authRepository.logout()
        assertNull(authRepository.currentUser.value)
        assertFalse(authRepository.isUserLoggedIn())
    }

    @Test
    fun `addCoins and updateStats correctly modify user profile`() {
        authRepository.loginAsGuest("Hero", 2)
        val initialCoins = authRepository.currentUser.value!!.coins

        authRepository.addCoins(500)
        assertEquals(initialCoins + 500, authRepository.currentUser.value?.coins)

        authRepository.updateStats(won = true, ratingChange = 25)
        val updated = authRepository.currentUser.value!!
        assertEquals(1, updated.wins)
        assertEquals(1, updated.totalGames)
        assertEquals(1, updated.currentStreak)
        assertEquals(1225, updated.rating)
    }
}
