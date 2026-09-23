package com.example.firebase

import android.content.Context
import android.util.Patterns
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Authentication repository managing Firebase Authentication for:
 * 1. Email & Password registration, sign in, and password recovery
 * 2. Modern Google Sign-In via Android Credential Manager & GoogleIdTokenCredential
 * 3. Guest play authentication with anonymous Firebase session fallback
 * 4. User profile local persistence and remote Firestore synchronization
 */
class AuthRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("mina_backgammon_auth", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private var firestoreRepository: FirestoreRepository? = null

    val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    init {
        loadPersistedUser()
        setupAuthStateListener()
    }

    fun setFirestoreRepository(repo: FirestoreRepository) {
        this.firestoreRepository = repo
        _currentUser.value?.let { profile ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repo.syncUserProfile(profile)
                } catch (_: Exception) {}
            }
        }
    }

    private fun setupAuthStateListener() {
        try {
            firebaseAuth?.addAuthStateListener { auth ->
                val fbUser = auth.currentUser
                if (fbUser == null && _currentUser.value != null && _currentUser.value?.authProvider != "guest") {
                    // Signed out of Firebase
                    // If user was logged in via email or google, clear session
                    prefs.edit().clear().apply()
                    _currentUser.value = null
                }
            }
        } catch (_: Exception) {}
    }

    private fun loadPersistedUser() {
        val savedUserId = prefs.getString("user_id", null)
        if (savedUserId != null) {
            val username = prefs.getString("username", "GrandMaster") ?: "GrandMaster"
            val email = prefs.getString("email", "") ?: ""
            val avatarIndex = prefs.getInt("avatar_index", 0)
            val authProvider = prefs.getString("auth_provider", if (savedUserId.startsWith("guest_")) "guest" else "email") ?: "guest"
            val rating = prefs.getInt("rating", 1200)
            val chessRating = prefs.getInt("chess_rating", rating)
            val dominoRating = prefs.getInt("domino_rating", rating)
            val backgammonRating = prefs.getInt("backgammon_rating", rating)
            val level = prefs.getInt("level", 1)
            val xp = prefs.getInt("xp", 250)
            val coins = prefs.getInt("coins", 1000)
            val wins = prefs.getInt("wins", 0)
            val losses = prefs.getInt("losses", 0)
            val draws = prefs.getInt("draws", 0)
            val totalGames = prefs.getInt("total_games", 0)
            val currentStreak = prefs.getInt("current_streak", 0)
            val bestStreak = prefs.getInt("best_streak", 0)

            _currentUser.value = UserProfile(
                userId = savedUserId,
                username = username,
                email = email,
                avatarIndex = avatarIndex,
                authProvider = authProvider,
                rating = rating,
                chessRating = chessRating,
                dominoRating = dominoRating,
                backgammonRating = backgammonRating,
                level = level,
                xp = xp,
                coins = coins,
                wins = wins,
                losses = losses,
                draws = draws,
                totalGames = totalGames,
                currentStreak = currentStreak,
                bestStreak = bestStreak
            )
        } else {
            // Check if already authenticated via Firebase
            val fbUser = try { firebaseAuth?.currentUser } catch (_: Exception) { null }
            if (fbUser != null) {
                val provider = when {
                    fbUser.isAnonymous -> "guest"
                    fbUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } -> "google"
                    else -> "email"
                }
                val profile = UserProfile(
                    userId = fbUser.uid,
                    username = fbUser.displayName?.ifBlank { "Player" } ?: "Player",
                    email = fbUser.email ?: "",
                    avatarIndex = 0,
                    authProvider = provider
                )
                saveUser(profile)
            }
        }
    }

    /**
     * Authenticate an existing account using Email & Password.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your email address."))
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address format."))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your password."))
        }

        return try {
            val auth = firebaseAuth
            if (auth != null) {
                val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
                val fbUser = authResult.user ?: throw Exception("Firebase user is null after authentication.")

                // Try to load user profile from Firestore if available
                val existingProfile = try {
                    firestoreRepository?.getUserProfile(fbUser.uid)
                } catch (_: Exception) {
                    null
                }

                val profile = existingProfile ?: UserProfile(
                    userId = fbUser.uid,
                    username = fbUser.displayName?.ifBlank { cleanEmail.substringBefore("@") } ?: cleanEmail.substringBefore("@"),
                    email = cleanEmail,
                    avatarIndex = 0,
                    authProvider = "email",
                    rating = 1200,
                    chessRating = 1200,
                    dominoRating = 1200,
                    backgammonRating = 1200,
                    level = 1,
                    xp = 250,
                    coins = 1200
                )

                saveUser(profile)
                try {
                    firestoreRepository?.syncUserProfile(profile)
                } catch (_: Exception) {}

                Result.success(profile)
            } else {
                // Local authentication fallback when Firebase is not active
                val existingId = prefs.getString("user_id", null)
                val profile = if (existingId != null && prefs.getString("email", "") == cleanEmail) {
                    _currentUser.value ?: UserProfile(
                        userId = existingId,
                        username = cleanEmail.substringBefore("@"),
                        email = cleanEmail,
                        authProvider = "email"
                    )
                } else {
                    UserProfile(
                        userId = "usr_" + UUID.randomUUID().toString().take(8),
                        username = cleanEmail.substringBefore("@"),
                        email = cleanEmail,
                        authProvider = "email",
                        coins = 1200
                    )
                }
                saveUser(profile)
                Result.success(profile)
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No account found registered with this email. Please check your spelling or register."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Incorrect password or email credentials. Please check and try again."))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Sign in failed. Please try again."))
        }
    }

    /**
     * Create a new account with Email, Password, Display Name, and chosen Avatar.
     */
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        avatarIndex: Int = 0
    ): Result<UserProfile> {
        val cleanEmail = email.trim()
        val cleanName = displayName.trim()

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please provide a display name for your Royal profile."))
        }
        if (cleanEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your email address."))
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address format."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters long."))
        }

        return try {
            val auth = firebaseAuth
            if (auth != null) {
                val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                val fbUser = authResult.user ?: throw Exception("Failed to create Firebase user.")

                // Set user display name in Firebase Auth
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanName)
                        .build()
                    fbUser.updateProfile(profileUpdates).await()
                } catch (_: Exception) {}

                val newProfile = UserProfile(
                    userId = fbUser.uid,
                    username = cleanName,
                    email = cleanEmail,
                    avatarIndex = avatarIndex,
                    authProvider = "email",
                    rating = 1200,
                    chessRating = 1200,
                    dominoRating = 1200,
                    backgammonRating = 1200,
                    level = 1,
                    xp = 350,
                    coins = 1500, // 1500 Welcome Bonus!
                    wins = 0,
                    losses = 0,
                    draws = 0,
                    totalGames = 0,
                    currentStreak = 0,
                    bestStreak = 0
                )

                saveUser(newProfile)
                try {
                    firestoreRepository?.syncUserProfile(newProfile)
                } catch (_: Exception) {}

                Result.success(newProfile)
            } else {
                // Local registration fallback
                val userId = "usr_" + UUID.randomUUID().toString().take(8)
                val newProfile = UserProfile(
                    userId = userId,
                    username = cleanName,
                    email = cleanEmail,
                    avatarIndex = avatarIndex,
                    authProvider = "email",
                    rating = 1200,
                    coins = 1500
                )
                saveUser(newProfile)
                Result.success(newProfile)
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("An account with this email already exists. Please sign in instead."))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("The chosen password is too weak. Please use at least 6 characters with a combination of letters and numbers."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("The email address provided is invalid. Please check the spelling."))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Registration failed. Please try again."))
        }
    }

    /**
     * Send password reset instructions to registered email.
     */
    suspend fun sendPasswordReset(email: String): Result<String> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your registered email address."))
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        return try {
            val auth = firebaseAuth
            if (auth != null) {
                auth.sendPasswordResetEmail(cleanEmail).await()
                Result.success("Password reset instructions have been sent to $cleanEmail. Please check your inbox.")
            } else {
                Result.success("Password reset instructions simulated for $cleanEmail.")
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No registered account found with this email address."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid email format."))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to send reset email."))
        }
    }

    /**
     * Modern Google Sign-In using Android Credential Manager and GoogleIdTokenCredential.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<UserProfile> {
        val webClientId = getEffectiveWebClientId(activityContext)
        if (webClientId.isBlank()) {
            return Result.failure(
                Exception("Google Sign-In Web Client ID is not configured in Google Services. You can sign in using Email/Password or continue playing immediately as Guest.")
            )
        }

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Sign in with Firebase using Google Auth Credential
                val auth = firebaseAuth
                if (auth != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val fbUser = authResult.user ?: throw Exception("Google user is null.")

                    val existingProfile = try {
                        firestoreRepository?.getUserProfile(fbUser.uid)
                    } catch (_: Exception) {
                        null
                    }

                    val profile = existingProfile ?: UserProfile(
                        userId = fbUser.uid,
                        username = fbUser.displayName?.ifBlank { googleIdTokenCredential.displayName ?: "Google Player" }
                            ?: (googleIdTokenCredential.displayName ?: "Google Player"),
                        email = fbUser.email ?: googleIdTokenCredential.id,
                        avatarIndex = 2, // Crown avatar
                        authProvider = "google",
                        rating = 1200,
                        chessRating = 1200,
                        dominoRating = 1200,
                        backgammonRating = 1200,
                        level = 1,
                        xp = 350,
                        coins = 1500
                    )

                    saveUser(profile)
                    try {
                        firestoreRepository?.syncUserProfile(profile)
                    } catch (_: Exception) {}

                    Result.success(profile)
                } else {
                    // Local fallback with verified Google credentials
                    val profile = signInWithGoogleCredentials(
                        displayName = googleIdTokenCredential.displayName ?: "Google Player",
                        email = googleIdTokenCredential.id,
                        googleId = "google_" + UUID.randomUUID().toString().take(8)
                    )
                    Result.success(profile)
                }
            } else {
                Result.failure(Exception("Unsupported credential type returned from Google sign-in."))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google Sign-In was cancelled."))
        } catch (e: NoCredentialException) {
            Result.failure(Exception("No Google account found on device. Please sign in with Email or play as Guest."))
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Google Sign-In failed: ${e.localizedMessage ?: "Unknown credential error"}"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Google authentication failed."))
        }
    }

    /**
     * Fallback direct Google account sign in
     */
    fun signInWithGoogleCredentials(displayName: String, email: String, googleId: String): UserProfile {
        val current = _currentUser.value
        val profile = (current ?: UserProfile()).copy(
            userId = googleId,
            username = displayName.ifBlank { "Google Player" },
            email = email,
            authProvider = "google",
            coins = maxOf(current?.coins ?: 1000, 1500)
        )
        saveUser(profile)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestoreRepository?.syncUserProfile(profile)
            } catch (_: Exception) {}
        }
        return profile
    }

    /**
     * Fast access guest login. Authenticates anonymously in Firebase when available.
     */
    fun loginAsGuest(customName: String? = null, avatarIndex: Int = 0): UserProfile {
        val randomDigits = (1000..9999).random()
        val defaultName = customName?.takeIf { it.isNotBlank() } ?: "Player_$randomDigits"
        val userId = "guest_" + UUID.randomUUID().toString().take(8)

        val profile = UserProfile(
            userId = userId,
            username = defaultName,
            email = "$userId@royalboard3d.local",
            avatarIndex = avatarIndex,
            authProvider = "guest",
            rating = 1200,
            chessRating = 1200,
            dominoRating = 1200,
            backgammonRating = 1200,
            level = 1,
            xp = 250,
            coins = 1000
        )
        saveUser(profile)

        // Asynchronously sign in anonymously to Firebase so Firestore reads/writes succeed
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = firebaseAuth
                if (auth != null && auth.currentUser == null) {
                    val anonResult = auth.signInAnonymously().await()
                    anonResult.user?.let { fbUser ->
                        val updatedWithUid = profile.copy(userId = fbUser.uid)
                        saveUser(updatedWithUid)
                        firestoreRepository?.syncUserProfile(updatedWithUid)
                    }
                }
            } catch (_: Exception) {}
        }

        return profile
    }

    fun saveUser(profile: UserProfile) {
        _currentUser.value = profile
        prefs.edit()
            .putString("user_id", profile.userId)
            .putString("username", profile.username)
            .putString("email", profile.email)
            .putInt("avatar_index", profile.avatarIndex)
            .putString("auth_provider", profile.authProvider)
            .putInt("rating", profile.rating)
            .putInt("chess_rating", profile.chessRating)
            .putInt("domino_rating", profile.dominoRating)
            .putInt("backgammon_rating", profile.backgammonRating)
            .putInt("level", profile.level)
            .putInt("xp", profile.xp)
            .putInt("coins", profile.coins)
            .putInt("wins", profile.wins)
            .putInt("losses", profile.losses)
            .putInt("draws", profile.draws)
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestoreRepository?.syncUserProfile(updated)
            } catch (_: Exception) {}
        }
    }

    fun addCoins(amount: Int) {
        val curr = _currentUser.value ?: return
        val updated = curr.copy(coins = curr.coins + amount)
        saveUser(updated)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestoreRepository?.updateUserCoins(curr.userId, updated.coins)
            } catch (_: Exception) {}
        }
    }

    fun getFirebaseUser(): FirebaseUser? {
        return try {
            firebaseAuth?.currentUser
        } catch (_: Exception) {
            null
        }
    }

    fun isUserLoggedIn(): Boolean {
        return _currentUser.value != null
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    companion object {
        fun getEffectiveWebClientId(context: Context): String {
            // 1. Check generated default_web_client_id from google-services.json
            val defaultIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (defaultIdRes != 0) {
                val id = context.getString(defaultIdRes).trim()
                if (id.isNotEmpty() && !id.contains("YOUR_WEB_CLIENT_ID")) return id
            }
            // 2. Check custom google_server_client_id if defined in strings.xml
            val customIdRes = context.resources.getIdentifier("google_server_client_id", "string", context.packageName)
            if (customIdRes != 0) {
                val id = context.getString(customIdRes).trim()
                if (id.isNotEmpty() && !id.contains("YOUR_WEB_CLIENT_ID")) return id
            }
            return ""
        }
    }
}
