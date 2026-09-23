package com.example.test.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Item Categories
enum class ItemCategory(val displayName: String) {
    TOOL("Nástroje"),
    WEAPON("Zbrane"),
    FOOD("Jedlo & Voda"),
    MATERIAL("Suroviny"),
    MEDICAL("Medicína"),
    STRUCTURE("Stavby")
}

// Item Definition
data class ItemType(
    val id: String,
    val name: String,
    val description: String,
    val category: ItemCategory,
    val iconEmoji: String,
    val maxStack: Int = 10,
    val carbsGain: Float = 0f,
    val proteinGain: Float = 0f,
    val fatGain: Float = 0f,
    val hydrationGain: Float = 0f,
    val healthGain: Float = 0f,
    val isEquippable: Boolean = false,
    val damage: Float = 10f
)

object Items {
    val STICK = ItemType("stick", "Drevená palica", "Základné drevo zo stromu.", ItemCategory.MATERIAL, "🪵")
    val LONG_STICK = ItemType("long_stick", "Dlhá palica", "Silná dlhá vetva na zbrane a stavby.", ItemCategory.MATERIAL, "🦯")
    val STONE = ItemType("stone", "Kameň", "Tvrdý kameň z rieky.", ItemCategory.MATERIAL, "🪨")
    val SHARP_STONE = ItemType("sharp_stone", "Ostrý kameň", "Ostrezaný kameň vhodný na rezanie.", ItemCategory.TOOL, "🔪", isEquippable = true, damage = 15f)
    val FIBER = ItemType("fiber", "Rastlinné vlákno", "Pevné vlákno z liany.", ItemCategory.MATERIAL, "🌿")
    val RESIN = ItemType("resin", "Živica", "Horľavá smola zo stromov.", ItemCategory.MATERIAL, "💧")
    val PALM_LEAF = ItemType("palm_leaf", "Palmový list", "Veľký list na prístrešky.", ItemCategory.MATERIAL, "🍃")
    val MOLINERIA_LEAF = ItemType("molineria", "List Molinerie", "Liečivá rastlina s hojivým účinkom.", ItemCategory.MEDICAL, "🌱")
    val BANANA = ItemType("banana", "Divoký banán", "Bohatý na sacharidy.", ItemCategory.FOOD, "🍌", carbsGain = 30f, hydrationGain = 10f)
    val COCONUT = ItemType("coconut", "Kokosový orech", "Obsahuje tuky a sviežu vodu.", ItemCategory.FOOD, "🥥", fatGain = 25f, hydrationGain = 20f)
    val RAW_MEAT = ItemType("raw_meat", "Surové mäso", "Surové mäso z lovu. Môže spôsobiť parazity!", ItemCategory.FOOD, "🥩", proteinGain = 35f, healthGain = -10f)
    val COOKED_MEAT = ItemType("cooked_meat", "Upečené mäso", "Upečené mäso plné bielkovín a energie.", ItemCategory.FOOD, "🍖", proteinGain = 50f, fatGain = 20f, healthGain = 20f)
    val COCONUT_CANTEEN = ItemType("canteen", "Čutora z kokosu", "Čerstvá čista pitná voda.", ItemCategory.FOOD, "🧉", hydrationGain = 50f, isEquippable = true)
    
    val STONE_AXE = ItemType("stone_axe", "Kamená sekera", "Nástroj na rúbanie stromov a obrana.", ItemCategory.TOOL, "🪓", isEquippable = true, damage = 35f)
    val WOODEN_SPEAR = ItemType("wooden_spear", "Drevená kopija", "Smrtiaca zbraň na lov zvody a obrana.", ItemCategory.WEAPON, "🗡️", isEquippable = true, damage = 50f)
    val FIRE_TORCH = ItemType("fire_torch", "Hooriaca fakľa", "Osvetľuje temnú džungľu v noci.", ItemCategory.TOOL, "🔥", isEquippable = true, damage = 15f)
    val LEAF_BANDAGE = ItemType("bandage", "Bylinkový obväz", "Lieči rany, popáleniny a uštipnutia.", ItemCategory.MEDICAL, "🩹", healthGain = 40f, isEquippable = true)
    
    val CAMPFIRE_ITEM = ItemType("campfire_item", "Stavebnica Ohniska", "Miesto na varenie a teplo.", ItemCategory.STRUCTURE, "🏕️")
    val SHELTER_ITEM = ItemType("shelter_item", "Prístrešok", "Bezpečné miesto na spánok a uloženie gry.", ItemCategory.STRUCTURE, "🛖")

    val ALL = listOf(
        STICK, LONG_STICK, STONE, SHARP_STONE, FIBER, RESIN, PALM_LEAF, MOLINERIA_LEAF,
        BANANA, COCONUT, RAW_MEAT, COOKED_MEAT, COCONUT_CANTEEN, STONE_AXE, WOODEN_SPEAR,
        FIRE_TORCH, LEAF_BANDAGE, CAMPFIRE_ITEM, SHELTER_ITEM
    )

    fun getById(id: String): ItemType = ALL.firstOrNull { it.id == id } ?: STICK
}

data class ItemStack(
    val item: ItemType,
    var count: Int
)

data class RecipeRequirement(
    val item: ItemType,
    val count: Int
)

data class CraftingRecipe(
    val resultItem: ItemType,
    val resultCount: Int = 1,
    val requirements: List<RecipeRequirement>
)

object CraftingRecipes {
    val ALL = listOf(
        CraftingRecipe(Items.SHARP_STONE, 1, listOf(RecipeRequirement(Items.STONE, 2))),
        CraftingRecipe(Items.STONE_AXE, 1, listOf(RecipeRequirement(Items.STICK, 2), RecipeRequirement(Items.STONE, 1), RecipeRequirement(Items.FIBER, 1))),
        CraftingRecipe(Items.WOODEN_SPEAR, 1, listOf(RecipeRequirement(Items.LONG_STICK, 1), RecipeRequirement(Items.SHARP_STONE, 1))),
        CraftingRecipe(Items.FIRE_TORCH, 1, listOf(RecipeRequirement(Items.STICK, 1), RecipeRequirement(Items.FIBER, 1), RecipeRequirement(Items.RESIN, 1))),
        CraftingRecipe(Items.LEAF_BANDAGE, 1, listOf(RecipeRequirement(Items.MOLINERIA_LEAF, 2))),
        CraftingRecipe(Items.COCONUT_CANTEEN, 1, listOf(RecipeRequirement(Items.COCONUT, 1), RecipeRequirement(Items.FIBER, 1))),
        CraftingRecipe(Items.CAMPFIRE_ITEM, 1, listOf(RecipeRequirement(Items.STICK, 4), RecipeRequirement(Items.STONE, 4))),
        CraftingRecipe(Items.SHELTER_ITEM, 1, listOf(RecipeRequirement(Items.LONG_STICK, 4), RecipeRequirement(Items.PALM_LEAF, 6)))
    )
}

// Body Part for Body Inspection system
enum class BodyPart(val displayName: String) {
    LEFT_ARM("Ľavá ruka"),
    RIGHT_ARM("Pravá ruka"),
    LEFT_LEG("Ľavá noha"),
    RIGHT_LEG("Pravá noha")
}

data class BodyAffliction(
    val bodyPart: BodyPart,
    val type: String, // "Leech", "Cut", "SnakeBite"
    val description: String,
    val requiredTreatment: String // "Hand" or "Bandage"
)

// Placed World Object
data class WorldStructure(
    val id: String,
    val type: String, // "campfire", "shelter"
    val x: Float,
    val y: Float,
    val z: Float,
    var isLit: Boolean = false,
    var hasMeatCooking: Boolean = false,
    var cookTime: Float = 0f
)

// Main Game Engine State
class SurvivalGame(private val context: Context) {
    // Vitals (Green Hell System)
    var health: Float = 100f
    var stamina: Float = 100f
    var hydration: Float = 80f
    var carbs: Float = 70f
    var protein: Float = 60f
    var fat: Float = 50f
    var parasites: Int = 0
    var poisonLevel: Float = 0f

    // Day & Night Cycle (Day length = 600s = 10 min)
    var timeOfDay: Float = 10.0f // 10:00 AM start
    var dayCount: Int = 1

    // Inventory & Hotbar (6 Slots)
    val inventory = mutableListOf<ItemStack>()
    val hotbar = Array<ItemStack?>(6) { null }
    var selectedHotbarIndex: Int = 0

    // Body Afflictions
    val afflictions = mutableListOf<BodyAffliction>()

    // Placed World Structures
    val worldStructures = mutableListOf<WorldStructure>()

    // Player position in 3D world
    var playerX = 0f
    var playerY = 1.6f
    var playerZ = 0f
    var playerYaw = 0f
    var playerPitch = 0f

    // Toast message for UI
    var currentToast: String? = null
    var toastTimer: Float = 0f

    init {
        // Starter Survival Items
        addItem(Items.STONE_AXE, 1)
        addItem(Items.BANANA, 3)
        addItem(Items.COCONUT, 2)
        addItem(Items.MOLINERIA_LEAF, 2)
        
        // Auto equip axe in hotbar
        hotbar[0] = ItemStack(Items.STONE_AXE, 1)
        hotbar[1] = ItemStack(Items.WOODEN_SPEAR, 1)
        hotbar[2] = ItemStack(Items.FIRE_TORCH, 1)

        // Initial affliction: 1 Leech on left arm
        afflictions.add(BodyAffliction(BodyPart.LEFT_ARM, "Leech", "Prisatá pijavica pije tvoju krv!", "Hand"))
    }

    fun showToast(msg: String) {
        currentToast = msg
        toastTimer = 2.5f
    }

    fun update(deltaTime: Float) {
        // Update Toast
        if (toastTimer > 0f) {
            toastTimer -= deltaTime
            if (toastTimer <= 0f) currentToast = null
        }

        // Time flow: 1 real second = 2.4 in-game minutes (24h = 10 real minutes)
        timeOfDay += (deltaTime * (24f / 600f))
        if (timeOfDay >= 24f) {
            timeOfDay -= 24f
            dayCount++
            showToast("Prežil si $dayCount. deň v džungli!")
        }

        // Vitals drain over time
        hydration = (hydration - deltaTime * 0.15f).coerceIn(0f, 100f)
        carbs = (carbs - deltaTime * 0.1f).coerceIn(0f, 100f)
        protein = (protein - deltaTime * 0.08f).coerceIn(0f, 100f)
        fat = (fat - deltaTime * 0.05f).coerceIn(0f, 100f)

        // Affliction effects
        if (afflictions.isNotEmpty()) {
            health -= deltaTime * 0.2f * afflictions.size
        }

        // Dehydration or starvation health drain
        if (hydration <= 0f || (carbs <= 0f && protein <= 0f)) {
            health -= deltaTime * 0.5f
        }

        // Stamina regeneration
        if (hydration > 20f && carbs > 10f) {
            stamina = (stamina + deltaTime * 5f).coerceIn(0f, 100f)
        }

        health = health.coerceIn(0f, 100f)

        // Cook meat on campfires
        for (struct in worldStructures) {
            if (struct.type == "campfire" && struct.isLit && struct.hasMeatCooking) {
                struct.cookTime += deltaTime
                if (struct.cookTime >= 10f) { // 10s cook time
                    struct.hasMeatCooking = false
                    struct.cookTime = 0f
                    addItem(Items.COOKED_MEAT, 1)
                    showToast("Mäso je upečené! Uložené do batohu.")
                }
            }
        }
    }

    fun addItem(item: ItemType, count: Int): Boolean {
        val existing = inventory.firstOrNull { it.item.id == item.id && it.count < item.maxStack }
        if (existing != null) {
            existing.count += count
        } else {
            inventory.add(ItemStack(item, count))
        }

        // Auto populate hotbar if space available
        for (i in hotbar.indices) {
            if (hotbar[i] == null && item.isEquippable) {
                hotbar[i] = ItemStack(item, 1)
                break
            }
        }
        return true
    }

    fun removeItem(item: ItemType, count: Int): Boolean {
        var remaining = count
        val iterator = inventory.iterator()
        while (iterator.hasNext()) {
            val stack = iterator.next()
            if (stack.item.id == item.id) {
                if (stack.count > remaining) {
                    stack.count -= remaining
                    remaining = 0
                    break
                } else {
                    remaining -= stack.count
                    iterator.remove()
                }
            }
        }
        return remaining == 0
    }

    fun getCount(item: ItemType): Int {
        return inventory.filter { it.item.id == item.id }.sumOf { it.count }
    }

    fun consumeItem(item: ItemType) {
        if (removeItem(item, 1)) {
            carbs = (carbs + item.carbsGain).coerceIn(0f, 100f)
            protein = (protein + item.proteinGain).coerceIn(0f, 100f)
            fat = (fat + item.fatGain).coerceIn(0f, 100f)
            hydration = (hydration + item.hydrationGain).coerceIn(0f, 100f)
            health = (health + item.healthGain).coerceIn(0f, 100f)
            
            showToast("Zjedol/vypil si: ${item.name}")

            // Also check if consuming item reduces hotbar count
            val slot = hotbar[selectedHotbarIndex]
            if (slot != null && slot.item.id == item.id) {
                slot.count--
                if (slot.count <= 0) hotbar[selectedHotbarIndex] = null
            }
        }
    }

    fun canCraft(recipe: CraftingRecipe): Boolean {
        return recipe.requirements.all { req -> getCount(req.item) >= req.count }
    }

    fun craft(recipe: CraftingRecipe): Boolean {
        if (!canCraft(recipe)) return false
        recipe.requirements.forEach { req -> removeItem(req.item, req.count) }
        addItem(recipe.resultItem, recipe.resultCount)
        showToast("Vyrobil si: ${recipe.resultItem.name}")
        return true
    }

    fun treatAffliction(affliction: BodyAffliction, toolItem: ItemType?) {
        if (affliction.requiredTreatment == "Hand" || (toolItem != null && toolItem.id == Items.LEAF_BANDAGE.id)) {
            if (toolItem?.id == Items.LEAF_BANDAGE.id) {
                removeItem(Items.LEAF_BANDAGE, 1)
            }
            afflictions.remove(affliction)
            health = (health + 15f).coerceIn(0f, 100f)
            showToast("Ošetril si ${affliction.bodyPart.displayName}!")
        } else {
            showToast("Na toto ošetrenie potrebuješ obväz!")
        }
    }

    fun sleepInShelter() {
        timeOfDay = 7.0f // Wake up at 7:00 AM
        dayCount++
        stamina = 100f
        health = (health + 30f).coerceIn(0f, 100f)
        showToast("Vyspal si sa do nového dňa! Hra bola uložená.")
        saveGame()
    }

    fun saveGame() {
        try {
            val prefs = context.getSharedPreferences("green_hell_save", Context.MODE_PRIVATE)
            val json = JSONObject().apply {
                put("health", health)
                put("stamina", stamina)
                put("hydration", hydration)
                put("carbs", carbs)
                put("protein", protein)
                put("fat", fat)
                put("dayCount", dayCount)
                put("timeOfDay", timeOfDay)
                put("playerX", playerX)
                put("playerY", playerY)
                put("playerZ", playerZ)
            }
            prefs.edit().putString("save_data", json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadGame() {
        try {
            val prefs = context.getSharedPreferences("green_hell_save", Context.MODE_PRIVATE)
            val str = prefs.getString("save_data", null) ?: return
            val json = JSONObject(str)
            health = json.optDouble("health", 100.0).toFloat()
            stamina = json.optDouble("stamina", 100.0).toFloat()
            hydration = json.optDouble("hydration", 80.0).toFloat()
            carbs = json.optDouble("carbs", 70.0).toFloat()
            protein = json.optDouble("protein", 60.0).toFloat()
            fat = json.optDouble("fat", 50.0).toFloat()
            dayCount = json.optInt("dayCount", 1)
            timeOfDay = json.optDouble("timeOfDay", 10.0).toFloat()
            playerX = json.optDouble("playerX", 0.0).toFloat()
            playerY = json.optDouble("playerY", 1.6).toFloat()
            playerZ = json.optDouble("playerZ", 0.0).toFloat()
            showToast("Uložená hra načítaná!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
