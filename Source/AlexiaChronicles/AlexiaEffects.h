#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Actor.h"
#include "AlexiaTypes.h"
#include "AlexiaEffects.generated.h"

class UTextRenderComponent;
class UStaticMeshComponent;
class AAlexiaHero;
class AAlexiaEnemy;

/** Lietajúce číslo poškodenia / text nad hlavou. */
UCLASS()
class ALEXIACHRONICLES_API AAlexiaFloatText : public AActor
{
	GENERATED_BODY()

public:
	AAlexiaFloatText();
	virtual void Tick(float DeltaSeconds) override;

	static void Spawn(UWorld* World, const FVector& Location, const FString& Str, const FLinearColor& Color, float Size = 60.f);

	UPROPERTY() TObjectPtr<UTextRenderComponent> Text;
	float Life = 1.1f;
};

/** Ohnivá guľa, šíp alebo iný projektil hrdinu. */
UCLASS()
class ALEXIACHRONICLES_API AAlexiaProjectile : public AActor
{
	GENERATED_BODY()

public:
	AAlexiaProjectile();
	virtual void Tick(float DeltaSeconds) override;

	void Init(AAlexiaHero* InOwner, const FVector& InVelocity, float InDamage, const FLinearColor& Color, float InRadius, bool bInFire, bool bInPierce);

private:
	TWeakObjectPtr<AAlexiaHero> Hero;
	TSet<TWeakObjectPtr<AAlexiaEnemy>> AlreadyHit;
	FVector Velocity = FVector::ZeroVector;
	float Damage = 10.f;
	float Radius = 30.f;
	float Life = 1.4f;
	bool bFire = false;
	bool bPierce = false;
};

/** Zlato, elixír alebo predmet ležiaci na zemi. */
UCLASS()
class ALEXIACHRONICLES_API AAlexiaPickup : public AActor
{
	GENERATED_BODY()

public:
	AAlexiaPickup();
	virtual void Tick(float DeltaSeconds) override;

	void Init(const FAlexiaItem& InItem, int32 InGold, bool bInPotion);

private:
	UPROPERTY() TObjectPtr<USceneComponent> Spin;
	FAlexiaItem Item;
	int32 Gold = 0;
	bool bPotion = false;
	float Age = 0.f;
	FVector BaseLoc;
};
