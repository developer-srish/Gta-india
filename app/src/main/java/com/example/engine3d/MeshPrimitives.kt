package com.example.engine3d

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MeshBuffer(data: FloatArray, val vertexCount: Int) {
  private val vboId: Int

  init {
    val ids = IntArray(1)
    GLES30.glGenBuffers(1, ids, 0)
    vboId = ids[0]

    val buffer: FloatBuffer =
      ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    buffer.put(data).position(0)

    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, buffer, GLES30.GL_STATIC_DRAW)
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
  }

  fun draw() {
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
    GLES30.glEnableVertexAttribArray(0)
    GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 24, 0)
    GLES30.glEnableVertexAttribArray(1)
    GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 24, 12)
    GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
  }

  fun dispose() {
    GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
  }
}

object MeshFactory {

  /** Unit cube centered at origin (-0.5..0.5 on each axis), 36 vertices with normals. */
  fun createUnitCube(): MeshBuffer {
    val p = 0.5f
    val n = -0.5f
    val raw =
      floatArrayOf(
        // Front (+Z)
        n, n, p, 0f, 0f, 1f, p, n, p, 0f, 0f, 1f, p, p, p, 0f, 0f, 1f,
        n, n, p, 0f, 0f, 1f, p, p, p, 0f, 0f, 1f, n, p, p, 0f, 0f, 1f,
        // Back (-Z)
        p, n, n, 0f, 0f, -1f, n, n, n, 0f, 0f, -1f, n, p, n, 0f, 0f, -1f,
        p, n, n, 0f, 0f, -1f, n, p, n, 0f, 0f, -1f, p, p, n, 0f, 0f, -1f,
        // Right (+X)
        p, n, p, 1f, 0f, 0f, p, n, n, 1f, 0f, 0f, p, p, n, 1f, 0f, 0f,
        p, n, p, 1f, 0f, 0f, p, p, n, 1f, 0f, 0f, p, p, p, 1f, 0f, 0f,
        // Left (-X)
        n, n, n, -1f, 0f, 0f, n, n, p, -1f, 0f, 0f, n, p, p, -1f, 0f, 0f,
        n, n, n, -1f, 0f, 0f, n, p, p, -1f, 0f, 0f, n, p, n, -1f, 0f, 0f,
        // Top (+Y)
        n, p, p, 0f, 1f, 0f, p, p, p, 0f, 1f, 0f, p, p, n, 0f, 1f, 0f,
        n, p, p, 0f, 1f, 0f, p, p, n, 0f, 1f, 0f, n, p, n, 0f, 1f, 0f,
        // Bottom (-Y)
        n, n, n, 0f, -1f, 0f, p, n, n, 0f, -1f, 0f, p, n, p, 0f, -1f, 0f,
        n, n, n, 0f, -1f, 0f, p, n, p, 0f, -1f, 0f, n, n, p, 0f, -1f, 0f,
      )
    return MeshBuffer(raw, 36)
  }

  /** Unit XZ plane centered at origin (-0.5..0.5) facing +Y. */
  fun createUnitPlane(): MeshBuffer {
    val p = 0.5f
    val n = -0.5f
    val raw =
      floatArrayOf(
        n, 0f, p, 0f, 1f, 0f, p, 0f, p, 0f, 1f, 0f, p, 0f, n, 0f, 1f, 0f,
        n, 0f, p, 0f, 1f, 0f, p, 0f, n, 0f, 1f, 0f, n, 0f, n, 0f, 1f, 0f,
      )
    return MeshBuffer(raw, 6)
  }

  /** Unit cylinder along Y axis (radius 0.5, height 1.0 centered at 0). */
  fun createUnitCylinder(segments: Int = 14): MeshBuffer {
    val verts = ArrayList<Float>(segments * 12 * 6)
    val r = 0.5f
    val h = 0.5f
    for (i in 0 until segments) {
      val a0 = (2.0 * PI * i / segments).toFloat()
      val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
      val x0 = cos(a0) * r
      val z0 = sin(a0) * r
      val x1 = cos(a1) * r
      val z1 = sin(a1) * r
      val nx0 = cos(a0)
      val nz0 = sin(a0)
      val nx1 = cos(a1)
      val nz1 = sin(a1)

      // Side quad (2 tris)
      addVert(verts, x0, -h, z0, nx0, 0f, nz0)
      addVert(verts, x1, -h, z1, nx1, 0f, nz1)
      addVert(verts, x1, h, z1, nx1, 0f, nz1)

      addVert(verts, x0, -h, z0, nx0, 0f, nz0)
      addVert(verts, x1, h, z1, nx1, 0f, nz1)
      addVert(verts, x0, h, z0, nx0, 0f, nz0)

      // Top cap
      addVert(verts, 0f, h, 0f, 0f, 1f, 0f)
      addVert(verts, x0, h, z0, 0f, 1f, 0f)
      addVert(verts, x1, h, z1, 0f, 1f, 0f)

      // Bottom cap
      addVert(verts, 0f, -h, 0f, 0f, -1f, 0f)
      addVert(verts, x1, -h, z1, 0f, -1f, 0f)
      addVert(verts, x0, -h, z0, 0f, -1f, 0f)
    }
    return MeshBuffer(verts.toFloatArray(), verts.size / 6)
  }

  private fun addVert(
    out: ArrayList<Float>,
    x: Float,
    y: Float,
    z: Float,
    nx: Float,
    ny: Float,
    nz: Float,
  ) {
    out.add(x)
    out.add(y)
    out.add(z)
    out.add(nx)
    out.add(ny)
    out.add(nz)
  }
}
