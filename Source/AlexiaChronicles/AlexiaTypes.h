#pragma once

#include "CoreMinimal.h"
#include "Animation/AnimInstance.h"
#include "AlexiaTypes.generated.h"

class USkeletalMesh;
class UAnimMontage;

UENUM(BlueprintType)
enum class EHeroClass : uint8
{
	Warrior,
	Assassin,
	Mage,
	Archer
};

UENUM(BlueprintType)
enum class EItemSlot : uint8
{
	Weapon,
	Armor,
	Ring
};

UENUM(BlueprintType)
enum class EItemRarity : uint8
{
	Common,
	Rare,
	Epic,
	Legendary
};

UENUM(BlueprintType)
enum class EEnemyKind : uint8
{
	Slime,
	Wolf,
	Bandit,
	Spider,
	Skeleton,
	Scorpion,
	Yeti,
	Golem,
	Treant,
	SandKing,
	FrostWyrm
};

UENUM(BlueprintType)
enum class EBiome : uint8
{
	Grass,
	Forest,
	Sand,
	Snow
};

/** Predmet výbavy (zbraň, zbroj, šperk). */
USTRUCT(BlueprintType)
struct FAlexiaItem
{
	GENERATED_BODY()

	UPROPERTY(SaveGame, BlueprintReadOnly) FString Name;
	UPROPERTY(SaveGame, BlueprintReadOnly) EItemSlot Slot = EItemSlot::Weapon;
	UPROPERTY(SaveGame, BlueprintReadOnly) EItemRarity Rarity = EItemRarity::Common;
	UPROPERTY(SaveGame, BlueprintReadOnly) int32 Atk = 0;
	UPROPERTY(SaveGame, BlueprintReadOnly) int32 Def = 0;
	UPROPERTY(SaveGame, BlueprintReadOnly) int32 Hp = 0;
	UPROPERTY(SaveGame, BlueprintReadOnly) int32 Level = 1;

	bool IsValid() const { return !Name.IsEmpty(); }
	int32 Power() const { return Atk * 3 + Def * 2 + Hp / 2; }
	int32 SellPrice() const { return (5 + Power()) * ((int32)Rarity + 1); }
	FString StatLine() const;
};

/** Grafika postavy z Fab / Marketplace – nastavuje sa v Project Settings → Alexia Chronicles. */
USTRUCT(BlueprintType)
struct FAlexiaCharacterArt
{
	GENERATED_BODY()

	UPROPERTY(EditAnywhere, Category = "Art") TSoftObjectPtr<USkeletalMesh> Mesh;
	UPROPERTY(EditAnywhere, Category = "Art") TSoftClassPtr<UAnimInstance> AnimClass;
	/** 3 animácie pre combo (1., 2., 3. úder). */
	UPROPERTY(EditAnywhere, Category = "Art") TArray<TSoftObjectPtr<UAnimMontage>> AttackMontages;
	UPROPERTY(EditAnywhere, Category = "Art") TSoftObjectPtr<UAnimMontage> SkillMontage;
	UPROPERTY(EditAnywhere, Category = "Art") TSoftObjectPtr<UAnimMontage> DeathMontage;
	UPROPERTY(EditAnywhere, Category = "Art") float MeshScale = 1.f;
	UPROPERTY(EditAnywhere, Category = "Art") FVector MeshOffset = FVector(0, 0, -90);
	UPROPERTY(EditAnywhere, Category = "Art") FRotator MeshRotation = FRotator(0, -90, 0);
};

struct FHeroClassInfo
{
	const TCHAR* Title;
	const TCHAR* Desc;
	FLinearColor Color;
	FLinearColor Accent;
	float HpMul;
	float AtkMul;
	float Speed;
	float Crit;
	bool bRanged;
	const TCHAR* Skill1;
	const TCHAR* Skill2;
};

struct FEnemyInfo
{
	const TCHAR* Title;
	FLinearColor Color;
	float HpMul;
	float AtkMul;
	float Speed;
	float Radius;
	float HalfHeight;
	float XpMul;
	bool bBoss;
};

namespace AlexiaData
{
	const FHeroClassInfo& Hero(EHeroClass C);
	const FEnemyInfo& Enemy(EEnemyKind K);
	FLinearColor RarityColor(EItemRarity R);
	FString RarityName(EItemRarity R);
	FString SlotName(EItemSlot S);
	FString BiomeName(EBiome B);
	FAlexiaItem RollItem(int32 Level, FRandomStream& Rng, bool bBoss);
}
