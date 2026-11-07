#include "AlexiaHero.h"
#include "AlexiaEnemy.h"
#include "AlexiaEffects.h"
#include "AlexiaSaveGame.h"
#include "AlexiaSettings.h"
#include "AlexiaVisuals.h"
#include "AlexiaWorldGen.h"
#include "Camera/CameraComponent.h"
#include "Components/CapsuleComponent.h"
#include "Components/InputComponent.h"
#include "Components/SkeletalMeshComponent.h"
#include "Components/StaticMeshComponent.h"
#include "Animation/AnimInstance.h"
#include "Animation/AnimMontage.h"
#include "Engine/SkeletalMesh.h"
#include "EngineUtils.h"
#include "GameFramework/CharacterMovementComponent.h"
#include "GameFramework/SpringArmComponent.h"
#include "Kismet/GameplayStatics.h"

AAlexiaHero::AAlexiaHero()
{
	PrimaryActorTick.bCanEverTick = true;
	GetCapsuleComponent()->InitCapsuleSize(40.f, 90.f);

	bUseControllerRotationYaw = false;
	bUseControllerRotationPitch = false;
	bUseControllerRotationRoll = false;

	UCharacterMovementComponent* Move = GetCharacterMovement();
	Move->bOrientRotationToMovement = true;
	Move->RotationRate = FRotator(0.f, 900.f, 0.f);
	Move->MaxWalkSpeed = 620.f;
	Move->BrakingDecelerationWalking = 4000.f;
	Move->GroundFriction = 10.f;

	// Kamera zhora pod uhlom ako v HIT2 (quarter view)
	CameraBoom = CreateDefaultSubobject<USpringArmComponent>(TEXT("CameraBoom"));
	CameraBoom->SetupAttachment(RootComponent);
	CameraBoom->SetUsingAbsoluteRotation(true);
	CameraBoom->SetRelativeRotation(FRotator(-48.f, -40.f, 0.f));
	CameraBoom->TargetArmLength = 1500.f;
	CameraBoom->bDoCollisionTest = false;
	CameraBoom->bEnableCameraLag = true;
	CameraBoom->CameraLagSpeed = 9.f;

	Camera = CreateDefaultSubobject<UCameraComponent>(TEXT("Camera"));
	Camera->SetupAttachment(CameraBoom, USpringArmComponent::SocketName);
	Camera->FieldOfView = 55.f;

	VisualRoot = CreateDefaultSubobject<USceneComponent>(TEXT("VisualRoot"));
	VisualRoot->SetupAttachment(RootComponent);

	Rng.GenerateNewSeed();
}

void AAlexiaHero::BeginPlay()
{
	Super::BeginPlay();
	ApplyClassVisuals();
	RecalcStats(false);
	Hp = GetMaxHp();
}

void AAlexiaHero::SetupPlayerInputComponent(UInputComponent* Input)
{
	Super::SetupPlayerInputComponent(Input);
	Input->BindAxis(TEXT("MoveForward"), this, &AAlexiaHero::MoveForward);
	Input->BindAxis(TEXT("MoveRight"), this, &AAlexiaHero::MoveRight);
	Input->BindAxis(TEXT("CameraZoom"), this, &AAlexiaHero::CameraZoom);
	Input->BindAction(TEXT("Attack"), IE_Pressed, this, &AAlexiaHero::OnAttackPressed);
	Input->BindAction(TEXT("Attack"), IE_Released, this, &AAlexiaHero::OnAttackReleased);
	Input->BindAction(TEXT("Skill1"), IE_Pressed, this, &AAlexiaHero::UseSkill1);
	Input->BindAction(TEXT("Skill2"), IE_Pressed, this, &AAlexiaHero::UseSkill2);
	Input->BindAction(TEXT("Potion"), IE_Pressed, this, &AAlexiaHero::UsePotion);
}

// ======================================================================== štart / ukladanie

FVector AAlexiaHero::SpawnPoint() const
{
	if (AAlexiaWorldGen* W = AAlexiaWorldGen::Get(GetWorld())) return W->SpawnPoint();
	return FVector(0, 0, 150);
}

void AAlexiaHero::StartNewGame(EHeroClass InClass)
{
	HeroClass = InClass;
	Level = 1; Xp = 0; Gold = 0; Potions = 3;
	Inventory.Reset();
	Armor = FAlexiaItem();
	Ring = FAlexiaItem();
	Weapon = FAlexiaItem();
	Weapon.Name = TEXT("Cvičná zbraň");
	Weapon.Atk = 4;
	ApplyClassVisuals();
	RecalcStats(false);
	Hp = GetMaxHp();
	SetActorLocation(SpawnPoint());
	bStarted = true;
	InventoryRevision++;
	Say(FString::Printf(TEXT("Vitaj v Alexii, %s! Bossovia čakajú v rohoch mapy."), Info().Title));
	SaveGame();
}

bool AAlexiaHero::HasSave()
{
	return UGameplayStatics::DoesSaveGameExist(UAlexiaSaveGame::SlotName(), 0);
}

bool AAlexiaHero::LoadGame()
{
	UAlexiaSaveGame* S = Cast<UAlexiaSaveGame>(UGameplayStatics::LoadGameFromSlot(UAlexiaSaveGame::SlotName(), 0));
	if (!S) return false;
	HeroClass = S->HeroClass;
	Level = S->Level; Xp = S->Xp; Gold = S->Gold; Potions = S->Potions;
	Weapon = S->Weapon; Armor = S->Armor; Ring = S->Ring;
	Inventory = S->Inventory;
	ApplyClassVisuals();
	RecalcStats(false);
	Hp = GetMaxHp();
	SetActorLocation(S->Location.IsNearlyZero() ? SpawnPoint() : S->Location + FVector(0, 0, 50));
	bStarted = true;
	InventoryRevision++;
	Say(FString::Printf(TEXT("Vitaj späť! %s, level %d"), Info().Title, Level));
	return true;
}

void AAlexiaHero::SaveGame()
{
	if (!bStarted) return;
	UAlexiaSaveGame* S = Cast<UAlexiaSaveGame>(UGameplayStatics::CreateSaveGameObject(UAlexiaSaveGame::StaticClass()));
	if (!S) return;
	S->HeroClass = HeroClass;
	S->Level = Level; S->Xp = Xp; S->Gold = Gold; S->Potions = Potions;
	S->Weapon = Weapon; S->Armor = Armor; S->Ring = Ring;
	S->Inventory = Inventory;
	S->Location = bDead ? FVector::ZeroVector : GetActorLocation();
	UGameplayStatics::SaveGameToSlot(S, UAlexiaSaveGame::SlotName(), 0);
}

// ======================================================================== vzhľad

void AAlexiaHero::ApplyClassVisuals()
{
	for (UStaticMeshComponent* P : Parts) if (P) P->DestroyComponent();
	Parts.Reset();
	if (WeaponPivot) { WeaponPivot->DestroyComponent(); WeaponPivot = nullptr; }
	if (OffhandPivot) { OffhandPivot->DestroyComponent(); OffhandPivot = nullptr; }

	const FHeroClassInfo& I = Info();
	GetCharacterMovement()->MaxWalkSpeed = I.Speed;

	// Grafika z Fab, ak je nastavená v Project Settings
	bUsingArtMesh = false;
	if (const FAlexiaCharacterArt* Art = UAlexiaSettings::Get()->HeroArt.Find(HeroClass))
	{
		if (USkeletalMesh* Mesh = Art->Mesh.LoadSynchronous())
		{
			GetMesh()->SetSkeletalMesh(Mesh);
			if (UClass* Anim = Art->AnimClass.LoadSynchronous()) GetMesh()->SetAnimInstanceClass(Anim);
			GetMesh()->SetRelativeLocationAndRotation(Art->MeshOffset, Art->MeshRotation);
			GetMesh()->SetRelativeScale3D(FVector(Art->MeshScale));
			GetMesh()->SetVisibility(true);
			bUsingArtMesh = true;
		}
	}
	if (bUsingArtMesh) { VisualRoot->SetVisibility(false, true); return; }
	GetMesh()->SetVisibility(false);
	VisualRoot->SetVisibility(true, true);

	const FLinearColor Skin(0.95f, 0.75f, 0.58f);
	const FLinearColor Dark = I.Color * 0.45f;
	auto Add = [this](const TCHAR* S, FVector L, FVector Sc, FLinearColor C, FRotator R = FRotator::ZeroRotator, USceneComponent* Parent = nullptr)
	{
		UStaticMeshComponent* P = AlexiaVisuals::AddPart(this, Parent ? Parent : VisualRoot.Get(), S, L, Sc, C, R);
		Parts.Add(P);
		return P;
	};

	// telo (spoločné)
	Add(TEXT("Cylinder"), FVector(0, -16, -68), FVector(0.24f, 0.24f, 0.42f), Dark);
	Add(TEXT("Cylinder"), FVector(0, 16, -68), FVector(0.24f, 0.24f, 0.42f), Dark);
	Add(TEXT("Cylinder"), FVector(0, 0, -12), FVector(0.62f, 0.62f, 0.72f), I.Color);
	Add(TEXT("Cylinder"), FVector(0, 0, -30), FVector(0.66f, 0.66f, 0.12f), FLinearColor(0.35f, 0.22f, 0.1f));
	Add(TEXT("Sphere"), FVector(0, -34, 16), FVector(0.32f), I.Accent);
	Add(TEXT("Sphere"), FVector(0, 34, 16), FVector(0.32f), I.Accent);
	Add(TEXT("Sphere"), FVector(0, 0, 50), FVector(0.44f), Skin);
	Add(TEXT("Sphere"), FVector(18, -8, 54), FVector(0.07f), FLinearColor(0.05f, 0.05f, 0.05f));
	Add(TEXT("Sphere"), FVector(18, 8, 54), FVector(0.07f), FLinearColor(0.05f, 0.05f, 0.05f));
	Add(TEXT("Cube"), FVector(-14, 0, 0), FVector(0.12f, 0.6f, 0.9f), Dark); // plášť

	WeaponPivot = NewObject<USceneComponent>(this);
	WeaponPivot->SetupAttachment(VisualRoot);
	WeaponPivot->SetRelativeLocation(FVector(6, 36, 6));
	WeaponPivot->RegisterComponent();
	OffhandPivot = NewObject<USceneComponent>(this);
	OffhandPivot->SetupAttachment(VisualRoot);
	OffhandPivot->SetRelativeLocation(FVector(6, -36, 6));
	OffhandPivot->RegisterComponent();

	switch (HeroClass)
	{
	case EHeroClass::Warrior:
		Add(TEXT("Sphere"), FVector(0, 0, 62), FVector(0.48f, 0.48f, 0.32f), FLinearColor(0.6f, 0.63f, 0.68f));
		Add(TEXT("Cube"), FVector(60, 0, 0), FVector(1.2f, 0.06f, 0.16f), FLinearColor(0.88f, 0.92f, 0.96f), FRotator::ZeroRotator, WeaponPivot);
		Add(TEXT("Cube"), FVector(4, 0, 0), FVector(0.08f, 0.36f, 0.08f), FLinearColor(0.85f, 0.65f, 0.2f), FRotator::ZeroRotator, WeaponPivot);
		Add(TEXT("Cube"), FVector(-8, 0, 0), FVector(0.22f, 0.08f, 0.08f), FLinearColor(0.35f, 0.22f, 0.1f), FRotator::ZeroRotator, WeaponPivot);
		Add(TEXT("Cube"), FVector(18, 0, 0), FVector(0.1f, 0.55f, 0.7f), FLinearColor(0.55f, 0.12f, 0.12f), FRotator(0, 90, 0), OffhandPivot);
		Add(TEXT("Sphere"), FVector(24, 0, 0), FVector(0.18f), FLinearColor(0.9f, 0.7f, 0.2f), FRotator::ZeroRotator, OffhandPivot);
		break;
	case EHeroClass::Assassin:
		Add(TEXT("Cone"), FVector(-4, 0, 70), FVector(0.55f, 0.55f, 0.5f), FLinearColor(0.12f, 0.1f, 0.18f));
		Add(TEXT("Cube"), FVector(34, 0, 0), FVector(0.6f, 0.04f, 0.1f), I.Accent, FRotator::ZeroRotator, WeaponPivot);
		Add(TEXT("Cube"), FVector(34, 0, 0), FVector(0.6f, 0.04f, 0.1f), I.Accent, FRotator::ZeroRotator, OffhandPivot);
		Add(TEXT("Cube"), FVector(0, 0, 38), FVector(0.3f, 0.46f, 0.12f), FLinearColor(0.12f, 0.1f, 0.18f)); // šatka
		break;
	case EHeroClass::Mage:
		Add(TEXT("Cylinder"), FVector(0, 0, 66), FVector(0.7f, 0.7f, 0.05f), FLinearColor(0.45f, 0.06f, 0.1f));
		Add(TEXT("Cone"), FVector(0, 0, 96), FVector(0.48f, 0.48f, 0.7f), FLinearColor(0.45f, 0.06f, 0.1f));
		Add(TEXT("Cylinder"), FVector(0, 0, 20), FVector(0.07f, 0.07f, 1.7f), FLinearColor(0.45f, 0.3f, 0.15f), FRotator::ZeroRotator, WeaponPivot);
		Add(TEXT("Sphere"), FVector(0, 0, 110), FVector(0.3f), FLinearColor(1.f, 0.65f, 0.15f), FRotator::ZeroRotator, WeaponPivot);
		break;
	case EHeroClass::Archer:
		Add(TEXT("Sphere"), FVector(-4, 0, 60), FVector(0.48f, 0.48f, 0.34f), FLinearColor(0.12f, 0.32f, 0.12f));
		Add(TEXT("Cylinder"), FVector(-30, 10, 20), FVector(0.16f, 0.16f, 0.6f), FLinearColor(0.45f, 0.28f, 0.12f), FRotator(0, 0, 20)); // tulec
		Add(TEXT("Cube"), FVector(24, 0, 0), FVector(0.05f, 0.05f, 1.2f), FLinearColor(0.5f, 0.32f, 0.15f), FRotator::ZeroRotator, OffhandPivot);
		Add(TEXT("Cube"), FVector(30, 0, 44), FVector(0.05f, 0.05f, 0.4f), FLinearColor(0.5f, 0.32f, 0.15f), FRotator(-30, 0, 0), OffhandPivot);
		Add(TEXT("Cube"), FVector(30, 0, -44), FVector(0.05f, 0.05f, 0.4f), FLinearColor(0.5f, 0.32f, 0.15f), FRotator(30, 0, 0), OffhandPivot);
		break;
	}

	WhirlRing = Add(TEXT("Cylinder"), FVector(0, 0, -60), FVector(3.6f, 3.6f, 0.03f), FLinearColor(0.9f, 0.95f, 1.f));
	WhirlRing->SetVisibility(false);
}

void AAlexiaHero::PlayArtMontage(int32 Index, bool bSkill)
{
	if (!bUsingArtMesh) return;
	const FAlexiaCharacterArt* Art = UAlexiaSettings::Get()->HeroArt.Find(HeroClass);
	if (!Art) return;
	UAnimMontage* M = nullptr;
	if (bSkill) M = Art->SkillMontage.LoadSynchronous();
	else if (Art->AttackMontages.Num() > 0) M = Art->AttackMontages[Index % Art->AttackMontages.Num()].LoadSynchronous();
	if (M) PlayAnimMontage(M);
}

// ======================================================================== staty

const FAlexiaItem& AAlexiaHero::Equipped(EItemSlot Slot) const
{
	switch (Slot)
	{
	case EItemSlot::Armor: return Armor;
	case EItemSlot::Ring: return Ring;
	default: return Weapon;
	}
}

int32 AAlexiaHero::GetAtk() const
{
	return FMath::RoundToInt((8 + Level * 3 + Weapon.Atk + Armor.Atk + Ring.Atk) * Info().AtkMul);
}

int32 AAlexiaHero::GetDef() const
{
	return Level + Weapon.Def + Armor.Def + Ring.Def;
}

float AAlexiaHero::GetMaxHp() const
{
	return (100.f + Level * 20.f) * Info().HpMul + Weapon.Hp + Armor.Hp + Ring.Hp;
}

void AAlexiaHero::RecalcStats(bool bKeepRatio)
{
	(void)bKeepRatio;
	Hp = FMath::Min(Hp, GetMaxHp());
}

void AAlexiaHero::Say(const FString& Msg)
{
	Message = Msg;
	MessageTimer = 3.5f;
}

// ======================================================================== slučka

void AAlexiaHero::Tick(float Dt)
{
	Super::Tick(Dt);
	if (MessageTimer > 0.f) MessageTimer -= Dt;
	if (!bStarted) return;

	if (bDead)
	{
		RespawnTimer -= Dt;
		if (RespawnTimer <= 0.f) Respawn();
		return;
	}

	ComboTimer -= Dt; AttackTimer -= Dt; SwingTimer -= Dt; WhirlTimer -= Dt;
	Skill1Cd -= Dt; Skill2Cd -= Dt; PotionCd -= Dt; Invuln -= Dt;

	SaveTimer += Dt;
	if (SaveTimer > 30.f) { SaveTimer = 0.f; SaveGame(); }

	if (DashTimer > 0.f)
	{
		DashTimer -= Dt;
		AddActorWorldOffset(DashVelocity * Dt, true);
	}

	if (bAttackHeld && AttackTimer <= 0.f) DoAttack();

	if (AAlexiaWorldGen* W = AAlexiaWorldGen::Get(GetWorld()))
	{
		FVector L = GetActorLocation();
		if (W->ClampToWorld(L)) SetActorLocation(L);
		ZoneName = AlexiaData::BiomeName(W->BiomeAt(L));
		ZoneLevel = W->ZoneLevel(L);
	}

	// boss v boji
	ActiveBoss = nullptr;
	for (TActorIterator<AAlexiaEnemy> It(GetWorld()); It; ++It)
	{
		if (It->IsBoss() && It->IsAlive() && It->bAggro && FVector::Dist2D(It->GetActorLocation(), GetActorLocation()) < 3500.f)
		{
			ActiveBoss = *It;
			break;
		}
	}

	// procedurálna animácia (keď nie je model z Fab)
	if (!bUsingArtMesh && WeaponPivot)
	{
		const float Speed = GetVelocity().Size2D();
		WalkAnim += Dt * (Speed > 50.f ? 12.f : 3.f);
		VisualRoot->SetRelativeLocation(FVector(0, 0, Speed > 50.f ? FMath::Abs(FMath::Sin(WalkAnim)) * 6.f : FMath::Sin(WalkAnim) * 1.5f));
		if (SwingTimer > 0.f && !Info().bRanged)
		{
			const float T = 1.f - SwingTimer / SwingDuration;
			const float Dir = (ComboStep % 2 == 0) ? -1.f : 1.f;
			const float Yaw = FMath::Lerp(-100.f, 100.f, T) * Dir;
			const float Pitch = ComboStep == 3 ? FMath::Lerp(60.f, -40.f, T) : 0.f;
			WeaponPivot->SetRelativeRotation(FRotator(Pitch, Yaw, 0));
			if (HeroClass == EHeroClass::Assassin) OffhandPivot->SetRelativeRotation(FRotator(0, -Yaw, 0));
		}
		else if (SwingTimer > 0.f)
		{
			OffhandPivot->SetRelativeLocation(FVector(6.f - 8.f * (SwingTimer / SwingDuration), -36, 6));
		}
		else
		{
			const float Idle = FMath::Sin(WalkAnim * 0.5f) * 6.f;
			WeaponPivot->SetRelativeRotation(FRotator(HeroClass == EHeroClass::Mage ? 0.f : 35.f + Idle, 20.f, 0));
			OffhandPivot->SetRelativeRotation(FRotator(0, HeroClass == EHeroClass::Archer ? 0.f : -10.f, 0));
		}
		if (WhirlRing)
		{
			WhirlRing->SetVisibility(WhirlTimer > 0.f);
			if (WhirlTimer > 0.f)
			{
				WhirlRing->SetWorldRotation(FRotator(0, GetWorld()->GetTimeSeconds() * 1500.f, 0));
				VisualRoot->SetRelativeRotation(FRotator(0, GetWorld()->GetTimeSeconds() * 1500.f, 0));
			}
			else VisualRoot->SetRelativeRotation(FRotator::ZeroRotator);
		}
	}
}

// ======================================================================== pohyb

void AAlexiaHero::MoveForward(float Value)
{
	MoveInput.X = Value;
	if (!bStarted || bDead || DashTimer > 0.f || FMath::IsNearlyZero(Value)) return;
	const FRotator Yaw(0.f, CameraBoom->GetComponentRotation().Yaw, 0.f);
	const float Slow = (AttackTimer > 0.f && !Info().bRanged) ? 0.55f : 1.f;
	AddMovementInput(FRotationMatrix(Yaw).GetUnitAxis(EAxis::X), Value * Slow);
}

void AAlexiaHero::MoveRight(float Value)
{
	MoveInput.Y = Value;
	if (!bStarted || bDead || DashTimer > 0.f || FMath::IsNearlyZero(Value)) return;
	const FRotator Yaw(0.f, CameraBoom->GetComponentRotation().Yaw, 0.f);
	const float Slow = (AttackTimer > 0.f && !Info().bRanged) ? 0.55f : 1.f;
	AddMovementInput(FRotationMatrix(Yaw).GetUnitAxis(EAxis::Y), Value * Slow);
}

void AAlexiaHero::CameraZoom(float Value)
{
	if (!FMath::IsNearlyZero(Value)) CameraBoom->TargetArmLength = FMath::Clamp(CameraBoom->TargetArmLength - Value * 120.f, 800.f, 2600.f);
}

FVector AAlexiaHero::MoveDir() const
{
	if (MoveInput.SizeSquared() > 0.02f)
	{
		const FRotator Yaw(0.f, CameraBoom->GetComponentRotation().Yaw, 0.f);
		const FRotationMatrix M(Yaw);
		return (M.GetUnitAxis(EAxis::X) * MoveInput.X + M.GetUnitAxis(EAxis::Y) * MoveInput.Y).GetSafeNormal2D();
	}
	return GetActorForwardVector().GetSafeNormal2D();
}

void AAlexiaHero::FaceTowards(const FVector& Target)
{
	FVector D = Target - GetActorLocation();
	D.Z = 0;
	if (!D.IsNearlyZero()) SetActorRotation(D.Rotation());
}

// ======================================================================== boj

AAlexiaEnemy* AAlexiaHero::NearestEnemy(float Range) const
{
	AAlexiaEnemy* Best = nullptr;
	float BestD = Range;
	for (TActorIterator<AAlexiaEnemy> It(GetWorld()); It; ++It)
	{
		if (!It->IsAlive()) continue;
		const float D = FVector::Dist2D(It->GetActorLocation(), GetActorLocation()) - It->GetRadius();
		if (D < BestD) { BestD = D; Best = *It; }
	}
	return Best;
}

void AAlexiaHero::DoAttack()
{
	const FHeroClassInfo& I = Info();
	if (AAlexiaEnemy* T = NearestEnemy(I.bRanged ? 1900.f : 700.f)) FaceTowards(T->GetActorLocation());
	ComboStep = ComboTimer > 0.f ? ComboStep % 3 + 1 : 1;
	ComboTimer = 0.85f;
	const float A = (float)GetAtk();
	PlayArtMontage(ComboStep - 1, false);

	switch (HeroClass)
	{
	case EHeroClass::Warrior:
	{
		static const float Mult[] = { 1.f, 1.25f, 2.2f };
		AttackTimer = ComboStep == 3 ? 0.5f : 0.32f;
		SwingDuration = AttackTimer * 0.8f;
		Melee(ComboStep == 3 ? 330.f : 260.f, ComboStep == 3 ? 240.f : 140.f, A * Mult[ComboStep - 1]);
		break;
	}
	case EHeroClass::Assassin:
		AttackTimer = 0.18f;
		SwingDuration = 0.16f;
		Melee(220.f, 130.f, A * (ComboStep == 3 ? 1.9f : 0.8f));
		break;
	case EHeroClass::Mage:
		AttackTimer = 0.45f;
		SwingDuration = 0.3f;
		if (ComboStep == 3) for (int32 k = -1; k <= 1; ++k) Shoot(k * 14.f, 1500.f, A * 1.2f, FLinearColor(1.f, 0.35f, 0.05f), 40.f, true, false);
		else Shoot(0.f, 1600.f, A * 1.1f, FLinearColor(1.f, 0.45f, 0.1f), 34.f, true, false);
		break;
	case EHeroClass::Archer:
		AttackTimer = 0.32f;
		SwingDuration = 0.25f;
		Shoot(0.f, 2600.f, A * (ComboStep == 3 ? 1.8f : 1.f), FLinearColor(0.95f, 0.9f, 0.8f), 25.f, false, ComboStep == 3);
		break;
	}
	SwingTimer = SwingDuration;
}

void AAlexiaHero::Melee(float Range, float ArcDeg, float Damage, float CritBonus)
{
	const FVector Loc = GetActorLocation();
	const FVector Fwd = GetActorForwardVector().GetSafeNormal2D();
	TArray<AAlexiaEnemy*> Targets;
	for (TActorIterator<AAlexiaEnemy> It(GetWorld()); It; ++It) if (It->IsAlive()) Targets.Add(*It);
	for (AAlexiaEnemy* E : Targets)
	{
		FVector To = E->GetActorLocation() - Loc;
		To.Z = 0;
		const float D = To.Size() - E->GetRadius();
		if (D > Range) continue;
		const float Angle = FMath::RadiansToDegrees(FMath::Acos(FVector::DotProduct(Fwd, To.GetSafeNormal())));
		if (Angle <= ArcDeg * 0.5f || D < 40.f) DamageEnemy(E, Damage, CritBonus);
	}
}

void AAlexiaHero::DamageArea(const FVector& Center, float Radius, float Damage)
{
	TArray<AAlexiaEnemy*> Targets;
	for (TActorIterator<AAlexiaEnemy> It(GetWorld()); It; ++It)
	{
		if (It->IsAlive() && FVector::Dist2D(It->GetActorLocation(), Center) - It->GetRadius() < Radius) Targets.Add(*It);
	}
	for (AAlexiaEnemy* E : Targets) DamageEnemy(E, Damage, 0.f);
}

void AAlexiaHero::DamageEnemy(AAlexiaEnemy* E, float Base, float CritBonus)
{
	if (!E || !E->IsAlive()) return;
	const bool bCrit = Rng.FRand() < Info().Crit + CritBonus;
	const float Dmg = Base * Rng.FRandRange(0.9f, 1.1f) * (bCrit ? 2.f : 1.f);
	E->ReceiveHeroDamage(Dmg, GetActorLocation(), bCrit, this);
}

void AAlexiaHero::Shoot(float YawOffset, float Speed, float Damage, const FLinearColor& Color, float Radius, bool bFire, bool bPierce)
{
	const FRotator Rot(0.f, GetActorRotation().Yaw + YawOffset, 0.f);
	const FVector Dir = Rot.Vector();
	FActorSpawnParameters P;
	P.SpawnCollisionHandlingOverride = ESpawnActorCollisionHandlingMethod::AlwaysSpawn;
	if (AAlexiaProjectile* Pr = GetWorld()->SpawnActor<AAlexiaProjectile>(AAlexiaProjectile::StaticClass(), GetActorLocation() + Dir * 70.f + FVector(0, 0, 20), Rot, P))
	{
		Pr->Init(this, Dir * Speed, Damage, Color, Radius, bFire, bPierce);
	}
}

void AAlexiaHero::Dash(const FVector& Dir)
{
	DashTimer = 0.2f;
	DashVelocity = Dir * 2600.f;
	Invuln = 0.35f;
	SetActorRotation(Dir.Rotation());
}

void AAlexiaHero::UseSkill1()
{
	if (!bStarted || bDead || Skill1Cd > 0.f) return;
	const float A = (float)GetAtk();
	switch (HeroClass)
	{
	case EHeroClass::Warrior:
		Skill1Cd = 6.f;
		WhirlTimer = 0.5f;
		DamageArea(GetActorLocation(), 420.f, A * 2.5f);
		break;
	case EHeroClass::Assassin:
	{
		AAlexiaEnemy* T = NearestEnemy(1100.f);
		if (!T) { Say(TEXT("Žiadny cieľ v dosahu")); return; }
		Skill1Cd = 5.f;
		FVector Dir = (T->GetActorLocation() - GetActorLocation()).GetSafeNormal2D();
		const FVector Behind = T->GetActorLocation() + Dir * (T->GetRadius() + 80.f);
		SetActorLocation(FVector(Behind.X, Behind.Y, GetActorLocation().Z), false, nullptr, ETeleportType::TeleportPhysics);
		FaceTowards(T->GetActorLocation());
		Invuln = 0.4f;
		DamageEnemy(T, A * 3.8f, 1.f);
		AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, 150), TEXT("TIEŇ!"), Info().Accent, 70.f);
		break;
	}
	case EHeroClass::Mage:
	{
		Skill1Cd = 7.f;
		AAlexiaEnemy* T = NearestEnemy(1500.f);
		const FVector Target = T ? T->GetActorLocation() : GetActorLocation() + GetActorForwardVector() * 700.f;
		AAlexiaFloatText::Spawn(GetWorld(), Target + FVector(0, 0, 200), TEXT("METEOR!"), FLinearColor(1.f, 0.4f, 0.05f), 110.f);
		DamageArea(Target, 450.f, A * 3.2f);
		break;
	}
	case EHeroClass::Archer:
	{
		Skill1Cd = 5.f;
		if (AAlexiaEnemy* T = NearestEnemy(1900.f)) FaceTowards(T->GetActorLocation());
		for (int32 k = -3; k <= 3; ++k) Shoot(k * 9.f, 2500.f, A * 1.3f, FLinearColor(1.f, 0.88f, 0.55f), 25.f, false, false);
		break;
	}
	}
	SwingTimer = SwingDuration = 0.3f;
	PlayArtMontage(0, true);
}

void AAlexiaHero::UseSkill2()
{
	if (!bStarted || bDead || Skill2Cd > 0.f) return;
	const FVector Dir = MoveDir();
	if (HeroClass == EHeroClass::Mage)
	{
		Skill2Cd = 4.f;
		SetActorLocation(GetActorLocation() + Dir * 650.f, true);
		Invuln = 0.35f;
		AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, 120), TEXT("Teleport"), Info().Accent, 50.f);
	}
	else
	{
		Skill2Cd = HeroClass == EHeroClass::Assassin ? 2.f : 3.f;
		Dash(Dir);
	}
}

void AAlexiaHero::UsePotion()
{
	if (!bStarted || bDead || PotionCd > 0.f) return;
	if (Potions <= 0) { Say(TEXT("Nemáš elixíry – kúp ich v taške")); return; }
	if (Hp >= GetMaxHp()) return;
	Potions--;
	PotionCd = 1.f;
	const float Heal = GetMaxHp() * 0.45f;
	Hp = FMath::Min(GetMaxHp(), Hp + Heal);
	AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, 130), FString::Printf(TEXT("+%d"), FMath::RoundToInt(Heal)), FLinearColor(0.3f, 1.f, 0.45f), 60.f);
}

void AAlexiaHero::ReceiveEnemyDamage(float Raw)
{
	if (!bStarted || bDead || Invuln > 0.f) return;
	const float Dmg = Raw * 100.f / (100.f + GetDef() * 4.f);
	Hp -= Dmg;
	Invuln = 0.15f;
	AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, 120), FString::Printf(TEXT("-%d"), FMath::RoundToInt(Dmg)), FLinearColor(1.f, 0.2f, 0.2f), 55.f);
	if (Hp <= 0.f) Die();
}

void AAlexiaHero::Die()
{
	Hp = 0.f;
	bDead = true;
	RespawnTimer = 3.f;
	const int32 Lost = Gold / 10;
	Gold -= Lost;
	Say(FString::Printf(TEXT("Padol si! Stratil si %d zlata."), Lost));
	VisualRoot->SetRelativeRotation(FRotator(0, 0, 90));
	GetCharacterMovement()->DisableMovement();
}

void AAlexiaHero::Respawn()
{
	bDead = false;
	VisualRoot->SetRelativeRotation(FRotator::ZeroRotator);
	GetCharacterMovement()->SetMovementMode(MOVE_Walking);
	SetActorLocation(SpawnPoint(), false, nullptr, ETeleportType::TeleportPhysics);
	Hp = GetMaxHp();
	Invuln = 2.f;
	Say(TEXT("Prebudil si sa v Údolí Alexie."));
}

// ======================================================================== odmeny a taška

void AAlexiaHero::AddXp(int32 Amount)
{
	Xp += Amount;
	AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, 170), FString::Printf(TEXT("+%d XP"), Amount), FLinearColor(0.6f, 0.85f, 1.f), 45.f);
	while (Xp >= XpNext())
	{
		Xp -= XpNext();
		Level++;
		RecalcStats(false);
		Hp = GetMaxHp();
		AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, 220), FString::Printf(TEXT("LEVEL %d!"), Level), FLinearColor(1.f, 0.82f, 0.25f), 110.f);
		Say(FString::Printf(TEXT("Nový level %d! Silnejší útok a viac života."), Level));
	}
}

void AAlexiaHero::AddGold(int32 Amount)
{
	Gold += Amount;
	AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, 150), FString::Printf(TEXT("+%d zlata"), Amount), FLinearColor(1.f, 0.82f, 0.25f), 45.f);
}

void AAlexiaHero::AddPotion()
{
	Potions++;
	AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, 150), TEXT("+1 elixír"), FLinearColor(0.3f, 1.f, 0.45f), 45.f);
}

void AAlexiaHero::AddItem(const FAlexiaItem& Item)
{
	if (Inventory.Num() >= 40)
	{
		Gold += Item.SellPrice();
		Say(TEXT("Taška je plná – predmet predaný"));
		return;
	}
	Inventory.Add(Item);
	InventoryRevision++;
	AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, 160), Item.Name, AlexiaData::RarityColor(Item.Rarity), 45.f);
	if ((int32)Item.Rarity >= 2) Say(FString::Printf(TEXT("%s: %s!"), *AlexiaData::RarityName(Item.Rarity), *Item.Name));
}

void AAlexiaHero::EquipFromInventory(int32 Index)
{
	if (!Inventory.IsValidIndex(Index)) return;
	const FAlexiaItem New = Inventory[Index];
	Inventory.RemoveAt(Index);
	const float Ratio = Hp / FMath::Max(1.f, GetMaxHp());
	FAlexiaItem* Slot = New.Slot == EItemSlot::Armor ? &Armor : New.Slot == EItemSlot::Ring ? &Ring : &Weapon;
	if (Slot->IsValid()) Inventory.Add(*Slot);
	*Slot = New;
	Hp = FMath::Max(1.f, GetMaxHp() * Ratio);
	InventoryRevision++;
	Say(FString::Printf(TEXT("Nasadené: %s"), *New.Name));
}

void AAlexiaHero::SellJunk()
{
	int32 Count = 0, Total = 0;
	for (int32 i = Inventory.Num() - 1; i >= 0; --i)
	{
		if (!IsUpgrade(Inventory[i]))
		{
			Total += Inventory[i].SellPrice();
			Inventory.RemoveAt(i);
			Count++;
		}
	}
	if (Count == 0) { Say(TEXT("Nič slabšie na predaj")); return; }
	Gold += Total;
	InventoryRevision++;
	Say(FString::Printf(TEXT("Predané %d predmetov za %d zlata"), Count, Total));
}

void AAlexiaHero::BuyPotion()
{
	if (Gold < PotionPrice()) { Say(FString::Printf(TEXT("Elixír stojí %d zlata"), PotionPrice())); return; }
	Gold -= PotionPrice();
	Potions++;
	Say(FString::Printf(TEXT("Kúpený elixír (%d)"), Potions));
}
