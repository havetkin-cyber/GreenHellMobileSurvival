package com.example.test.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
            .size(100.dp)
            .clip(CircleShape)
            .background(Color(0xCC111F13))
            .border(2.dp, Color(0xFFFFD700), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(90.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val scale = 2.5f // 1 meter = 2.5 canvas pixels

            // Draw North Indicator Line
            val radYaw = Math.toRadians(game.playerYaw.toDouble())
            val northX = center.x + sin(-radYaw).toFloat() * (size.width / 2f - 8f)
            val northY = center.y - cos(-radYaw).toFloat() * (size.height / 2f - 8f)
            drawLine(Color.Yellow, center, Offset(northX, northY), strokeWidth = 3f)

            // Draw World Entities on Minimap
            renderer?.entities?.forEach { entity ->
                val dx = (entity.x - game.playerX) * scale
                val dz = (entity.z - game.playerZ) * scale

                // Rotate offset relative to player yaw
                val rotX = (dx * cos(radYaw) - dz * sin(radYaw)).toFloat()
                val rotY = (dx * sin(radYaw) + dz * cos(radYaw)).toFloat()

                val point = Offset(center.x + rotX, center.y + rotY)

                // Only draw if inside minimap circle
                if ((point - center).getDistance() < size.width / 2f - 6f) {
                    val color = when (entity.type) {
                        "monster" -> Color.Red
                        "snake" -> Color(0xFFFF6600)
                        "animal" -> Color.White
                        "tree", "palm" -> Color(0xFF2E8B57)
                        else -> Color.Gray
                    }
                    val radius = if (entity.type == "monster") 5f else 3f
                    drawCircle(color, radius = radius, center = point)
                }
            }

            // Draw Player Marker at Center
            drawCircle(Color(0xFF00FFFF), radius = 4f, center = center)
        }
    }
}
