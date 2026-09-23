package com.example.test.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.game.GameScreen
import com.example.test.game.SurvivalGame

@Composable
fun MainMenuView(
    game: SurvivalGame,
    onQuitApp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09140C)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xEE142217))
                .border(2.dp, Color(0xFF3F5E43), RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // AAA Title Banner
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🌴 GREEN HELL 3D",
                    color = Color(0xFFFFD700),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "MOBILE SURVIVAL AAA EDITION",
                    color = Color(0xFF88FF88),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Menu Buttons List
            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Start New Game Button
                Button(
                    onClick = { game.startNewGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8B57))
                ) {
                    Text("🚀 NOVÁ HRA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Load Game Button
                Button(
                    onClick = { game.loadGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B573F))
                ) {
                    Text("📂 NAČÍTAŤ HRU", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Settings Button
                Button(
                    onClick = { game.currentScreen = GameScreen.SETTINGS },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D4232))
                ) {
                    Text("⚙️ NASTAVENIA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Quit Game Button
                Button(
                    onClick = onQuitApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B3A3A))
                ) {
                    Text("🚪 ODÍSŤ Z HRY", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Footer Credits
            Text(
                text = "Vyvinuté pre havetkin-cyber • Android 3D Engine",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}
