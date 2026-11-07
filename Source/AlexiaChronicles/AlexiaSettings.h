#pragma once

#include "CoreMinimal.h"
#include "Engine/DeveloperSettings.h"
#include "AlexiaTypes.h"
#include "AlexiaSettings.generated.h"

class UStaticMesh;
class UMaterialInterface;

/**
 * Project Settings → Game → Alexia Chronicles.
 * Sem priraď modely a animácie stiahnuté zadarmo z Fab.com. Prázdne polia = hra použije vlastné tvary.
 */
UCLASS(Config = Game, DefaultConfig, meta = (DisplayName = "Alexia Chronicles"))
class ALEXIACHRONICLES_API UAlexiaSettings : public UDeveloperSettings
{
	GENERATED_BODY()

public:
	UPROPERTY(Config, EditAnywhere, Category = "Postavy")
	TMap<EHeroClass, FAlexiaCharacterArt> HeroArt;

	UPROPERTY(Config, EditAnywhere, Category = "Nepriatelia")
	TMap<EEnemyKind, FAlexiaCharacterArt> EnemyArt;

	/** Vygenerovať otvorený svet (vypni, ak máš vlastnú mapu s krajinou z Fab). */
	UPROPERTY(Config, EditAnywhere, Category = "Svet")
	bool bGenerateWorld = true;

	UPROPERTY(Config, EditAnywhere, Category = "Svet")
	TMap<EBiome, TSoftObjectPtr<UMaterialInterface>> GroundMaterials;

	UPROPERTY(Config, EditAnywhere, Category = "Svet")
	TSoftObjectPtr<UStaticMesh> TreeMesh;

	UPROPERTY(Config, EditAnywhere, Category = "Svet")
	TSoftObjectPtr<UStaticMesh> PineMesh;

	UPROPERTY(Config, EditAnywhere, Category = "Svet")
	TSoftObjectPtr<UStaticMesh> RockMesh;

	virtual FName GetCategoryName() const override { return TEXT("Game"); }

	static const UAlexiaSettings* Get() { return GetDefault<UAlexiaSettings>(); }
};
