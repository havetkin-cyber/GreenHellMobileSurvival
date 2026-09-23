package com.example.test.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.game.SurvivalGame

@Composable
fun SmartwatchView(
    game: SurvivalGame,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB000000)),
        contentAlignment = Alignment.Center
    ) {
        // Smartwatch Dial Body
        Column(
            modifier = Modifier
                .size(330.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B241D))
                .border(6.dp, Color(0xFF3E4E40), CircleShape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Time & Day
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val hourInt = game.timeOfDay.toInt()
                val minInt = ((game.timeOfDay - hourInt) * 60).toInt()
                val timeStr = String.format("%02d:%02d", hourInt, minInt)

                Text(
                    text = "NÚDZOVO - INTELIGENTNÉ HODINKY",
                    color = Color(0xFF88AA88),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeStr,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${game.dayCount}. DEŇ PREŽITIA",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Green Hell Macro Gauges (Carbs, Protein, Fat, Water)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Carbs Gauge (Yellow)
                MacroGaugeRow("Sacharidy (C)", game.carbs, Color(0xFFFFCC00))
                // Protein Gauge (Red)
                MacroGaugeRow("Bielkoviny (P)", game.protein, Color(0xFFFF3333))
                // Fat Gauge (Green)
                MacroGaugeRow("Tuky (F)", game.fat, Color(0xFF33CC33))
                // Water Gauge (Blue)
                MacroGaugeRow("Hydratácia (H2O)", game.hydration, Color(0xFF3399FF))
            }

            // Health & Heart Rate Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❤️ Tep: ${(70 + (100 - game.health) * 0.5f).toInt()} BPM",
                    color = if (game.health < 30f) Color.Red else Color.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "🧭 Sever (N)",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            // Close Button
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E4E40)),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Zatvoriť Hodinky", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun MacroGaugeRow(label: String, value: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontSize = 11.sp)
            Text("${value.toInt()}%", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF2B3A2D)
        )
    }
}
