package com.example.test.gl

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.test.game.BodyAffliction
import com.example.test.game.BodyPart
import com.example.test.game.Items
import com.example.test.game.SoundManager
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

    // Shaders
    private var programId: Int = 0
    private var uMVPMatrixLoc: Int = 0
    private var uLightPosLoc: Int = 0
    private var uLightColorLoc: Int = 0
    private var uColorLoc: Int = 0
    private var aPositionLoc: Int = 0
    private var aNormalLoc: Int = 0

    // Mesh Buffers
    private lateinit var cubeVertexBuffer: FloatBuffer
    private lateinit var cubeNormalBuffer: FloatBuffer
    private lateinit var cubeIndexBuffer: ShortBuffer
    private var cubeIndexCount: Int = 0

    // Player Animation & Actions
    var isSwinging: Boolean = false
    private var swingProgress: Float = 0f

    // Interactive Raycast Targeting
    var targetObjectType: String? = null
    var targetObjectId: Int = -1

    // World Entities (Trees, Rocks, Bushes, Animals, Snakes, Fish, Monsters, Ancient Boss)
    data class WorldEntity(
        val id: Int,
        val type: String, // "tree", "palm", "bush", "rock", "animal", "snake", "fish", "monster", "boss", "banana", "coconut", "tobacco"
        var x: Float,
        var y: Float,
        var z: Float,
        var scale: Float = 1f,
        var health: Float = 100f,
        var attackTimer: Float = 0f
    )

    val entities = mutableListOf<WorldEntity>()
    private var lastTimeMs: Long = System.currentTimeMillis()

    // Rain Particles
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

        // Add Animals & Tribal Cannibals
        entities.add(WorldEntity(entityId++, "animal", 8f, 0f, 6f, scale = 1f))
        entities.add(WorldEntity(entityId++, "animal", -10f, 0f, -8f, scale = 0.9f))
        entities.add(WorldEntity(entityId++, "snake", 4f, 0f, 2f, scale = 0.8f))
        entities.add(WorldEntity(entityId++, "snake", -6f, 0f, 5f, scale = 0.8f))

        // Monsters
        entities.add(WorldEntity(entityId++, "monster", 14f, 0f, 12f, scale = 1.1f, health = 120f))
        entities.add(WorldEntity(entityId++, "monster", -12f, 0f, 14f, scale = 1.1f, health = 120f))

        // Giant Ancient Boss in Ruins Biome (At coords X: 25, Z: -25)
        entities.add(WorldEntity(entityId++, "boss", 25f, 0f, -25f, scale = 2.2f, health = game.bossHealth))

        // River Fish
        entities.add(WorldEntity(entityId++, "fish", 0f, -0.4f, -10f, scale = 0.5f))
        entities.add(WorldEntity(entityId++, "fish", 8f, -0.4f, -9.5f, scale = 0.5f))
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.2f, 0.5f, 0.8f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)

        initShaders()
        initCubeBuffers()
    }

    private fun initShaders() {
        val vertexShaderCode = """
            #version 300 es
            uniform mat4 uMVPMatrix;
            in vec3 aPosition;
            in vec3 aNormal;
            out vec3 vNormal;
            out vec3 vPosition;
            
            void main() {
                vPosition = aPosition;
                vNormal = aNormal;
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """.trimIndent()

        val fragmentShaderCode = """
            #version 300 es
            precision mediump float;
            uniform vec3 uLightPos;
            uniform vec3 uLightColor;
            uniform vec4 uColor;
            in vec3 vNormal;
            in vec3 vPosition;
            out vec4 fragColor;
            
            void main() {
                vec3 norm = normalize(vNormal);
                vec3 lightDir = normalize(uLightPos - vPosition);
                float diff = max(dot(norm, lightDir), 0.3);
                vec3 diffuse = diff * uLightColor;
                fragColor = vec4(uColor.rgb * diffuse, uColor.a);
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
        aPositionLoc = GLES30.glGetAttribLocation(programId, "aPosition")
        aNormalLoc = GLES30.glGetAttribLocation(programId, "aNormal")
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

        // Sky Color based on Day/Night & Weather
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

        // Light setup
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

        // 1. Terrain & River
        drawBox(0f, -0.5f, 0f, 120f, 0.1f, 120f, 0.15f, 0.45f, 0.15f)
        drawBox(0f, -0.45f, -10f, 120f, 0.05f, 8f, 0.1f, 0.4f, 0.8f)

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
                            SoundManager.playHitSound()
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
                                SoundManager.playHitSound()
                            }
                        }
                    }
                }
                "boss" -> {
                    // Giant Ancient Boss AI & Attack Slam
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
                                game.applyDamageToPlayer(40f) // Massive boss damage
                                SoundManager.playHitSound()
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
                    drawBox(entity.x, 2.5f * entity.scale, entity.z, 0.6f * entity.scale, 5f * entity.scale, 0.6f * entity.scale, 0.4f, 0.25f, 0.1f)
                    drawBox(entity.x, 5.5f * entity.scale, entity.z, 3f * entity.scale, 2.5f * entity.scale, 3f * entity.scale, 0.1f, 0.5f, 0.15f)
                }
                "palm" -> {
                    drawBox(entity.x, 3f * entity.scale, entity.z, 0.5f * entity.scale, 6f * entity.scale, 0.5f * entity.scale, 0.45f, 0.3f, 0.12f)
                    drawBox(entity.x, 6f * entity.scale, entity.z, 4f * entity.scale, 0.3f * entity.scale, 4f * entity.scale, 0.15f, 0.6f, 0.1f)
                }
                "bush" -> drawBox(entity.x, 0.6f, entity.z, 1.5f, 1.2f, 1.5f, 0.2f, 0.6f, 0.2f)
                "rock" -> drawBox(entity.x, 0.3f, entity.z, 0.8f * entity.scale, 0.6f * entity.scale, 0.8f * entity.scale, 0.5f, 0.5f, 0.5f)
                "banana" -> drawBox(entity.x, 0.3f, entity.z, 0.4f, 0.3f, 0.4f, 0.9f, 0.85f, 0.1f)
                "coconut" -> drawBox(entity.x, 0.2f, entity.z, 0.35f, 0.35f, 0.35f, 0.35f, 0.2f, 0.05f)
                "tobacco" -> drawBox(entity.x, 0.4f, entity.z, 0.6f, 0.8f, 0.6f, 0.2f, 0.7f, 0.3f)
                "animal" -> {
                    drawBox(entity.x, 0.6f, entity.z, 1.2f, 0.8f, 0.7f, 0.5f, 0.35f, 0.2f)
                    drawBox(entity.x + 0.7f, 0.9f, entity.z, 0.5f, 0.4f, 0.4f, 0.6f, 0.4f, 0.25f)
                }
                "snake" -> drawBox(entity.x, 0.1f, entity.z, 0.8f, 0.15f, 0.2f, 0.1f, 0.5f, 0.1f)
                "monster" -> {
                    drawBox(entity.x, 0.9f, entity.z, 0.6f, 1.8f, 0.6f, 0.3f, 0.2f, 0.15f)
                    drawBox(entity.x, 1.9f, entity.z, 0.4f, 0.4f, 0.4f, 0.9f, 0.9f, 0.8f)
                    drawBox(entity.x + 0.4f, 1.0f, entity.z, 0.08f, 0.08f, 1.6f, 0.5f, 0.3f, 0.1f)
                }
                "boss" -> {
                    // Giant Ancient Boss 3D Mesh
                    drawBox(entity.x, 2.2f, entity.z, 1.4f, 4.2f, 1.4f, 0.8f, 0.2f, 0.1f) // Giant Red Body
                    drawBox(entity.x, 4.4f, entity.z, 0.9f, 0.9f, 0.9f, 1.0f, 0.85f, 0.0f) // Gold Crown Skull Head
                    drawBox(entity.x + 0.9f, 2.5f, entity.z, 0.2f, 0.2f, 3.5f, 0.9f, 0.7f, 0.1f) // Giant Staff
                }
                "fish" -> drawBox(entity.x, entity.y, entity.z, 0.5f, 0.2f, 0.15f, 0.9f, 0.5f, 0.2f)
            }
        }

        // 3. Render World Base Structures
        for (struct in game.worldStructures) {
            when (struct.type) {
                "campfire" -> {
                    drawBox(struct.x, 0.1f, struct.z, 1.2f, 0.2f, 1.2f, 0.4f, 0.4f, 0.4f)
                    if (struct.isLit) {
                        val fireFlicker = (sin(now / 100f) * 0.1f).toFloat()
                        drawBox(struct.x, 0.5f + fireFlicker, struct.z, 0.6f, 0.8f + fireFlicker, 0.6f, 1.0f, 0.4f, 0.0f)
                    }
                }
                "shelter" -> drawBox(struct.x, 1.2f, struct.z, 2.5f, 2.4f, 2.0f, 0.25f, 0.55f, 0.15f)
                "log_wall" -> drawBox(struct.x, 1.5f, struct.z, 3.0f, 3.0f, 0.4f, 0.4f, 0.25f, 0.1f)
                "gate" -> {
                    drawBox(struct.x - 1.2f, 1.5f, struct.z, 0.4f, 3.0f, 0.4f, 0.3f, 0.2f, 0.1f)
                    drawBox(struct.x + 1.2f, 1.5f, struct.z, 0.4f, 3.0f, 0.4f, 0.3f, 0.2f, 0.1f)
                    drawBox(struct.x, 1.5f, struct.z, 2.0f, 2.6f, 0.2f, 0.5f, 0.3f, 0.15f)
                }
                "leaf_bed" -> drawBox(struct.x, 0.2f, struct.z, 1.8f, 0.3f, 2.2f, 0.15f, 0.6f, 0.15f)
                "chest" -> drawBox(struct.x, 0.4f, struct.z, 1.0f, 0.8f, 0.8f, 0.45f, 0.3f, 0.15f)
                "spike_trap" -> drawBox(struct.x, 0.3f, struct.z, 1.5f, 0.6f, 1.5f, 0.6f, 0.1f, 0.1f)
                "water_collector" -> {
                    drawBox(struct.x, 0.6f, struct.z, 1.2f, 1.2f, 1.2f, 0.3f, 0.4f, 0.3f)
                    drawBox(struct.x, 0.9f, struct.z, 1.0f, 0.2f, 1.0f, 0.1f, 0.5f, 0.9f)
                }
                "drying_rack" -> drawBox(struct.x, 1.0f, struct.z, 1.8f, 2.0f, 0.4f, 0.45f, 0.3f, 0.15f)
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
                drawBoxTransform(0f, 0f, 0f, 0.05f, 0.6f, 0.05f, 0.4f, 0.25f, 0.1f)
                drawBoxTransform(0.08f, 0.25f, 0f, 0.22f, 0.15f, 0.08f, 0.5f, 0.5f, 0.5f)
            }
            Items.WOODEN_SPEAR.id, Items.BONE_SPEAR.id -> {
                drawBoxTransform(0f, 0f, -0.3f, 0.04f, 0.04f, 1.4f, 0.45f, 0.3f, 0.12f)
                drawBoxTransform(0f, 0f, -1.0f, 0.06f, 0.06f, 0.3f, 0.6f, 0.6f, 0.6f)
            }
            Items.SURVIVAL_BOW.id -> {
                drawBoxTransform(-0.1f, 0f, 0f, 0.04f, 0.8f, 0.04f, 0.5f, 0.3f, 0.1f)
                drawBoxTransform(-0.1f, 0f, -0.2f, 0.02f, 0.02f, 0.7f, 0.9f, 0.9f, 0.9f)
            }
            Items.FIRE_TORCH.id -> {
                drawBoxTransform(0f, 0f, 0f, 0.06f, 0.7f, 0.06f, 0.4f, 0.25f, 0.1f)
                drawBoxTransform(0f, 0.4f, 0f, 0.12f, 0.2f, 0.12f, 1.0f, 0.5f, 0.0f)
            }
            Items.COCONUT_CANTEEN.id -> drawBoxTransform(0f, 0f, 0f, 0.18f, 0.22f, 0.18f, 0.35f, 0.2f, 0.05f)
            Items.LEAF_BANDAGE.id, Items.ANTIVENOM_BANDAGE.id -> drawBoxTransform(0f, 0f, 0f, 0.2f, 0.1f, 0.15f, 0.2f, 0.7f, 0.2f)
            else -> drawBoxTransform(0f, 0f, 0f, 0.12f, 0.12f, 0.12f, 0.8f, 0.7f, 0.2f)
        }
    }

    private fun drawBox(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
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

        SoundManager.playChopSound()

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
                        SoundManager.playChopSound()
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
                        SoundManager.playHitSound()

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
                        SoundManager.playHitSound()

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
