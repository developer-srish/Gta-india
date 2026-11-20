package com.example.data

sealed interface AppScreen {
  data object OpeningCinematic : AppScreen

  data object MainMenu : AppScreen

  data object Gameplay : AppScreen
}

enum class GraphicsQuality(val label: String, val drawDistance: Float, val rainParticles: Int) {
  LOW("Performance (30+ FPS)", 150f, 60),
  MEDIUM("Balanced", 230f, 140),
  HIGH("Ultra Cinematic", 340f, 240),
}

enum class WeatherType(val label: String) {
  CLEAR_GOLDEN("Golden Dusk"),
  MONSOON_RAIN("Monsoon Rain"),
  NEON_NIGHT("Midnight Neon"),
}

enum class VehicleType(
  val displayName: String,
  val category: String,
  val price: Int,
  val maxSpeed: Float,
  val acceleration: Float,
  val turnRate: Float,
  val length: Float,
  val width: Float,
  val height: Float,
  val bodyR: Float,
  val bodyG: Float,
  val bodyB: Float,
) {
  AUTO_RICKSHAW(
    displayName = "Metro Auto-Rickshaw",
    category = "Street",
    price = 0,
    maxSpeed = 24f,
    acceleration = 16f,
    turnRate = 2.6f,
    length = 2.8f,
    width = 1.45f,
    height = 1.65f,
    bodyR = 0.96f,
    bodyG = 0.78f,
    bodyB = 0.08f,
  ),
  HATCHBACK(
    displayName = "Desi Swift Hatchback",
    category = "Indian Car",
    price = 4500,
    maxSpeed = 33f,
    acceleration = 19f,
    turnRate = 2.2f,
    length = 3.8f,
    width = 1.75f,
    height = 1.45f,
    bodyR = 0.78f,
    bodyG = 0.82f,
    bodyB = 0.88f,
  ),
  SPORTS_COUPE(
    displayName = "Vayu GT-R Coupe",
    category = "Sports Car",
    price = 18000,
    maxSpeed = 52f,
    acceleration = 30f,
    turnRate = 2.5f,
    length = 4.3f,
    width = 1.9f,
    height = 1.25f,
    bodyR = 0.95f,
    bodyG = 0.30f,
    bodyB = 0.06f,
  ),
  SUV(
    displayName = "Sherpa 4x4 SUV",
    category = "SUV",
    price = 11000,
    maxSpeed = 38f,
    acceleration = 22f,
    turnRate = 1.95f,
    length = 4.5f,
    width = 2.05f,
    height = 1.85f,
    bodyR = 0.14f,
    bodyG = 0.18f,
    bodyB = 0.24f,
  ),
  MOTORCYCLE(
    displayName = "Bullet 500 Cruiser",
    category = "Motorcycle",
    price = 3200,
    maxSpeed = 44f,
    acceleration = 27f,
    turnRate = 3.0f,
    length = 2.2f,
    width = 0.75f,
    height = 1.1f,
    bodyR = 0.68f,
    bodyG = 0.12f,
    bodyB = 0.14f,
  ),
  CITY_BUS(
    displayName = "Mahanagar Express Bus",
    category = "Bus / Heavy",
    price = 9500,
    maxSpeed = 27f,
    acceleration = 12f,
    turnRate = 1.35f,
    length = 7.6f,
    width = 2.35f,
    height = 2.95f,
    bodyR = 0.85f,
    bodyG = 0.16f,
    bodyB = 0.16f,
  ),
  POLICE_SUV(
    displayName = "PCR Interceptor",
    category = "Police",
    price = 25000,
    maxSpeed = 47f,
    acceleration = 26f,
    turnRate = 2.3f,
    length = 4.4f,
    width = 2.0f,
    height = 1.75f,
    bodyR = 0.92f,
    bodyG = 0.94f,
    bodyB = 0.97f,
  ),
}

data class CharacterStyle(
  val outfitName: String = "Saffron Bomber",
  val jacketR: Float = 0.95f,
  val jacketG: Float = 0.44f,
  val jacketB: Float = 0.08f,
  val pantsR: Float = 0.14f,
  val pantsG: Float = 0.16f,
  val pantsB: Float = 0.22f,
  val hasSunglasses: Boolean = true,
) {
  companion object {
    val PRESETS =
      listOf(
        CharacterStyle("Saffron Bomber", 0.95f, 0.44f, 0.08f, 0.14f, 0.16f, 0.22f, true),
        CharacterStyle("Mumbai Kurta", 0.92f, 0.90f, 0.84f, 0.18f, 0.22f, 0.30f, false),
        CharacterStyle("Midnight Stealth", 0.12f, 0.14f, 0.18f, 0.08f, 0.09f, 0.12f, true),
        CharacterStyle("Emerald Street", 0.10f, 0.58f, 0.30f, 0.16f, 0.16f, 0.20f, true),
      )
  }
}

data class MissionSpec(
  val id: Int,
  val title: String,
  val typeLabel: String,
  val description: String,
  val targetX: Float,
  val targetZ: Float,
  val rewardCash: Int,
)

data class GameSettings(
  val masterVolume: Float = 1.0f,
  val sfxVolume: Float = 0.85f,
  val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
  val cameraSensitivity: Float = 1.0f,
  val dayCycleSpeed: Float = 1.0f,
  val showFps: Boolean = true,
)

data class SaveGameData(
  val hasSave: Boolean = false,
  val cash: Int = 5000,
  val wantedLevel: Int = 0,
  val playerX: Float = 0f,
  val playerZ: Float = 18f,
  val playerYaw: Float = 0f,
  val timeOfDay: Float = 17.6f,
  val weather: WeatherType = WeatherType.CLEAR_GOLDEN,
  val activeMissionIndex: Int = 0,
  val completedMissions: Int = 0,
  val ownedVehicles: Set<VehicleType> = setOf(VehicleType.AUTO_RICKSHAW),
  val outfitIndex: Int = 0,
  val lastSavedEpochMs: Long = 0L,
)
