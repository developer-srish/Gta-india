package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.gamePrefsDataStore by preferencesDataStore(name = "grand_theft_india_store")

class GameStorageRepository(private val context: Context) {

  private object Keys {
    val SETTINGS_JSON = stringPreferencesKey("settings_json")
    val SAVE_GAME_JSON = stringPreferencesKey("save_game_json")
  }

  val settingsFlow: Flow<GameSettings> =
    context.gamePrefsDataStore.data.map { prefs ->
      val raw = prefs[Keys.SETTINGS_JSON]
      if (raw.isNullOrBlank()) GameSettings() else parseSettings(raw)
    }

  val saveGameFlow: Flow<SaveGameData> =
    context.gamePrefsDataStore.data.map { prefs ->
      val raw = prefs[Keys.SAVE_GAME_JSON]
      if (raw.isNullOrBlank()) SaveGameData() else parseSave(raw)
    }

  suspend fun loadSettingsOnce(): GameSettings = settingsFlow.first()

  suspend fun loadSaveOnce(): SaveGameData = saveGameFlow.first()

  suspend fun saveSettings(settings: GameSettings) {
    val json =
      JSONObject().apply {
        put("masterVolume", settings.masterVolume.toDouble())
        put("sfxVolume", settings.sfxVolume.toDouble())
        put("graphicsQuality", settings.graphicsQuality.name)
        put("cameraSensitivity", settings.cameraSensitivity.toDouble())
        put("dayCycleSpeed", settings.dayCycleSpeed.toDouble())
        put("showFps", settings.showFps)
      }
    context.gamePrefsDataStore.edit { prefs -> prefs[Keys.SETTINGS_JSON] = json.toString() }
  }

  suspend fun saveGame(save: SaveGameData) {
    val vehiclesArr = JSONArray().apply { save.ownedVehicles.forEach { put(it.name) } }
    val json =
      JSONObject().apply {
        put("hasSave", true)
        put("cash", save.cash)
        put("wantedLevel", save.wantedLevel)
        put("playerX", save.playerX.toDouble())
        put("playerZ", save.playerZ.toDouble())
        put("playerYaw", save.playerYaw.toDouble())
        put("timeOfDay", save.timeOfDay.toDouble())
        put("weather", save.weather.name)
        put("activeMissionIndex", save.activeMissionIndex)
        put("completedMissions", save.completedMissions)
        put("ownedVehicles", vehiclesArr)
        put("outfitIndex", save.outfitIndex)
        put("lastSavedEpochMs", System.currentTimeMillis())
      }
    context.gamePrefsDataStore.edit { prefs -> prefs[Keys.SAVE_GAME_JSON] = json.toString() }
  }

  private fun parseSettings(raw: String): GameSettings =
    runCatching {
        val obj = JSONObject(raw)
        val quality =
          runCatching { GraphicsQuality.valueOf(obj.optString("graphicsQuality", "HIGH")) }
            .getOrDefault(GraphicsQuality.HIGH)
        GameSettings(
          masterVolume = obj.optDouble("masterVolume", 1.0).toFloat().coerceIn(0f, 1f),
          sfxVolume = obj.optDouble("sfxVolume", 0.85).toFloat().coerceIn(0f, 1f),
          graphicsQuality = quality,
          cameraSensitivity =
            obj.optDouble("cameraSensitivity", 1.0).toFloat().coerceIn(0.4f, 2.2f),
          dayCycleSpeed = obj.optDouble("dayCycleSpeed", 1.0).toFloat().coerceIn(0f, 4f),
          showFps = obj.optBoolean("showFps", true),
        )
      }
      .getOrDefault(GameSettings())

  private fun parseSave(raw: String): SaveGameData =
    runCatching {
        val obj = JSONObject(raw)
        val weather =
          runCatching { WeatherType.valueOf(obj.optString("weather", "CLEAR_GOLDEN")) }
            .getOrDefault(WeatherType.CLEAR_GOLDEN)
        val owned = mutableSetOf<VehicleType>()
        val arr = obj.optJSONArray("ownedVehicles")
        if (arr != null) {
          for (i in 0 until arr.length()) {
            runCatching { VehicleType.valueOf(arr.getString(i)) }.getOrNull()?.let { owned.add(it) }
          }
        }
        if (owned.isEmpty()) owned.add(VehicleType.AUTO_RICKSHAW)
        SaveGameData(
          hasSave = obj.optBoolean("hasSave", false),
          cash = obj.optInt("cash", 5000),
          wantedLevel = obj.optInt("wantedLevel", 0).coerceIn(0, 5),
          playerX = obj.optDouble("playerX", 0.0).toFloat(),
          playerZ = obj.optDouble("playerZ", 18.0).toFloat(),
          playerYaw = obj.optDouble("playerYaw", 0.0).toFloat(),
          timeOfDay = obj.optDouble("timeOfDay", 17.6).toFloat(),
          weather = weather,
          activeMissionIndex = obj.optInt("activeMissionIndex", 0),
          completedMissions = obj.optInt("completedMissions", 0),
          ownedVehicles = owned,
          outfitIndex = obj.optInt("outfitIndex", 0),
          lastSavedEpochMs = obj.optLong("lastSavedEpochMs", 0L),
        )
      }
      .getOrDefault(SaveGameData())
}
