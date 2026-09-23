package com.example.test.gl

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
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

    // World Entities (Trees, Rocks, Bushes, Animals)
    data class WorldEntity(
        val id: Int,
        val type: String, // "tree", "palm", "bush", "rock", "animal", "banana", "coconut"
        var x: Float,
        var y: Float,
        var z: Float,
        var scale: Float = 1f,
        var health: Float = 100f
    )

    val entities = mutableListOf<WorldEntity>()
    private var lastTimeMs: Long = System.currentTimeMillis()

    init {
        generateJungleWorld()
    }

    private fun generateJungleWorld() {
        var entityId = 1
        // Generate dense Amazonian jungle world around origin
        for (i in -15..15) {
            for (j in -15..15) {
                if (i * i + j * j < 9) continue // Clear space around player spawn

                val rx = i * 4f + (Math.random().toFloat() - 0.5f) * 2f
                val rz = j * 4f + (Math.random().toFloat() - 0.5f) * 2f
                val typeVal = Math.random()

                when {
                    typeVal < 0.35 -> entities.add(WorldEntity(entityId++, "palm", rx, 0f, rz, scale = 1f + Math.random().toFloat() * 0.5f))
                    typeVal < 0.60 -> entities.add(WorldEntity(entityId++, "tree", rx, 0f, rz, scale = 1.2f + Math.random().toFloat() * 0.8f))
                    typeVal < 0.75 -> entities.add(WorldEntity(entityId++, "bush", rx, 0f, rz, scale = 0.8f))
                    typeVal < 0.88 -> entities.add(WorldEntity(entityId++, "rock", rx, 0f, rz, scale = 0.5f + Math.random().toFloat() * 0.4f))
                    typeVal < 0.94 -> entities.add(WorldEntity(entityId++, "banana", rx, 0f, rz, scale = 0.6f))
                    else -> entities.add(WorldEntity(entityId++, "coconut", rx, 0f, rz, scale = 0.5f))
                }
            }
        }

        // Add 3d Animals (Tapir / Jaguar wandering)
        entities.add(WorldEntity(entityId++, "animal", 8f, 0f, 6f, scale = 1f))
        entities.add(WorldEntity(entityId++, "animal", -10f, 0f, -8f, scale = 0.9f))
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
            // Front face
            -0.5f, -0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            // Back face
            -0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
            // Top face
            -0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f,  0.5f, -0.5f,
            // Bottom face
            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,
            // Right face
             0.5f, -0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
            // Left face
            -0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f, -0.5f
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
            0, 1, 2,  0, 2, 3,
            4, 5, 6,  4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23
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

        // Sky Color based on Day/Night Cycle
        val hour = game.timeOfDay
        val sunFactor = sin((hour - 6f) / 12f * Math.PI).toFloat().coerceIn(0f, 1f)
        val skyRed = 0.1f + 0.4f * sunFactor
        val skyGreen = 0.15f + 0.5f * sunFactor
        val skyBlue = 0.3f + 0.6f * sunFactor
        GLES30.glClearColor(skyRed, skyGreen, skyBlue, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(programId)

        // Light setup (Sun position)
        val sunAngle = (hour / 24f) * Math.PI * 2.0
        val lightX = sin(sunAngle).toFloat() * 50f
        val lightY = cos(sunAngle).toFloat() * 50f + 20f
        val lightZ = 20f
        GLES30.glUniform3f(uLightPosLoc, lightX, lightY, lightZ)
        GLES30.glUniform3f(uLightColorLoc, 1.0f, 0.95f, 0.85f)

        // Set Vertex attributes
        GLES30.glEnableVertexAttribArray(aPositionLoc)
        GLES30.glVertexAttribPointer(aPositionLoc, 3, GLES30.GL_FLOAT, false, 0, cubeVertexBuffer)

        GLES30.glEnableVertexAttribArray(aNormalLoc)
        GLES30.glVertexAttribPointer(aNormalLoc, 3, GLES30.GL_FLOAT, false, 0, cubeNormalBuffer)

        // 1. Draw Terrain Ground (Jungle Grass)
        drawBox(0f, -0.5f, 0f, 120f, 0.1f, 120f, 0.15f, 0.45f, 0.15f)

        // 2. Draw River Stream
        drawBox(0f, -0.45f, -10f, 120f, 0.05f, 8f, 0.1f, 0.4f, 0.8f)

        // 3. Render World Entities
        targetObjectType = null
        targetObjectId = -1
        var minTargetDist = 4.0f // Target interaction range

        val entityIterator = entities.iterator()
        while (entityIterator.hasNext()) {
            val entity = entityIterator.next()

            // Animal Movement
            if (entity.type == "animal") {
                entity.x += sin(now / 1000f + entity.id) * 0.02f
                entity.z += cos(now / 1000f + entity.id) * 0.02f
            }

            // Check distance to player for targeting
            val dx = entity.x - game.playerX
            val dz = entity.z - game.playerZ
            val dist = kotlin.math.sqrt(dx * dx + dz * dz)

            if (dist < minTargetDist) {
                minTargetDist = dist
                targetObjectType = entity.type
                targetObjectId = entity.id
            }

            // Render object by type
            when (entity.type) {
                "tree" -> {
                    // Trunk
                    drawBox(entity.x, 2.5f * entity.scale, entity.z, 0.6f * entity.scale, 5f * entity.scale, 0.6f * entity.scale, 0.4f, 0.25f, 0.1f)
                    // Leaves
                    drawBox(entity.x, 5.5f * entity.scale, entity.z, 3f * entity.scale, 2.5f * entity.scale, 3f * entity.scale, 0.1f, 0.5f, 0.15f)
                }
                "palm" -> {
                    // Curved Trunk
                    drawBox(entity.x, 3f * entity.scale, entity.z, 0.5f * entity.scale, 6f * entity.scale, 0.5f * entity.scale, 0.45f, 0.3f, 0.12f)
                    // Palm Top
                    drawBox(entity.x, 6f * entity.scale, entity.z, 4f * entity.scale, 0.3f * entity.scale, 4f * entity.scale, 0.15f, 0.6f, 0.1f)
                }
                "bush" -> drawBox(entity.x, 0.6f, entity.z, 1.5f, 1.2f, 1.5f, 0.2f, 0.6f, 0.2f)
                "rock" -> drawBox(entity.x, 0.3f, entity.z, 0.8f * entity.scale, 0.6f * entity.scale, 0.8f * entity.scale, 0.5f, 0.5f, 0.5f)
                "banana" -> drawBox(entity.x, 0.3f, entity.z, 0.4f, 0.3f, 0.4f, 0.9f, 0.85f, 0.1f)
                "coconut" -> drawBox(entity.x, 0.2f, entity.z, 0.35f, 0.35f, 0.35f, 0.35f, 0.2f, 0.05f)
                "animal" -> {
                    // 3D Animal Body & Head
                    drawBox(entity.x, 0.6f, entity.z, 1.2f, 0.8f, 0.7f, 0.5f, 0.35f, 0.2f)
                    drawBox(entity.x + 0.7f, 0.9f, entity.z, 0.5f, 0.4f, 0.4f, 0.6f, 0.4f, 0.25f)
                }
            }
        }

        // 4. Render Placed Structures (Campfires & Shelters)
        for (struct in game.worldStructures) {
            if (struct.type == "campfire") {
                // Stone Ring
                drawBox(struct.x, 0.1f, struct.z, 1.2f, 0.2f, 1.2f, 0.4f, 0.4f, 0.4f)
                // Fire Flame
                if (struct.isLit) {
                    val fireFlicker = (sin(now / 100f) * 0.1f).toFloat()
                    drawBox(struct.x, 0.5f + fireFlicker, struct.z, 0.6f, 0.8f + fireFlicker, 0.6f, 1.0f, 0.4f, 0.0f)
                }
            } else if (struct.type == "shelter") {
                // Wooden Lean-To Frame
                drawBox(struct.x, 1.2f, struct.z, 2.5f, 2.4f, 2.0f, 0.25f, 0.55f, 0.15f)
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

        // First-Person Camera Space Transformation
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, game.playerX, game.playerY, game.playerZ)
        Matrix.rotateM(modelMatrix, 0, -game.playerYaw, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, game.playerPitch, 1f, 0f, 0f)

        // Offset to bottom right of screen (Holding in hand)
        Matrix.translateM(modelMatrix, 0, 0.4f, -0.3f - swingOffset, -0.6f + swingOffset * 0.2f)

        // Draw 3D Held Item Model
        when (item.id) {
            Items.STONE_AXE.id -> {
                // Wooden Handle
                drawBoxTransform(0f, 0f, 0f, 0.05f, 0.6f, 0.05f, 0.4f, 0.25f, 0.1f)
                // Stone Axe Head
                drawBoxTransform(0.08f, 0.25f, 0f, 0.22f, 0.15f, 0.08f, 0.5f, 0.5f, 0.5f)
            }
            Items.WOODEN_SPEAR.id -> {
                // Long Shaft
                drawBoxTransform(0f, 0f, -0.3f, 0.04f, 0.04f, 1.4f, 0.45f, 0.3f, 0.12f)
                // Spear Tip
                drawBoxTransform(0f, 0f, -1.0f, 0.06f, 0.06f, 0.3f, 0.6f, 0.6f, 0.6f)
            }
            Items.FIRE_TORCH.id -> {
                // Torch Stick
                drawBoxTransform(0f, 0f, 0f, 0.06f, 0.7f, 0.06f, 0.4f, 0.25f, 0.1f)
                // Torch Flame
                drawBoxTransform(0f, 0.4f, 0f, 0.12f, 0.2f, 0.12f, 1.0f, 0.5f, 0.0f)
            }
            Items.COCONUT_CANTEEN.id -> drawBoxTransform(0f, 0f, 0f, 0.18f, 0.22f, 0.18f, 0.35f, 0.2f, 0.05f)
            Items.LEAF_BANDAGE.id -> drawBoxTransform(0f, 0f, 0f, 0.2f, 0.1f, 0.15f, 0.2f, 0.7f, 0.2f)
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

        // Interact with targeted 3D entity
        val targetId = targetObjectId
        val targetType = targetObjectType

        if (targetId != -1 && targetType != null) {
            val entity = entities.firstOrNull { it.id == targetId }
            if (entity != null) {
                when (targetType) {
                    "tree", "palm" -> {
                        game.addItem(Items.STICK, 2)
                        game.addItem(Items.PALM_LEAF, 1)
                        if (Math.random() < 0.3) game.addItem(Items.LONG_STICK, 1)
                        game.showToast("+2 Drevená palica, +1 Palmový list")
                        SoundManager.playChopSound()
                    }
                    "bush" -> {
                        game.addItem(Items.FIBER, 2)
                        game.addItem(Items.MOLINERIA_LEAF, 1)
                        entities.remove(entity)
                        game.showToast("+2 Rastlinné vlákno, +1 Molineria")
                    }
                    "rock" -> {
                        game.addItem(Items.STONE, 2)
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
                        if (activeItem?.id == Items.WOODEN_SPEAR.id) {
                            game.addItem(Items.RAW_MEAT, 2)
                            entities.remove(entity)
                            game.showToast("Ulovil si zviera! +2 Surové mäso")
                        } else {
                            game.showToast("Na lov zveri potrebuješ kopiju!")
                        }
                    }
                }
            }
        } else {
            // Build structures if structure item active
            if (activeItem?.id == Items.CAMPFIRE_ITEM.id) {
                game.worldStructures.add(
                    com.example.test.game.WorldStructure(
                        "struct_${System.currentTimeMillis()}",
                        "campfire",
                        game.playerX + sin(Math.toRadians(game.playerYaw.toDouble())).toFloat() * 2f,
                        0f,
                        game.playerZ - cos(Math.toRadians(game.playerYaw.toDouble())).toFloat() * 2f,
                        isLit = true,
                        hasMeatCooking = true
                    )
                )
                game.removeItem(Items.CAMPFIRE_ITEM, 1)
                game.showToast("Postavil si a zapálil Ohnisko! Mäso sa varí.")
            } else if (activeItem?.id == Items.SHELTER_ITEM.id) {
                game.worldStructures.add(
                    com.example.test.game.WorldStructure(
                        "struct_${System.currentTimeMillis()}",
                        "shelter",
                        game.playerX + sin(Math.toRadians(game.playerYaw.toDouble())).toFloat() * 2.5f,
                        0f,
                        game.playerZ - cos(Math.toRadians(game.playerYaw.toDouble())).toFloat() * 2.5f
                    )
                )
                game.removeItem(Items.SHELTER_ITEM, 1)
                game.showToast("Postavil si Prístrešok na spanie!")
            }
        }
    }
}
