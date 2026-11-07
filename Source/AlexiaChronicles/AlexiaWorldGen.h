#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Actor.h"
#include "AlexiaTypes.h"
#include "AlexiaWorldGen.generated.h"

class UInstancedStaticMeshComponent;
class UStaticMesh;
class UMaterialInterface;
class AAlexiaEnemy;

/**
 * Procedurálny otvorený svet: ostrov s biómami (lúky, les, púšť, sneh), cestami,
 * stromami, skalami, 4 dúpätami bossov a priebežným spawnovaním monštier podľa oblasti.
 */
UCLASS()
class ALEXIACHRONICLES_API AAlexiaWorldGen : public AActor
{
	GENERATED_BODY()

public:
	AAlexiaWorldGen();
	virtual void BeginPlay() override;
	virtual void Tick(float DeltaSeconds) override;

	static AAlexiaWorldGen* Get(const UWorld* World);

	EBiome BiomeAt(const FVector& L) const;
	int32 ZoneLevel(const FVector& L) const;
	FVector SpawnPoint() const { return FVector(0.f, 900.f, 150.f); }
	bool ClampToWorld(FVector& L) const;
	void OnBossKilled(AAlexiaEnemy* Boss);

	UPROPERTY(EditAnywhere, Category = "Svet") float WorldRadius = 60000.f;
	UPROPERTY(EditAnywhere, Category = "Svet") float TileSize = 2500.f;
	UPROPERTY(EditAnywhere, Category = "Svet") int32 Seed = 1337;
	UPROPERTY(EditAnywhere, Category = "Svet") int32 MaxEnemiesNearPlayer = 20;

private:
	struct FLair
	{
		FVector Loc = FVector::ZeroVector;
		EEnemyKind Kind = EEnemyKind::Golem;
		FString Name;
		float Respawn = 0.f;
		TWeakObjectPtr<AAlexiaEnemy> Alive;
	};

	void BuildLights();
	void BuildTerrain();
	void UpdateSpawns(float Dt);
	void UpdateLairs(float Dt);
	AAlexiaEnemy* SpawnEnemy(EEnemyKind Kind, int32 Level, const FVector& Loc, const FString& BossName);
	UInstancedStaticMeshComponent* MakeIsm(const TCHAR* ShapeName, const FLinearColor& Color, bool bCollide, UStaticMesh* MeshOverride = nullptr, UMaterialInterface* MatOverride = nullptr);

	float Hash(int32 X, int32 Y, int32 S) const;
	float ValueNoise(float X, float Y, int32 S) const;
	float Fbm(float X, float Y, int32 S) const;

	UPROPERTY() TArray<TObjectPtr<UInstancedStaticMeshComponent>> Ism;

	TArray<FLair> Lairs;
	float SpawnTimer = 0.f;
	static TWeakObjectPtr<AAlexiaWorldGen> Instance;
};
