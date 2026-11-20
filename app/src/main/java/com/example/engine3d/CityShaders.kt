package com.example.engine3d

import android.opengl.GLES30
import android.util.Log

class CityShaderProgram {

  val programId: Int

  val uMvpMatrixLoc: Int
  val uModelMatrixLoc: Int
  val uColorLoc: Int
  val uSunDirLoc: Int
  val uSunColorLoc: Int
  val uAmbientColorLoc: Int
  val uFogColorLoc: Int
  val uFogRangeLoc: Int
  val uCameraPosLoc: Int
  val uSpecularLoc: Int
  val uEmissiveLoc: Int

  init {
    val vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER)
    val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
    programId = GLES30.glCreateProgram()
    GLES30.glAttachShader(programId, vs)
    GLES30.glAttachShader(programId, fs)
    GLES30.glLinkProgram(programId)

    val linkStatus = IntArray(1)
    GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
    if (linkStatus[0] == 0) {
      Log.e("CityShader", "Link error: ${GLES30.glGetProgramInfoLog(programId)}")
    }
    GLES30.glDeleteShader(vs)
    GLES30.glDeleteShader(fs)

    uMvpMatrixLoc = GLES30.glGetUniformLocation(programId, "uMvpMatrix")
    uModelMatrixLoc = GLES30.glGetUniformLocation(programId, "uModelMatrix")
    uColorLoc = GLES30.glGetUniformLocation(programId, "uColor")
    uSunDirLoc = GLES30.glGetUniformLocation(programId, "uSunDir")
    uSunColorLoc = GLES30.glGetUniformLocation(programId, "uSunColor")
    uAmbientColorLoc = GLES30.glGetUniformLocation(programId, "uAmbientColor")
    uFogColorLoc = GLES30.glGetUniformLocation(programId, "uFogColor")
    uFogRangeLoc = GLES30.glGetUniformLocation(programId, "uFogRange")
    uCameraPosLoc = GLES30.glGetUniformLocation(programId, "uCameraPos")
    uSpecularLoc = GLES30.glGetUniformLocation(programId, "uSpecular")
    uEmissiveLoc = GLES30.glGetUniformLocation(programId, "uEmissive")
  }

  fun use() {
    GLES30.glUseProgram(programId)
  }

  private fun compileShader(type: Int, source: String): Int {
    val shader = GLES30.glCreateShader(type)
    GLES30.glShaderSource(shader, source)
    GLES30.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
      Log.e("CityShader", "Shader compile error: ${GLES30.glGetShaderInfoLog(shader)}")
    }
    return shader
  }

  companion object {
    private const val VERTEX_SHADER =
      """#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;

uniform mat4 uMvpMatrix;
uniform mat4 uModelMatrix;

out vec3 vWorldPos;
out vec3 vNormal;

void main() {
    vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
    vWorldPos = worldPos.xyz;
    vNormal = normalize(mat3(uModelMatrix) * aNormal);
    gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
}
"""

    private const val FRAGMENT_SHADER =
      """#version 300 es
precision mediump float;

in vec3 vWorldPos;
in vec3 vNormal;

uniform vec4 uColor;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uAmbientColor;
uniform vec3 uFogColor;
uniform vec2 uFogRange;
uniform vec3 uCameraPos;
uniform float uSpecular;
uniform float uEmissive;

out vec4 fragColor;

void main() {
    vec3 N = normalize(vNormal);
    vec3 L = normalize(uSunDir);
    vec3 V = normalize(uCameraPos - vWorldPos);

    float diff = max(dot(N, L), 0.0);
    // Subtle sky-ground hemisphere bounce for rich 3D depth
    float hemi = 0.5 + 0.5 * N.y;
    vec3 ambient = uAmbientColor * mix(0.65, 1.15, hemi);

    vec3 H = normalize(L + V);
    float specPow = mix(12.0, 48.0, clamp(uSpecular, 0.0, 1.0));
    float spec = pow(max(dot(N, H), 0.0), specPow) * uSpecular;

    vec3 litColor = uColor.rgb * (ambient + uSunColor * diff) + uSunColor * spec;
    litColor = mix(litColor, uColor.rgb * 1.45, clamp(uEmissive, 0.0, 1.0));

    float dist = length(uCameraPos - vWorldPos);
    float fogFactor = clamp((dist - uFogRange.x) / max(uFogRange.y - uFogRange.x, 1.0), 0.0, 1.0);
    vec3 finalRgb = mix(litColor, uFogColor, fogFactor);

    fragColor = vec4(finalRgb, uColor.a);
}
"""
  }
}
