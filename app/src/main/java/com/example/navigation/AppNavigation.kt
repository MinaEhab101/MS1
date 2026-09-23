package com.example.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.audio.MusicManager
import com.example.audio.SoundManager
import com.example.firebase.AuthRepository
import com.example.firebase.FirestoreRepository
import com.example.chess.ui.ChessScreen
import com.example.chess.viewmodel.ChessViewModel
import com.example.domino.ui.DominoScreen
import com.example.domino.viewmodel.DominoViewModel
import com.example.game.GameMode
import com.example.ui.screens.friends.FriendsScreen
import com.example.ui.screens.game.GameScreen
import com.example.ui.screens.game.GameViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.login.LoginScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.shop.AppearanceShopScreen
import com.example.ui.screens.splash.SplashScreen

/**
 * Main Jetpack Compose navigation shell for the application.
 * Manages route transitions between Splash, Login, Home, Game, Domino, Profile, and secondary screens.
 */
@Composable
fun AppNavigation(
    soundManager: SoundManager,
    musicManager: MusicManager,
    authRepository: AuthRepository,
    firestoreRepository: FirestoreRepository,
    navController: NavHostController = rememberNavController()
) {
    val currentUser by authRepository.currentUser.collectAsState()
    var activeGameMode by remember { mutableStateOf<GameMode>(GameMode.Local2Player) }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    val destination = if (currentUser != null) Routes.HOME else Routes.LOGIN
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                authRepository = authRepository,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                profile = currentUser,
                firestoreRepository = firestoreRepository,
                onStartGame = { mode ->
                    activeGameMode = mode
                    navController.navigate(Routes.GAME)
                },
                onNavigateToChess = {
                    navController.navigate(Routes.CHESS)
                },
                onNavigateToDomino = {
                    navController.navigate(Routes.DOMINO)
                },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                onNavigateToFriends = { navController.navigate(Routes.FRIENDS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToShop = { navController.navigate(Routes.SHOP) },
                soundManager = soundManager
            )
        }

        composable(Routes.CHESS) {
            val chessViewModel = remember {
                ChessViewModel(
                    soundManager = soundManager,
                    authRepository = authRepository,
                    firestoreRepository = firestoreRepository
                )
            }

            ChessScreen(
                viewModel = chessViewModel,
                userProfile = currentUser,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLeaderboard = { navController.navigate(Routes.LEADERBOARD) }
            )
        }

        composable(Routes.GAME) {
            val gameViewModel = remember(activeGameMode) {
                GameViewModel(
                    context = navController.context,
                    gameMode = activeGameMode,
                    soundManager = soundManager,
                    authRepository = authRepository
                )
            }

            GameScreen(
                viewModel = gameViewModel,
                userProfile = currentUser,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToDomino = {
                    navController.navigate(Routes.DOMINO) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.DOMINO) {
            val dominoViewModel = remember {
                DominoViewModel(
                    soundManager = soundManager,
                    authRepository = authRepository,
                    firestoreRepository = firestoreRepository
                )
            }

            DominoScreen(
                viewModel = dominoViewModel,
                userProfile = currentUser,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBackgammon = {
                    navController.navigate(Routes.GAME) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(
                firestoreRepository = firestoreRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                profile = currentUser,
                authRepository = authRepository,
                firestoreRepository = firestoreRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FRIENDS) {
            FriendsScreen(
                firestoreRepository = firestoreRepository,
                onStartGame = { mode ->
                    activeGameMode = mode
                    navController.navigate(Routes.GAME)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                soundManager = soundManager,
                musicManager = musicManager,
                authRepository = authRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToShop = { navController.navigate(Routes.SHOP) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SHOP) {
            AppearanceShopScreen(
                userProfile = currentUser,
                authRepository = authRepository,
                firestoreRepository = firestoreRepository,
                soundManager = soundManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
