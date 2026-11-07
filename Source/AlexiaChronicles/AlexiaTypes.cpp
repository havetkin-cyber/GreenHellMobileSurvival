#include "AlexiaTypes.h"

FString FAlexiaItem::StatLine() const
{
	TArray<FString> Parts;
	if (Atk > 0) Parts.Add(FString::Printf(TEXT("ÚT +%d"), Atk));
	if (Def > 0) Parts.Add(FString::Printf(TEXT("OB +%d"), Def));
	if (Hp > 0) Parts.Add(FString::Printf(TEXT("HP +%d"), Hp));
	return FString::Join(Parts, TEXT("  "));
}

namespace AlexiaData
{
	static const FHeroClassInfo GHeroes[] = {
		{ TEXT("Bojovník"), TEXT("Ťažký meč a štít. Veľa života, combo 3 úderov."),
		  FLinearColor(0.12f, 0.28f, 0.85f), FLinearColor(0.85f, 0.87f, 0.92f), 1.35f, 1.0f, 620.f, 0.10f, false, TEXT("Vír"), TEXT("Úskok") },
		{ TEXT("Vrah"), TEXT("Dve dýky, najrýchlejší, veľa kritických zásahov."),
		  FLinearColor(0.22f, 0.16f, 0.32f), FLinearColor(0.55f, 0.95f, 1.0f), 0.95f, 0.9f, 720.f, 0.30f, false, TEXT("Tieň"), TEXT("Skok") },
		{ TEXT("Mág"), TEXT("Ohnivé gule na diaľku, Meteor zničí celú skupinu."),
		  FLinearColor(0.70f, 0.12f, 0.16f), FLinearColor(1.0f, 0.75f, 0.25f), 0.8f, 1.25f, 580.f, 0.12f, true, TEXT("Meteor"), TEXT("Teleport") },
		{ TEXT("Lukostrelec"), TEXT("Šípy na veľkú vzdialenosť, salva 7 šípov."),
		  FLinearColor(0.18f, 0.45f, 0.18f), FLinearColor(0.90f, 0.75f, 0.50f), 0.9f, 1.05f, 650.f, 0.20f, true, TEXT("Salva"), TEXT("Úskok") },
	};

	static const FEnemyInfo GEnemies[] = {
		{ TEXT("Sliz"),        FLinearColor(0.35f, 0.85f, 0.35f), 0.8f, 0.7f, 260.f, 45.f, 45.f, 0.8f, false },
		{ TEXT("Vlk"),         FLinearColor(0.45f, 0.47f, 0.52f), 0.9f, 1.0f, 520.f, 45.f, 50.f, 1.0f, false },
		{ TEXT("Lupič"),       FLinearColor(0.65f, 0.25f, 0.18f), 1.1f, 1.1f, 380.f, 40.f, 88.f, 1.2f, false },
		{ TEXT("Pavúk"),       FLinearColor(0.30f, 0.20f, 0.38f), 0.9f, 1.2f, 470.f, 50.f, 40.f, 1.1f, false },
		{ TEXT("Kostlivec"),   FLinearColor(0.90f, 0.88f, 0.80f), 1.0f, 1.2f, 340.f, 40.f, 88.f, 1.2f, false },
		{ TEXT("Škorpión"),    FLinearColor(0.75f, 0.40f, 0.15f), 1.2f, 1.3f, 360.f, 55.f, 40.f, 1.3f, false },
		{ TEXT("Yeti"),        FLinearColor(0.92f, 0.95f, 1.0f),  1.8f, 1.4f, 320.f, 65.f, 110.f, 1.7f, false },
		{ TEXT("Kamenný golem"),  FLinearColor(0.45f, 0.43f, 0.40f), 14.f, 2.0f, 260.f, 120.f, 200.f, 18.f, true },
		{ TEXT("Prastarý ent"),   FLinearColor(0.36f, 0.25f, 0.13f), 13.f, 2.0f, 240.f, 120.f, 220.f, 18.f, true },
		{ TEXT("Piesočný kráľ"),  FLinearColor(0.85f, 0.62f, 0.20f), 12.f, 2.2f, 330.f, 90.f, 170.f, 18.f, true },
		{ TEXT("Ľadový drak"),    FLinearColor(0.50f, 0.82f, 0.96f), 15.f, 2.3f, 300.f, 140.f, 140.f, 20.f, true },
	};

	const FHeroClassInfo& Hero(EHeroClass C) { return GHeroes[(int32)C]; }
	const FEnemyInfo& Enemy(EEnemyKind K) { return GEnemies[(int32)K]; }

	FLinearColor RarityColor(EItemRarity R)
	{
		switch (R)
		{
		case EItemRarity::Rare: return FLinearColor(0.20f, 0.55f, 1.0f);
		case EItemRarity::Epic: return FLinearColor(0.65f, 0.30f, 1.0f);
		case EItemRarity::Legendary: return FLinearColor(1.0f, 0.60f, 0.10f);
		default: return FLinearColor(0.80f, 0.80f, 0.80f);
		}
	}

	FString RarityName(EItemRarity R)
	{
		switch (R)
		{
		case EItemRarity::Rare: return TEXT("Vzácny");
		case EItemRarity::Epic: return TEXT("Epický");
		case EItemRarity::Legendary: return TEXT("Legendárny");
		default: return TEXT("Bežný");
		}
	}

	FString SlotName(EItemSlot S)
	{
		switch (S)
		{
		case EItemSlot::Armor: return TEXT("Zbroj");
		case EItemSlot::Ring: return TEXT("Šperk");
		default: return TEXT("Zbraň");
		}
	}

	FString BiomeName(EBiome B)
	{
		switch (B)
		{
		case EBiome::Forest: return TEXT("Šepkajúci les");
		case EBiome::Sand: return TEXT("Spálená púšť");
		case EBiome::Snow: return TEXT("Mrazivé štíty");
		default: return TEXT("Zelené pláne");
		}
	}

	FAlexiaItem RollItem(int32 Level, FRandomStream& Rng, bool bBoss)
	{
		static const TCHAR* Weapons[] = { TEXT("meč"), TEXT("sekera"), TEXT("kladivo"), TEXT("kopija"), TEXT("dýka"), TEXT("čepeľ") };
		static const TCHAR* Armors[] = { TEXT("kožená zbroj"), TEXT("krúžková zbroj"), TEXT("plátová zbroj"), TEXT("plášť") };
		static const TCHAR* Rings[] = { TEXT("prsteň"), TEXT("amulet"), TEXT("talizman") };
		static const TCHAR* Prefix[4][3] = {
			{ TEXT("Starý"), TEXT("Pevný"), TEXT("Jednoduchý") },
			{ TEXT("Ostrý"), TEXT("Kalený"), TEXT("Strážcov") },
			{ TEXT("Búrkový"), TEXT("Mesačný"), TEXT("Drakobijcov") },
			{ TEXT("Kráľovský"), TEXT("Hviezdny"), TEXT("Alexiin") },
		};
		static const float Mul[] = { 1.0f, 1.45f, 1.95f, 2.7f };

		FAlexiaItem It;
		const float R = Rng.FRand();
		if (bBoss) It.Rarity = R < 0.35f ? EItemRarity::Legendary : EItemRarity::Epic;
		else It.Rarity = R < 0.02f ? EItemRarity::Legendary : R < 0.10f ? EItemRarity::Epic : R < 0.35f ? EItemRarity::Rare : EItemRarity::Common;
		It.Slot = (EItemSlot)Rng.RandRange(0, 2);
		It.Level = Level;
		const float M = Mul[(int32)It.Rarity];
		const float Base = 3.f + Level * 2.f;
		const float Var = Rng.FRandRange(0.8f, 1.2f);
		FString BaseName;
		switch (It.Slot)
		{
		case EItemSlot::Weapon:
			It.Atk = FMath::RoundToInt(Base * 1.6f * M * Var);
			It.Hp = (int32)It.Rarity >= 2 ? FMath::RoundToInt(Base * M) : 0;
			BaseName = Weapons[Rng.RandRange(0, UE_ARRAY_COUNT(Weapons) - 1)];
			break;
		case EItemSlot::Armor:
			It.Def = FMath::RoundToInt(Base * 1.1f * M * Var);
			It.Hp = FMath::RoundToInt(Base * 4.f * M * Var);
			BaseName = Armors[Rng.RandRange(0, UE_ARRAY_COUNT(Armors) - 1)];
			break;
		default:
			It.Atk = FMath::RoundToInt(Base * 0.6f * M * Var);
			It.Def = FMath::RoundToInt(Base * 0.4f * M);
			It.Hp = FMath::RoundToInt(Base * 2.f * M * Var);
			BaseName = Rings[Rng.RandRange(0, UE_ARRAY_COUNT(Rings) - 1)];
			break;
		}
		It.Name = FString::Printf(TEXT("%s %s"), Prefix[(int32)It.Rarity][Rng.RandRange(0, 2)], *BaseName);
		return It;
	}
}
