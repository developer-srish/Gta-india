package com.example.engine3d

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import com.example.data.CharacterStyle
import com.example.data.VehicleType
import com.example.data.WeatherType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class CityRenderer(
  private val sim: CityWorldSimulation,
  private val onHudFrame: (HudSnapshot) -> Unit,
) : GLSurfaceView.Renderer {

  private lateinit var shader: CityShaderProgram
  private lateinit var cubeMesh: MeshBuffer
  private lateinit var planeMesh: MeshBuffer
  private lateinit var cylinderMesh: MeshBuffer

  private val projMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val vpMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)

  private var lastFrameMs = 0L
  private var hudThrottleAccum = 0f
  private var fpsAccumTime = 0f
  private var fpsFrames = 0
  private var currentFps = 60

  private var aspectRatio = 16f / 9f
  private var camX = 0f
  private var camY = 12f
  private var camZ = 40f

  // Lighting uniforms computed per frame
  private var sunDirX = 0.6f
  private var sunDirY = 0.45f
  private var sunDirZ = -0.65f
  private var sunR = 1.0f
  private var sunG = 0.72f
  private var sunB = 0.45f
  private var ambR = 0.34f
  private var ambG = 0.28f
  private var ambB = 0.36f
  private var fogR = 0.22f
  private var fogG = 0.16f
  private var fogB = 0.24f
  private var nightFactor = 0.5f

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    GLES30.glEnable(GLES30.GL_BLEND)
    GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

    shader = CityShaderProgram()
    cubeMesh = MeshFactory.createUnitCube()
    planeMesh = MeshFactory.createUnitPlane()
    cylinderMesh = MeshFactory.createUnitCylinder(14)

    lastFrameMs = SystemClock.elapsedRealtime()
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES30.glViewport(0, 0, width, height)
    aspectRatio = width.toFloat() / height.coerceAtLeast(1).toFloat()
    val farPlane = sim.settings.graphicsQuality.drawDistance
    Matrix.perspectiveM(projMatrix, 0, 56f, aspectRatio, 0.3f, farPlane)
  }

  override fun onDrawFrame(gl: GL10?) {
    val now = SystemClock.elapsedRealtime()
    val dt = if (lastFrameMs == 0L) 0.016f else ((now - lastFrameMs) / 1000f).coerceIn(0.001f, 0.05f)
    lastFrameMs = now

    fpsAccumTime += dt
    fpsFrames += 1
    if (fpsAccumTime >= 0.5f) {
      currentFps = (fpsFrames / fpsAccumTime).toInt().coerceIn(1, 120)
      fpsAccumTime = 0f
      fpsFrames = 0
    }

    sim.step(dt)

    synchronized(sim.lock) {
      updateAtmosphere()
      GLES30.glClearColor(fogR, fogG, fogB, 1f)
      GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

      val farPlane = sim.settings.graphicsQuality.drawDistance
      Matrix.perspectiveM(projMatrix, 0, 56f, aspectRatio, 0.3f, farPlane)
      updateCamera()
      Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

      shader.use()
      GLES30.glUniform3f(shader.uSunDirLoc, sunDirX, sunDirY, sunDirZ)
      GLES30.glUniform3f(shader.uSunColorLoc, sunR, sunG, sunB)
      GLES30.glUniform3f(shader.uAmbientColorLoc, ambR, ambG, ambB)
      GLES30.glUniform3f(shader.uFogColorLoc, fogR, fogG, fogB)
      GLES30.glUniform2f(shader.uFogRangeLoc, farPlane * 0.35f, farPlane * 0.96f)
      GLES30.glUniform3f(shader.uCameraPosLoc, camX, camY, camZ)

      drawTerrainAndRoads()
      drawFlyover()
      drawLandmarks()
      drawBuildings()
      drawPalms()
      drawVehicles()
      if (sim.mode == EngineMode.PLAYABLE_WORLD && sim.currentVehicleId == null) {
        drawCharacter(
          x = sim.playerX,
          y = sim.playerY,
          z = sim.playerZ,
          yaw = sim.playerYaw,
          walkPhase = sim.playerWalkPhase,
          style = CharacterStyle.PRESETS[sim.outfitIndex],
        )
      }
      drawPedestrians()
      if (sim.mode == EngineMode.PLAYABLE_WORLD) {
        drawMissionBeacon()
      }
      drawRain()
    }

    hudThrottleAccum += dt
    if (hudThrottleAccum >= 0.05f) {
      hudThrottleAccum = 0f
      onHudFrame(sim.snapshotHud(currentFps))
    }
  }

  private fun updateAtmosphere() {
    val t = sim.timeOfDay
    // Smooth day/night curve
    val sunAngle = ((t - 6f) / 12f) * PI.toFloat()
    val daylight = sin(sunAngle).coerceIn(-0.4f, 1f)
    nightFactor = ((0.3f - daylight) / 0.7f).coerceIn(0f, 1f)

    sunDirX = cos(sunAngle * 0.8f) * 0.75f
    sunDirY = (0.28f + daylight * 0.65f).coerceAtLeast(0.18f)
    sunDirZ = -0.55f

    if (sim.weather == WeatherType.MONSOON_RAIN) {
      sunR = 0.48f
      sunG = 0.52f
      sunB = 0.60f
      ambR = 0.25f
      ambG = 0.28f
      ambB = 0.34f
      fogR = 0.16f
      fogG = 0.20f
      fogB = 0.27f
      nightFactor = maxOf(nightFactor, 0.55f)
    } else if (daylight > 0.35f) {
      // Bright Indian Afternoon
      sunR = 1.0f
      sunG = 0.94f
      sunB = 0.82f
      ambR = 0.42f
      ambG = 0.44f
      ambB = 0.48f
      fogR = 0.48f
      fogG = 0.66f
      fogB = 0.86f
    } else if (daylight > -0.05f) {
      // Golden Sunset / Dusk
      sunR = 1.0f
      sunG = 0.56f
      sunB = 0.24f
      ambR = 0.34f
      ambG = 0.25f
      ambB = 0.32f
      fogR = 0.34f
      fogG = 0.20f
      fogB = 0.26f
    } else {
      // Neon Midnight
      sunR = 0.28f
      sunG = 0.36f
      sunB = 0.58f
      ambR = 0.16f
      ambG = 0.18f
      ambB = 0.28f
      fogR = 0.05f
      fogG = 0.06f
      fogB = 0.12f
    }
  }

  private fun updateCamera() {
    if (sim.mode == EngineMode.CINEMATIC_MENU) {
      val t = sim.menuOrbitTime * 0.14f
      val radius = 64f + sin(t * 0.7f) * 14f
      camX = sin(t) * radius
      camZ = cos(t) * radius - 12f
      camY = 21f + sin(t * 1.3f) * 5f
      Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, 0f, 6f, -10f, 0f, 1f, 0f)
    } else {
      val targetY = sim.playerY + if (sim.currentVehicleId != null) 1.4f else 1.55f
      val horiz = cos(sim.camPitch) * sim.camDist
      camX = sim.playerX - sin(sim.camYaw) * horiz
      camZ = sim.playerZ - cos(sim.camYaw) * horiz
      camY = targetY + sin(sim.camPitch) * sim.camDist
      Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, sim.playerX, targetY, sim.playerZ, 0f, 1f, 0f)
    }
  }

  private fun drawTerrainAndRoads() {
    val wetSpec = if (sim.weather == WeatherType.MONSOON_RAIN) 0.75f else 0.12f

    // Countryside lush base ground
    drawBox(0f, -0.15f, 0f, 320f, 0.2f, 320f, 0.13f, 0.24f, 0.12f, spec = 0.04f)

    // Urban concrete district slab
    drawBox(0f, -0.05f, 0f, 204f, 0.1f, 204f, 0.28f, 0.29f, 0.31f, spec = 0.08f)

    // Asphalt grid roads
    val roadColor = if (sim.weather == WeatherType.MONSOON_RAIN) 0.09f else 0.13f
    for (r in CityWorldSimulation.ROAD_LINES) {
      // North-South road
      drawBox(r, 0.01f, 0f, 12.2f, 0.03f, 236f, roadColor, roadColor, roadColor + 0.02f, spec = wetSpec)
      // East-West road
      drawBox(0f, 0.015f, r, 236f, 0.03f, 12.2f, roadColor, roadColor, roadColor + 0.02f, spec = wetSpec)

      // Dashed yellow center stripes
      var s = -104f
      while (s <= 104f) {
        drawBox(r, 0.035f, s, 0.25f, 0.02f, 3.6f, 0.94f, 0.74f, 0.12f, emissive = 0.15f)
        drawBox(s, 0.035f, r, 3.6f, 0.02f, 0.25f, 0.94f, 0.74f, 0.12f, emissive = 0.15f)
        s += 9f
      }
    }

    // Coastal Promenade Water (South edge beyond Z = -108)
    drawBox(0f, -0.08f, -140f, 320f, 0.12f, 64f, 0.06f, 0.20f, 0.36f, spec = 0.85f)
  }

  private fun drawFlyover() {
    val fy = CityWorldSimulation.FLYOVER_Y
    // Main elevated concrete deck along Z=0, X=-72..72
    drawBox(0f, fy - 0.3f, 0f, 144f, 0.6f, 10.4f, 0.34f, 0.35f, 0.38f)
    // Asphalt top
    drawBox(0f, fy + 0.02f, 0f, 144f, 0.05f, 9.2f, 0.12f, 0.12f, 0.14f, spec = 0.3f)
    // Guardrails
    drawBox(0f, fy + 0.45f, 4.9f, 144f, 0.7f, 0.3f, 0.72f, 0.72f, 0.74f)
    drawBox(0f, fy + 0.45f, -4.9f, 144f, 0.7f, 0.3f, 0.72f, 0.72f, 0.74f)

    // Support piers
    var px = -64f
    while (px <= 64f) {
      drawCylinder(px, fy * 0.5f, 0f, 1.8f, fy, 1.8f, 0.42f, 0.42f, 0.45f)
      // Glowing saffron flyover streetlamps
      drawCylinder(px, fy + 2.5f, 4.7f, 0.16f, 4.6f, 0.16f, 0.25f, 0.28f, 0.32f)
      drawBox(px, fy + 4.8f, 4.1f, 0.5f, 0.22f, 1.2f, 1.0f, 0.75f, 0.28f, emissive = 0.9f)
      px += 32f
    }

    // West & East sloped ramps (X = -92..-72 and +72..+92)
    drawSlopedBox(-82f, fy * 0.5f - 0.25f, 0f, 20.8f, 0.55f, 9.6f, 15.1f, 0.30f, 0.31f, 0.34f)
    drawSlopedBox(82f, fy * 0.5f - 0.25f, 0f, 20.8f, 0.55f, 9.6f, -15.1f, 0.30f, 0.31f, 0.34f)
  }

  private fun drawLandmarks() {
    // 1. Marine Archway Monument (India Gate inspired) at (0, -66)
    val mx = 0f
    val mz = -66f
    // Stone stepped base
    drawBox(mx, 0.6f, mz, 22f, 1.2f, 12f, 0.72f, 0.60f, 0.44f)
    // Left & Right Pillars
    drawBox(mx - 6.2f, 8.5f, mz, 4.4f, 15f, 5.2f, 0.80f, 0.67f, 0.48f)
    drawBox(mx + 6.2f, 8.5f, mz, 4.4f, 15f, 5.2f, 0.80f, 0.67f, 0.48f)
    // Grand Crown Lintel
    drawBox(mx, 17.5f, mz, 18.6f, 3.6f, 5.8f, 0.84f, 0.70f, 0.50f)
    drawBox(mx, 20.1f, mz, 15f, 1.6f, 4.8f, 0.75f, 0.61f, 0.42f)
    // Tricolor monument illumination strips at night
    val glow = 0.45f + nightFactor * 0.55f
    drawBox(mx, 15.8f, mz + 2.95f, 16f, 0.35f, 0.15f, 1.0f, 0.48f, 0.05f, emissive = glow)
    drawBox(mx, 15.3f, mz + 2.95f, 16f, 0.35f, 0.15f, 0.96f, 0.96f, 0.96f, emissive = glow)
    drawBox(mx, 14.8f, mz + 2.95f, 16f, 0.35f, 0.15f, 0.08f, 0.68f, 0.25f, emissive = glow)

    // 2. Chhatrapati Central Railway Station at (-66, 44)
    val sx = -66f
    val sz = 44f
    drawBox(sx, 5.5f, sz, 28f, 11f, 16f, 0.68f, 0.46f, 0.36f)
    // Station vaulted platform roof & clock tower
    drawBox(sx, 12.2f, sz, 24f, 2.4f, 18f, 0.32f, 0.36f, 0.42f)
    drawBox(sx, 18f, sz - 5f, 5.5f, 14f, 5.5f, 0.72f, 0.50f, 0.38f)
    drawCylinder(sx, 26.5f, sz - 5f, 4.4f, 3.6f, 4.4f, 0.84f, 0.68f, 0.34f)
    // Glowing clock face & neon station sign
    drawBox(sx, 19.5f, sz - 2.15f, 2.6f, 2.6f, 0.2f, 1.0f, 0.92f, 0.65f, emissive = 0.85f)
    drawBox(sx, 8.8f, sz - 8.1f, 14f, 1.1f, 0.2f, 1.0f, 0.50f, 0.08f, emissive = 0.9f)
  }

  private fun drawBuildings() {
    val winGlow = 0.20f + nightFactor * 0.75f
    for (b in sim.buildings) {
      // Main tower body
      drawBox(b.x, b.h * 0.5f, b.z, b.w, b.h, b.d, b.r, b.g, b.b, spec = 0.15f)
      // Ground-floor colorful Indian bazaar shop awning
      drawBox(
        b.x,
        3.2f,
        b.z,
        b.w + 0.9f,
        0.55f,
        b.d + 0.9f,
        b.shopAwningR,
        b.shopAwningG,
        b.shopAwningB,
        emissive = 0.15f + nightFactor * 0.35f,
      )
      // Lit apartment window bands
      if (b.hasLitWindows) {
        var wy = 6.5f
        while (wy < b.h - 2.5f) {
          drawBox(
            b.x,
            wy,
            b.z,
            b.w + 0.14f,
            1.15f,
            b.d + 0.14f,
            0.96f,
            0.84f,
            0.55f,
            emissive = winGlow,
          )
          wy += 4.6f
        }
      }
      // Rooftop water tank (signature Indian rooftop silhouette!)
      drawCylinder(
        b.x + b.w * 0.22f,
        b.h + 1.1f,
        b.z - b.d * 0.2f,
        1.8f,
        2.1f,
        1.8f,
        0.12f,
        0.14f,
        0.18f,
      )
    }
  }

  private fun drawPalms() {
    for (p in sim.palms) {
      drawCylinder(p.x, p.height * 0.5f, p.z, 0.45f, p.height, 0.45f, 0.44f, 0.32f, 0.20f)
      drawBox(p.x, p.height + 0.4f, p.z, 3.8f, 0.7f, 1.3f, 0.14f, 0.52f, 0.18f)
      drawBox(p.x, p.height + 0.5f, p.z, 1.3f, 0.7f, 3.8f, 0.16f, 0.56f, 0.20f)
    }
  }

  private fun drawVehicles() {
    val sirenFlashA = if (sin(sim.sirenPhase) > 0f) 1f else 0.15f
    val sirenFlashB = if (sin(sim.sirenPhase) <= 0f) 1f else 0.15f

    for (v in sim.vehicles) {
      val t = v.type
      val deg = (v.yaw * 180f / PI.toFloat())
      val bx = v.x
      val by = v.y
      val bz = v.z

      if (t == VehicleType.AUTO_RICKSHAW) {
        // Iconic Indian yellow/green/black 3-wheeler auto-rickshaw
        drawOrientedBox(bx, by + 0.65f, bz, deg, t.width, 0.75f, t.length, 0.96f, 0.78f, 0.08f, spec = 0.55f)
        drawOrientedBox(bx, by + 1.32f, bz - 0.15f, deg, t.width * 0.92f, 0.68f, t.length * 0.68f, 0.12f, 0.13f, 0.15f)
        drawOrientedBox(bx, by + 0.45f, bz, deg, t.width + 0.04f, 0.22f, t.length + 0.04f, 0.10f, 0.62f, 0.22f)
      } else if (t == VehicleType.CITY_BUS) {
        drawOrientedBox(bx, by + t.height * 0.52f, bz, deg, t.width, t.height * 0.85f, t.length, t.bodyR, t.bodyG, t.bodyB, spec = 0.45f)
        drawOrientedBox(bx, by + t.height * 0.66f, bz, deg, t.width + 0.06f, 0.85f, t.length * 0.88f, 0.16f, 0.24f, 0.34f, emissive = 0.25f)
      } else if (t == VehicleType.MOTORCYCLE) {
        drawOrientedBox(bx, by + 0.58f, bz, deg, t.width * 0.55f, 0.48f, t.length * 0.8f, t.bodyR, t.bodyG, t.bodyB, spec = 0.7f)
        drawOrientedBox(bx, by + 0.95f, bz + 0.35f, deg, 0.68f, 0.12f, 0.16f, 0.75f, 0.78f, 0.82f, spec = 0.8f)
      } else {
        // Sedan / Hatchback / Sports Coupe / SUV / Police
        drawOrientedBox(bx, by + 0.58f, bz, deg, t.width, t.height * 0.52f, t.length, v.colorR, v.colorG, v.colorB, spec = 0.75f)
        // Cabin & tinted windows
        drawOrientedBox(bx, by + 1.05f, bz, deg, t.width * 0.82f, t.height * 0.44f, t.length * 0.56f, 0.14f, 0.18f, 0.24f, spec = 0.85f)
        if (t == VehicleType.SPORTS_COUPE) {
          // Rear racing wing
          val sx = bx - sin(v.yaw) * (t.length * 0.44f)
          val sz = bz - cos(v.yaw) * (t.length * 0.44f)
          drawOrientedBox(sx, by + 1.08f, sz, deg, t.width * 0.95f, 0.10f, 0.35f, 0.10f, 0.10f, 0.12f)
        }
        if (v.isPolice) {
          // Police PCR Blue stripe + flashing Red/Blue lightbar
          drawOrientedBox(bx, by + 0.62f, bz, deg, t.width + 0.04f, 0.24f, t.length * 0.9f, 0.10f, 0.32f, 0.85f)
          val activeSiren = sim.wantedLevel > 0
          val eRed = if (activeSiren) sirenFlashA else 0.25f
          val eBlue = if (activeSiren) sirenFlashB else 0.25f
          val lx = bx - cos(v.yaw) * 0.35f
          val lz = bz + sin(v.yaw) * 0.35f
          val rx = bx + cos(v.yaw) * 0.35f
          val rz = bz - sin(v.yaw) * 0.35f
          drawOrientedBox(lx, by + 1.48f, lz, deg, 0.48f, 0.20f, 0.26f, 1.0f, 0.15f, 0.15f, emissive = eRed)
          drawOrientedBox(rx, by + 1.48f, rz, deg, 0.48f, 0.20f, 0.26f, 0.18f, 0.45f, 1.0f, emissive = eBlue)
        }
      }

      // Headlights & Taillights
      val fx = bx + sin(v.yaw) * (t.length * 0.49f)
      val fz = bz + cos(v.yaw) * (t.length * 0.49f)
      val tx = bx - sin(v.yaw) * (t.length * 0.49f)
      val tz = bz - cos(v.yaw) * (t.length * 0.49f)
      drawOrientedBox(fx, by + 0.55f, fz, deg, t.width * 0.75f, 0.16f, 0.12f, 1.0f, 0.96f, 0.80f, emissive = 0.85f)
      drawOrientedBox(tx, by + 0.58f, tz, deg, t.width * 0.72f, 0.15f, 0.12f, 0.98f, 0.12f, 0.12f, emissive = 0.80f)

      // Wheels (4 wheels or 2 for motorcycle)
      val wheelZOff = t.length * 0.32f
      val wheelXOff = if (t == VehicleType.MOTORCYCLE) 0f else t.width * 0.47f
      val sY = sin(v.yaw)
      val cY = cos(v.yaw)
      if (t == VehicleType.MOTORCYCLE) {
        drawWheel(bx + sY * wheelZOff, by + 0.32f, bz + cY * wheelZOff, deg, 0.24f, 0.64f)
        drawWheel(bx - sY * wheelZOff, by + 0.32f, bz - cY * wheelZOff, deg, 0.24f, 0.64f)
      } else {
        drawWheel(bx + sY * wheelZOff + cY * wheelXOff, by + 0.32f, bz + cY * wheelZOff - sY * wheelXOff, deg, 0.28f, 0.64f)
        drawWheel(bx + sY * wheelZOff - cY * wheelXOff, by + 0.32f, bz + cY * wheelZOff + sY * wheelXOff, deg, 0.28f, 0.64f)
        drawWheel(bx - sY * wheelZOff + cY * wheelXOff, by + 0.32f, bz - cY * wheelZOff - sY * wheelXOff, deg, 0.28f, 0.64f)
        drawWheel(bx - sY * wheelZOff - cY * wheelXOff, by + 0.32f, bz - cY * wheelZOff + sY * wheelXOff, deg, 0.28f, 0.64f)
      }
    }
  }

  private fun drawPedestrians() {
    for (p in sim.pedestrians) {
      drawCharacter(
        x = p.x,
        y = 0f,
        z = p.z,
        yaw = p.yaw,
        walkPhase = p.walkPhase,
        style = CharacterStyle("NPC", p.shirtR, p.shirtG, p.shirtB, 0.18f, 0.20f, 0.26f, false),
      )
    }
  }

  private fun drawCharacter(
    x: Float,
    y: Float,
    z: Float,
    yaw: Float,
    walkPhase: Float,
    style: CharacterStyle,
  ) {
    val deg = yaw * 180f / PI.toFloat()
    val swing = sin(walkPhase) * 28f
    val cY = cos(yaw)
    val sY = sin(yaw)

    // Torso
    drawOrientedBox(x, y + 1.15f, z, deg, 0.46f, 0.58f, 0.26f, style.jacketR, style.jacketG, style.jacketB)
    // Head + Hair
    drawOrientedBox(x, y + 1.62f, z, deg, 0.28f, 0.30f, 0.28f, 0.66f, 0.46f, 0.34f)
    drawOrientedBox(x, y + 1.77f, z, deg, 0.30f, 0.11f, 0.30f, 0.08f, 0.07f, 0.07f)
    if (style.hasSunglasses) {
      drawOrientedBox(x + sY * 0.14f, y + 1.64f, z + cY * 0.14f, deg, 0.26f, 0.07f, 0.05f, 0.06f, 0.06f, 0.08f, spec = 0.9f)
    }

    // Swinging Arms & Legs
    val armOff = 0.31f
    val legOff = 0.13f
    drawLimb(x + cY * armOff, y + 1.38f, z - sY * armOff, deg, swing, 0.14f, 0.52f, 0.14f, style.jacketR, style.jacketG, style.jacketB)
    drawLimb(x - cY * armOff, y + 1.38f, z + sY * armOff, deg, -swing, 0.14f, 0.52f, 0.14f, style.jacketR, style.jacketG, style.jacketB)
    drawLimb(x + cY * legOff, y + 0.84f, z - sY * legOff, deg, -swing, 0.16f, 0.78f, 0.18f, style.pantsR, style.pantsG, style.pantsB)
    drawLimb(x - cY * legOff, y + 0.84f, z + sY * legOff, deg, swing, 0.16f, 0.78f, 0.18f, style.pantsR, style.pantsG, style.pantsB)
  }

  private fun drawMissionBeacon() {
    val mission = CityWorldSimulation.MISSIONS.getOrNull(sim.activeMissionIndex) ?: return
    val pulse = 0.65f + 0.35f * sin(sim.sirenPhase * 0.5f)
    drawCylinderAlpha(
      mission.targetX,
      14f,
      mission.targetZ,
      3.4f,
      28f,
      3.4f,
      1.0f,
      0.70f,
      0.10f,
      0.32f * pulse,
      emissive = 1.0f,
    )
    drawCylinderAlpha(
      mission.targetX,
      0.2f,
      mission.targetZ,
      6.4f,
      0.2f,
      6.4f,
      1.0f,
      0.55f,
      0.05f,
      0.55f,
      emissive = 1.0f,
    )
  }

  private fun drawRain() {
    if (sim.weather != WeatherType.MONSOON_RAIN) return
    val count = sim.settings.graphicsQuality.rainParticles.coerceAtMost(sim.rainDrops.size)
    for (i in 0 until count) {
      val r = sim.rainDrops[i]
      drawBox(r.x, r.y, r.z, 0.04f, 0.95f, 0.04f, 0.72f, 0.84f, 0.98f, alpha = 0.52f, emissive = 0.45f)
    }
  }

  private fun drawBox(
    x: Float,
    y: Float,
    z: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    r: Float,
    g: Float,
    b: Float,
    alpha: Float = 1f,
    spec: Float = 0.1f,
    emissive: Float = 0f,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitDraw(cubeMesh, r, g, b, alpha, spec, emissive)
  }

  private fun drawSlopedBox(
    x: Float,
    y: Float,
    z: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    tiltZDeg: Float,
    r: Float,
    g: Float,
    b: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.rotateM(modelMatrix, 0, tiltZDeg, 0f, 0f, 1f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitDraw(cubeMesh, r, g, b, 1f, 0.15f, 0f)
  }

  private fun drawOrientedBox(
    x: Float,
    y: Float,
    z: Float,
    yawDeg: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    r: Float,
    g: Float,
    b: Float,
    spec: Float = 0.15f,
    emissive: Float = 0f,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitDraw(cubeMesh, r, g, b, 1f, spec, emissive)
  }

  private fun drawLimb(
    x: Float,
    y: Float,
    z: Float,
    yawDeg: Float,
    pitchDeg: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    r: Float,
    g: Float,
    b: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
    Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
    Matrix.translateM(modelMatrix, 0, 0f, -sy * 0.5f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitDraw(cubeMesh, r, g, b, 1f, 0.1f, 0f)
  }

  private fun drawWheel(
    x: Float,
    y: Float,
    z: Float,
    yawDeg: Float,
    width: Float,
    diam: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
    Matrix.rotateM(modelMatrix, 0, 90f, 0f, 0f, 1f)
    Matrix.scaleM(modelMatrix, 0, diam, width, diam)
    submitDraw(cylinderMesh, 0.10f, 0.10f, 0.12f, 1f, 0.25f, 0f)
  }

  private fun drawCylinder(
    x: Float,
    y: Float,
    z: Float,
    diamX: Float,
    height: Float,
    diamZ: Float,
    r: Float,
    g: Float,
    b: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, diamX, height, diamZ)
    submitDraw(cylinderMesh, r, g, b, 1f, 0.15f, 0f)
  }

  private fun drawCylinderAlpha(
    x: Float,
    y: Float,
    z: Float,
    diamX: Float,
    height: Float,
    diamZ: Float,
    r: Float,
    g: Float,
    b: Float,
    alpha: Float,
    emissive: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, diamX, height, diamZ)
    submitDraw(cylinderMesh, r, g, b, alpha, 0f, emissive)
  }

  private fun submitDraw(
    mesh: MeshBuffer,
    r: Float,
    g: Float,
    b: Float,
    alpha: Float,
    spec: Float,
    emissive: Float,
  ) {
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
    GLES30.glUniformMatrix4fv(shader.uMvpMatrixLoc, 1, false, mvpMatrix, 0)
    GLES30.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
    GLES30.glUniform4f(shader.uColorLoc, r, g, b, alpha)
    GLES30.glUniform1f(shader.uSpecularLoc, spec)
    GLES30.glUniform1f(shader.uEmissiveLoc, emissive)
    mesh.draw()
  }
}
