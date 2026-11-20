package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppScreen
import com.example.data.GameSettings
import com.example.data.GameStorageRepository
import com.example.data.SaveGameData
import com.example.data.VehicleType
import com.example.data.WeatherType
import com.example.engine3d.CityWorldSimulation
import com.example.engine3d.EngineMode
import com.example.engine3d.HudSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = GameStorageRepository(application)
  val worldSim = CityWorldSimulation()

  private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.OpeningCinematic)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  private val _settings = MutableStateFlow(GameSettings())
  val settings: StateFlow<GameSettings> = _settings.asStateFlow()

  private val _saveData = MutableStateFlow(SaveGameData())
  val saveData: StateFlow<SaveGameData> = _saveData.asStateFlow()

  private val _hudState = MutableStateFlow(HudSnapshot())
  val hudState: StateFlow<HudSnapshot> = _hudState.asStateFlow()

  private val _isPaused = MutableStateFlow(false)
  val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

  private val _statusToast = MutableStateFlow<String?>(null)
  val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

  init {
    viewModelScope.launch {
      repository.settingsFlow.collect { loaded ->
        _settings.value = loaded
        worldSim.settings = loaded
      }
    }
    viewModelScope.launch {
      repository.saveGameFlow.collect { saved -> _saveData.value = saved }
    }
  }

  fun onIntroCompleted() {
    worldSim.mode = EngineMode.CINEMATIC_MENU
    worldSim.isPaused = false
    _isPaused.value = false
    _currentScreen.value = AppScreen.MainMenu
  }

  fun replayIntroCinematic() {
    _isPaused.value = false
    worldSim.isPaused = false
    _currentScreen.value = AppScreen.OpeningCinematic
  }

  fun startNewGame() {
    worldSim.startNewGame()
    _isPaused.value = false
    _currentScreen.value = AppScreen.Gameplay
    viewModelScope.launch { repository.saveGame(worldSim.exportSaveData()) }
  }

  fun continueSavedGame() {
    val saved = _saveData.value
    if (!saved.hasSave) return
    worldSim.loadFromSave(saved)
    _isPaused.value = false
    _currentScreen.value = AppScreen.Gameplay
  }

  fun togglePause(paused: Boolean) {
    _isPaused.value = paused
    worldSim.isPaused = paused
  }

  fun saveCurrentGame() {
    val exported = worldSim.exportSaveData()
    viewModelScope.launch {
      repository.saveGame(exported)
      _statusToast.value = "Progress saved to device!"
    }
  }

  fun clearStatusToast() {
    _statusToast.value = null
  }

  fun returnToMainMenu() {
    saveCurrentGame()
    _isPaused.value = false
    worldSim.isPaused = false
    worldSim.mode = EngineMode.CINEMATIC_MENU
    _currentScreen.value = AppScreen.MainMenu
  }

  fun updateSettings(updated: GameSettings) {
    _settings.value = updated
    worldSim.settings = updated
    viewModelScope.launch { repository.saveSettings(updated) }
  }

  fun onHudFrame(snapshot: HudSnapshot) {
    _hudState.value = snapshot
  }

  fun purchaseOrSpawnVehicle(type: VehicleType) {
    val ok = worldSim.purchaseOrSpawnVehicle(type)
    if (ok) {
      saveCurrentGame()
    }
  }

  fun setWeather(weather: WeatherType) {
    worldSim.setWeatherMode(weather)
  }

  fun setOutfit(index: Int) {
    worldSim.setOutfit(index)
  }
}
