#pragma once

#include "CoreMinimal.h"
#include "GameFramework/SaveGame.h"
#include "AlexiaTypes.h"
#include "AlexiaSaveGame.generated.h"

UCLASS()
class ALEXIACHRONICLES_API UAlexiaSaveGame : public USaveGame
{
	GENERATED_BODY()

public:
	UPROPERTY() EHeroClass HeroClass = EHeroClass::Warrior;
	UPROPERTY() int32 Level = 1;
	UPROPERTY() int32 Xp = 0;
	UPROPERTY() int32 Gold = 0;
	UPROPERTY() int32 Potions = 3;
	UPROPERTY() FAlexiaItem Weapon;
	UPROPERTY() FAlexiaItem Armor;
	UPROPERTY() FAlexiaItem Ring;
	UPROPERTY() TArray<FAlexiaItem> Inventory;
	UPROPERTY() FVector Location = FVector::ZeroVector;

	static const TCHAR* SlotName() { return TEXT("AlexiaChronicles"); }
};
