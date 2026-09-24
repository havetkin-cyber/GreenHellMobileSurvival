package com.example.test.gl

import android.content.Context
import android.opengl.GLES30
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class ObjModel private constructor(
    private val vertexBuffer: FloatBuffer,
    private val normalBuffer: FloatBuffer,
    private val uvBuffer: FloatBuffer,
    private val indexBuffer: ShortBuffer,
    val indexCount: Int
) {
    fun draw(aPosLoc: Int, aNormLoc: Int, aUVLoc: Int) {
        GLES30.glEnableVertexAttribArray(aPosLoc)
        GLES30.glVertexAttribPointer(aPosLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)

        GLES30.glEnableVertexAttribArray(aNormLoc)
        GLES30.glVertexAttribPointer(aNormLoc, 3, GLES30.GL_FLOAT, false, 0, normalBuffer)

        GLES30.glEnableVertexAttribArray(aUVLoc)
        GLES30.glVertexAttribPointer(aUVLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, indexBuffer)
    }

    companion object {
        fun loadFromAssets(context: Context, assetPath: String): ObjModel {
            val rawPositions = mutableListOf<FloatArray>()
            val rawNormals = mutableListOf<FloatArray>()
            val rawUVs = mutableListOf<FloatArray>()

            val outVertices = mutableListOf<Float>()
            val outNormals = mutableListOf<Float>()
            val outUVs = mutableListOf<Float>()
            val outIndices = mutableListOf<Short>()

            val vertexCache = mutableMapOf<String, Short>()
            var nextIndex: Short = 0

            val reader = BufferedReader(InputStreamReader(context.assets.open(assetPath)))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val l = line!!.trim()
                if (l.startsWith("v ")) {
                    val parts = l.split("\\s+".toRegex())
                    rawPositions.add(floatArrayOf(parts[1].toFloat(), parts[2].toFloat(), parts[3].toFloat()))
                } else if (l.startsWith("vn ")) {
                    val parts = l.split("\\s+".toRegex())
                    rawNormals.add(floatArrayOf(parts[1].toFloat(), parts[2].toFloat(), parts[3].toFloat()))
                } else if (l.startsWith("vt ")) {
                    val parts = l.split("\\s+".toRegex())
                    rawUVs.add(floatArrayOf(parts[1].toFloat(), parts[2].toFloat()))
                } else if (l.startsWith("f ")) {
                    val parts = l.split("\\s+".toRegex()).drop(1)
                    val faceIndices = mutableListOf<Short>()

                    for (p in parts) {
                        if (vertexCache.containsKey(p)) {
                            faceIndices.add(vertexCache[p]!!)
                        } else {
                            val tokens = p.split("/")
                            val posIdx = tokens[0].toInt() - 1
                            val pos = rawPositions[posIdx]

                            val uv = if (tokens.size > 1 && tokens[1].isNotEmpty()) {
                                rawUVs[tokens[1].toInt() - 1]
                            } else floatArrayOf(0f, 0f)

                            val norm = if (tokens.size > 2 && tokens[2].isNotEmpty()) {
                                rawNormals[tokens[2].toInt() - 1]
                            } else floatArrayOf(0f, 1f, 0f)

                            outVertices.add(pos[0])
                            outVertices.add(pos[1])
                            outVertices.add(pos[2])

                            outUVs.add(uv[0])
                            outUVs.add(uv[1])

                            outNormals.add(norm[0])
                            outNormals.add(norm[1])
                            outNormals.add(norm[2])

                            val idx = nextIndex
                            vertexCache[p] = idx
                            faceIndices.add(idx)
                            nextIndex++
                        }
                    }

                    // Triangulate faces
                    for (i in 1 until faceIndices.size - 1) {
                        outIndices.add(faceIndices[0])
                        outIndices.add(faceIndices[i])
                        outIndices.add(faceIndices[i + 1])
                    }
                }
            }
            reader.close()

            val vBuffer = ByteBuffer.allocateDirect(outVertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            outVertices.forEach { vBuffer.put(it) }
            vBuffer.position(0)

            val nBuffer = ByteBuffer.allocateDirect(outNormals.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            outNormals.forEach { nBuffer.put(it) }
            nBuffer.position(0)

            val uBuffer = ByteBuffer.allocateDirect(outUVs.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            outUVs.forEach { uBuffer.put(it) }
            uBuffer.position(0)

            val iBuffer = ByteBuffer.allocateDirect(outIndices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            outIndices.forEach { iBuffer.put(it) }
            iBuffer.position(0)

            return ObjModel(vBuffer, nBuffer, uBuffer, iBuffer, outIndices.size)
        }
    }
}
