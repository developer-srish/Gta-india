package com.example.engine3d

import com.example.data.CharacterStyle
import com.example.data.GameSettings
import com.example.data.MissionSpec
import com.example.data.SaveGameData
import com.example.data.VehicleType
import com.example.data.WeatherType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class EngineMode {
  CINEMATIC_MENU,
  PLAYABLE_WORLD,
}

data class BuildingBlock(
  val x: Float,
  val z: Float,
  val w: Float,
  val d: Float,
  val h: Float,
  val r: Float,
  val g: Float,
  val b: Float,
  val shopAwningR: Float,
  val shopAwningG: Float,
  val shopAwningB: Float,
  val hasLitWindows: Boolean,
)

data class PalmTree(val x: Float, val z: Float, val height: Float)

data class VehicleEntity(
  val id: Int,
  val type: VehicleType,
  var x: Float,
  var y: Float,
  var z: Float,
  var yaw: Float,
  var speed: Float,
  var steerAngle: Float,
  var wheelSpin: Float,
  val isTrafficAi: Boolean,
  val isPolice: Boolean,
  var laneAxisX: Boolean = true,
  var laneDir: Float = 1f,
  var laneCoord: Float = 0f,
  var colorR: Float = type.bodyR,
  var colorG: Float = type.bodyG,
  var colorB: Float = type.bodyB,
)

data class PedestrianEntity(
  var x: Float,
  var z: Float,
  var yaw: Float,
  var walkPhase: Float,
  var speed: Float,
  val shirtR: Float,
  val shirtG: Float,
  val shirtB: Float,
  var walkAxisX: Boolean,
  var walkDir: Float,
  var panicTimer: Float = 0f,
)

data class RainDrop(var x: Float, var y: Float, var z: Float, var speed: Float)

data class HudVehicleBlip(val x: Float, val z: Float, val isPolice: Boolean)

data class HudSnapshot(
  val cash: Int = 5000,
  val wantedLevel: Int = 0,
  val speedKmh: Int = 0,
  val inVehicle: Boolean = false,
  val currentVehicleName: String = "",
  val nearbyVehicleName: String? = null,
  val activeMission: MissionSpec? = null,
  val missionDistanceMeters: Int = 0,
  val completedMissions: Int = 0,
  val playerX: Float = 0f,
  val playerZ: Float = 18f,
  val playerYaw: Float = 0f,
  val timeOfDay: Float = 17.6f,
  val weather: WeatherType = WeatherType.CLEAR_GOLDEN,
  val fps: Int = 60,
  val missionBannerMessage: String? = null,
  val vehicleBlips: List<HudVehicleBlip> = emptyList(),
  val ownedVehicles: Set<VehicleType> = setOf(VehicleType.AUTO_RICKSHAW),
  val outfitIndex: Int = 0,
)

class CityWorldSimulation {

  companion object {
    val ROAD_LINES = floatArrayOf(-88f, -44f, 0f, 44f, 88f)
    const val WORLD_LIMIT = 145f
    const val FLYOVER_Y = 5.4f

    val MISSIONS =
      listOf(
        MissionSpec(
          id = 0,
          title = "Marine Archway Rendezvous",
          typeLabel = "STORY",
          description = "Reach the Marine Archway on the southern promenade to meet Vikram.",
          targetX = 0f,
          targetZ = -66f,
          rewardCash = 3500,
        ),
        MissionSpec(
          id = 1,
          title = "Central Station Pickup",
          typeLabel = "STORY",
          description = "Head to Chhatrapati Central Railway Station and secure the courier bag.",
          targetX = -66f,
          targetZ = 44f,
          rewardCash = 5000,
        ),
        MissionSpec(
          id = 2,
          title = "Express Flyover Sprint",
          typeLabel = "SIDE JOB",
          description = "Drive across the elevated Mumbai Flyover to the eastern toll checkpoint.",
          targetX = 88f,
          targetZ = 0f,
          rewardCash = 6500,
        ),
        MissionSpec(
          id = 3,
          title = "Bazaar Syndicate Drop",
          typeLabel = "STORY",
          description = "Deliver the package to the bustling Chandni Market square.",
          targetX = 44f,
          targetZ = 44f,
          rewardCash = 7500,
        ),
        MissionSpec(
          id = 4,
          title = "Countryside Palm Escape",
          typeLabel = "FINALE",
          description = "Shake off the heat and reach the coastal palm highway safehouse.",
          targetX = 96f,
          targetZ = -96f,
          rewardCash = 12000,
        ),
      )
  }

  val lock = Any()

  @Volatile var mode: EngineMode = EngineMode.CINEMATIC_MENU
  @Volatile var isPaused: Boolean = false
  @Volatile var settings: GameSettings = GameSettings()

  // Live control inputs from Compose UI
  @Volatile var joyX: Float = 0f
  @Volatile var joyY: Float = 0f
  @Volatile var sprintHeld: Boolean = false
  @Volatile var gasHeld: Boolean = false
  @Volatile var brakeHeld: Boolean = false
  @Volatile var steerLeftHeld: Boolean = false
  @Volatile var steerRightHeld: Boolean = false

  // Camera orbit state
  var camYaw: Float = 0.2f
  var camPitch: Float = 0.30f
  var camDist: Float = 9.5f
  var menuOrbitTime: Float = 0f

  // Player state
  var playerX: Float = 0f
  var playerY: Float = 0f
  var playerZ: Float = 18f
  var playerVy: Float = 0f
  var playerYaw: Float = 0f
  var playerWalkPhase: Float = 0f
  var isGrounded: Boolean = true
  var currentVehicleId: Int? = null
  var outfitIndex: Int = 0

  // Progression & Atmosphere
  var cash: Int = 5000
  var wantedLevel: Int = 0
  var wantedCooldown: Float = 0f
  var timeOfDay: Float = 17.6f
  var weather: WeatherType = WeatherType.CLEAR_GOLDEN
  var activeMissionIndex: Int = 0
  var completedMissions: Int = 0
  var ownedVehicles: MutableSet<VehicleType> = mutableSetOf(VehicleType.AUTO_RICKSHAW)
  var missionBannerMessage: String? = null
  var missionBannerTimer: Float = 0f
  var hornTimer: Float = 0f
  var sirenPhase: Float = 0f

  // Procedural world geometry
  val buildings = ArrayList<BuildingBlock>()
  val palms = ArrayList<PalmTree>()
  val vehicles = ArrayList<VehicleEntity>()
  val pedestrians = ArrayList<PedestrianEntity>()
  val rainDrops = ArrayList<RainDrop>()

  private var nextVehicleId = 1

  init {
    buildCityLayout()
    spawnInitialEntities()
  }

  private fun buildCityLayout() {
    val rng = Random(2026)
    val centers = floatArrayOf(-66f, -22f, 22f, 66f)
    val awnings =
      arrayOf(
        floatArrayOf(0.96f, 0.45f, 0.08f),
        floatArrayOf(0.10f, 0.65f, 0.34f),
        floatArrayOf(0.12f, 0.48f, 0.85f),
        floatArrayOf(0.85f, 0.18f, 0.38f),
        floatArrayOf(0.95f, 0.78f, 0.14f),
      )

    for (cx in centers) {
      for (cz in centers) {
        // Leave Central Railway Station block (-66, 44) and Archway Plaza (0..22,-66) clear for landmarks
        if (abs(cx - (-66f)) < 2f && abs(cz - 44f) < 2f) continue

        // 2-3 buildings per city block with alleyway gaps
        val subOffsets =
          arrayOf(
            floatArrayOf(-7.5f, -7.5f),
            floatArrayOf(7.5f, -7.0f),
            floatArrayOf(-7.0f, 7.5f),
            floatArrayOf(7.5f, 7.5f),
          )
        for (sub in subOffsets) {
          if (rng.nextFloat() < 0.16f) continue
          val bx = cx + sub[0]
          val bz = cz + sub[1]
          val bw = 10.5f + rng.nextFloat() * 3.2f
          val bd = 10.5f + rng.nextFloat() * 3.2f
          val distCenter = hypot(bx, bz)
          val bh =
            if (distCenter < 52f) 18f + rng.nextFloat() * 32f
            else 10f + rng.nextFloat() * 18f
          val tone = 0.22f + rng.nextFloat() * 0.25f
          val warmTint = rng.nextFloat() * 0.08f
          val awn = awnings[rng.nextInt(awnings.size)]
          buildings.add(
            BuildingBlock(
              x = bx,
              z = bz,
              w = bw,
              d = bd,
              h = bh,
              r = tone + warmTint,
              g = tone + warmTint * 0.6f,
              b = tone + 0.04f,
              shopAwningR = awn[0],
              shopAwningG = awn[1],
              shopAwningB = awn[2],
              hasLitWindows = true,
            )
          )
        }
      }
    }

    // Palm trees along Marine promenade & countryside outskirts
    for (i in -5..5) {
      val px = i * 18f
      palms.add(PalmTree(px, -96f, 7.5f + (abs(i) % 3) * 1.2f))
      palms.add(PalmTree(px, 98f, 7.0f + (abs(i) % 2) * 1.5f))
    }
    for (i in -4..4) {
      val pz = i * 20f
      if (abs(pz) > 10f) {
        palms.add(PalmTree(-98f, pz, 7.8f))
        palms.add(PalmTree(98f, pz, 8.2f))
      }
    }

    // Rain particles
    for (i in 0 until 240) {
      rainDrops.add(
        RainDrop(
          x = (rng.nextFloat() - 0.5f) * 90f,
          y = rng.nextFloat() * 34f,
          z = (rng.nextFloat() - 0.5f) * 90f,
          speed = 26f + rng.nextFloat() * 14f,
        )
      )
    }
  }

  private fun spawnInitialEntities() {
    vehicles.clear()
    pedestrians.clear()
    nextVehicleId = 1

    // Hero parked vehicles near starting intersection (0, 18)
    spawnVehicle(VehicleType.SPORTS_COUPE, 6.5f, 0f, 16f, 0f, isTrafficAi = false)
    spawnVehicle(VehicleType.AUTO_RICKSHAW, -6.5f, 0f, 20f, 0.4f, isTrafficAi = false)
    spawnVehicle(VehicleType.MOTORCYCLE, 5.5f, 0f, 26f, -0.5f, isTrafficAi = false)
    spawnVehicle(VehicleType.SUV, -24f, 0f, 40f, 1.57f, isTrafficAi = false)
    spawnVehicle(VehicleType.HATCHBACK, 24f, 0f, -40f, 0f, isTrafficAi = false)
    spawnVehicle(VehicleType.CITY_BUS, -44f, 0f, -20f, 0f, isTrafficAi = false)

    // Traffic AI vehicles cruising the Indian avenues
    val trafficTypes =
      listOf(
        VehicleType.AUTO_RICKSHAW,
        VehicleType.HATCHBACK,
        VehicleType.SUV,
        VehicleType.SPORTS_COUPE,
        VehicleType.CITY_BUS,
        VehicleType.MOTORCYCLE,
      )
    for (i in 0 until 12) {
      val axisX = i % 2 == 0
      val road = ROAD_LINES[i % ROAD_LINES.size]
      val dir = if (i % 3 == 0) -1f else 1f
      val laneOffset = dir * 2.6f
      val along = -80f + (i * 15f)
      val type = trafficTypes[i % trafficTypes.size]
      vehicles.add(
        VehicleEntity(
          id = nextVehicleId++,
          type = type,
          x = if (axisX) along else road + laneOffset,
          y = 0f,
          z = if (axisX) road - laneOffset else along,
          yaw = if (axisX) (if (dir > 0) PI.toFloat() / 2f else -PI.toFloat() / 2f) else (if (dir > 0) 0f else PI.toFloat()),
          speed = 9f + (i % 4) * 2.5f,
          steerAngle = 0f,
          wheelSpin = 0f,
          isTrafficAi = true,
          isPolice = false,
          laneAxisX = axisX,
          laneDir = dir,
          laneCoord = road,
        )
      )
    }

    // 2 Patrol Police PCR SUVs
    vehicles.add(
      VehicleEntity(
        id = nextVehicleId++,
        type = VehicleType.POLICE_SUV,
        x = 44f + 2.6f,
        y = 0f,
        z = -30f,
        yaw = 0f,
        speed = 13f,
        steerAngle = 0f,
        wheelSpin = 0f,
        isTrafficAi = true,
        isPolice = true,
        laneAxisX = false,
        laneDir = 1f,
        laneCoord = 44f,
      )
    )
    vehicles.add(
      VehicleEntity(
        id = nextVehicleId++,
        type = VehicleType.POLICE_SUV,
        x = -35f,
        y = 0f,
        z = -44f - 2.6f,
        yaw = PI.toFloat() / 2f,
        speed = 13f,
        steerAngle = 0f,
        wheelSpin = 0f,
        isTrafficAi = true,
        isPolice = true,
        laneAxisX = true,
        laneDir = 1f,
        laneCoord = -44f,
      )
    )

    // Pedestrians on sidewalks
    val shirtColors =
      arrayOf(
        floatArrayOf(0.92f, 0.45f, 0.12f),
        floatArrayOf(0.18f, 0.56f, 0.85f),
        floatArrayOf(0.88f, 0.86f, 0.80f),
        floatArrayOf(0.82f, 0.22f, 0.42f),
        floatArrayOf(0.18f, 0.65f, 0.38f),
      )
    val rng = Random(99)
    for (i in 0 until 18) {
      val axisX = i % 2 == 0
      val road = ROAD_LINES[(i + 1) % ROAD_LINES.size]
      val sidewalkOffset = if (i % 4 < 2) 7.2f else -7.2f
      val c = shirtColors[i % shirtColors.size]
      pedestrians.add(
        PedestrianEntity(
          x = if (axisX) -70f + i * 8f else road + sidewalkOffset,
          z = if (axisX) road + sidewalkOffset else -70f + i * 8f,
          yaw = 0f,
          walkPhase = rng.nextFloat() * 6.28f,
          speed = 1.9f + rng.nextFloat() * 0.9f,
          shirtR = c[0],
          shirtG = c[1],
          shirtB = c[2],
          walkAxisX = axisX,
          walkDir = if (i % 3 == 0) -1f else 1f,
        )
      )
    }
  }

  fun spawnVehicle(
    type: VehicleType,
    x: Float,
    y: Float,
    z: Float,
    yaw: Float,
    isTrafficAi: Boolean,
  ): VehicleEntity {
    val v =
      VehicleEntity(
        id = nextVehicleId++,
        type = type,
        x = x,
        y = y,
        z = z,
        yaw = yaw,
        speed = 0f,
        steerAngle = 0f,
        wheelSpin = 0f,
        isTrafficAi = isTrafficAi,
        isPolice = type == VehicleType.POLICE_SUV,
      )
    vehicles.add(v)
    return v
  }

  fun startNewGame() =
    synchronized(lock) {
      mode = EngineMode.PLAYABLE_WORLD
      isPaused = false
      playerX = 0f
      playerY = 0f
      playerZ = 18f
      playerVy = 0f
      playerYaw = 0f
      currentVehicleId = null
      camYaw = 0f
      camPitch = 0.24f
      camDist = 6.2f
      cash = 5000
      wantedLevel = 0
      wantedCooldown = 0f
      timeOfDay = 17.6f
      weather = WeatherType.CLEAR_GOLDEN
      activeMissionIndex = 0
      completedMissions = 0
      ownedVehicles = mutableSetOf(VehicleType.AUTO_RICKSHAW)
      outfitIndex = 0
      showBanner("NEW GAME — Explore Mumbai-Delhi District! Reach Marine Archway.")
      spawnInitialEntities()
    }

  fun loadFromSave(save: SaveGameData) =
    synchronized(lock) {
      mode = EngineMode.PLAYABLE_WORLD
      isPaused = false
      playerX = save.playerX
      playerY = 0f
      playerZ = save.playerZ
      playerVy = 0f
      playerYaw = save.playerYaw
      currentVehicleId = null
      camYaw = save.playerYaw
      camPitch = 0.24f
      camDist = 6.2f
      cash = save.cash
      wantedLevel = save.wantedLevel
      wantedCooldown = if (wantedLevel > 0) 12f else 0f
      timeOfDay = save.timeOfDay
      weather = save.weather
      activeMissionIndex = save.activeMissionIndex.coerceIn(0, MISSIONS.lastIndex)
      completedMissions = save.completedMissions
      ownedVehicles = save.ownedVehicles.toMutableSet()
      outfitIndex = save.outfitIndex.coerceIn(0, CharacterStyle.PRESETS.lastIndex)
      showBanner("SAVE LOADED — Welcome back to Grand Theft India!")
      spawnInitialEntities()
    }

  fun exportSaveData(): SaveGameData =
    synchronized(lock) {
      SaveGameData(
        hasSave = true,
        cash = cash,
        wantedLevel = wantedLevel,
        playerX = playerX,
        playerZ = playerZ,
        playerYaw = playerYaw,
        timeOfDay = timeOfDay,
        weather = weather,
        activeMissionIndex = activeMissionIndex,
        completedMissions = completedMissions,
        ownedVehicles = ownedVehicles.toSet(),
        outfitIndex = outfitIndex,
        lastSavedEpochMs = System.currentTimeMillis(),
      )
    }

  fun triggerJump() =
    synchronized(lock) {
      if (currentVehicleId == null && isGrounded) {
        playerVy = 7.4f
        isGrounded = false
      }
    }

  fun toggleEnterExitVehicle() =
    synchronized(lock) {
      if (currentVehicleId != null) {
        val v = vehicles.find { it.id == currentVehicleId }
        if (v != null) {
          playerX = v.x - cos(v.yaw) * (v.type.width * 0.9f + 0.8f)
          playerZ = v.z + sin(v.yaw) * (v.type.width * 0.9f + 0.8f)
          playerY = v.y
          v.speed *= 0.35f
        }
        currentVehicleId = null
        camDist = 6.2f
      } else {
        val nearest = findNearestVehicle(4.6f)
        if (nearest != null) {
          currentVehicleId = nearest.id
          camDist = max(7.2f, nearest.type.length * 2.0f)
          showBanner("Driving ${nearest.type.displayName}")
        }
      }
    }

  fun triggerHorn() =
    synchronized(lock) {
      if (currentVehicleId != null) {
        hornTimer = 0.55f
        // Scatter nearby pedestrians playfully
        for (p in pedestrians) {
          if (hypot(p.x - playerX, p.z - playerZ) < 16f) {
            p.panicTimer = 2.5f
          }
        }
      }
    }

  fun purchaseOrSpawnVehicle(type: VehicleType): Boolean =
    synchronized(lock) {
      if (!ownedVehicles.contains(type)) {
        if (cash < type.price) {
          showBanner("Need ₹${type.price - cash} more to buy ${type.displayName}!")
          return false
        }
        cash -= type.price
        ownedVehicles.add(type)
        showBanner("Purchased ${type.displayName}! Delivered nearby.")
      } else {
        showBanner("${type.displayName} delivered nearby!")
      }
      val spawnX = (playerX + sin(playerYaw) * 5.2f).coerceIn(-WORLD_LIMIT, WORLD_LIMIT)
      val spawnZ = (playerZ + cos(playerYaw) * 5.2f).coerceIn(-WORLD_LIMIT, WORLD_LIMIT)
      val spawned = spawnVehicle(type, spawnX, 0f, spawnZ, playerYaw, isTrafficAi = false)
      if (currentVehicleId == null) {
        currentVehicleId = spawned.id
        camDist = max(7.2f, type.length * 2.0f)
      }
      return true
    }

  fun setWeatherMode(newWeather: WeatherType) =
    synchronized(lock) {
      weather = newWeather
      if (newWeather == WeatherType.NEON_NIGHT) {
        timeOfDay = 22.5f
      } else if (newWeather == WeatherType.CLEAR_GOLDEN) {
        timeOfDay = 17.6f
      }
      showBanner("Atmosphere: ${newWeather.label}")
    }

  fun setOutfit(index: Int) =
    synchronized(lock) {
      outfitIndex = index.coerceIn(0, CharacterStyle.PRESETS.lastIndex)
      showBanner("Equipped ${CharacterStyle.PRESETS[outfitIndex].outfitName}")
    }

  fun addCameraDrag(dx: Float, dy: Float) =
    synchronized(lock) {
      val sens = 0.0052f * settings.cameraSensitivity
      camYaw -= dx * sens
      camPitch = (camPitch + dy * sens).coerceIn(0.06f, 0.92f)
    }

  fun step(dtRaw: Float) =
    synchronized(lock) {
      val dt = dtRaw.coerceIn(0.001f, 0.05f)
      menuOrbitTime += dt
      sirenPhase += dt * 7.5f
      if (hornTimer > 0f) hornTimer = max(0f, hornTimer - dt)
      if (missionBannerTimer > 0f) {
        missionBannerTimer -= dt
        if (missionBannerTimer <= 0f) missionBannerMessage = null
      }

      if (mode == EngineMode.CINEMATIC_MENU) {
        // Gentle golden-dusk progression and lively traffic on menu background
        updateTrafficAndPedestrians(dt)
        updateRain(dt)
        return@synchronized
      }

      if (isPaused) return@synchronized

      // Day/night progression
      timeOfDay = (timeOfDay + dt * 0.08f * settings.dayCycleSpeed) % 24f

      val drivingId = currentVehicleId
      if (drivingId != null) {
        val car = vehicles.find { it.id == drivingId }
        if (car != null) {
          stepPlayerVehicle(car, dt)
        } else {
          currentVehicleId = null
        }
      } else {
        stepPlayerOnFoot(dt)
      }

      updateTrafficAndPedestrians(dt)
      updatePoliceAndWanted(dt)
      checkMissionCompletion()
      updateRain(dt)
    }

  private fun stepPlayerOnFoot(dt: Float) {
    val mag = hypot(joyX, joyY).coerceAtMost(1f)
    if (mag > 0.08f) {
      val moveAngle = atan2(joyX, -joyY) + camYaw
      val speed = if (sprintHeld || mag > 0.85f) 10.5f else 5.6f
      val nextX = playerX + sin(moveAngle) * speed * mag * dt
      val nextZ = playerZ + cos(moveAngle) * speed * mag * dt

      if (!collidesWithBuildings(nextX, playerZ, 0.45f)) playerX = nextX
      if (!collidesWithBuildings(playerX, nextZ, 0.45f)) playerZ = nextZ

      playerYaw = lerpAngle(playerYaw, moveAngle, dt * 11f)
      playerWalkPhase += dt * speed * 1.4f
    } else {
      playerWalkPhase = 0f
    }

    val groundH = computeSurfaceHeight(playerX, playerZ)
    playerVy -= 20f * dt
    playerY += playerVy * dt
    if (playerY <= groundH) {
      playerY = groundH
      playerVy = 0f
      isGrounded = true
    }

    playerX = playerX.coerceIn(-WORLD_LIMIT, WORLD_LIMIT)
    playerZ = playerZ.coerceIn(-WORLD_LIMIT, WORLD_LIMIT)
  }

  private fun stepPlayerVehicle(car: VehicleEntity, dt: Float) {
    val spec = car.type
    val throttle = (if (gasHeld) 1f else 0f) - (if (brakeHeld) 1f else 0f)
    val steerInput = (if (steerLeftHeld) 1f else 0f) - (if (steerRightHeld) 1f else 0f) + (-joyX * 0.7f)

    if (throttle > 0f) {
      car.speed += spec.acceleration * throttle * dt
    } else if (throttle < 0f) {
      if (car.speed > 1.5f) {
        car.speed -= spec.acceleration * 1.7f * dt
      } else {
        car.speed += spec.acceleration * 0.55f * throttle * dt
      }
    } else {
      // Natural rolling drag
      car.speed *= (1f - min(0.95f, dt * 1.8f))
    }

    car.speed = car.speed.coerceIn(-spec.maxSpeed * 0.35f, spec.maxSpeed)
    car.steerAngle = steerInput.coerceIn(-1f, 1f)

    val speedRatio = (abs(car.speed) / 8f).coerceIn(0f, 1f)
    val dirSign = if (car.speed >= -0.1f) 1f else -1f
    car.yaw += car.steerAngle * spec.turnRate * speedRatio * dirSign * dt

    val nx = car.x + sin(car.yaw) * car.speed * dt
    val nz = car.z + cos(car.yaw) * car.speed * dt

    val halfW = spec.width * 0.55f
    if (!collidesWithBuildings(nx, car.z, halfW)) {
      car.x = nx
    } else {
      car.speed *= -0.25f
    }
    if (!collidesWithBuildings(car.x, nz, halfW)) {
      car.z = nz
    } else {
      car.speed *= -0.25f
    }

    car.x = car.x.coerceIn(-WORLD_LIMIT, WORLD_LIMIT)
    car.z = car.z.coerceIn(-WORLD_LIMIT, WORLD_LIMIT)
    car.y = computeSurfaceHeight(car.x, car.z)
    car.wheelSpin += car.speed * dt * 2.5f

    // Smoothly align chase camera behind vehicle when moving forward
    if (abs(car.speed) > 3f) {
      camYaw = lerpAngle(camYaw, car.yaw, dt * 1.8f)
    }

    playerX = car.x
    playerY = car.y
    playerZ = car.z
    playerYaw = car.yaw

    // Check collisions with AI traffic -> trigger wanted stars
    if (abs(car.speed) > 14f) {
      for (other in vehicles) {
        if (other.id == car.id) continue
        val dist = hypot(other.x - car.x, other.z - car.z)
        if (dist < (car.type.width + other.type.width) * 0.75f) {
          car.speed *= 0.45f
          other.speed *= 0.2f
          raiseWanted(if (other.isPolice) 2 else 1)
          break
        }
      }
    }
  }

  private fun updateTrafficAndPedestrians(dt: Float) {
    for (v in vehicles) {
      if (v.id == currentVehicleId || !v.isTrafficAi) continue

      if (v.isPolice && wantedLevel > 0 && mode == EngineMode.PLAYABLE_WORLD) {
        // Police AI pursues the player!
        val toPlayerX = playerX - v.x
        val toPlayerZ = playerZ - v.z
        val dist = hypot(toPlayerX, toPlayerZ)
        val desiredYaw = atan2(toPlayerX, toPlayerZ)
        v.yaw = lerpAngle(v.yaw, desiredYaw, dt * 2.8f)
        val targetSpd = if (dist > 7f) min(v.type.maxSpeed, 22f + wantedLevel * 4f) else 4f
        v.speed += (targetSpd - v.speed) * min(1f, dt * 3f)
        val nx = v.x + sin(v.yaw) * v.speed * dt
        val nz = v.z + cos(v.yaw) * v.speed * dt
        if (!collidesWithBuildings(nx, v.z, 1.1f)) v.x = nx
        if (!collidesWithBuildings(v.x, nz, 1.1f)) v.z = nz
        v.wheelSpin += v.speed * dt * 2.5f
      } else {
        // Standard lane AI
        val nearPlayer = hypot(v.x - playerX, v.z - playerZ) < 9.5f && mode == EngineMode.PLAYABLE_WORLD
        val targetSpeed = if (nearPlayer) 0f else 10.5f
        v.speed += (targetSpeed - v.speed) * min(1f, dt * 3.5f)
        if (v.laneAxisX) {
          v.x += v.laneDir * v.speed * dt
          if (v.x > 110f) v.x = -110f
          if (v.x < -110f) v.x = 110f
          v.yaw = if (v.laneDir > 0f) PI.toFloat() / 2f else -PI.toFloat() / 2f
        } else {
          v.z += v.laneDir * v.speed * dt
          if (v.z > 110f) v.z = -110f
          if (v.z < -110f) v.z = 110f
          v.yaw = if (v.laneDir > 0f) 0f else PI.toFloat()
        }
        v.wheelSpin += v.speed * dt * 2.5f
      }
    }

    for (p in pedestrians) {
      if (p.panicTimer > 0f) p.panicTimer = max(0f, p.panicTimer - dt)
      val mult = if (p.panicTimer > 0f) 2.2f else 1f
      val step = p.speed * mult * dt
      if (p.walkAxisX) {
        p.x += p.walkDir * step
        if (p.x > 96f || p.x < -96f) p.walkDir = -p.walkDir
        p.yaw = if (p.walkDir > 0f) PI.toFloat() / 2f else -PI.toFloat() / 2f
      } else {
        p.z += p.walkDir * step
        if (p.z > 96f || p.z < -96f) p.walkDir = -p.walkDir
        p.yaw = if (p.walkDir > 0f) 0f else PI.toFloat()
      }
      p.walkPhase += step * 2.4f
    }
  }

  private fun updatePoliceAndWanted(dt: Float) {
    if (wantedLevel <= 0) return
    val closestPolice =
      vehicles.filter { it.isPolice }.minOfOrNull { hypot(it.x - playerX, it.z - playerZ) } ?: 999f
    if (closestPolice > 42f) {
      wantedCooldown -= dt
      if (wantedCooldown <= 0f) {
        wantedLevel = 0
        cash += 500
        showBanner("Police Evaded! +₹500 Street Rep Bonus")
      }
    } else {
      wantedCooldown = 11f
    }
  }

  private fun raiseWanted(delta: Int) {
    val next = (wantedLevel + delta).coerceIn(1, 5)
    if (next > wantedLevel) {
      wantedLevel = next
      showBanner("WANTED LEVEL $wantedLevel ★ — Police PCR Units Dispatched!")
    }
    wantedCooldown = 12f
  }

  private fun checkMissionCompletion() {
    val mission = MISSIONS.getOrNull(activeMissionIndex) ?: return
    val dist = hypot(playerX - mission.targetX, playerZ - mission.targetZ)
    if (dist < 6.5f) {
      cash += mission.rewardCash
      completedMissions += 1
      activeMissionIndex = (activeMissionIndex + 1) % MISSIONS.size
      val nextTitle = MISSIONS[activeMissionIndex].title
      showBanner("MISSION PASSED! +₹${mission.rewardCash} • Next: $nextTitle")
    }
  }

  private fun updateRain(dt: Float) {
    if (weather != WeatherType.MONSOON_RAIN) return
    val count = settings.graphicsQuality.rainParticles.coerceAtMost(rainDrops.size)
    for (i in 0 until count) {
      val r = rainDrops[i]
      r.y -= r.speed * dt
      if (r.y < 0f) {
        r.y = 30f
        r.x = playerX + ((i * 37 % 80) - 40f)
        r.z = playerZ + ((i * 53 % 80) - 40f)
      }
    }
  }

  private fun computeSurfaceHeight(x: Float, z: Float): Float {
    // Elevated Flyover along Z = 0, X from -72 to +72, with ramps from |X| 72..92
    if (abs(z) < 5.2f) {
      val ax = abs(x)
      if (ax <= 72f) return FLYOVER_Y
      if (ax <= 92f) {
        val t = 1f - (ax - 72f) / 20f
        return FLYOVER_Y * t
      }
    }
    return 0f
  }

  private fun collidesWithBuildings(x: Float, z: Float, radius: Float): Boolean {
    for (b in buildings) {
      val hw = b.w * 0.5f + radius
      val hd = b.d * 0.5f + radius
      if (abs(x - b.x) < hw && abs(z - b.z) < hd) return true
    }
    return false
  }

  private fun findNearestVehicle(maxRadius: Float): VehicleEntity? {
    var best: VehicleEntity? = null
    var bestDist = maxRadius
    for (v in vehicles) {
      val d = hypot(v.x - playerX, v.z - playerZ)
      if (d < bestDist) {
        bestDist = d
        best = v
      }
    }
    return best
  }

  private fun showBanner(msg: String) {
    missionBannerMessage = msg
    missionBannerTimer = 4.2f
  }

  private fun lerpAngle(from: Float, to: Float, t: Float): Float {
    var diff = (to - from) % (2f * PI.toFloat())
    if (diff > PI) diff -= 2f * PI.toFloat()
    if (diff < -PI) diff += 2f * PI.toFloat()
    return from + diff * t.coerceIn(0f, 1f)
  }

  fun snapshotHud(fps: Int): HudSnapshot =
    synchronized(lock) {
      val activeVehicle = currentVehicleId?.let { id -> vehicles.find { it.id == id } }
      val nearby = if (activeVehicle == null) findNearestVehicle(4.6f) else null
      val mission = MISSIONS.getOrNull(activeMissionIndex)
      val mDist =
        if (mission != null) hypot(playerX - mission.targetX, playerZ - mission.targetZ).toInt()
        else 0
      val blips =
        vehicles.map { HudVehicleBlip(it.x, it.z, it.isPolice) }
      HudSnapshot(
        cash = cash,
        wantedLevel = wantedLevel,
        speedKmh = abs((activeVehicle?.speed ?: 0f) * 3.6f).toInt(),
        inVehicle = activeVehicle != null,
        currentVehicleName = activeVehicle?.type?.displayName ?: "",
        nearbyVehicleName = nearby?.type?.displayName,
        activeMission = mission,
        missionDistanceMeters = mDist,
        completedMissions = completedMissions,
        playerX = playerX,
        playerZ = playerZ,
        playerYaw = playerYaw,
        timeOfDay = timeOfDay,
        weather = weather,
        fps = fps,
        missionBannerMessage = missionBannerMessage,
        vehicleBlips = blips,
        ownedVehicles = ownedVehicles.toSet(),
        outfitIndex = outfitIndex,
      )
    }
}
