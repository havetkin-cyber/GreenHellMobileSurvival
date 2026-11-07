#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Character.h"
#include "AlexiaTypes.h"
#include "AlexiaHero.generated.h"

class USpringArmComponent;
class UCameraComponent;
class UStaticMeshComponent;
class AAlexiaEnemy;

/** Hrateľný hrdina – 4 triedy, combo boj, schopnosti, výbava, level-up, ukladanie. */
UCLASS()
class ALEXIACHRONICLES_API AAlexiaHero : public ACharacter
{
	GENERATED_BODY()

public:
	AAlexiaHero();

	virtual void BeginPlay() override;
	virtual void Tick(float DeltaSeconds) override;
	virtual void SetupPlayerInputComponent(UInputComponent* PlayerInputComponent) override;

	// ---- štart hry
	void StartNewGame(EHeroClass InClass);
	bool LoadGame();
	void SaveGame();
	static bool HasSave();
	bool HasStarted() const { return bStarted; }

	// ---- ovládanie (volá HUD aj klávesnica)
	void SetAttackHeld(bool bHeld) { bAttackHeld = bHeld; }
	void UseSkill1();
	void UseSkill2();
	void UsePotion();

	// ---- boj
	void DamageEnemy(AAlexiaEnemy* Enemy, float Base, float CritBonus);
	void DamageArea(const FVector& Center, float Radius, float Damage);
	void ReceiveEnemyDamage(float Raw);
	bool IsAlive() const { return !bDead; }

	// ---- odmeny
	void AddXp(int32 Amount);
	void AddGold(int32 Amount);
	void AddPotion();
	void AddItem(const FAlexiaItem& Item);

	// ---- taška
	void EquipFromInventory(int32 Index);
	void SellJunk();
	void BuyPotion();
	int32 PotionPrice() const { return 40 + Level * 5; }
	const FAlexiaItem& Equipped(EItemSlot Slot) const;
	bool IsUpgrade(const FAlexiaItem& Item) const { return Item.Power() > Equipped(Item.Slot).Power(); }

	// ---- staty
	int32 GetAtk() const;
	int32 GetDef() const;
	float GetMaxHp() const;
	int32 XpNext() const { return FMath::RoundToInt(50.f * FMath::Pow((float)Level, 1.5f)); }
	const FHeroClassInfo& Info() const { return AlexiaData::Hero(HeroClass); }

	void Say(const FString& Msg);

	// ---- stav pre HUD
	EHeroClass HeroClass = EHeroClass::Warrior;
	int32 Level = 1;
	int32 Xp = 0;
	int32 Gold = 0;
	int32 Potions = 3;
	float Hp = 100.f;
	int32 ComboStep = 0;
	float ComboTimer = 0.f;
	float Skill1Cd = 0.f;
	float Skill2Cd = 0.f;
	float PotionCd = 0.f;
	FString Message;
	float MessageTimer = 0.f;
	FString ZoneName;
	int32 ZoneLevel = 1;
	TWeakObjectPtr<AAlexiaEnemy> ActiveBoss;
	int32 InventoryRevision = 0;

	UPROPERTY() FAlexiaItem Weapon;
	UPROPERTY() FAlexiaItem Armor;
	UPROPERTY() FAlexiaItem Ring;
	UPROPERTY() TArray<FAlexiaItem> Inventory;

	UPROPERTY(VisibleAnywhere) TObjectPtr<USpringArmComponent> CameraBoom;
	UPROPERTY(VisibleAnywhere) TObjectPtr<UCameraComponent> Camera;
	UPROPERTY(VisibleAnywhere) TObjectPtr<USceneComponent> VisualRoot;

private:
	void MoveForward(float Value);
	void MoveRight(float Value);
	void CameraZoom(float Value);
	void OnAttackPressed() { SetAttackHeld(true); }
	void OnAttackReleased() { SetAttackHeld(false); }

	void DoAttack();
	void Melee(float Range, float ArcDeg, float Damage, float CritBonus = 0.f);
	void Shoot(float YawOffset, float Speed, float Damage, const FLinearColor& Color, float Radius, bool bFire, bool bPierce);
	AAlexiaEnemy* NearestEnemy(float Range) const;
	void FaceTowards(const FVector& Target);
	void Dash(const FVector& Dir);
	void Die();
	void Respawn();
	void RecalcStats(bool bKeepRatio);
	void ApplyClassVisuals();
	void PlayArtMontage(int32 Index, bool bSkill);
	FVector SpawnPoint() const;
	FVector MoveDir() const;

	UPROPERTY() TArray<TObjectPtr<UStaticMeshComponent>> Parts;
	UPROPERTY() TObjectPtr<USceneComponent> WeaponPivot;
	UPROPERTY() TObjectPtr<USceneComponent> OffhandPivot;
	UPROPERTY() TObjectPtr<UStaticMeshComponent> WhirlRing;

	bool bStarted = false;
	bool bDead = false;
	bool bAttackHeld = false;
	bool bUsingArtMesh = false;
	float RespawnTimer = 0.f;
	float AttackTimer = 0.f;
	float SwingTimer = 0.f;
	float SwingDuration = 0.25f;
	float WhirlTimer = 0.f;
	float DashTimer = 0.f;
	FVector DashVelocity = FVector::ZeroVector;
	float Invuln = 0.f;
	float SaveTimer = 0.f;
	float WalkAnim = 0.f;
	FVector2D MoveInput = FVector2D::ZeroVector;
	FRandomStream Rng;
};
