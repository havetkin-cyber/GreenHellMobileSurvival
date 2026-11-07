#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Character.h"
#include "AlexiaTypes.h"
#include "AlexiaEnemy.generated.h"

class AAlexiaHero;
class UStaticMeshComponent;
class UTextRenderComponent;

/** Monštrum alebo boss s jednoduchou AI: túlanie, prenasledovanie, útok s varovaním, plošný úder bossa. */
UCLASS()
class ALEXIACHRONICLES_API AAlexiaEnemy : public ACharacter
{
	GENERATED_BODY()

public:
	AAlexiaEnemy();

	void Init(EEnemyKind InKind, int32 InLevel, const FString& InBossName);
	virtual void Tick(float DeltaSeconds) override;

	void ReceiveHeroDamage(float Damage, const FVector& From, bool bCrit, AAlexiaHero* By);

	bool IsAlive() const { return Hp > 0.f && !bDying; }
	bool IsBoss() const { return AlexiaData::Enemy(Kind).bBoss; }
	float GetRadius() const { return AlexiaData::Enemy(Kind).Radius; }
	FString DisplayName() const { return BossName.IsEmpty() ? FString(AlexiaData::Enemy(Kind).Title) : BossName; }

	EEnemyKind Kind = EEnemyKind::Slime;
	int32 Level = 1;
	float Hp = 30.f;
	float MaxHp = 30.f;
	float Atk = 6.f;
	bool bAggro = false;
	FString BossName;

private:
	void BuildVisuals();
	void UpdateLabel();
	void Die(AAlexiaHero* By);
	void MoveToward(const FVector& Target, float SpeedScale);

	UPROPERTY() TObjectPtr<USceneComponent> Body;
	UPROPERTY() TObjectPtr<UStaticMeshComponent> Telegraph;
	UPROPERTY() TObjectPtr<UTextRenderComponent> Label;
	UPROPERTY() TArray<TObjectPtr<UStaticMeshComponent>> Parts;

	FVector Home = FVector::ZeroVector;
	FVector WanderTarget = FVector::ZeroVector;
	float WanderTimer = 0.f;
	float AttackCd = 1.f;
	float Windup = 0.f;
	float SlamCd = 5.f;
	float SlamWindup = 0.f;
	float Anim = 0.f;
	float HitFlash = 0.f;
	float DeathTimer = 0.f;
	bool bDying = false;
	bool bUsingArtMesh = false;
};
