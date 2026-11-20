package com.example.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.CharacterStyle
import com.example.data.GameSettings
import com.example.data.VehicleType
import com.example.data.WeatherType
import com.example.engine3d.CityWorldSimulation
import com.example.engine3d.HudSnapshot
import com.example.ui.menu.SettingsDialog
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.CrimsonSiren
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.IndiaEmerald
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PoliceBlue
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.WarmIvory
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun GameHudScreen(
  sim: CityWorldSimulation,
  hud: HudSnapshot,
  settings: GameSettings,
  isPaused: Boolean,
  statusToast: String?,
  onClearToast: () -> Unit,
  onTogglePause: (Boolean) -> Unit,
  onSaveGame: () -> Unit,
  onSaveSettings: (GameSettings) -> Unit,
  onPurchaseOrSpawnVehicle: (VehicleType) -> Unit,
  onSetWeather: (WeatherType) -> Unit,
  onSetOutfit: (Int) -> Unit,
  onReturnToMainMenu: () -> Unit,
) {
  var showGarage by remember { mutableStateOf(false) }
  var showWardrobe by remember { mutableStateOf(false) }
  var showSettings by remember { mutableStateOf(false) }

  BackHandler {
    when {
      showGarage -> showGarage = false
      showWardrobe -> showWardrobe = false
      showSettings -> showSettings = false
      else -> onTogglePause(!isPaused)
    }
  }

  LaunchedEffect(statusToast) {
    if (statusToast != null) {
      delay(2500L)
      onClearToast()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // Right-half invisible camera touch orbit pad
    Box(
      modifier =
        Modifier
          .align(Alignment.CenterEnd)
          .fillMaxHeight()
          .fillMaxWidth(0.55f)
          .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              sim.addCameraDrag(dragAmount.x, dragAmount.y)
            }
          },
    )

    // Safe-area padded HUD overlay
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      // Top-Left: Circular Minimap Radar + Weather/Time Chip
      Column(
        modifier = Modifier.align(Alignment.TopStart),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MinimapRadar(hud = hud)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            color = ObsidianBlack.copy(alpha = 0.72f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, WarmIvory.copy(alpha = 0.16f)),
            modifier =
              Modifier
                .clickable {
                  val all = WeatherType.entries
                  val next = all[(all.indexOf(hud.weather) + 1) % all.size]
                  onSetWeather(next)
                }
                .heightIn(min = 36.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.WbSunny,
                contentDescription = "Cycle Weather",
                tint = AmberGlow,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${formatClock(hud.timeOfDay)} • ${hud.weather.label}",
                style = MaterialTheme.typography.labelSmall,
                color = WarmIvory,
              )
            }
          }
        }
      }

      // Top-Center: Mission Objective / Banner
      Column(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .widthIn(max = 360.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        hud.activeMission?.let { mission ->
          Surface(
            color = ObsidianBlack.copy(alpha = 0.74f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.45f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                  text = "${mission.typeLabel}: ${mission.title.uppercase()}",
                  style = MaterialTheme.typography.labelLarge,
                  color = SaffronGold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${mission.description} (${hud.missionDistanceMeters}m)",
                  style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                  color = WarmIvory.copy(alpha = 0.88f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        val banner = statusToast ?: hud.missionBannerMessage
        AnimatedVisibility(
          visible = banner != null,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.padding(top = 6.dp),
        ) {
          Surface(
            color = IndiaEmerald.copy(alpha = 0.92f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = banner ?: "",
              style = MaterialTheme.typography.labelLarge,
              color = WarmIvory,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
          }
        }
      }

      // Top-Right: Cash, Wanted Stars, Garage, Wardrobe, Pause
      Column(
        modifier = Modifier.align(Alignment.TopEnd),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (settings.showFps) {
            Surface(
              color = ObsidianBlack.copy(alpha = 0.70f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = "${hud.fps} FPS",
                style = MaterialTheme.typography.labelSmall,
                color = IndiaEmerald,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              )
            }
          }

          Surface(
            color = ObsidianBlack.copy(alpha = 0.78f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, IndiaEmerald.copy(alpha = 0.5f)),
          ) {
            Text(
              text = "₹ %,d".format(hud.cash),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = Color(0xFF4ADE80),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }

          Surface(
            color = ObsidianBlack.copy(alpha = 0.78f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, WarmIvory.copy(alpha = 0.2f)),
            modifier =
              Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { showGarage = true }
                .testTag("hud_garage_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Filled.DirectionsCar, contentDescription = "Vehicle Garage", tint = AmberGlow)
            }
          }

          Surface(
            color = ObsidianBlack.copy(alpha = 0.78f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, WarmIvory.copy(alpha = 0.2f)),
            modifier =
              Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { showWardrobe = true }
                .testTag("hud_wardrobe_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Filled.Checkroom, contentDescription = "Character Style", tint = WarmIvory)
            }
          }

          Surface(
            color = ObsidianBlack.copy(alpha = 0.82f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.6f)),
            modifier =
              Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onTogglePause(true) }
                .testTag("hud_pause_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Filled.Pause, contentDescription = "Pause Menu", tint = SaffronGold)
            }
          }
        }

        // Wanted Stars Row
        if (hud.wantedLevel > 0) {
          Surface(
            color = ObsidianBlack.copy(alpha = 0.78f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CrimsonSiren.copy(alpha = 0.65f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Filled.LocalPolice,
                contentDescription = "Wanted",
                tint = PoliceBlue,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              for (star in 1..5) {
                Icon(
                  imageVector = Icons.Filled.Star,
                  contentDescription = null,
                  tint = if (star <= hud.wantedLevel) CrimsonSiren else MutedSlate.copy(alpha = 0.3f),
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      }

      // Bottom-Left Controls: On-Foot Joystick OR In-Vehicle Left/Right Steering
      Box(modifier = Modifier.align(Alignment.BottomStart)) {
        if (hud.inVehicle) {
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HoldControlButton(
              label = "LEFT",
              icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
              accentColor = WarmIvory,
              tag = "hud_steer_left_button",
              onPressedChange = { sim.steerLeftHeld = it },
            )
            HoldControlButton(
              label = "RIGHT",
              icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              accentColor = WarmIvory,
              tag = "hud_steer_right_button",
              onPressedChange = { sim.steerRightHeld = it },
            )
          }
        } else {
          VirtualJoystick(
            onMove = { jx, jy ->
              sim.joyX = jx
              sim.joyY = jy
            },
          )
        }
      }

      // Bottom-Right Controls: Jump/Sprint/Enter Vehicle OR Gas/Brake/Horn/Exit + Speedometer
      Column(
        modifier = Modifier.align(Alignment.BottomEnd),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (hud.inVehicle) {
          // Digital Speedometer + Exit/Horn row
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              color = ObsidianBlack.copy(alpha = 0.78f),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.45f)),
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.End,
              ) {
                Text(
                  text = "${hud.speedKmh} KM/H",
                  style = MaterialTheme.typography.headlineMedium,
                  color = SaffronGold,
                )
                Text(
                  text = hud.currentVehicleName.uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  color = WarmIvory.copy(alpha = 0.8f),
                )
              }
            }

            ActionCircleButton(
              label = "HORN",
              icon = Icons.Filled.NotificationsActive,
              color = AmberGlow,
              tag = "hud_horn_button",
              onClick = { sim.triggerHorn() },
            )
            ActionCircleButton(
              label = "EXIT",
              icon = Icons.Filled.ExitToApp,
              color = CrimsonSiren,
              tag = "hud_vehicle_toggle_button",
              onClick = { sim.toggleEnterExitVehicle() },
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HoldControlButton(
              label = "BRAKE",
              icon = Icons.Filled.Close,
              accentColor = CrimsonSiren,
              tag = "hud_brake_button",
              onPressedChange = { sim.brakeHeld = it },
            )
            HoldControlButton(
              label = "GAS",
              icon = Icons.Filled.ArrowUpward,
              accentColor = IndiaEmerald,
              tag = "hud_gas_button",
              onPressedChange = { sim.gasHeld = it },
            )
          }
        } else {
          // On-foot action buttons
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            if (hud.nearbyVehicleName != null) {
              Button(
                onClick = { sim.toggleEnterExitVehicle() },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = ObsidianBlack),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.heightIn(min = 52.dp).testTag("hud_vehicle_toggle_button"),
              ) {
                Icon(Icons.Filled.DirectionsCar, contentDescription = "Enter Vehicle")
                Spacer(modifier = Modifier.width(6.dp))
                Text("ENTER ${hud.nearbyVehicleName.uppercase()}", style = MaterialTheme.typography.labelLarge)
              }
            }

            HoldControlButton(
              label = "SPRINT",
              icon = Icons.Filled.DirectionsRun,
              accentColor = AmberGlow,
              tag = "hud_sprint_button",
              onPressedChange = { sim.sprintHeld = it },
            )

            ActionCircleButton(
              label = "JUMP",
              icon = Icons.Filled.ArrowUpward,
              color = SaffronGold,
              tag = "hud_jump_button",
              onClick = { sim.triggerJump() },
            )
          }
        }
      }
    }

    // Pause Menu Modal
    if (isPaused) {
      PauseMenuOverlay(
        onResume = { onTogglePause(false) },
        onSaveGame = onSaveGame,
        onOpenGarage = { showGarage = true },
        onOpenWardrobe = { showWardrobe = true },
        onOpenSettings = { showSettings = true },
        onMainMenu = onReturnToMainMenu,
      )
    }

    if (showGarage) {
      VehicleGarageDialog(
        cash = hud.cash,
        owned = hud.ownedVehicles,
        onSelectVehicle = {
          onPurchaseOrSpawnVehicle(it)
          showGarage = false
          onTogglePause(false)
        },
        onDismiss = { showGarage = false },
      )
    }

    if (showWardrobe) {
      CharacterWardrobeDialog(
        currentIndex = hud.outfitIndex,
        onSelectOutfit = {
          onSetOutfit(it)
          showWardrobe = false
        },
        onDismiss = { showWardrobe = false },
      )
    }

    if (showSettings) {
      SettingsDialog(
        settings = settings,
        showReplayIntro = false,
        onSaveSettings = onSaveSettings,
        onReplayIntro = {},
        onDismiss = { showSettings = false },
      )
    }
  }
}

@Composable
private fun MinimapRadar(hud: HudSnapshot) {
  Surface(
    shape = CircleShape,
    color = ObsidianBlack.copy(alpha = 0.80f),
    border = BorderStroke(2.dp, SaffronGold.copy(alpha = 0.75f)),
    modifier = Modifier.size(116.dp),
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val scale = size.width / 220f

      // City road grid lines relative to player
      for (road in CityWorldSimulation.ROAD_LINES) {
        val rx = cx + (road - hud.playerX) * scale
        if (rx in 0f..size.width) {
          drawLine(Color.White.copy(alpha = 0.16f), Offset(rx, 0f), Offset(rx, size.height), strokeWidth = 2.5f)
        }
        val rz = cy + (road - hud.playerZ) * scale
        if (rz in 0f..size.height) {
          drawLine(Color.White.copy(alpha = 0.16f), Offset(0f, rz), Offset(size.width, rz), strokeWidth = 2.5f)
        }
      }

      // Nearby vehicles & Police blips
      for (b in hud.vehicleBlips) {
        val bx = cx + (b.x - hud.playerX) * scale
        val bz = cy + (b.z - hud.playerZ) * scale
        if (hypot(bx - cx, bz - cy) <= size.width * 0.45f) {
          drawCircle(
            color = if (b.isPolice) PoliceBlue else Color.White.copy(alpha = 0.55f),
            radius = if (b.isPolice) 3.5f else 2.2f,
            center = Offset(bx, bz),
          )
        }
      }

      // Mission beacon clamped to radar ring
      hud.activeMission?.let { m ->
        var dx = (m.targetX - hud.playerX) * scale
        var dz = (m.targetZ - hud.playerZ) * scale
        val dist = hypot(dx, dz)
        val maxR = size.width * 0.42f
        if (dist > maxR) {
          dx = dx / dist * maxR
          dz = dz / dist * maxR
        }
        drawCircle(color = SaffronGold, radius = 5.5f, center = Offset(cx + dx, cy + dz))
      }

      // Player directional arrow at center
      val s = sin(hud.playerYaw)
      val c = cos(hud.playerYaw)
      val tip = Offset(cx + s * 8f, cy + c * 8f)
      val left = Offset(cx - c * 5f - s * 4f, cy + s * 5f - c * 4f)
      val right = Offset(cx + c * 5f - s * 4f, cy - s * 5f - c * 4f)
      val path =
        Path().apply {
          moveTo(tip.x, tip.y)
          lineTo(left.x, left.y)
          lineTo(right.x, right.y)
          close()
        }
      drawPath(path, color = Color(0xFF4ADE80))
      drawCircle(color = WarmIvory.copy(alpha = 0.25f), radius = size.width * 0.48f, style = Stroke(1.5f))
    }
  }
}

@Composable
private fun VirtualJoystick(onMove: (Float, Float) -> Unit) {
  var knobOffset by remember { mutableStateOf(Offset.Zero) }
  val maxRadiusPx = 96f

  DisposableEffect(Unit) {
    onDispose { onMove(0f, 0f) }
  }

  Box(
    modifier =
      Modifier
        .size(132.dp)
        .clip(CircleShape)
        .background(ObsidianBlack.copy(alpha = 0.52f))
        .border(1.5.dp, WarmIvory.copy(alpha = 0.28f), CircleShape)
        .testTag("hud_joystick")
        .pointerInput(Unit) {
          detectDragGestures(
            onDragEnd = {
              knobOffset = Offset.Zero
              onMove(0f, 0f)
            },
            onDragCancel = {
              knobOffset = Offset.Zero
              onMove(0f, 0f)
            },
            onDrag = { change, dragAmount ->
              change.consume()
              val candidate = knobOffset + dragAmount
              val len = hypot(candidate.x, candidate.y)
              val clamped =
                if (len > maxRadiusPx) {
                  Offset(candidate.x / len * maxRadiusPx, candidate.y / len * maxRadiusPx)
                } else {
                  candidate
                }
              knobOffset = clamped
              onMove((clamped.x / maxRadiusPx).coerceIn(-1f, 1f), (clamped.y / maxRadiusPx).coerceIn(-1f, 1f))
            },
          )
        },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      drawCircle(color = WarmIvory.copy(alpha = 0.10f), radius = size.minDimension * 0.34f, center = center)
      drawCircle(
        color = SaffronGold.copy(alpha = 0.85f),
        radius = 24.dp.toPx(),
        center = center + knobOffset,
      )
    }
  }
}

@Composable
private fun HoldControlButton(
  label: String,
  icon: ImageVector,
  accentColor: Color,
  tag: String,
  onPressedChange: (Boolean) -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    onDispose { onPressedChange(false) }
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = if (pressed) accentColor.copy(alpha = 0.88f) else ObsidianBlack.copy(alpha = 0.72f),
    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.75f)),
    modifier =
      Modifier
        .size(width = 76.dp, height = 64.dp)
        .testTag(tag)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed = true
            onPressedChange(true)
            waitForUpOrCancellation()
            pressed = false
            onPressedChange(false)
          }
        },
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (pressed) ObsidianBlack else accentColor,
        modifier = Modifier.size(22.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (pressed) ObsidianBlack else WarmIvory,
      )
    }
  }
}

@Composable
private fun ActionCircleButton(
  label: String,
  icon: ImageVector,
  color: Color,
  tag: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = CircleShape,
    color = ObsidianBlack.copy(alpha = 0.76f),
    border = BorderStroke(1.5.dp, color.copy(alpha = 0.8f)),
    modifier =
      Modifier
        .size(64.dp)
        .clip(CircleShape)
        .clickable(onClick = onClick)
        .testTag(tag),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
      Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = WarmIvory)
    }
  }
}

@Composable
private fun PauseMenuOverlay(
  onResume: () -> Unit,
  onSaveGame: () -> Unit,
  onOpenGarage: () -> Unit,
  onOpenWardrobe: () -> Unit,
  onOpenSettings: () -> Unit,
  onMainMenu: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().background(ObsidianBlack.copy(alpha = 0.76f)),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      color = AsphaltSurface,
      shape = RoundedCornerShape(22.dp),
      border = BorderStroke(1.5.dp, SaffronGold.copy(alpha = 0.55f)),
      modifier = Modifier.fillMaxWidth(0.56f).widthIn(min = 280.dp, max = 420.dp),
    ) {
      Column(
        modifier = Modifier.padding(22.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.pause_title),
          style = MaterialTheme.typography.displayMedium,
          color = SaffronGold,
        )
        PauseRowButton(stringResource(R.string.pause_resume), Icons.Filled.PlayArrow, true, "pause_resume_button", onResume)
        PauseRowButton(stringResource(R.string.pause_save), Icons.Filled.Save, false, "pause_save_button", onSaveGame)
        PauseRowButton(stringResource(R.string.pause_garage), Icons.Filled.DirectionsCar, false, "pause_garage_button", onOpenGarage)
        PauseRowButton(stringResource(R.string.pause_wardrobe), Icons.Filled.Checkroom, false, "pause_wardrobe_button", onOpenWardrobe)
        PauseRowButton(stringResource(R.string.menu_settings), Icons.Filled.Settings, false, "pause_settings_button", onOpenSettings)
        PauseRowButton(stringResource(R.string.pause_main_menu), Icons.Filled.ExitToApp, false, "pause_main_menu_button", onMainMenu)
      }
    }
  }
}

@Composable
private fun PauseRowButton(
  text: String,
  icon: ImageVector,
  primary: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    colors =
      ButtonDefaults.buttonColors(
        containerColor = if (primary) SaffronGold else ElevatedCard,
        contentColor = if (primary) ObsidianBlack else WarmIvory,
      ),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag),
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(10.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
  }
}

@Composable
private fun VehicleGarageDialog(
  cash: Int,
  owned: Set<VehicleType>,
  onSelectVehicle: (VehicleType) -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = AsphaltSurface,
      border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).heightIn(max = 440.dp),
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("INDIAN VEHICLE DEALERSHIP", style = MaterialTheme.typography.headlineMedium, color = SaffronGold)
            Text("Wallet: ₹%,d".format(cash), style = MaterialTheme.typography.labelLarge, color = Color(0xFF4ADE80))
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = WarmIvory)
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(VehicleType.entries) { type ->
            val isOwned = owned.contains(type)
            val canAfford = cash >= type.price
            Surface(
              color = ElevatedCard,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, if (isOwned) IndiaEmerald.copy(alpha = 0.5f) else WarmIvory.copy(alpha = 0.1f)),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(type.displayName, style = MaterialTheme.typography.titleMedium, color = WarmIvory)
                  Text(
                    "${type.category} • Top Speed ${(type.maxSpeed * 3.6f).toInt()} km/h",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate,
                  )
                }
                Button(
                  onClick = { onSelectVehicle(type) },
                  enabled = isOwned || canAfford,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = if (isOwned) IndiaEmerald else SaffronGold,
                      contentColor = if (isOwned) WarmIvory else ObsidianBlack,
                    ),
                  modifier = Modifier.heightIn(min = 48.dp),
                ) {
                  Text(if (isOwned) "SPAWN" else "BUY ₹${type.price}", style = MaterialTheme.typography.labelLarge)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CharacterWardrobeDialog(
  currentIndex: Int,
  onSelectOutfit: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = AsphaltSurface,
      border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("CHARACTER STYLE", style = MaterialTheme.typography.headlineMedium, color = SaffronGold)
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = WarmIvory)
          }
        }
        CharacterStyle.PRESETS.forEachIndexed { idx, preset ->
          FilterChip(
            selected = idx == currentIndex,
            onClick = { onSelectOutfit(idx) },
            label = {
              Text(
                text = "${preset.outfitName} ${if (preset.hasSunglasses) "• Aviator Shades" else ""}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
              )
            },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = SaffronGold,
                selectedLabelColor = ObsidianBlack,
                containerColor = ElevatedCard,
                labelColor = WarmIvory,
              ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
          )
        }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
          Text("Done", color = WarmIvory)
        }
      }
    }
  }
}

private fun formatClock(timeOfDay: Float): String {
  val hour = timeOfDay.toInt() % 24
  val min = ((timeOfDay - timeOfDay.toInt()) * 60).toInt() % 60
  return "%02d:%02d".format(hour, min)
}
