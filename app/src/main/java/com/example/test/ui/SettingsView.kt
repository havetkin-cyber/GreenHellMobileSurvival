package com.example.test.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.game.GameScreen
import com.example.test.game.GraphicsQuality
import com.example.test.game.SurvivalGame

@Composable
fun SettingsView(
    game: SurvivalGame
) {
    var sensitivity by remember { mutableStateOf(game.lookSensitivity) }
    var volume by remember { mutableStateOf(game.soundVolume) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09140C)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xEE142217))
                .border(2.dp, Color(0xFF3F5E43), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Text(
                text = "⚙️ NASTAVENIA HRY",
                color = Color(0xFFFFD700),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // Settings Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Graphics Quality Presets
                Column {
                    Text("🎨 Grafická Kvalita & Rendering:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GraphicsQuality.values().forEach { quality ->
                            val isSelected = quality == game.graphicsQuality
                            Button(
                                onClick = { game.graphicsQuality = quality },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF4CAF50) else Color(0xFF223325)
                                )
                            ) {
                                Text(quality.name, fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }

                // Camera Look Sensitivity
                Column {
                    Text("🎮 Citlivosť Kamery: ${(sensitivity * 100).toInt()}%", color = Color.White, fontSize = 14.sp)
                    Slider(
                        value = sensitivity,
                        onValueChange = {
                            sensitivity = it
                            game.lookSensitivity = it
                        },
                        valueRange = 0.1f..0.8f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFF4CAF50))
                    )
                }

                // Sound Volume
                Column {
                    Text("🔊 Hlasitosť Efektov & Hudby: ${(volume * 100).toInt()}%", color = Color.White, fontSize = 14.sp)
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            game.soundVolume = it
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFF4CAF50))
                    )
                }
            }

            // Back Button
            Button(
                onClick = { game.currentScreen = GameScreen.MAIN_MENU },
                modifier = Modifier.fillMaxWidth(0.6f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F5E43))
            ) {
                Text("Späť do Hlavného Menu", color = Color.White)
            }
        }
    }
}
