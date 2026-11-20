package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppScreen
import com.example.engine3d.CityWorldViewport
import com.example.ui.GameViewModel
import com.example.ui.game.GameHudScreen
import com.example.ui.intro.OpeningCinematicScreen
import com.example.ui.menu.MainMenuScreen
import com.example.ui.theme.GrandTheftIndiaTheme

class MainActivity : ComponentActivity() {

  private val viewModel: GameViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    configureImmersiveSystemBars()

    setContent {
      GrandTheftIndiaTheme {
        val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        val saveData by viewModel.saveData.collectAsStateWithLifecycle()
        val hudState by viewModel.hudState.collectAsStateWithLifecycle()
        val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
        val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
          val isWorldActive = currentScreen != AppScreen.OpeningCinematic

          Crossfade(
            targetState = isWorldActive,
            animationSpec = tween(durationMillis = 600),
            label = "intro_to_world_transition",
          ) { worldActive ->
            if (!worldActive) {
              OpeningCinematicScreen(
                masterVolume = settings.masterVolume,
                onFinished = viewModel::onIntroCompleted,
              )
            } else {
              Box(modifier = Modifier.fillMaxSize()) {
                // Single persistent OpenGL ES 3.0 3D Indian City shared between Main Menu and Gameplay
                CityWorldViewport(
                  sim = viewModel.worldSim,
                  onHudFrame = viewModel::onHudFrame,
                  modifier = Modifier.fillMaxSize(),
                )

                Crossfade(
                  targetState = currentScreen,
                  animationSpec = tween(durationMillis = 400),
                  label = "menu_hud_transition",
                ) { activeOverlay ->
                  if (activeOverlay == AppScreen.MainMenu) {
                    MainMenuScreen(
                      saveData = saveData,
                      settings = settings,
                      onNewGame = viewModel::startNewGame,
                      onContinue = viewModel::continueSavedGame,
                      onSaveSettings = viewModel::updateSettings,
                      onReplayIntro = viewModel::replayIntroCinematic,
                      onExitGame = { finishAffinity() },
                    )
                  } else if (activeOverlay == AppScreen.Gameplay) {
                    GameHudScreen(
                      sim = viewModel.worldSim,
                      hud = hudState,
                      settings = settings,
                      isPaused = isPaused,
                      statusToast = statusToast,
                      onClearToast = viewModel::clearStatusToast,
                      onTogglePause = viewModel::togglePause,
                      onSaveGame = viewModel::saveCurrentGame,
                      onSaveSettings = viewModel::updateSettings,
                      onPurchaseOrSpawnVehicle = viewModel::purchaseOrSpawnVehicle,
                      onSetWeather = viewModel::setWeather,
                      onSetOutfit = viewModel::setOutfit,
                      onReturnToMainMenu = viewModel::returnToMainMenu,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      configureImmersiveSystemBars()
    }
  }

  private fun configureImmersiveSystemBars() {
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
  }
}
