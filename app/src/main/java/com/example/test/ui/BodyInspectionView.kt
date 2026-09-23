package com.example.test.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.game.BodyPart
import com.example.test.game.Items
import com.example.test.game.SurvivalGame

@Composable
fun BodyInspectionView(
    game: SurvivalGame,
    onClose: () -> Unit
) {
    var selectedPart by remember { mutableStateOf(BodyPart.LEFT_ARM) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE111812)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E2820))
                .border(2.dp, Color(0xFF4A6B50), RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "KONTROLA TELA & RÁN",
                    color = Color(0xFFFFD700),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Skontroluj si končatiny na pijavice, rany a uštipnutia",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            // Body Part Selection Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BodyPart.values().forEach { part ->
                    val isSelected = part == selectedPart
                    val hasAffliction = game.afflictions.any { it.bodyPart == part }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(0xFF4A6B50) else Color(0xFF2A382C)
                            )
                            .border(
                                width = if (hasAffliction) 2.dp else 1.dp,
                                color = if (hasAffliction) Color.Red else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedPart = part }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${part.displayName}${if (hasAffliction) " 🩸" else ""}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Limb Inspection Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF162018))
                    .border(1.dp, Color(0xFF334A38), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val partAfflictions = game.afflictions.filter { it.bodyPart == selectedPart }

                if (partAfflictions.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${selectedPart.displayName} je čistá a zdravá!",
                            color = Color(0xFF88FF88),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        partAfflictions.forEach { aff ->
                            Text(
                                text = "⚠️ ${aff.description}",
                                color = Color(0xFFFF6666),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Action Buttons to Treat Wound
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (aff.requiredTreatment == "Hand") {
                                    Button(
                                        onClick = { game.treatAffliction(aff, null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
                                    ) {
                                        Text("✋ Odtrhnúť Pijavicu rukou", color = Color.White)
                                    }
                                }

                                val bandageCount = game.getCount(Items.LEAF_BANDAGE)
                                Button(
                                    onClick = { game.treatAffliction(aff, Items.LEAF_BANDAGE) },
                                    enabled = bandageCount > 0,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8B57))
                                ) {
                                    Text("🩹 Priložiť Obväz ($bandageCount)", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Close Button
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B50)),
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Text("Návrat do hry", color = Color.White)
            }
        }
    }
}
