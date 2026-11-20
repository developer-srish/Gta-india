package com.example.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.GameSettings
import com.example.data.GraphicsQuality
import com.example.data.SaveGameData
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.IndiaEmerald
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.WarmIvory

@Composable
fun MainMenuScreen(
  saveData: SaveGameData,
  settings: GameSettings,
  onNewGame: () -> Unit,
  onContinue: () -> Unit,
  onSaveSettings: (GameSettings) -> Unit,
  onReplayIntro: () -> Unit,
  onExitGame: () -> Unit,
) {
  var showSettings by remember { mutableStateOf(false) }
  var menuRevealed by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { menuRevealed = true }

  Box(modifier = Modifier.fillMaxSize()) {
    // Cinematic left-to-right scrim so the 3D live Indian city gleams on the right
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              0.0f to ObsidianBlack.copy(alpha = 0.90f),
              0.42f to ObsidianBlack.copy(alpha = 0.62f),
              0.80f to ObsidianBlack.copy(alpha = 0.16f),
              1.0f to Color.Transparent,
            )
          ),
    )

    AnimatedVisibility(
      visible = menuRevealed,
      enter = fadeIn(tween(650)),
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left Column: GRAND THEFT INDIA Logo + 4 Core Menu Actions
        Column(
          modifier =
            Modifier
              .widthIn(min = 270.dp, max = 360.dp)
              .fillMaxHeight()
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.Center,
        ) {
          GrandTheftIndiaLogo()

          Spacer(modifier = Modifier.height(22.dp))

          // 1. New Game
          MenuPrimaryButton(
            label = stringResource(R.string.menu_new_game),
            subtitle = "Start a fresh career in the city",
            icon = Icons.Filled.PlayArrow,
            highlighted = true,
            enabled = true,
            tag = "menu_new_game_button",
            onClick = onNewGame,
          )

          Spacer(modifier = Modifier.height(10.dp))

          // 2. Continue
          MenuPrimaryButton(
            label = stringResource(R.string.menu_continue),
            subtitle =
              if (saveData.hasSave) "Resume • ₹${saveData.cash} • ${saveData.completedMissions} Missions"
              else "No saved progress found",
            icon = Icons.Filled.Restore,
            highlighted = false,
            enabled = saveData.hasSave,
            tag = "menu_continue_button",
            onClick = onContinue,
          )

          Spacer(modifier = Modifier.height(10.dp))

          // 3. Settings
          MenuPrimaryButton(
            label = stringResource(R.string.menu_settings),
            subtitle = "${settings.graphicsQuality.label} • Audio ${(settings.masterVolume * 100).toInt()}%",
            icon = Icons.Filled.Settings,
            highlighted = false,
            enabled = true,
            tag = "menu_settings_button",
            onClick = { showSettings = true },
          )

          Spacer(modifier = Modifier.height(10.dp))

          // 4. Exit Game
          MenuPrimaryButton(
            label = stringResource(R.string.menu_exit_game),
            subtitle = "Close Grand Theft India",
            icon = Icons.Filled.ExitToApp,
            highlighted = false,
            enabled = true,
            tag = "menu_exit_button",
            onClick = onExitGame,
          )
        }

        // Right Column: Live Camera District Tag at bottom-right
        Box(
          modifier = Modifier.fillMaxHeight().weight(1f),
          contentAlignment = Alignment.BottomEnd,
        ) {
          Surface(
            color = ObsidianBlack.copy(alpha = 0.62f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, WarmIvory.copy(alpha = 0.16f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier
                    .size(8.dp)
                    .background(IndiaEmerald, RoundedCornerShape(4.dp)),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "LIVE 3D INDIAN METROPOLIS • GOLDEN DUSK",
                style = MaterialTheme.typography.labelSmall,
                color = WarmIvory.copy(alpha = 0.9f),
              )
            }
          }
        }
      }
    }

    if (showSettings) {
      SettingsDialog(
        settings = settings,
        showReplayIntro = true,
        onSaveSettings = onSaveSettings,
        onReplayIntro = {
          showSettings = false
          onReplayIntro()
        },
        onDismiss = { showSettings = false },
      )
    }
  }
}

@Composable
fun GrandTheftIndiaLogo() {
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(modifier = Modifier.width(26.dp).height(4.dp).background(SaffronGold))
      Spacer(modifier = Modifier.width(4.dp))
      Box(modifier = Modifier.width(26.dp).height(4.dp).background(WarmIvory))
      Spacer(modifier = Modifier.width(4.dp))
      Box(modifier = Modifier.width(26.dp).height(4.dp).background(IndiaEmerald))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "GRAND THEFT",
      style =
        MaterialTheme.typography.displayMedium.copy(
          shadow = Shadow(color = Color.Black, offset = Offset(3f, 3f), blurRadius = 6f),
        ),
      color = WarmIvory,
    )
    Text(
      text = "INDIA",
      style =
        MaterialTheme.typography.displayLarge.copy(
          fontSize = 62.sp,
          lineHeight = 56.sp,
          shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 8f),
        ),
      color = SaffronGold,
    )
    Text(
      text = "MUMBAI • DELHI • OPEN WORLD EDITION",
      style = MaterialTheme.typography.labelSmall,
      color = AmberGlow,
    )
  }
}

@Composable
private fun MenuPrimaryButton(
  label: String,
  subtitle: String,
  icon: ImageVector,
  highlighted: Boolean,
  enabled: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  val containerColor =
    when {
      !enabled -> AsphaltSurface.copy(alpha = 0.45f)
      highlighted -> SaffronGold
      else -> AsphaltSurface.copy(alpha = 0.84f)
    }
  val contentColor =
    when {
      !enabled -> MutedSlate.copy(alpha = 0.55f)
      highlighted -> ObsidianBlack
      else -> WarmIvory
    }

  Button(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(14.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = AsphaltSurface.copy(alpha = 0.45f),
        disabledContentColor = MutedSlate.copy(alpha = 0.5f),
      ),
    border =
      if (highlighted) null
      else BorderStroke(1.dp, WarmIvory.copy(alpha = if (enabled) 0.18f else 0.08f)),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 54.dp)
        .testTag(tag),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      modifier = Modifier.size(24.dp),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
        color = contentColor.copy(alpha = 0.78f),
      )
    }
  }
}

@Composable
fun SettingsDialog(
  settings: GameSettings,
  showReplayIntro: Boolean,
  onSaveSettings: (GameSettings) -> Unit,
  onReplayIntro: () -> Unit,
  onDismiss: () -> Unit,
) {
  var draft by remember(settings) { mutableStateOf(settings) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = AsphaltSurface,
      border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
    ) {
      Column(
        modifier =
          Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "GAME SETTINGS",
            style = MaterialTheme.typography.headlineMedium,
            color = SaffronGold,
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close Settings", tint = WarmIvory)
          }
        }

        HorizontalDivider(color = WarmIvory.copy(alpha = 0.12f))
        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Master Audio Volume (${(draft.masterVolume * 100).toInt()}%)",
          style = MaterialTheme.typography.titleMedium,
          color = WarmIvory,
        )
        Slider(
          value = draft.masterVolume,
          onValueChange = { draft = draft.copy(masterVolume = it) },
          colors = SliderDefaults.colors(thumbColor = SaffronGold, activeTrackColor = SaffronGold),
        )

        Text(
          text = "Camera Sensitivity (${String.format("%.1fx", draft.cameraSensitivity)})",
          style = MaterialTheme.typography.titleMedium,
          color = WarmIvory,
        )
        Slider(
          value = draft.cameraSensitivity,
          valueRange = 0.4f..2.2f,
          onValueChange = { draft = draft.copy(cameraSensitivity = it) },
          colors = SliderDefaults.colors(thumbColor = SaffronGold, activeTrackColor = SaffronGold),
        )

        Text(
          text = "Day / Night Cycle Speed (${String.format("%.1fx", draft.dayCycleSpeed)})",
          style = MaterialTheme.typography.titleMedium,
          color = WarmIvory,
        )
        Slider(
          value = draft.dayCycleSpeed,
          valueRange = 0f..3.5f,
          onValueChange = { draft = draft.copy(dayCycleSpeed = it) },
          colors = SliderDefaults.colors(thumbColor = SaffronGold, activeTrackColor = SaffronGold),
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Graphics Quality (URP-Style Shadows & Draw Distance)",
          style = MaterialTheme.typography.titleMedium,
          color = WarmIvory,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          GraphicsQuality.entries.forEach { quality ->
            val selected = draft.graphicsQuality == quality
            FilterChip(
              selected = selected,
              onClick = { draft = draft.copy(graphicsQuality = quality) },
              label = { Text(quality.name, style = MaterialTheme.typography.labelSmall) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SaffronGold,
                  selectedLabelColor = ObsidianBlack,
                  containerColor = ElevatedCard,
                  labelColor = WarmIvory,
                ),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Show Live FPS Counter", style = MaterialTheme.typography.bodyLarge, color = WarmIvory)
          Switch(
            checked = draft.showFps,
            onCheckedChange = { draft = draft.copy(showFps = it) },
            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBlack, checkedTrackColor = SaffronGold),
          )
        }

        if (showReplayIntro) {
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = onReplayIntro,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            border = BorderStroke(1.dp, AmberGlow.copy(alpha = 0.5f)),
          ) {
            Icon(Icons.Filled.Movie, contentDescription = null, tint = AmberGlow)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.menu_replay_intro),
              color = AmberGlow,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Cancel", color = MutedSlate)
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              onSaveSettings(draft)
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = ObsidianBlack),
            modifier = Modifier.heightIn(min = 48.dp).testTag("settings_apply_button"),
          ) {
            Text("Apply Settings", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
