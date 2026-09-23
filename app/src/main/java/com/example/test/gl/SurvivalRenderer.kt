package com.example.test.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import com.example.test.game.BodyAffliction
import com.example.test.game.BodyPart
import com.example.test.game.Items
import com.example.test.game.SurvivalGame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class SurvivalRenderer(
    private val context: Context,
    val game: SurvivalGame
) : GLSurfaceView.Renderer {

    // Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Shaders & Uniforms
    private var programId: Int = 0
    private var uMVPMatrixLoc: Int = 0
    private var uLightPosLoc: Int = 0
    private var uLightColorLoc: Int = 0
    private var uColorLoc: Int = 0
    private var uTextureLoc: Int = 0
    private var uUseTextureLoc: Int = 0
    private var aPositionLoc: Int = 0
    private var aNormalLoc: Int = 0
    private var aUVLoc: Int = 0

    // Texture IDs
    private var texGrass: Int = 0
    private var texWood: Int = 0
    private var texLeaves: Int = 0
    private var texRock: Int = 0
    private var texWater: Int = 0
    private var texMonster: Int = 0

    // Mesh Buffers
    private lateinit var cubeVertexBuffer: FloatBuffer
    private lateinit var cubeNormalBuffer: FloatBuffer
    private lateinit var cubeUVBuffer: FloatBuffer
    private lateinit var cubeIndexBuffer: ShortBuffer
    private var cubeIndexCount: Int = 0

    // Player Animation & Actions
    var isSwinging: Boolean = false
    private var swingProgress: Float = 0f

    // Target
    var targetObjectType: String? = null
    var targetObjectId: Int = -1

    data class WorldEntity(
        val id: Int,
        val type: String,
        var x: Float,
        var y: Float,
        var z: Float,
        var scale: Float = 1f,
        var health: Float = 100f,
        var attackTimer: Float = 0f
    )

    val entities = mutableListOf<WorldEntity>()
    private var lastTimeMs: Long = System.currentTimeMillis()

    private val rainDrops = Array(60) {
        floatArrayOf(
            (Math.random().toFloat() - 0.5f) * 40f,
            Math.random().toFloat() * 15f,
            (Math.random().toFloat() - 0.5f) * 40f
        )
    }

    init {
        generateJungleWorld()
    }

    private fun generateJungleWorld() {
        var entityId = 1
        for (i in -15..15) {
            for (j in -15..15) {
                if (i * i + j * j < 9) continue

                val rx = i * 4f + (Math.random().toFloat() - 0.5f) * 2f
                val rz = j * 4f + (Math.random().toFloat() - 0.5f) * 2f
                val typeVal = Math.random()

                when {
                    typeVal < 0.30 -> entities.add(WorldEntity(entityId++, "palm", rx, 0f, rz, scale = 1f + Math.random().toFloat() * 0.5f))
                    typeVal < 0.50 -> entities.add(WorldEntity(entityId++, "tree", rx, 0f, rz, scale = 1.2f + Math.random().toFloat() * 0.8f))
                    typeVal < 0.65 -> entities.add(WorldEntity(entityId++, "bush", rx, 0f, rz, scale = 0.8f))
                    typeVal < 0.75 -> entities.add(WorldEntity(entityId++, "rock", rx, 0f, rz, scale = 0.5f + Math.random().toFloat() * 0.4f))
                    typeVal < 0.83 -> entities.add(WorldEntity(entityId++, "banana", rx, 0f, rz, scale = 0.6f))
                    typeVal < 0.90 -> entities.add(WorldEntity(entityId++, "coconut", rx, 0f, rz, scale = 0.5f))
                    else -> entities.add(WorldEntity(entityId++, "tobacco", rx, 0f, rz, scale = 0.7f))
                }
            }
        }

        entities.add(WorldEntity(entityId++, "animal", 8f, 0f, 6f, scale = 1f))
        entities.add(WorldEntity(entityId++, "animal", -10f, 0f, -8f, scale = 0.9f))
        entities.add(WorldEntity(entityId++, "snake", 4f, 0f, 2f, scale = 0.8f))
        entities.add(WorldEntity(entityId++, "snake", -6f, 0f, 5f, scale = 0.8f))

        entities.add(WorldEntity(entityId++, "monster", 14f, 0f, 12f, scale = 1.1f, health = 120f))
        entities.add(WorldEntity(entityId++, "monster", -12f, 0f, 14f, scale = 1.1f, health = 120f))
        entities.add(WorldEntity(entityId++, "boss", 25f, 0f, -25f, scale = 2.2f, health = game.bossHealth))

        entities.add(WorldEntity(entityId++, "fish", 0f, -0.4f, -10f, scale = 0.5f))
        entities.add(WorldEntity(entityId++, "fish", 8f, -0.4f, -9.5f, scale = 0.5f))
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.2f, 0.5f, 0.8f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)

        initShaders()
        initCubeBuffers()
        initProcedural3DTextures()
    }

    private fun initProcedural3DTextures() {
        texGrass = generateTexture(128, 128, Color.rgb(35, 120, 35), Color.rgb(50, 160, 40))
        texWood = generateTexture(128, 128, Color.rgb(110, 65, 30), Color.rgb(80, 45, 20))
        texLeaves = generateTexture(128, 128, Color.rgb(20, 140, 50), Color.rgb(40, 180, 70))
        texRock = generateTexture(128, 128, Color.rgb(110, 110, 110), Color.rgb(150, 150, 150))
        texWater = generateTexture(128, 128, Color.rgb(20, 100, 200), Color.rgb(40, 150, 240))
        texMonster = generateTexture(128, 128, Color.rgb(60, 40, 30), Color.rgb(200, 50, 30))
    }

    private fun generateTexture(width: Int, height: Int, baseColor: Int, noiseColor: Int): Int {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val br = Color.red(baseColor)
        val bg = Color.green(baseColor)
        val bb = Color.blue(baseColor)

        val nr = Color.red(noiseColor)
        val ng = Color.green(noiseColor)
        val nb = Color.blue(noiseColor)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val factor = (Math.random() * 0.6 + 0.4).toFloat()
                val r = (br * (1f - factor) + nr * factor).toInt().coerceIn(0, 255)
                val g = (bg * (1f - factor) + ng * factor).toInt().coerceIn(0, 255)
                val b = (bb * (1f - factor) + nb * factor).toInt().coerceIn(0, 255)
                bitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }

        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val textureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        bitmap.recycle()

        return textureId
    }

    private fun initShaders() {
        val vertexShaderCode = """
            #version 300 es
            uniform mat4 uMVPMatrix;
            in vec3 aPosition;
            in vec3 aNormal;
            in vec2 aUV;
            out vec3 vNormal;
            out vec3 vPosition;
            out vec2 vUV;
            
            void main() {
                vPosition = aPosition;
                vNormal = aNormal;
                vUV = aUV;
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """.trimIndent()

        val fragmentShaderCode = """
            #version 300 es
            precision mediump float;
            uniform vec3 uLightPos;
            uniform vec3 uLightColor;
            uniform vec4 uColor;
            uniform sampler2D uTexture;
            uniform bool uUseTexture;
            
            in vec3 vNormal;
            in vec3 vPosition;
            in vec2 vUV;
            out vec4 fragColor;
            
            void main() {
                vec3 norm = normalize(vNormal);
                vec3 lightDir = normalize(uLightPos - vPosition);
                float diff = max(dot(norm, lightDir), 0.35);
                vec3 diffuse = diff * uLightColor;
                vec4 texColor = uUseTexture ? texture(uTexture, vUV) : vec4(1.0);
                fragColor = vec4(uColor.rgb * texColor.rgb * diffuse, uColor.a * texColor.a);
            }
        """.trimIndent()

        val vShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vShader)
        GLES30.glAttachShader(programId, fShader)
        GLES30.glLinkProgram(programId)

        uMVPMatrixLoc = GLES30.glGetUniformLocation(programId, "uMVPMatrix")
        uLightPosLoc = GLES30.glGetUniformLocation(programId, "uLightPos")
        uLightColorLoc = GLES30.glGetUniformLocation(programId, "uLightColor")
        uColorLoc = GLES30.glGetUniformLocation(programId, "uColor")
        uTextureLoc = GLES30.glGetUniformLocation(programId, "uTexture")
        uUseTextureLoc = GLES30.glGetUniformLocation(programId, "uUseTexture")
        aPositionLoc = GLES30.glGetAttribLocation(programId, "aPosition")
        aNormalLoc = GLES30.glGetAttribLocation(programId, "aNormal")
        aUVLoc = GLES30.glGetAttribLocation(programId, "aUV")
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, code)
        GLES30.glCompileShader(shader)
        return shader
    }

    private fun initCubeBuffers() {
        val vertices = floatArrayOf(
            -0.5f, -0.5f,  0.5f,   0.5f, -0.5f,  0.5f,   0.5f,  0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,  -0.5f,  0.5f,  0.5f,   0.5f,  0.5f,  0.5f,   0.5f,  0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,   0.5f, -0.5f, -0.5f,   0.5f, -0.5f,  0.5f,  -0.5f, -0.5f,  0.5f,
             0.5f, -0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f,  0.5f,  0.5f,   0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,  -0.5f, -0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,  -0.5f,  0.5f, -0.5f
        )

        val normals = floatArrayOf(
            0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
            0f, 0f,-1f,  0f, 0f,-1f,  0f, 0f,-1f,  0f, 0f,-1f,
            0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,
            0f,-1f, 0f,  0f,-1f, 0f,  0f,-1f, 0f,  0f,-1f, 0f,
            1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,
           -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f
        )

        val uvs = floatArrayOf(
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f
        )

        val indices = shortArrayOf(
            0, 1, 2,  0, 2, 3,     4, 5, 6,  4, 6, 7,
            8, 9, 10, 8, 10, 11,   12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19, 20, 21, 22, 20, 22, 23
        )

        cubeIndexCount = indices.size

        cubeVertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices)
        cubeVertexBuffer.position(0)

        cubeNormalBuffer = ByteBuffer.allocateDirect(normals.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(normals)
        cubeNormalBuffer.position(0)

        cubeUVBuffer = ByteBuffer.allocateDirect(uvs.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(uvs)
        cubeUVBuffer.position(0)

        cubeIndexBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().put(indices)
        cubeIndexBuffer.position(0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 60f, aspect, 0.1f, 200f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.currentTimeMillis()
        val deltaTime = (now - lastTimeMs) / 1000f
        lastTimeMs = now

        game.update(deltaTime)
        updateCameraView()

        val hour = game.timeOfDay
        val sunFactor = sin((hour - 6f) / 12f * Math.PI).toFloat().coerceIn(0f, 1f)
        var skyRed = 0.1f + 0.4f * sunFactor
        var skyGreen = 0.15f + 0.5f * sunFactor
        var skyBlue = 0.3f + 0.6f * sunFactor

        if (game.isRaining) {
            skyRed *= 0.4f
            skyGreen *= 0.4f
            skyBlue *= 0.4f
        }

        GLES30.glClearColor(skyRed, skyGreen, skyBlue, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(programId)

        val sunAngle = (hour / 24f) * Math.PI * 2.0
        val lightX = sin(sunAngle).toFloat() * 50f
        val lightY = cos(sunAngle).toFloat() * 50f + 20f
        val lightZ = 20f
        GLES30.glUniform3f(uLightPosLoc, lightX, lightY, lightZ)
        GLES30.glUniform3f(uLightColorLoc, if (game.isRaining) 0.5f else 1.0f, if (game.isRaining) 0.5f else 0.95f, if (game.isRaining) 0.6f else 0.85f)

        GLES30.glEnableVertexAttribArray(aPositionLoc)
        GLES30.glVertexAttribPointer(aPositionLoc, 3, GLES30.GL_FLOAT, false, 0, cubeVertexBuffer)

        GLES30.glEnableVertexAttribArray(aNormalLoc)
        GLES30.glVertexAttribPointer(aNormalLoc, 3, GLES30.GL_FLOAT, false, 0, cubeNormalBuffer)

        GLES30.glEnableVertexAttribArray(aUVLoc)
        GLES30.glVertexAttribPointer(aUVLoc, 2, GLES30.GL_FLOAT, false, 0, cubeUVBuffer)

        // 1. Terrain & River with 3D Textures
        drawBoxTextured(0f, -0.5f, 0f, 120f, 0.1f, 120f, 0.9f, 0.9f, 0.9f, texGrass)
        drawBoxTextured(0f, -0.45f, -10f, 120f, 0.05f, 8f, 0.9f, 0.9f, 1.0f, texWater)

        // 2. Render World Entities
        targetObjectType = null
        targetObjectId = -1
        var minTargetDist = 4.0f

        val entityIterator = entities.iterator()
        while (entityIterator.hasNext()) {
            val entity = entityIterator.next()

            when (entity.type) {
                "animal" -> {
                    entity.x += sin(now / 1000f + entity.id) * 0.02f
                    entity.z += cos(now / 1000f + entity.id) * 0.02f
                }
                "snake" -> {
                    entity.x += sin(now / 800f + entity.id) * 0.015f
                    val dx = entity.x - game.playerX
                    val dz = entity.z - game.playerZ
                    if (kotlin.math.sqrt(dx * dx + dz * dz) < 1.2f) {
                        if (!game.afflictions.any { it.type == "SnakeBite" }) {
                            game.afflictions.add(BodyAffliction(BodyPart.RIGHT_LEG, "SnakeBite", "⚠️ Uštipol ťa jedovatý had!", "Antivenom"))
                            game.poisonLevel = 50f
                            game.showToast("🐍 Had ťa uštipol! Získal si jed!")
                        }
                    }
                }
                "monster" -> {
                    val dx = game.playerX - entity.x
                    val dz = game.playerZ - entity.z
                    val dist = kotlin.math.sqrt(dx * dx + dz * dz)

                    if (dist < 15f) {
                        entity.x += (dx / dist) * deltaTime * 1.8f
                        entity.z += (dz / dist) * deltaTime * 1.8f

                        if (dist < 1.5f) {
                            entity.attackTimer += deltaTime
                            if (entity.attackTimer >= 1.5f) {
                                entity.attackTimer = 0f
                                game.applyDamageToPlayer(25f)
                            }
                        }
                    }
                }
                "boss" -> {
                    val dx = game.playerX - entity.x
                    val dz = game.playerZ - entity.z
                    val dist = kotlin.math.sqrt(dx * dx + dz * dz)

                    if (dist < 20f) {
                        entity.x += (dx / dist) * deltaTime * 1.2f
                        entity.z += (dz / dist) * deltaTime * 1.2f

                        if (dist < 2.5f) {
                            entity.attackTimer += deltaTime
                            if (entity.attackTimer >= 2.0f) {
                                entity.attackTimer = 0f
                                game.applyDamageToPlayer(40f)
                                game.showToast("💥 PRASTARÝ ŠAMAN ŤA ZASIAHOL DUPNUTÍM!")
                            }
                        }
                    }
                }
                "fish" -> {
                    entity.x += cos(now / 1200f + entity.id) * 0.03f
                }
            }

            val dx = entity.x - game.playerX
            val dz = entity.z - game.playerZ
            val dist = kotlin.math.sqrt(dx * dx + dz * dz)

            if (dist < minTargetDist) {
                minTargetDist = dist
                targetObjectType = entity.type
                targetObjectId = entity.id
            }

            when (entity.type) {
                "tree" -> {
                    drawBoxTextured(entity.x, 2.5f * entity.scale, entity.z, 0.6f * entity.scale, 5f * entity.scale, 0.6f * entity.scale, 0.9f, 0.9f, 0.9f, texWood)
                    drawBoxTextured(entity.x, 5.5f * entity.scale, entity.z, 3f * entity.scale, 2.5f * entity.scale, 3f * entity.scale, 0.9f, 0.9f, 0.9f, texLeaves)
                }
                "palm" -> {
                    drawBoxTextured(entity.x, 3f * entity.scale, entity.z, 0.5f * entity.scale, 6f * entity.scale, 0.5f * entity.scale, 0.9f, 0.9f, 0.9f, texWood)
                    drawBoxTextured(entity.x, 6f * entity.scale, entity.z, 4f * entity.scale, 0.3f * entity.scale, 4f * entity.scale, 0.9f, 0.9f, 0.9f, texLeaves)
                }
                "bush" -> drawBoxTextured(entity.x, 0.6f, entity.z, 1.5f, 1.2f, 1.5f, 0.9f, 0.9f, 0.9f, texLeaves)
                "rock" -> drawBoxTextured(entity.x, 0.3f, entity.z, 0.8f * entity.scale, 0.6f * entity.scale, 0.8f * entity.scale, 0.9f, 0.9f, 0.9f, texRock)
                "banana" -> drawBox(entity.x, 0.3f, entity.z, 0.4f, 0.3f, 0.4f, 0.9f, 0.85f, 0.1f)
                "coconut" -> drawBox(entity.x, 0.2f, entity.z, 0.35f, 0.35f, 0.35f, 0.35f, 0.2f, 0.05f)
                "tobacco" -> drawBoxTextured(entity.x, 0.4f, entity.z, 0.6f, 0.8f, 0.6f, 0.9f, 0.9f, 0.9f, texLeaves)
                "animal" -> {
                    drawBoxTextured(entity.x, 0.6f, entity.z, 1.2f, 0.8f, 0.7f, 0.9f, 0.9f, 0.9f, texWood)
                    drawBoxTextured(entity.x + 0.7f, 0.9f, entity.z, 0.5f, 0.4f, 0.4f, 0.9f, 0.9f, 0.9f, texWood)
                }
                "snake" -> drawBox(entity.x, 0.1f, entity.z, 0.8f, 0.15f, 0.2f, 0.1f, 0.5f, 0.1f)
                "monster" -> {
                    drawBoxTextured(entity.x, 0.9f, entity.z, 0.6f, 1.8f, 0.6f, 1f, 1f, 1f, texMonster)
                    drawBoxTextured(entity.x, 1.9f, entity.z, 0.4f, 0.4f, 0.4f, 0.9f, 0.9f, 0.9f, texRock)
                    drawBoxTextured(entity.x + 0.4f, 1.0f, entity.z, 0.08f, 0.08f, 1.6f, 0.9f, 0.9f, 0.9f, texWood)
                }
                "boss" -> {
                    drawBoxTextured(entity.x, 2.2f, entity.z, 1.4f, 4.2f, 1.4f, 1f, 1f, 1f, texMonster)
                    drawBox(entity.x, 4.4f, entity.z, 0.9f, 0.9f, 0.9f, 1.0f, 0.85f, 0.0f)
                    drawBoxTextured(entity.x + 0.9f, 2.5f, entity.z, 0.2f, 0.2f, 3.5f, 0.9f, 0.9f, 0.9f, texWood)
                }
                "fish" -> drawBox(entity.x, entity.y, entity.z, 0.5f, 0.2f, 0.15f, 0.9f, 0.5f, 0.2f)
            }
        }

        // 3. Render World Base Structures
        for (struct in game.worldStructures) {
            when (struct.type) {
                "campfire" -> {
                    drawBoxTextured(struct.x, 0.1f, struct.z, 1.2f, 0.2f, 1.2f, 0.9f, 0.9f, 0.9f, texRock)
                    if (struct.isLit) {
                        val fireFlicker = (sin(now / 100f) * 0.1f).toFloat()
                        drawBox(struct.x, 0.5f + fireFlicker, struct.z, 0.6f, 0.8f + fireFlicker, 0.6f, 1.0f, 0.4f, 0.0f)
                    }
                }
                "shelter" -> drawBoxTextured(struct.x, 1.2f, struct.z, 2.5f, 2.4f, 2.0f, 0.9f, 0.9f, 0.9f, texWood)
                "log_wall" -> drawBoxTextured(struct.x, 1.5f, struct.z, 3.0f, 3.0f, 0.4f, 0.9f, 0.9f, 0.9f, texWood)
                "gate" -> {
                    drawBoxTextured(struct.x - 1.2f, 1.5f, struct.z, 0.4f, 3.0f, 0.4f, 0.9f, 0.9f, 0.9f, texWood)
                    drawBoxTextured(struct.x + 1.2f, 1.5f, struct.z, 0.4f, 3.0f, 0.4f, 0.9f, 0.9f, 0.9f, texWood)
                    drawBoxTextured(struct.x, 1.5f, struct.z, 2.0f, 2.6f, 0.2f, 0.9f, 0.9f, 0.9f, texWood)
                }
                "leaf_bed" -> drawBoxTextured(struct.x, 0.2f, struct.z, 1.8f, 0.3f, 2.2f, 0.9f, 0.9f, 0.9f, texLeaves)
                "chest" -> drawBoxTextured(struct.x, 0.4f, struct.z, 1.0f, 0.8f, 0.8f, 0.9f, 0.9f, 0.9f, texWood)
                "spike_trap" -> drawBoxTextured(struct.x, 0.3f, struct.z, 1.5f, 0.6f, 1.5f, 0.9f, 0.9f, 0.9f, texWood)
                "water_collector" -> {
                    drawBoxTextured(struct.x, 0.6f, struct.z, 1.2f, 1.2f, 1.2f, 0.9f, 0.9f, 0.9f, texWood)
                    drawBoxTextured(struct.x, 0.9f, struct.z, 1.0f, 0.2f, 1.0f, 0.9f, 0.9f, 1.0f, texWater)
                }
                "drying_rack" -> drawBoxTextured(struct.x, 1.0f, struct.z, 1.8f, 2.0f, 0.4f, 0.9f, 0.9f, 0.9f, texWood)
            }
        }

        // 4. Render Rain Particles
        if (game.isRaining) {
            for (drop in rainDrops) {
                drop[1] -= deltaTime * 12f
                if (drop[1] < 0f) {
                    drop[1] = 15f
                    drop[0] = game.playerX + (Math.random().toFloat() - 0.5f) * 30f
                    drop[2] = game.playerZ + (Math.random().toFloat() - 0.5f) * 30f
                }
                drawBox(drop[0], drop[1], drop[2], 0.05f, 0.4f, 0.05f, 0.7f, 0.8f, 1.0f)
            }
        }

        // 5. Render Held Weapon/Tool in First-Person View
        renderHeldFirstPersonItem()
    }

    private fun updateCameraView() {
        val radYaw = Math.toRadians(game.playerYaw.toDouble())
        val radPitch = Math.toRadians(game.playerPitch.toDouble())

        val dirX = (sin(radYaw) * cos(radPitch)).toFloat()
        val dirY = sin(radPitch).toFloat()
        val dirZ = (-cos(radYaw) * cos(radPitch)).toFloat()

        Matrix.setLookAtM(
            viewMatrix, 0,
            game.playerX, game.playerY, game.playerZ,
            game.playerX + dirX, game.playerY + dirY, game.playerZ + dirZ,
            0f, 1f, 0f
        )
    }

    private fun renderHeldFirstPersonItem() {
        val activeStack = game.hotbar[game.selectedHotbarIndex] ?: return
        val item = activeStack.item

        if (isSwinging) {
            swingProgress += 0.15f
            if (swingProgress >= 1f) {
                swingProgress = 0f
                isSwinging = false
            }
        }

        val swingOffset = sin(swingProgress * Math.PI).toFloat() * 0.4f

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, game.playerX, game.playerY, game.playerZ)
        Matrix.rotateM(modelMatrix, 0, -game.playerYaw, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, game.playerPitch, 1f, 0f, 0f)

        Matrix.translateM(modelMatrix, 0, 0.4f, -0.3f - swingOffset, -0.6f + swingOffset * 0.2f)

        when (item.id) {
            Items.STONE_AXE.id, Items.OBSIDIAN_AXE.id -> {
                drawBoxTransformTextured(0f, 0f, 0f, 0.05f, 0.6f, 0.05f, 0.9f, 0.9f, 0.9f, texWood)
                drawBoxTransformTextured(0.08f, 0.25f, 0f, 0.22f, 0.15f, 0.08f, 0.9f, 0.9f, 0.9f, texRock)
            }
            Items.WOODEN_SPEAR.id, Items.BONE_SPEAR.id -> {
                drawBoxTransformTextured(0f, 0f, -0.3f, 0.04f, 0.04f, 1.4f, 0.9f, 0.9f, 0.9f, texWood)
                drawBoxTransformTextured(0f, 0f, -1.0f, 0.06f, 0.06f, 0.3f, 0.9f, 0.9f, 0.9f, texRock)
            }
            Items.SURVIVAL_BOW.id -> {
                drawBoxTransformTextured(-0.1f, 0f, 0f, 0.04f, 0.8f, 0.04f, 0.9f, 0.9f, 0.9f, texWood)
                drawBoxTransform(-0.1f, 0f, -0.2f, 0.02f, 0.02f, 0.7f, 0.9f, 0.9f, 0.9f)
            }
            Items.FIRE_TORCH.id -> {
                drawBoxTransformTextured(0f, 0f, 0f, 0.06f, 0.7f, 0.06f, 0.9f, 0.9f, 0.9f, texWood)
                drawBoxTransform(0f, 0.4f, 0f, 0.12f, 0.2f, 0.12f, 1.0f, 0.5f, 0.0f)
            }
            Items.COCONUT_CANTEEN.id -> drawBoxTransformTextured(0f, 0f, 0f, 0.18f, 0.22f, 0.18f, 0.9f, 0.9f, 0.9f, texWood)
            Items.LEAF_BANDAGE.id, Items.ANTIVENOM_BANDAGE.id -> drawBoxTransformTextured(0f, 0f, 0f, 0.2f, 0.1f, 0.15f, 0.9f, 0.9f, 0.9f, texLeaves)
            else -> drawBoxTransform(0f, 0f, 0f, 0.12f, 0.12f, 0.12f, 0.8f, 0.7f, 0.2f)
        }
    }

    private fun drawBox(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        GLES30.glUniform1i(uUseTextureLoc, 0)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(uColorLoc, r, g, b, 1.0f)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, cubeIndexCount, GLES30.GL_UNSIGNED_SHORT, cubeIndexBuffer)
    }

    private fun drawBoxTextured(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, textureId: Int) {
        GLES30.glUniform1i(uUseTextureLoc, 1)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(uTextureLoc, 0)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(uColorLoc, r, g, b, 1.0f)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, cubeIndexCount, GLES30.GL_UNSIGNED_SHORT, cubeIndexBuffer)
    }

    private fun drawBoxTransform(tx: Float, ty: Float, tz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        GLES30.glUniform1i(uUseTextureLoc, 0)
        val tempMat = FloatArray(16)
        Matrix.setIdentityM(tempMat, 0)
        Matrix.translateM(tempMat, 0, tx, ty, tz)
        Matrix.scaleM(tempMat, 0, sx, sy, sz)

        val finalModel = FloatArray(16)
        Matrix.multiplyMM(finalModel, 0, modelMatrix, 0, tempMat, 0)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, finalModel, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(uColorLoc, r, g, b, 1.0f)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, cubeIndexCount, GLES30.GL_UNSIGNED_SHORT, cubeIndexBuffer)
    }

    private fun drawBoxTransformTextured(tx: Float, ty: Float, tz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, textureId: Int) {
        GLES30.glUniform1i(uUseTextureLoc, 1)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(uTextureLoc, 0)

        val tempMat = FloatArray(16)
        Matrix.setIdentityM(tempMat, 0)
        Matrix.translateM(tempMat, 0, tx, ty, tz)
        Matrix.scaleM(tempMat, 0, sx, sy, sz)

        val finalModel = FloatArray(16)
        Matrix.multiplyMM(finalModel, 0, modelMatrix, 0, tempMat, 0)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, finalModel, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(uColorLoc, r, g, b, 1.0f)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, cubeIndexCount, GLES30.GL_UNSIGNED_SHORT, cubeIndexBuffer)
    }

    fun handlePrimaryAction() {
        isSwinging = true
        swingProgress = 0f

        val activeStack = game.hotbar[game.selectedHotbarIndex]
        val activeItem = activeStack?.item

        val targetId = targetObjectId
        val targetType = targetObjectType

        if (targetId != -1 && targetType != null) {
            val entity = entities.firstOrNull { it.id == targetId }
            if (entity != null) {
                when (targetType) {
                    "tree", "palm" -> {
                        game.addItem(Items.LOG, 2)
                        game.addItem(Items.STICK, 2)
                        game.addItem(Items.PALM_LEAF, 1)
                        if (Math.random() < 0.3) game.addItem(Items.LONG_STICK, 1)
                        game.showToast("+2 Kmeň dreva, +2 Palica, +1 Palmový list")
                    }
                    "bush" -> {
                        game.addItem(Items.FIBER, 2)
                        game.addItem(Items.MOLINERIA_LEAF, 1)
                        entities.remove(entity)
                        game.showToast("+2 Rastlinné vlákno, +1 Molineria")
                    }
                    "tobacco" -> {
                        game.addItem(Items.TOBACCO_LEAF, 2)
                        entities.remove(entity)
                        game.showToast("+2 Tabakový list zozbieraný!")
                    }
                    "rock" -> {
                        game.addItem(Items.STONE, 2)
                        if (Math.random() < 0.2) game.addItem(Items.OBSIDIAN, 1)
                        entities.remove(entity)
                        game.showToast("+2 Kameň zozbieraný!")
                    }
                    "banana" -> {
                        game.addItem(Items.BANANA, 2)
                        entities.remove(entity)
                        game.showToast("+2 Divoký banán zozbieraný!")
                    }
                    "coconut" -> {
                        game.addItem(Items.COCONUT, 1)
                        entities.remove(entity)
                        game.showToast("+1 Kokosový orech zozbieraný!")
                    }
                    "animal" -> {
                        if (activeItem?.id == Items.WOODEN_SPEAR.id || activeItem?.id == Items.BONE_SPEAR.id || activeItem?.id == Items.SURVIVAL_BOW.id) {
                            if (activeItem.id == Items.SURVIVAL_BOW.id) game.removeItem(Items.ARROW, 1)
                            game.addItem(Items.RAW_MEAT, 2)
                            game.addItem(Items.BONE, 1)
                            entities.remove(entity)
                            game.showToast("Ulovil si zviera! +2 Surové mäso, +1 Kosť")
                        } else {
                            game.showToast("Na lov zveri potrebuješ kopiju alebo luk!")
                        }
                    }
                    "monster" -> {
                        var dmg = activeItem?.damage ?: 10f
                        if (game.isPerkUnlocked("hunter_instinct") && (activeItem?.id == Items.WOODEN_SPEAR.id || activeItem?.id == Items.BONE_SPEAR.id || activeItem?.id == Items.SURVIVAL_BOW.id)) {
                            dmg *= 1.3f
                        }

                        entity.health -= dmg

                        if (entity.health <= 0f) {
                            entities.remove(entity)
                            game.addItem(Items.BONE, 3)
                            game.addItem(Items.OBSIDIAN, 1)
                            game.sanity = (game.sanity + 15f).coerceAtMost(100f)
                            game.addXP(40)
                            game.showToast("☠️ Porazil si Kmeňové Monštrum! (+40 XP)")
                        } else {
                            game.showToast("Zásah monštra! HP Monštra: ${entity.health.toInt()}")
                        }
                    }
                    "boss" -> {
                        var dmg = activeItem?.damage ?: 10f
                        if (game.isPerkUnlocked("hunter_instinct") && (activeItem?.id == Items.BONE_SPEAR.id || activeItem?.id == Items.SURVIVAL_BOW.id)) {
                            dmg *= 1.3f
                        }

                        entity.health -= dmg
                        game.bossHealth = entity.health

                        if (entity.health <= 0f) {
                            entities.remove(entity)
                            game.isBossDefeated = true
                            game.addXP(200)
                            game.sanity = 100f
                            game.showToast("🏆 PORAZIL SI PRASTARÉHO ŠAMANA DŽUNGLE! VYHRAL SI KAMPAŇ!")
                        } else {
                            game.showToast("💥 Zásah Bossa! HP Šamana: ${entity.health.toInt()} / 300")
                        }
                    }
                    "snake" -> {
                        game.addItem(Items.RAW_MEAT, 1)
                        entities.remove(entity)
                        game.showToast("Zabil si hada! +1 Surové mäso")
                    }
                    "fish" -> {
                        if (activeItem?.id == Items.WOODEN_SPEAR.id || activeItem?.id == Items.BONE_SPEAR.id) {
                            game.addItem(Items.RAW_FISH, 1)
                            entities.remove(entity)
                            game.showToast("Ulovil si rybu v rieke! +1 Čerstvá ryba")
                        } else {
                            game.showToast("Na lov rýb v rieke potrebuješ kopiju!")
                        }
                    }
                }
            }
        } else {
            val yawRad = Math.toRadians(game.playerYaw.toDouble())
            val spawnX = game.playerX + sin(yawRad).toFloat() * 2f
            val spawnZ = game.playerZ - cos(yawRad).toFloat() * 2f

            when (activeItem?.id) {
                Items.CAMPFIRE_ITEM.id -> {
                    game.worldStructures.add(com.example.test.game.WorldStructure("struct_${System.currentTimeMillis()}", "campfire", spawnX, 0f, spawnZ, isLit = true, hasItemInProcess = true))
                    game.removeItem(Items.CAMPFIRE_ITEM, 1)
                    game.showToast("Postavil si a zapálil Ohnisko!")
                }
                Items.SHELTER_ITEM.id -> {
                    game.worldStructures.add(com.example.test.game.WorldStructure("struct_${System.currentTimeMillis()}", "shelter", spawnX, 0f, spawnZ))
                    game.removeItem(Items.SHELTER_ITEM, 1)
                    game.showToast("Postavil si Prístrešok na spanie!")
                }
                Items.LOG_WALL_ITEM.id -> {
                    game.worldStructures.add(com.example.test.game.WorldStructure("struct_${System.currentTimeMillis()}", "log_wall", spawnX, 0f, spawnZ))
                    game.removeItem(Items.LOG_WALL_ITEM, 1)
                    game.showToast("Postavil si Stenu Základne z Kmeňov!")
                }
                Items.GATE_ITEM.id -> {
                    game.worldStructures.add(com.example.test.game.WorldStructure("struct_${System.currentTimeMillis()}", "gate", spawnX, 0f, spawnZ))
                    game.removeItem(Items.GATE_ITEM, 1)
                    game.showToast("Postavil si Vstupnú Bránu Základne!")
                }
                Items.LEAF_BED_ITEM.id -> {
                    game.worldStructures.add(com.example.test.game.WorldStructure("struct_${System.currentTimeMillis()}", "leaf_bed", spawnX, 0f, spawnZ))
                    game.removeItem(Items.LEAF_BED_ITEM, 1)
                    game.showToast("Postavil si Posteľ z Listov v búde!")
                }
                Items.STORAGE_CHEST_ITEM.id -> {
                    game.worldStructures.add(com.example.test.game.WorldStructure("struct_${System.currentTimeMillis()}", "chest", spawnX, 0f, spawnZ))
                    game.removeItem(Items.STORAGE_CHEST_ITEM, 1)
                    game.showToast("Postavil si Úložnú Truhlicu!")
                }
                Items.SPIKE_TRAP_ITEM.id -> {
                    game.worldStructures.add(com.example.test.game.WorldStructure("struct_${System.currentTimeMillis()}", "spike_trap", spawnX, 0f, spawnZ))
                    game.removeItem(Items.SPIKE_TRAP_ITEM, 1)
                    game.showToast("Položil si Ostnatú Pascu na Monštrá!")
                }
                Items.WATER_COLLECTOR_ITEM.id -> {
                    game.worldStructures.add(com.example.test.game.WorldStructure("struct_${System.currentTimeMillis()}", "water_collector", spawnX, 0f, spawnZ))
                    game.removeItem(Items.WATER_COLLECTOR_ITEM, 1)
                    game.showToast("Postavil si Zberač Dažďovej Vody!")
                }
                Items.DRYING_RACK_ITEM.id -> {
                    game.worldStructures.add(com.example.test.game.WorldStructure("struct_${System.currentTimeMillis()}", "drying_rack", spawnX, 0f, spawnZ, hasItemInProcess = true))
                    game.removeItem(Items.DRYING_RACK_ITEM, 1)
                    game.showToast("Postavil si Sušiak na Mäso!")
                }
            }
        }
    }
}
