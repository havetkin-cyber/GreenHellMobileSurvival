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
import com.example.test.game.SoundManager
import com.example.test.game.SurvivalGame
import com.example.test.gl.SurvivalGLSurfaceView
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SurvivalGameApp(game: SurvivalGame) {
    var isSmartwatchOpen by remember { mutableStateOf(false) }
    var isBodyInspectionOpen by remember { mutableStateOf(false) }
    var isInventoryOpen by remember { mutableStateOf(false) }
    var isGuideOpen by remember { mutableStateOf(false) }

    var selectedHotbarIndex by remember { mutableIntStateOf(game.selectedHotbarIndex) }
    var glView by remember { mutableStateOf<SurvivalGLSurfaceView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 3D OpenGL Game Canvas
        AndroidView(
            factory = { ctx ->
                SurvivalGLSurfaceView(ctx, game).also { glView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Camera Drag Swipe Overlay (Right half of screen)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        game.playerYaw += dragAmount.x * game.lookSensitivity
                        game.playerPitch = (game.playerPitch - dragAmount.y * game.lookSensitivity).coerceIn(-80f, 80f)
                    }
                }
        )

        // 3. Top Status Bar with Compass & Mini-Vitals
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health, Sanity & Level Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xBB111B13))
                    .border(1.dp, Color(0xFF3B573F), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❤️ ${game.health.toInt()}%", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🧠 ${game.sanity.toInt()}%", color = Color(0xFF88CCFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⭐ Lvl ${game.level}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Top Action Menu Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { isGuideOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD3B573F)),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("📖 Zápisník", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = { isSmartwatchOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD2D4232)),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("⌚ Hodinky", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = { isBodyInspectionOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD3B573F)),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("🩸 Rany", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = { isInventoryOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD4A6B50)),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("🎒 Batoh", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // 4. Toast Notification Overlay
        game.currentToast?.let { toast ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xDD222222))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(toast, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        // 5. Left Virtual Movement Joystick
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
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
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xAA888888))
            )
        }

        // 6. Right Main Action / Attack / Swing Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 90.dp)
        ) {
            Button(
                onClick = {
                    glView?.renderer?.handlePrimaryAction()
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD8B0000))
            ) {
                Text("🪓", fontSize = 30.sp)
            }
        }

        // 7. Bottom Hotbar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            HotbarView(
                game = game,
                selectedIndex = selectedHotbarIndex,
                onSelectIndex = { idx ->
                    selectedHotbarIndex = idx
                    game.selectedHotbarIndex = idx
                }
            )
        }

        // 8. Modals Overlays
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
