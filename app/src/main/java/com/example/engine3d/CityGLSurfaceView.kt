package com.example.engine3d

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CityWorldViewport(
  sim: CityWorldSimulation,
  onHudFrame: (HudSnapshot) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val glView = remember {
    GLSurfaceView(context).apply {
      setEGLContextClientVersion(3)
      setEGLConfigChooser(8, 8, 8, 8, 16, 0)
      val renderer = CityRenderer(sim, onHudFrame)
      setRenderer(renderer)
      renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
  }

  DisposableEffect(lifecycleOwner, glView) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> glView.onResume()
        Lifecycle.Event.ON_PAUSE -> glView.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      glView.onPause()
    }
  }

  AndroidView(factory = { glView }, modifier = modifier)
}
