package com.example.ui.intro

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.WarmIvory
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun OpeningCinematicScreen(
  masterVolume: Float,
  onFinished: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var showBlackDelay by remember { mutableStateOf(true) }
  var showSkipButton by remember { mutableStateOf(false) }
  var isTransitioningOut by remember { mutableStateOf(false) }

  val exoPlayer = remember {
    val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.opening_cinematic}")
    val audioAttrs =
      AudioAttributes.Builder()
        .setUsage(C.USAGE_GAME)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    ExoPlayer.Builder(context).build().apply {
      setAudioAttributes(audioAttrs, true)
      volume = masterVolume.coerceIn(0f, 1f)
      setMediaItem(MediaItem.fromUri(videoUri))
      repeatMode = Player.REPEAT_MODE_OFF
      playWhenReady = false
      prepare()
    }
  }

  LaunchedEffect(masterVolume) {
    exoPlayer.volume = masterVolume.coerceIn(0f, 1f)
  }

  fun triggerSmoothExit() {
    if (!isTransitioningOut) {
      isTransitioningOut = true
    }
  }

  LaunchedEffect(isTransitioningOut) {
    if (isTransitioningOut) {
      delay(580L)
      exoPlayer.stop()
      onFinished()
    }
  }

  // Brief pure black screen on launch before starting the supplied MP4 cinematic
  LaunchedEffect(Unit) {
    delay(550L)
    showBlackDelay = false
    exoPlayer.playWhenReady = true
    delay(900L)
    showSkipButton = true
  }

  DisposableEffect(exoPlayer, lifecycleOwner) {
    val listener =
      object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
          if (playbackState == Player.STATE_ENDED) {
            triggerSmoothExit()
          }
        }
      }
    exoPlayer.addListener(listener)

    val lifecycleObserver = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
        Lifecycle.Event.ON_RESUME -> if (!showBlackDelay && !isTransitioningOut) exoPlayer.play()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
      exoPlayer.removeListener(listener)
      exoPlayer.release()
    }
  }

  val fadeOutAlpha by
    animateFloatAsState(
      targetValue = if (isTransitioningOut || showBlackDelay) 1f else 0f,
      animationSpec = tween(durationMillis = 550),
      label = "cinematic_fade",
    )

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(Color.Black),
  ) {
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          player = exoPlayer
          useController = false
          resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
          setShutterBackgroundColor(android.graphics.Color.BLACK)
          keepScreenOn = true
          layoutParams =
            ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Black screen cover for initial launch pause & smooth fade transition out
    if (fadeOutAlpha > 0.01f) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = fadeOutAlpha)),
      )
    }

    // Accessible Skip Intro button safely inset from edges
    AnimatedVisibility(
      visible = showSkipButton && !isTransitioningOut,
      enter = fadeIn(tween(350)),
      exit = fadeOut(tween(250)),
      modifier =
        Modifier
          .align(Alignment.BottomEnd)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
      OutlinedButton(
        onClick = { triggerSmoothExit() },
        shape = RoundedCornerShape(28.dp),
        colors =
          ButtonDefaults.outlinedButtonColors(
            containerColor = ObsidianBlack.copy(alpha = 0.78f),
            contentColor = WarmIvory,
          ),
        border = BorderStroke(1.5.dp, SaffronGold.copy(alpha = 0.85f)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier =
          Modifier
            .heightIn(min = 48.dp)
            .testTag("skip_intro_button"),
      ) {
        Text(
          text = stringResource(R.string.skip_intro).uppercase(),
          style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Filled.SkipNext,
          contentDescription = stringResource(R.string.skip_intro),
          tint = SaffronGold,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}
