package com.example.test.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.test.game.GameScreen
import com.example.test.game.SoundManager
import com.example.test.game.SurvivalGame
import com.example.test.gl.SurvivalGLSurfaceView
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SurvivalGameApp(
    game: SurvivalGame,
    onQuitApp: () -> Unit
) {
    var isSmartwatchOpen by remember { mutableStateOf(false) }
    var isBodyInspectionOpen by remember { mutableStateOf(false) }
    var isInventoryOpen by remember { mutableStateOf(false) }
    var isGuideOpen by remember { mutableStateOf(false) }

    var selectedHotbarIndex by remember { mutableIntStateOf(game.selectedHotbarIndex) }
    var glView by remember { mutableStateOf<SurvivalGLSurfaceView?>(null) }

    when (game.currentScreen) {
        GameScreen.MAIN_MENU -> {
            MainMenuView(game = game, onQuitApp = onQuitApp)
        }
        GameScreen.SETTINGS -> {
            SettingsView(game = game)
        }
        GameScreen.PLAYING, GameScreen.PAUSED -> {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. 3D OpenGL Game Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                game.playerYaw += dragAmount.x * game.lookSensitivity
                                game.playerPitch = (game.playerPitch - dragAmount.y * game.lookSensitivity).coerceIn(-80f, 80f)
                            }
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            SurvivalGLSurfaceView(ctx, game).also { glView = it }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. Top Bar: Minimap & Menu Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MinimapOverlayView(game = game, renderer = glView?.renderer)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xCC111B13))
                                .border(1.dp, Color(0xFF3B573F), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("❤️ ${game.health.toInt()}%", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🧠 ${game.sanity.toInt()}%", color = Color(0xFF88CCFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⭐ Lvl ${game.level}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { game.currentScreen = GameScreen.MAIN_MENU },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD8B3A3A)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("🏠 Menu", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = { isGuideOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD3B573F)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("📖 Zápisník", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = { isSmartwatchOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD2D4232)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("⌚ Hodinky", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = { isBodyInspectionOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD3B573F)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("🩸 Rany", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = { isInventoryOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD4A6B50)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("🎒 Batoh", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                // 3. Toast Overlay
                game.currentToast?.let { toast ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 110.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xEE222222))
                            .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(toast, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                // 4. Joystick
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 80.dp)
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0x77000000))
                        .border(2.dp, Color(0xAAFFFFFF), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val radYaw = Math.toRadians(game.playerYaw.toDouble())
                                    val dx = dragAmount.x * 0.03f
                                    val dz = dragAmount.y * 0.03f

                                    game.playerX += (sin(radYaw) * -dz + cos(radYaw) * dx).toFloat()
                                    game.playerZ += (-cos(radYaw) * -dz + sin(radYaw) * dx).toFloat()

                                    SoundManager.playFootstep()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xAA888888))
                    )
                }

                // 5. Action Swing Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 80.dp)
                ) {
                    Button(
                        onClick = {
                            glView?.renderer?.handlePrimaryAction()
                        },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xEE8B0000))
                    ) {
                        Text("🪓", fontSize = 28.sp)
                    }
                }

                // 6. Hotbar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                ) {
                    HotbarView(
                        game = game,
                        selectedIndex = selectedHotbarIndex,
                        onSelectIndex = { idx ->
                            selectedHotbarIndex = idx
                            game.selectedHotbarIndex = idx
                            val item = game.hotbar[idx]?.item
                            if (item != null) {
                                game.showToast("Vybraný predmet: ${item.name}")
                            } else {
                                game.showToast("Prázdny slot ${idx + 1}")
                            }
                        }
                    )
                }

                // 7. Modals
                AnimatedVisibility(visible = isGuideOpen) {
                    SurvivalGuideView(
                        game = game,
                        onClose = { isGuideOpen = false }
                    )
                }

                AnimatedVisibility(visible = isSmartwatchOpen) {
                    SmartwatchView(
                        game = game,
                        onClose = { isSmartwatchOpen = false }
                    )
                }

                AnimatedVisibility(visible = isBodyInspectionOpen) {
                    BodyInspectionView(
                        game = game,
                        onClose = { isBodyInspectionOpen = false }
                    )
                }

                AnimatedVisibility(visible = isInventoryOpen) {
                    InventoryCraftingView(
                        game = game,
                        onClose = { isInventoryOpen = false }
                    )
                }
            }
        }
    }
}
