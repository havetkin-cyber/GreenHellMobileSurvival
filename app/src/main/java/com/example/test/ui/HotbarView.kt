package com.example.test.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun HotbarView(
    game: SurvivalGame,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 6) {
            val slot = game.hotbar[i]
            val isSelected = (i == selectedIndex)

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) Color(0xDD382814) else Color(0xAA1E1B18)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFFFFD700) else Color(0x88888888),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectIndex(i) },
                contentAlignment = Alignment.Center
            ) {
                if (slot != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = slot.item.iconEmoji,
                            fontSize = 24.sp
                        )
                        if (slot.count > 1) {
                            Text(
                                text = "x${slot.count}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "${i + 1}",
                        color = Color(0x66FFFFFF),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
