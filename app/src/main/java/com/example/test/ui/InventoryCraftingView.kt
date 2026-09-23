package com.example.test.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.test.game.CraftingRecipe
import com.example.test.game.CraftingRecipes
import com.example.test.game.ItemStack
import com.example.test.game.SurvivalGame

@Composable
fun InventoryCraftingView(
    game: SurvivalGame,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableStateOf("inventory") } // "inventory" or "crafting"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0F1A12)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF19261B))
                .border(2.dp, Color(0xFF3F5E43), RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { activeTab = "inventory" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == "inventory") Color(0xFF3F5E43) else Color(0xFF233325)
                    )
                ) {
                    Text("🎒 BATOH (${game.inventory.size})", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { activeTab = "crafting" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == "crafting") Color(0xFF3F5E43) else Color(0xFF233325)
                    )
                ) {
                    Text("🛠️ VÝROBA & RECEPTY", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                if (activeTab == "inventory") {
                    InventoryGrid(game)
                } else {
                    CraftingGrid(game)
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { game.saveGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D4B31))
                ) {
                    Text("💾 Uložiť Hru", color = Color.White)
                }

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B3A3A))
                ) {
                    Text("Zatvoriť Batoh", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun InventoryGrid(game: SurvivalGame) {
    var selectedStack by remember { mutableStateOf<ItemStack?>(null) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Items Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(game.inventory) { stack ->
                val isSelected = stack == selectedStack
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF3F5E43) else Color(0xFF131D15))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFFFFD700) else Color(0xFF2A3D2D),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedStack = stack },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stack.item.iconEmoji, fontSize = 24.sp)
                        Text("x${stack.count}", color = Color.LightGray, fontSize = 10.sp)
                    }
                }
            }
        }

        // Details Panel
        Box(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .padding(start = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131D15))
                .padding(12.dp)
        ) {
            val item = selectedStack?.item
            if (item != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.description, color = Color.LightGray, fontSize = 12.sp)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.carbsGain > 0 || item.proteinGain > 0 || item.hydrationGain > 0) {
                            Button(
                                onClick = {
                                    game.consumeItem(item)
                                    selectedStack = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A35))
                            ) {
                                Text("🍽️ Zjesť / Vypiť", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        if (item.isEquippable) {
                            Button(
                                onClick = {
                                    game.hotbar[game.selectedHotbarIndex] = selectedStack
                                    game.showToast("Predmet vybavený do lišty!")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B5998))
                            ) {
                                Text("✋ Vybaviť", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                Text("Vyber predmet na zobrazenie detailov", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CraftingGrid(game: SurvivalGame) {
    var selectedRecipe by remember { mutableStateOf<CraftingRecipe?>(null) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Recipe List
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CraftingRecipes.ALL) { recipe ->
                val canCraft = game.canCraft(recipe)
                val isSelected = recipe == selectedRecipe

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF3F5E43) else Color(0xFF131D15))
                        .border(
                            1.dp,
                            if (canCraft) Color(0xFF4CAF50) else Color(0xFF555555),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedRecipe = recipe }
                        .padding(10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(recipe.resultItem.iconEmoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(recipe.resultItem.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(if (canCraft) "✅ Možné vyrobiť" else "❌ Chýbajú suroviny", color = if (canCraft) Color.Green else Color.Red, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Requirements & Craft Button
        Box(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .padding(start = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131D15))
                .padding(12.dp)
        ) {
            val recipe = selectedRecipe
            if (recipe != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Recept: ${recipe.resultItem.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Potrebné suroviny:", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        recipe.requirements.forEach { req ->
                            val currentCount = game.getCount(req.item)
                            val hasEnough = currentCount >= req.count
                            Text(
                                text = "• ${req.item.name}: $currentCount / ${req.count}",
                                color = if (hasEnough) Color.Green else Color.Red,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { game.craft(recipe) },
                        enabled = game.canCraft(recipe),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("⚒️ Vyrobiť Predmet", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("Vyber recept zo zoznamu", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
