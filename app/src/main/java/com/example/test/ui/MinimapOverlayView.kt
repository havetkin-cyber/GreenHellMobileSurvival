package com.example.test.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.game.SurvivalGame
import com.example.test.gl.SurvivalRenderer
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MinimapOverlayView(
    game: SurvivalGame,
    renderer: SurvivalRenderer?
) {
    Box(
        modifier = Modifier
            .size(145.dp)
            .clip(CircleShape)
            .background(Color(0xDD111F13))
            .border(3.dp, Color(0xFFFFD700), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f - 4f
            val scale = 3.2f

            val radYaw = Math.toRadians(game.playerYaw.toDouble())

            drawCircle(Color(0x33FFFFFF), radius = maxRadius * 0.4f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawCircle(Color(0x33FFFFFF), radius = maxRadius * 0.8f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))

            val northX = center.x + sin(-radYaw).toFloat() * (maxRadius - 8f)
            val northY = center.y - cos(-radYaw).toFloat() * (maxRadius - 8f)
            drawLine(Color(0xFFFFD700), center, Offset(northX, northY), strokeWidth = 3f)

            renderer?.entities?.forEach { entity ->
                val dx = (entity.x - game.playerX) * scale
                val dz = (entity.z - game.playerZ) * scale

                val rotX = (dx * cos(radYaw) - dz * sin(radYaw)).toFloat()
                val rotY = (dx * sin(radYaw) + dz * cos(radYaw)).toFloat()

                val point = Offset(center.x + rotX, center.y + rotY)

                if ((point - center).getDistance() < maxRadius) {
                    val color = when (entity.type) {
                        "boss" -> Color(0xFFFF00FF)
                        "monster" -> Color.Red
                        "snake" -> Color(0xFFFF6600)
                        "animal" -> Color.White
                        "tree", "palm" -> Color(0xFF2E8B57)
                        "rock" -> Color.LightGray
                        else -> Color(0xFF88CC88)
                    }
                    val radius = when (entity.type) {
                        "boss" -> 7f
                        "monster" -> 5f
                        "snake" -> 4f
                        else -> 3f
                    }
                    drawCircle(color, radius = radius, center = point)
                }
            }

            game.worldStructures.forEach { struct ->
                val dx = (struct.x - game.playerX) * scale
                val dz = (struct.z - game.playerZ) * scale

                val rotX = (dx * cos(radYaw) - dz * sin(radYaw)).toFloat()
                val rotY = (dx * sin(radYaw) + dz * cos(radYaw)).toFloat()

                val point = Offset(center.x + rotX, center.y + rotY)

                if ((point - center).getDistance() < maxRadius) {
                    val color = when (struct.type) {
                        "campfire" -> Color(0xFFFF8800)
                        "shelter" -> Color(0xFF00FF88)
                        "water_collector" -> Color(0xFF00BFFF)
                        else -> Color.Yellow
                    }
                    drawCircle(color, radius = 5f, center = point)
                }
            }

            drawCircle(Color(0xFF00FFFF), radius = 5f, center = center)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        ) {
            Text("N", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
