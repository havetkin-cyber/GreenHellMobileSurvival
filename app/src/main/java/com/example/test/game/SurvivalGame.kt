package com.example.test.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class GameScreen {
    MAIN_MENU,
    PLAYING,
    PAUSED,
    SETTINGS
}

enum class GraphicsQuality(val displayName: String) {
    LOW("Nízka (30 FPS)"),
    MEDIUM("Stredná (Vyvážená)"),
    ULTRA_3D("AAA Ultra 3D (60 FPS)")
}

// Item Categories
enum class ItemCategory(val displayName: String) {
    TOOL("Nástroje"),
    WEAPON("Zbrane"),
    ARMOR("Brnenie"),
    FOOD("Jedlo & Voda"),
    MATERIAL("Suroviny"),
    MEDICAL("Medicína"),
    STRUCTURE("Stavby & Búdy")
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
    val sanityGain: Float = 0f,
    val armorValue: Float = 0f,
    val isEquippable: Boolean = false,
    val damage: Float = 10f
)

object Items {
    val STICK = ItemType("stick", "Drevená palica", "Základné drevo zo stromu.", ItemCategory.MATERIAL, "🪵")
    val LONG_STICK = ItemType("long_stick", "Dlhá palica", "Silná dlhá vetva na zbrane a stavby.", ItemCategory.MATERIAL, "🦯")
    val LOG = ItemType("log", "Kmeň dreva", "Ťažký kmeň na stavbu stien búd.", ItemCategory.MATERIAL, "🪵", maxStack = 5)
    val STONE = ItemType("stone", "Kameň", "Tvrdý kameň z rieky.", ItemCategory.MATERIAL, "🪨")
    val SHARP_STONE = ItemType("sharp_stone", "Ostrý kameň", "Ostrezaný kameň vhodný na rezanie.", ItemCategory.TOOL, "🔪", isEquippable = true, damage = 15f)
    val BONE = ItemType("bone", "Kosť zo zvieraťa", "Pevná kosť z lovu alebo kanibalov.", ItemCategory.MATERIAL, "🦴")
    val OBSIDIAN = ItemType("obsidian", "Obsidián", "Ostré sopečné sklo na elitné nástroje.", ItemCategory.MATERIAL, "💎")
    val FIBER = ItemType("fiber", "Rastlinné vlákno", "Pevné vlákno z liany.", ItemCategory.MATERIAL, "🌿")
    val RESIN = ItemType("resin", "Živica", "Horľavá smola zo stromov.", ItemCategory.MATERIAL, "💧")
    val PALM_LEAF = ItemType("palm_leaf", "Palmový list", "Veľký list na prístrešky.", ItemCategory.MATERIAL, "🍃")

    val MOLINERIA_LEAF = ItemType("molineria", "List Molinerie", "Liečivá rastlina s hojivým účinkom.", ItemCategory.MEDICAL, "🌱")
    val TOBACCO_LEAF = ItemType("tobacco", "Tabakový list", "Utišuje protiváhové travy a jed.", ItemCategory.MEDICAL, "🍃")
    val CHARCOAL = ItemType("charcoal", "Drevené uhlie", "Aktívne uhlie lieči otravu a parazity.", ItemCategory.MEDICAL, "🖤", sanityGain = 5f, healthGain = 10f)
    val HERBAL_TEA = ItemType("herbal_tea", "Bylinkový Čaj", "Znižuje horúčku a lieči infekcie.", ItemCategory.MEDICAL, "☕", hydrationGain = 40f, healthGain = 30f, sanityGain = 15f)

    val BANANA = ItemType("banana", "Divoký banán", "Bohatý na sacharidy.", ItemCategory.FOOD, "🍌", carbsGain = 30f, hydrationGain = 10f)
    val COCONUT = ItemType("coconut", "Kokosový orech", "Obsahuje tuky a sviežu vodu.", ItemCategory.FOOD, "🥥", fatGain = 25f, hydrationGain = 20f)
    val RAW_MEAT = ItemType("raw_meat", "Surové mäso", "Surové mäso z lovu.", ItemCategory.FOOD, "🥩", proteinGain = 35f, healthGain = -10f, sanityGain = -15f)
    val COOKED_MEAT = ItemType("cooked_meat", "Upečené mäso", "Upečené mäso plné bielkovín.", ItemCategory.FOOD, "🍖", proteinGain = 50f, fatGain = 20f, healthGain = 20f, sanityGain = 10f)
    val DRIED_MEAT = ItemType("dried_meat", "Sušené mäso", "Trvanlivé sušené mäso zo sušiaka.", ItemCategory.FOOD, "🥓", proteinGain = 40f, fatGain = 15f, sanityGain = 5f)
    val RAW_FISH = ItemType("raw_fish", "Čerstvá ryba", "Ryba ulovená v rieke.", ItemCategory.FOOD, "🐟", proteinGain = 30f, fatGain = 10f)
    val COCONUT_CANTEEN = ItemType("canteen", "Čutora z kokosu", "Čerstvá čista pitná voda.", ItemCategory.FOOD, "🧉", hydrationGain = 50f, isEquippable = true)

    val STONE_AXE = ItemType("stone_axe", "Kamená sekera", "Nástroj na rúbanie stromov a obrana.", ItemCategory.TOOL, "🪓", isEquippable = true, damage = 35f)
    val OBSIDIAN_AXE = ItemType("obsidian_axe", "Obsidiánová sekera", "Extrémne ostrá sekera s vysokým poškodením.", ItemCategory.TOOL, "🪓", isEquippable = true, damage = 65f)
    val WOODEN_SPEAR = ItemType("wooden_spear", "Drevená kopija", "Smrtiaca zbraň na lov zvody a obrana.", ItemCategory.WEAPON, "🗡️", isEquippable = true, damage = 55f)
    val BONE_SPEAR = ItemType("bone_spear", "Kostená kopija", "Kopija s kosteným hrotom na monštrá.", ItemCategory.WEAPON, "🗡️", isEquippable = true, damage = 80f)
    val SURVIVAL_BOW = ItemType("bow", "Lovci luk", "Strieľa šípy na diaľku na dravcov.", ItemCategory.WEAPON, "🏹", isEquippable = true, damage = 70f)
    val ARROW = ItemType("arrow", "Šíp", "Šíp s kamenným hrotom do luku.", ItemCategory.WEAPON, "🏹", maxStack = 20)
    val FIRE_TORCH = ItemType("fire_torch", "Hooriaca fakľa", "Osvetľuje temnú džungľu v noci.", ItemCategory.TOOL, "🔥", isEquippable = true, damage = 15f)

    val BONE_ARMOR = ItemType("bone_armor", "Kostené brnenie", "Chráni telo pred útokmi monštier.", ItemCategory.ARMOR, "🛡️", armorValue = 40f, isEquippable = true)

    val LEAF_BANDAGE = ItemType("bandage", "Bylinkový obväz", "Lieči rany a uštipnutia.", ItemCategory.MEDICAL, "🩹", healthGain = 40f, isEquippable = true)
    val ANTIVENOM_BANDAGE = ItemType("antivenom_bandage", "Protijedový obväz", "Neutralizuje jed z hadieho uštipnutia.", ItemCategory.MEDICAL, "🧪", healthGain = 50f, isEquippable = true)

    val CAMPFIRE_ITEM = ItemType("campfire_item", "Ohnisko", "Miesto na varenie a teplo.", ItemCategory.STRUCTURE, "🏕️")
    val SHELTER_ITEM = ItemType("shelter_item", "Prístrešok", "Bezpečné miesto na spánok.", ItemCategory.STRUCTURE, "🛖")
    val LOG_WALL_ITEM = ItemType("log_wall_item", "Stena z Kmeňov", "Pevná stena základne proti monštrám.", ItemCategory.STRUCTURE, "🪵")
    val GATE_ITEM = ItemType("gate_item", "Drevená Brána", "Vstupná brána do tvojej búde/základne.", ItemCategory.STRUCTURE, "🚪")
    val LEAF_BED_ITEM = ItemType("leaf_bed_item", "Posteľ z Listov", "Pohodlné lôžko v búde na spánok.", ItemCategory.STRUCTURE, "🛏️")
    val STORAGE_CHEST_ITEM = ItemType("chest_item", "Úložná Truhlica", "Ukladaj svoje zásoby v bezpečia.", ItemCategory.STRUCTURE, "📦")
    val SPIKE_TRAP_ITEM = ItemType("spike_trap_item", "Ostnatá Pasca", "Spôsobí masívne poškodenie monštrám.", ItemCategory.STRUCTURE, "⚔️")
    val WATER_COLLECTOR_ITEM = ItemType("water_collector_item", "Zberač Dažďovej Vody", "Zbiera čerstvú dažďovú vodu.", ItemCategory.STRUCTURE, "🌧️")
    val DRYING_RACK_ITEM = ItemType("drying_rack_item", "Sušiak na Mäso", "Suší mäso na trvanlivé zásoby.", ItemCategory.STRUCTURE, "🥩")

    val ALL = listOf(
        STICK, LONG_STICK, LOG, STONE, SHARP_STONE, BONE, OBSIDIAN, FIBER, RESIN, PALM_LEAF,
        MOLINERIA_LEAF, TOBACCO_LEAF, CHARCOAL, HERBAL_TEA, BANANA, COCONUT, RAW_MEAT, COOKED_MEAT, DRIED_MEAT,
        RAW_FISH, COCONUT_CANTEEN, STONE_AXE, OBSIDIAN_AXE, WOODEN_SPEAR, BONE_SPEAR, SURVIVAL_BOW, ARROW,
        FIRE_TORCH, BONE_ARMOR, LEAF_BANDAGE, ANTIVENOM_BANDAGE, CAMPFIRE_ITEM, SHELTER_ITEM, LOG_WALL_ITEM,
        GATE_ITEM, LEAF_BED_ITEM, STORAGE_CHEST_ITEM, SPIKE_TRAP_ITEM, WATER_COLLECTOR_ITEM, DRYING_RACK_ITEM
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
        CraftingRecipe(Items.OBSIDIAN_AXE, 1, listOf(RecipeRequirement(Items.STICK, 2), RecipeRequirement(Items.OBSIDIAN, 1), RecipeRequirement(Items.FIBER, 2))),
        CraftingRecipe(Items.WOODEN_SPEAR, 1, listOf(RecipeRequirement(Items.LONG_STICK, 1), RecipeRequirement(Items.SHARP_STONE, 1))),
        CraftingRecipe(Items.BONE_SPEAR, 1, listOf(RecipeRequirement(Items.LONG_STICK, 1), RecipeRequirement(Items.BONE, 1), RecipeRequirement(Items.FIBER, 2))),
        CraftingRecipe(Items.SURVIVAL_BOW, 1, listOf(RecipeRequirement(Items.LONG_STICK, 1), RecipeRequirement(Items.FIBER, 2))),
        CraftingRecipe(Items.ARROW, 3, listOf(RecipeRequirement(Items.STICK, 1), RecipeRequirement(Items.SHARP_STONE, 1))),
        CraftingRecipe(Items.BONE_ARMOR, 1, listOf(RecipeRequirement(Items.BONE, 3), RecipeRequirement(Items.FIBER, 2))),
        CraftingRecipe(Items.HERBAL_TEA, 1, listOf(RecipeRequirement(Items.MOLINERIA_LEAF, 1), RecipeRequirement(Items.COCONUT, 1))),
        CraftingRecipe(Items.FIRE_TORCH, 1, listOf(RecipeRequirement(Items.STICK, 1), RecipeRequirement(Items.FIBER, 1), RecipeRequirement(Items.RESIN, 1))),
        CraftingRecipe(Items.LEAF_BANDAGE, 1, listOf(RecipeRequirement(Items.MOLINERIA_LEAF, 2))),
        CraftingRecipe(Items.ANTIVENOM_BANDAGE, 1, listOf(RecipeRequirement(Items.LEAF_BANDAGE, 1), RecipeRequirement(Items.TOBACCO_LEAF, 1))),
        CraftingRecipe(Items.COCONUT_CANTEEN, 1, listOf(RecipeRequirement(Items.COCONUT, 1), RecipeRequirement(Items.FIBER, 1))),
        
        CraftingRecipe(Items.CAMPFIRE_ITEM, 1, listOf(RecipeRequirement(Items.STICK, 4), RecipeRequirement(Items.STONE, 4))),
        CraftingRecipe(Items.SHELTER_ITEM, 1, listOf(RecipeRequirement(Items.LONG_STICK, 4), RecipeRequirement(Items.PALM_LEAF, 6))),
        CraftingRecipe(Items.LOG_WALL_ITEM, 1, listOf(RecipeRequirement(Items.LOG, 4), RecipeRequirement(Items.FIBER, 2))),
        CraftingRecipe(Items.GATE_ITEM, 1, listOf(RecipeRequirement(Items.LOG, 2), RecipeRequirement(Items.STICK, 4), RecipeRequirement(Items.FIBER, 2))),
        CraftingRecipe(Items.LEAF_BED_ITEM, 1, listOf(RecipeRequirement(Items.STICK, 4), RecipeRequirement(Items.PALM_LEAF, 8))),
        CraftingRecipe(Items.STORAGE_CHEST_ITEM, 1, listOf(RecipeRequirement(Items.LOG, 2), RecipeRequirement(Items.FIBER, 4))),
        CraftingRecipe(Items.SPIKE_TRAP_ITEM, 1, listOf(RecipeRequirement(Items.STICK, 6), RecipeRequirement(Items.SHARP_STONE, 2))),
        CraftingRecipe(Items.WATER_COLLECTOR_ITEM, 1, listOf(RecipeRequirement(Items.STICK, 4), RecipeRequirement(Items.PALM_LEAF, 4), RecipeRequirement(Items.COCONUT, 1))),
        CraftingRecipe(Items.DRYING_RACK_ITEM, 1, listOf(RecipeRequirement(Items.LONG_STICK, 4), RecipeRequirement(Items.FIBER, 4)))
    )
}

data class Perk(
    val id: String,
    val name: String,
    val description: String,
    val reqLevel: Int,
    val iconEmoji: String,
    var isUnlocked: Boolean = false
)

object Perks {
    val ALL = listOf(
        Perk("master_lumberjack", "Majster Drevorubač", "Získaj 2x viac dreva zo stromov", 2, "🪓"),
        Perk("hunter_instinct", "Lovecký Inštinkt", "Zvyšuje poškodenie kopijou a lukom o +30%", 3, "🎯"),
        Perk("hardened_skin", "Oceľová Pokožka", "Znižuje zranenie od monštier o -25%", 4, "🛡️"),
        Perk("herbalist", "Bylinkár", "Obväzy liečia o +50% viac HP", 5, "🌿"),
        Perk("iron_stomach", "Železný Žalúdok", "Imunitači proti otrave z jedla a parazitom", 6, "🥩")
    )
}

enum class BodyPart(val displayName: String) {
    LEFT_ARM("Ľavá ruka"),
    RIGHT_ARM("Pravá ruka"),
    LEFT_LEG("Ľavá noha"),
    RIGHT_LEG("Pravá noha")
}

data class BodyAffliction(
    val bodyPart: BodyPart,
    val type: String,
    val description: String,
    val requiredTreatment: String
)

data class WorldStructure(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val z: Float,
    var health: Float = 200f,
    var isLit: Boolean = false,
    var hasItemInProcess: Boolean = false,
    var processProgress: Float = 0f,
    var storedWater: Float = 0f
)

class SurvivalGame(private val context: Context) {
    // Current Active Screen
    var currentScreen: GameScreen = GameScreen.MAIN_MENU

    // Settings
    var graphicsQuality: GraphicsQuality = GraphicsQuality.ULTRA_3D
    var lookSensitivity: Float = 0.25f
    var soundVolume: Float = 0.8f

    // Vitals
    var health: Float = 100f
    var stamina: Float = 100f
    var hydration: Float = 80f
    var carbs: Float = 70f
    var protein: Float = 60f
    var fat: Float = 50f
    var sanity: Float = 100f
    var poisonLevel: Float = 0f
    var bodyTemp: Float = 36.6f
    var parasites: Int = 0

    // Boss State
    var bossHealth: Float = 300f
    var isBossDefeated: Boolean = false

    // XP & Leveling
    var xp: Int = 0
    var level: Int = 1
    val unlockedPerkIds = mutableSetOf<String>()

    // Armor
    var equippedArmorValue: Float = 0f

    // Weather Engine
    var isRaining: Boolean = false
    var rainIntensity: Float = 0f
    var weatherTimer: Float = 0f

    // Day & Night
    var timeOfDay: Float = 10.0f
    var dayCount: Int = 1

    // Inventory & Hotbar (6 Slots)
    val inventory = mutableListOf<ItemStack>()
    val hotbar = Array<ItemStack?>(6) { null }
    var selectedHotbarIndex: Int = 0

    // Afflictions
    val afflictions = mutableListOf<BodyAffliction>()

    // World Structures
    val worldStructures = mutableListOf<WorldStructure>()

    // Player Position & Camera
    var playerX = 0f
    var playerY = 1.6f
    var playerZ = 0f
    var playerYaw = 0f
    var playerPitch = 0f

    // Toast Messages
    var currentToast: String? = null
    var toastTimer: Float = 0f

    init {
        initStarterKit()
    }

    fun initStarterKit() {
        inventory.clear()
        for (i in hotbar.indices) hotbar[i] = null

        addItem(Items.STONE_AXE, 1)
        addItem(Items.WOODEN_SPEAR, 1)
        addItem(Items.SURVIVAL_BOW, 1)
        addItem(Items.ARROW, 10)
        addItem(Items.BANANA, 3)
        addItem(Items.COCONUT, 2)
        addItem(Items.MOLINERIA_LEAF, 2)
        addItem(Items.CHARCOAL, 2)

        hotbar[0] = ItemStack(Items.STONE_AXE, 1)
        hotbar[1] = ItemStack(Items.WOODEN_SPEAR, 1)
        hotbar[2] = ItemStack(Items.SURVIVAL_BOW, 1)
        hotbar[3] = ItemStack(Items.FIRE_TORCH, 1)

        afflictions.clear()
        afflictions.add(BodyAffliction(BodyPart.LEFT_ARM, "Leech", "Prisatá pijavica pije tvoju krv!", "Hand"))
    }

    fun startNewGame() {
        health = 100f
        stamina = 100f
        hydration = 80f
        carbs = 70f
        protein = 60f
        fat = 50f
        sanity = 100f
        poisonLevel = 0f
        bodyTemp = 36.6f
        bossHealth = 300f
        isBossDefeated = false
        dayCount = 1
        timeOfDay = 10.0f
        playerX = 0f
        playerY = 1.6f
        playerZ = 0f
        playerYaw = 0f
        playerPitch = 0f
        initStarterKit()
        currentScreen = GameScreen.PLAYING
        showToast("🎮 Nová 3D hra spustená! Preži v džungli!")
    }

    fun addXP(amount: Int) {
        xp += amount
        val reqXp = level * 100
        if (xp >= reqXp) {
            xp -= reqXp
            level++
            showToast("⭐ POSTÚPIL SI NA LEVEL $level! Odomknuté nové zručnosti v Zápisníku.")
            SoundManager.playCraftSound()
        }
    }

    fun isPerkUnlocked(perkId: String): Boolean {
        return unlockedPerkIds.contains(perkId)
    }

    fun unlockPerk(perk: Perk): Boolean {
        if (level >= perk.reqLevel && !isPerkUnlocked(perk.id)) {
            unlockedPerkIds.add(perk.id)
            showToast("🌟 Odomknutá zručnosť: ${perk.name}!")
            return true
        }
        return false
    }

    fun showToast(msg: String) {
        currentToast = msg
        toastTimer = 2.5f
    }

    fun applyDamageToPlayer(rawDamage: Float) {
        var finalArmor = equippedArmorValue
        if (isPerkUnlocked("hardened_skin")) finalArmor += 25f

        val finalDamage = (rawDamage * (1f - finalArmor / 100f)).coerceAtLeast(2f)
        health = (health - finalDamage).coerceIn(0f, 100f)
        sanity = (sanity - 5f).coerceIn(0f, 100f)
        showToast("⚠️ Zásah monštrom! -${finalDamage.toInt()} HP")
    }

    fun update(deltaTime: Float) {
        if (currentScreen != GameScreen.PLAYING) return

        if (toastTimer > 0f) {
            toastTimer -= deltaTime
            if (toastTimer <= 0f) currentToast = null
        }

        timeOfDay += (deltaTime * (24f / 600f))
        if (timeOfDay >= 24f) {
            timeOfDay -= 24f
            dayCount++
            addXP(50)
            showToast("Prežil si $dayCount. deň v džungli! +50 XP")
        }

        weatherTimer += deltaTime
        if (weatherTimer >= 60f) {
            weatherTimer = 0f
            if (Math.random() < 0.4) {
                isRaining = !isRaining
                rainIntensity = if (isRaining) 0.8f else 0f
                showToast(if (isRaining) "🌧️ Začal tropický lejak!" else "☀️ Búrka ustála.")
            }
        }

        if (isRaining) {
            for (struct in worldStructures) {
                if (struct.type == "water_collector") {
                    struct.storedWater = (struct.storedWater + deltaTime * 2f).coerceAtMost(100f)
                }
            }
        }

        hydration = (hydration - deltaTime * 0.15f).coerceIn(0f, 100f)
        carbs = (carbs - deltaTime * 0.1f).coerceIn(0f, 100f)
        protein = (protein - deltaTime * 0.08f).coerceIn(0f, 100f)
        fat = (fat - deltaTime * 0.05f).coerceIn(0f, 100f)

        if (poisonLevel > 0f || afflictions.size > 1) {
            bodyTemp = (bodyTemp + deltaTime * 0.02f).coerceAtMost(40.5f)
        } else {
            bodyTemp = (bodyTemp - deltaTime * 0.01f).coerceAtLeast(36.6f)
        }

        if (bodyTemp > 38.5f) {
            health -= deltaTime * 0.2f
            stamina = (stamina - deltaTime * 2f).coerceAtLeast(0f)
        }

        if (poisonLevel > 0f && !isPerkUnlocked("iron_stomach")) {
            health -= deltaTime * 0.4f
            poisonLevel = (poisonLevel - deltaTime * 0.05f).coerceAtLeast(0f)
        }

        if (afflictions.isNotEmpty()) {
            health -= deltaTime * 0.2f * afflictions.size
            sanity -= deltaTime * 0.1f * afflictions.size
        }

        if (sanity < 30f) {
            health -= deltaTime * 0.1f
        }

        if (hydration <= 0f || (carbs <= 0f && protein <= 0f)) {
            health -= deltaTime * 0.5f
        }

        if (hydration > 20f && carbs > 10f) {
            stamina = (stamina + deltaTime * 5f).coerceIn(0f, 100f)
        }

        health = health.coerceIn(0f, 100f)
        sanity = sanity.coerceIn(0f, 100f)

        for (struct in worldStructures) {
            if (struct.type == "campfire" && struct.isLit && struct.hasItemInProcess) {
                if (isRaining) {
                    struct.isLit = false
                    showToast("🌧️ Dážď zahasil ohnisko!")
                } else {
                    struct.processProgress += deltaTime
                    if (struct.processProgress >= 10f) {
                        struct.hasItemInProcess = false
                        struct.processProgress = 0f
                        addItem(Items.COOKED_MEAT, 1)
                        addXP(15)
                        showToast("🥩 Mäso je upečené! Uložené do batohu.")
                    }
                }
            } else if (struct.type == "drying_rack" && struct.hasItemInProcess) {
                struct.processProgress += deltaTime
                if (struct.processProgress >= 15f) {
                    struct.hasItemInProcess = false
                    struct.processProgress = 0f
                    addItem(Items.DRIED_MEAT, 1)
                    addXP(20)
                    showToast("🥓 Sušené mäso je hotové!")
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
            sanity = (sanity + item.sanityGain).coerceIn(0f, 100f)

            if (item.id == Items.HERBAL_TEA.id) {
                bodyTemp = 36.6f
                showToast("☕ Bylinkový čaj vyliečil horúčku!")
            } else if (item.id == Items.BONE_ARMOR.id) {
                equippedArmorValue = 40f
                showToast("🛡️ Vybavil si Kostené Brnenie (+40 Obrana)!")
            } else if (item.id == Items.CHARCOAL.id) {
                poisonLevel = 0f
                parasites = 0
                showToast("Drevené uhlie vyliečilo otravu a parazity!")
            } else {
                showToast("Zjedol/vypil si: ${item.name}")
            }

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
        addXP(25)
        showToast("Vyrobil si: ${recipe.resultItem.name} (+25 XP)")
        return true
    }

    fun treatAffliction(affliction: BodyAffliction, toolItem: ItemType?) {
        var cured = false
        var healBonus = 20f
        if (isPerkUnlocked("herbalist")) healBonus += 15f

        if (affliction.requiredTreatment == "Hand") {
            cured = true
        } else if (affliction.requiredTreatment == "Antivenom" && toolItem?.id == Items.ANTIVENOM_BANDAGE.id) {
            removeItem(Items.ANTIVENOM_BANDAGE, 1)
            poisonLevel = 0f
            cured = true
        } else if (toolItem?.id == Items.LEAF_BANDAGE.id) {
            removeItem(Items.LEAF_BANDAGE, 1)
            cured = true
        }

        if (cured) {
            afflictions.remove(affliction)
            health = (health + healBonus).coerceIn(0f, 100f)
            sanity = (sanity + 10f).coerceIn(0f, 100f)
            addXP(20)
            showToast("Ošetril si ${affliction.bodyPart.displayName}! (+20 XP)")
        } else {
            showToast("Na toto ošetrenie potrebuješ správny liek/obväz!")
        }
    }

    fun sleepInShelter() {
        timeOfDay = 7.0f
        dayCount++
        stamina = 100f
        health = (health + 30f).coerceIn(0f, 100f)
        sanity = (sanity + 25f).coerceIn(0f, 100f)
        addXP(30)
        showToast("Vyspal si sa do nového dňa v búde! Hra uložená.")
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
                put("sanity", sanity)
                put("poisonLevel", poisonLevel)
                put("bodyTemp", bodyTemp)
                put("bossHealth", bossHealth)
                put("isBossDefeated", isBossDefeated)
                put("xp", xp)
                put("level", level)
                put("unlockedPerks", JSONArray(unlockedPerkIds))
                put("dayCount", dayCount)
                put("timeOfDay", timeOfDay)
                put("playerX", playerX)
                put("playerY", playerY)
                put("playerZ", playerZ)
            }
            prefs.edit().putString("save_data", json.toString()).apply()
            showToast("💾 Hra bola úspešne uložená!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadGame() {
        try {
            val prefs = context.getSharedPreferences("green_hell_save", Context.MODE_PRIVATE)
            val str = prefs.getString("save_data", null)
            if (str == null) {
                showToast("❌ Žiadna uložená hra nebola nájdená!")
                return
            }
            val json = JSONObject(str)
            health = json.optDouble("health", 100.0).toFloat()
            stamina = json.optDouble("stamina", 100.0).toFloat()
            hydration = json.optDouble("hydration", 80.0).toFloat()
            carbs = json.optDouble("carbs", 70.0).toFloat()
            protein = json.optDouble("protein", 60.0).toFloat()
            fat = json.optDouble("fat", 50.0).toFloat()
            sanity = json.optDouble("sanity", 100.0).toFloat()
            poisonLevel = json.optDouble("poisonLevel", 0.0).toFloat()
            bodyTemp = json.optDouble("bodyTemp", 36.6).toFloat()
            bossHealth = json.optDouble("bossHealth", 300.0).toFloat()
            isBossDefeated = json.optBoolean("isBossDefeated", false)
            xp = json.optInt("xp", 0)
            level = json.optInt("level", 1)

            unlockedPerkIds.clear()
            val perksArray = json.optJSONArray("unlockedPerks")
            if (perksArray != null) {
                for (i in 0 until perksArray.length()) {
                    unlockedPerkIds.add(perksArray.getString(i))
                }
            }

            dayCount = json.optInt("dayCount", 1)
            timeOfDay = json.optDouble("timeOfDay", 10.0).toFloat()
            playerX = json.optDouble("playerX", 0.0).toFloat()
            playerY = json.optDouble("playerY", 1.6).toFloat()
            playerZ = json.optDouble("playerZ", 0.0).toFloat()
            currentScreen = GameScreen.PLAYING
            showToast("📂 Uložená 3D hra načítaná!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
