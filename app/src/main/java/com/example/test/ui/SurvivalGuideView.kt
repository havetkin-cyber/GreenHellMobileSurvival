package com.example.test.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.game.Perks
import com.example.test.game.SurvivalGame

@Composable
fun SurvivalGuideView(
    game: SurvivalGame,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0B140D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF152217))
                .border(2.dp, Color(0xFF3B573F), RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Level & XP Progress
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📖 ZÁPISNÍK PREŽITIA & ZRUČNOSTI",
                    color = Color(0xFFFFD700),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⭐ Level: ${game.level}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val reqXp = game.level * 100
                    Text("XP: ${game.xp} / $reqXp", color = Color(0xFF88FF88), fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                val reqXp = game.level * 100
                LinearProgressIndicator(
                    progress = { (game.xp.toFloat() / reqXp.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFFD700),
                    trackColor = Color(0xFF283A2C)
                )
            }

            // Perks List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(Perks.ALL) { perk ->
                    val isUnlocked = game.isPerkUnlocked(perk.id)
                    val canUnlock = game.level >= perk.reqLevel && !isUnlocked

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isUnlocked) Color(0xFF2B442E) else Color(0xFF1B2A1D))
                            .border(
                                1.dp,
                                if (isUnlocked) Color.Green else if (canUnlock) Color(0xFFFFD700) else Color.Gray,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(perk.iconEmoji, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(perk.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(perk.description, color = Color.LightGray, fontSize = 11.sp)
                                    Text("Požadovaný Level: ${perk.reqLevel}", color = if (canUnlock || isUnlocked) Color.Green else Color.Red, fontSize = 10.sp)
                                }
                            }

                            if (isUnlocked) {
                                Text("✅ Odomknuté", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Button(
                                    onClick = { game.unlockPerk(perk) },
                                    enabled = canUnlock,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Text("Odomknúť", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Close Button
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B573F)),
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Text("Zatvoriť Zápisník", color = Color.White)
            }
        }
    }
}
