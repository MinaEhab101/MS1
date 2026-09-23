package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ads.AdManager
import com.example.audio.MusicManager
import com.example.audio.SoundManager
import com.example.firebase.AuthRepository
import com.example.firebase.FirestoreRepository
import com.example.navigation.AppNavigation
import com.example.ui.theme.BackgammonKingTheme

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var musicManager: MusicManager
    private lateinit var authRepository: AuthRepository
    private lateinit var firestoreRepository: FirestoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        soundManager = SoundManager(this)
        musicManager = MusicManager(this)
        authRepository = AuthRepository(this)
        firestoreRepository = FirestoreRepository(this)
        authRepository.setFirestoreRepository(firestoreRepository)

        com.example.game.GraphicsSettings.initialize(this)
        AdManager.initialize(this)
        musicManager.startBackgroundMusic()

        setContent {
            BackgammonKingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        soundManager = soundManager,
                        musicManager = musicManager,
                        authRepository = authRepository,
                        firestoreRepository = firestoreRepository
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!musicManager.isPlaying && musicManager.musicVolume > 0.01f) {
            musicManager.startBackgroundMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        musicManager.stopBackgroundMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        musicManager.stopBackgroundMusic()
    }
}

