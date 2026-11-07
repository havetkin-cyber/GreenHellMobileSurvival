#include "AlexiaEnemy.h"
#include "AlexiaHero.h"
#include "AlexiaEffects.h"
#include "AlexiaSettings.h"
#include "AlexiaVisuals.h"
#include "AlexiaWorldGen.h"
#include "AIController.h"
#include "Animation/AnimInstance.h"
#include "Animation/AnimMontage.h"
#include "Components/CapsuleComponent.h"
#include "Components/SkeletalMeshComponent.h"
#include "Components/StaticMeshComponent.h"
#include "Components/TextRenderComponent.h"
#include "Engine/SkeletalMesh.h"
#include "GameFramework/CharacterMovementComponent.h"
#include "Kismet/GameplayStatics.h"
#include "Camera/PlayerCameraManager.h"

AAlexiaEnemy::AAlexiaEnemy()
{
	PrimaryActorTick.bCanEverTick = true;
	AutoPossessAI = EAutoPossessAI::PlacedInWorldOrSpawned;
	AIControllerClass = AAIController::StaticClass();
	bUseControllerRotationYaw = false;
	GetCharacterMovement()->bOrientRotationToMovement = true;
	GetCharacterMovement()->RotationRate = FRotator(0.f, 540.f, 0.f);
	GetCharacterMovement()->bRunPhysicsWithNoController = true;

	Body = CreateDefaultSubobject<USceneComponent>(TEXT("Body"));
	Body->SetupAttachment(RootComponent);

	Label = CreateDefaultSubobject<UTextRenderComponent>(TEXT("Label"));
	Label->SetupAttachment(RootComponent);
	Label->SetHorizontalAlignment(EHTA_Center);
	Label->SetCollisionEnabled(ECollisionEnabled::NoCollision);
	Label->SetCastShadow(false);
	Label->SetUsingAbsoluteRotation(true);
}

void AAlexiaEnemy::Init(EEnemyKind InKind, int32 InLevel, const FString& InBossName)
{
	Kind = InKind;
	Level = FMath::Max(1, InLevel);
	BossName = InBossName;
	const FEnemyInfo& I = AlexiaData::Enemy(Kind);
	GetCapsuleComponent()->SetCapsuleSize(I.Radius, I.HalfHeight);
	GetCharacterMovement()->MaxWalkSpeed = I.Speed;
	MaxHp = 30.f * I.HpMul * (1.f + (Level - 1) * 0.35f);
	Hp = MaxHp;
	Atk = 6.f * I.AtkMul * (1.f + (Level - 1) * 0.3f);
	Home = GetActorLocation();
	WanderTarget = Home;
	Anim = FMath::FRand() * 10.f;
	BuildVisuals();
	Label->SetRelativeLocation(FVector(0, 0, I.HalfHeight + (I.bBoss ? 140.f : 60.f)));
	Label->SetWorldSize(I.bBoss ? 60.f : 34.f);
	UpdateLabel();
}

void AAlexiaEnemy::BuildVisuals()
{
	const FEnemyInfo& I = AlexiaData::Enemy(Kind);
	if (const FAlexiaCharacterArt* Art = UAlexiaSettings::Get()->EnemyArt.Find(Kind))
	{
		if (USkeletalMesh* Mesh = Art->Mesh.LoadSynchronous())
		{
			GetMesh()->SetSkeletalMesh(Mesh);
			if (UClass* AnimCls = Art->AnimClass.LoadSynchronous()) GetMesh()->SetAnimInstanceClass(AnimCls);
			GetMesh()->SetRelativeLocationAndRotation(FVector(0, 0, -I.HalfHeight) + FVector(0, 0, Art->MeshOffset.Z + 90.f), Art->MeshRotation);
			GetMesh()->SetRelativeScale3D(FVector(Art->MeshScale));
			bUsingArtMesh = true;
		}
	}

	const float R = I.Radius;
	const float H = I.HalfHeight;
	const float Feet = -H;
	const FLinearColor C = I.Color;
	const FLinearColor Dark = C * 0.6f;
	const FLinearColor Eye(1.f, 0.85f, 0.2f);

	auto Add = [this](const TCHAR* S, FVector L, FVector Sc, FLinearColor Col, FRotator Rot = FRotator::ZeroRotator)
	{
		UStaticMeshComponent* P = AlexiaVisuals::AddPart(this, Body.Get(), S, L, Sc, Col, Rot);
		Parts.Add(P);
		return P;
	};

	if (!bUsingArtMesh)
	{
		GetMesh()->SetVisibility(false);
		switch (Kind)
		{
		case EEnemyKind::Slime:
			Add(TEXT("Sphere"), FVector(0, 0, Feet + R * 0.8f), FVector(R / 50.f, R / 50.f, R / 70.f), C);
			Add(TEXT("Sphere"), FVector(R * 0.7f, -R * 0.3f, Feet + R), FVector(0.16f), FLinearColor::White);
			Add(TEXT("Sphere"), FVector(R * 0.7f, R * 0.3f, Feet + R), FVector(0.16f), FLinearColor::White);
			break;
		case EEnemyKind::Wolf:
			Add(TEXT("Cube"), FVector(-10, 0, Feet + 50), FVector(1.1f, 0.45f, 0.45f), C);
			Add(TEXT("Sphere"), FVector(55, 0, Feet + 62), FVector(0.5f), C);
			Add(TEXT("Cone"), FVector(52, -14, Feet + 92), FVector(0.14f, 0.14f, 0.25f), Dark);
			Add(TEXT("Cone"), FVector(52, 14, Feet + 92), FVector(0.14f, 0.14f, 0.25f), Dark);
			Add(TEXT("Cube"), FVector(80, 0, Feet + 55), FVector(0.3f, 0.2f, 0.18f), Dark);
			Add(TEXT("Sphere"), FVector(74, -12, Feet + 68), FVector(0.07f), Eye);
			Add(TEXT("Sphere"), FVector(74, 12, Feet + 68), FVector(0.07f), Eye);
			Add(TEXT("Cylinder"), FVector(-70, 0, Feet + 60), FVector(0.12f, 0.12f, 0.5f), Dark, FRotator(50, 0, 0));
			for (int32 k = 0; k < 4; ++k)
				Add(TEXT("Cylinder"), FVector(k < 2 ? 30 : -45, (k % 2 ? 1 : -1) * 16, Feet + 15), FVector(0.12f, 0.12f, 0.32f), Dark);
			break;
		case EEnemyKind::Spider:
			Add(TEXT("Sphere"), FVector(-25, 0, Feet + 40), FVector(0.85f, 0.85f, 0.6f), C);
			Add(TEXT("Sphere"), FVector(30, 0, Feet + 35), FVector(0.45f), C);
			Add(TEXT("Sphere"), FVector(50, -8, Feet + 42), FVector(0.06f), FLinearColor(1.f, 0.1f, 0.1f));
			Add(TEXT("Sphere"), FVector(50, 8, Feet + 42), FVector(0.06f), FLinearColor(1.f, 0.1f, 0.1f));
			for (int32 k = 0; k < 8; ++k)
			{
				const float Side = k < 4 ? -1.f : 1.f;
				const float Ang = (k % 4 - 1.5f) * 30.f;
				Add(TEXT("Cube"), FVector(Ang * 0.8f, Side * 55.f, Feet + 25), FVector(0.06f, 0.9f, 0.06f), Dark, FRotator(0, Ang * Side, Side * 25.f));
			}
			break;
		case EEnemyKind::Scorpion:
			Add(TEXT("Sphere"), FVector(0, 0, Feet + 30), FVector(1.2f, 0.8f, 0.45f), C);
			Add(TEXT("Sphere"), FVector(70, -35, Feet + 30), FVector(0.35f), Dark);
			Add(TEXT("Sphere"), FVector(70, 35, Feet + 30), FVector(0.35f), Dark);
			Add(TEXT("Cylinder"), FVector(-60, 0, Feet + 70), FVector(0.14f, 0.14f, 0.7f), C, FRotator(-40, 0, 0));
			Add(TEXT("Cone"), FVector(-35, 0, Feet + 115), FVector(0.18f, 0.18f, 0.3f), FLinearColor(0.15f, 0.08f, 0.03f), FRotator(-120, 0, 0));
			break;
		case EEnemyKind::Yeti:
			Add(TEXT("Sphere"), FVector(0, 0, Feet + 110), FVector(1.3f, 1.3f, 1.6f), C);
			Add(TEXT("Sphere"), FVector(0, -70, Feet + 100), FVector(0.45f, 0.45f, 0.8f), C);
			Add(TEXT("Sphere"), FVector(0, 70, Feet + 100), FVector(0.45f, 0.45f, 0.8f), C);
			Add(TEXT("Sphere"), FVector(45, 0, Feet + 150), FVector(0.55f), FLinearColor(0.45f, 0.6f, 0.78f));
			Add(TEXT("Sphere"), FVector(68, -12, Feet + 160), FVector(0.08f), FLinearColor(0.05f, 0.05f, 0.1f));
			Add(TEXT("Sphere"), FVector(68, 12, Feet + 160), FVector(0.08f), FLinearColor(0.05f, 0.05f, 0.1f));
			break;
		case EEnemyKind::Golem:
			Add(TEXT("Cube"), FVector(0, 0, Feet + 200), FVector(1.8f, 2.2f, 1.6f), C);
			Add(TEXT("Cube"), FVector(10, 0, Feet + 330), FVector(0.9f, 0.9f, 0.8f), C * 1.1f);
			Add(TEXT("Cube"), FVector(0, -150, Feet + 180), FVector(0.7f, 0.7f, 1.6f), Dark);
			Add(TEXT("Cube"), FVector(0, 150, Feet + 180), FVector(0.7f, 0.7f, 1.6f), Dark);
			Add(TEXT("Cube"), FVector(0, -55, Feet + 55), FVector(0.7f, 0.7f, 1.1f), Dark);
			Add(TEXT("Cube"), FVector(0, 55, Feet + 55), FVector(0.7f, 0.7f, 1.1f), Dark);
			Add(TEXT("Sphere"), FVector(55, -20, Feet + 340), FVector(0.12f), FLinearColor(1.f, 0.45f, 0.05f));
			Add(TEXT("Sphere"), FVector(55, 20, Feet + 340), FVector(0.12f), FLinearColor(1.f, 0.45f, 0.05f));
			Add(TEXT("Sphere"), FVector(-20, 60, Feet + 290), FVector(0.4f, 0.4f, 0.2f), FLinearColor(0.25f, 0.5f, 0.2f));
			break;
		case EEnemyKind::Treant:
			Add(TEXT("Cylinder"), FVector(0, 0, Feet + 180), FVector(1.3f, 1.3f, 3.6f), C);
			Add(TEXT("Sphere"), FVector(0, 0, Feet + 420), FVector(3.0f, 3.0f, 2.2f), FLinearColor(0.18f, 0.42f, 0.18f));
			Add(TEXT("Sphere"), FVector(40, -120, Feet + 470), FVector(1.6f), FLinearColor(0.25f, 0.55f, 0.22f));
			Add(TEXT("Sphere"), FVector(-30, 110, Feet + 460), FVector(1.5f), FLinearColor(0.22f, 0.5f, 0.2f));
			Add(TEXT("Cylinder"), FVector(0, -110, Feet + 230), FVector(0.4f, 0.4f, 2.f), Dark, FRotator(0, 0, -45));
			Add(TEXT("Cylinder"), FVector(0, 110, Feet + 230), FVector(0.4f, 0.4f, 2.f), Dark, FRotator(0, 0, 45));
			Add(TEXT("Sphere"), FVector(66, -22, Feet + 260), FVector(0.14f), FLinearColor(0.6f, 1.f, 0.4f));
			Add(TEXT("Sphere"), FVector(66, 22, Feet + 260), FVector(0.14f), FLinearColor(0.6f, 1.f, 0.4f));
			break;
		case EEnemyKind::FrostWyrm:
			Add(TEXT("Sphere"), FVector(-160, 0, Feet + 90), FVector(0.8f), Dark);
			Add(TEXT("Sphere"), FVector(-60, 0, Feet + 110), FVector(1.5f, 1.3f, 1.2f), C);
			Add(TEXT("Sphere"), FVector(60, 0, Feet + 130), FVector(1.3f, 1.1f, 1.1f), C);
			Add(TEXT("Sphere"), FVector(160, 0, Feet + 170), FVector(0.8f), C * 1.1f);
			Add(TEXT("Cone"), FVector(150, -30, Feet + 230), FVector(0.2f, 0.2f, 0.6f), FLinearColor(0.9f, 0.97f, 1.f), FRotator(0, 0, -20));
			Add(TEXT("Cone"), FVector(150, 30, Feet + 230), FVector(0.2f, 0.2f, 0.6f), FLinearColor(0.9f, 0.97f, 1.f), FRotator(0, 0, 20));
			Add(TEXT("Cube"), FVector(0, -160, Feet + 180), FVector(1.6f, 2.2f, 0.05f), Dark, FRotator(0, 0, -25));
			Add(TEXT("Cube"), FVector(0, 160, Feet + 180), FVector(1.6f, 2.2f, 0.05f), Dark, FRotator(0, 0, 25));
			Add(TEXT("Sphere"), FVector(195, -18, Feet + 180), FVector(0.12f), FLinearColor(0.8f, 1.f, 1.f));
			Add(TEXT("Sphere"), FVector(195, 18, Feet + 180), FVector(0.12f), FLinearColor(0.8f, 1.f, 1.f));
			break;
		default: // humanoidi: lupič, kostlivec, piesočný kráľ
		{
			const float K = H / 88.f;
			const FLinearColor Head = Kind == EEnemyKind::Skeleton ? FLinearColor(0.95f, 0.93f, 0.85f) : Kind == EEnemyKind::SandKing ? FLinearColor(0.7f, 0.5f, 0.2f) : FLinearColor(0.9f, 0.7f, 0.55f);
			Add(TEXT("Cylinder"), FVector(0, -16 * K, Feet + 22 * K), FVector(0.24f * K, 0.24f * K, 0.42f * K), Dark);
			Add(TEXT("Cylinder"), FVector(0, 16 * K, Feet + 22 * K), FVector(0.24f * K, 0.24f * K, 0.42f * K), Dark);
			Add(TEXT("Cylinder"), FVector(0, 0, Feet + 78 * K), FVector(0.6f * K, 0.6f * K, 0.72f * K), C);
			Add(TEXT("Sphere"), FVector(0, 0, Feet + 140 * K), FVector(0.44f * K), Head);
			Add(TEXT("Sphere"), FVector(18 * K, -8 * K, Feet + 144 * K), FVector(0.07f * K), Kind == EEnemyKind::Skeleton ? FLinearColor(0.9f, 0.1f, 0.05f) : FLinearColor::Black);
			Add(TEXT("Sphere"), FVector(18 * K, 8 * K, Feet + 144 * K), FVector(0.07f * K), Kind == EEnemyKind::Skeleton ? FLinearColor(0.9f, 0.1f, 0.05f) : FLinearColor::Black);
			Add(TEXT("Cube"), FVector(45 * K, 34 * K, Feet + 90 * K), FVector(0.9f * K, 0.06f * K, 0.12f * K), FLinearColor(0.75f, 0.75f, 0.78f), FRotator(20, 0, 0));
			if (Kind == EEnemyKind::Bandit) Add(TEXT("Cube"), FVector(5 * K, 0, Feet + 146 * K), FVector(0.48f * K, 0.48f * K, 0.1f * K), FLinearColor(0.2f, 0.12f, 0.1f));
			if (Kind == EEnemyKind::SandKing)
			{
				Add(TEXT("Cylinder"), FVector(0, 0, Feet + 172 * K), FVector(0.4f * K, 0.4f * K, 0.18f * K), FLinearColor(1.f, 0.8f, 0.15f));
				for (int32 k = 0; k < 5; ++k)
				{
					const float A = k * 72.f;
					Add(TEXT("Cone"), FVector(FMath::Cos(FMath::DegreesToRadians(A)) * 16 * K, FMath::Sin(FMath::DegreesToRadians(A)) * 16 * K, Feet + 190 * K), FVector(0.08f * K, 0.08f * K, 0.18f * K), FLinearColor(1.f, 0.85f, 0.2f));
				}
				Add(TEXT("Cube"), FVector(-20 * K, 0, Feet + 80 * K), FVector(0.1f * K, 0.9f * K, 1.3f * K), FLinearColor(0.55f, 0.1f, 0.12f));
			}
			break;
		}
		}
	}

	// kruh varovania pred plošným úderom
	Telegraph = AlexiaVisuals::AddPart(this, RootComponent, TEXT("Cylinder"), FVector(0, 0, Feet + 2), FVector(0.1f, 0.1f, 0.01f), FLinearColor(0.9f, 0.05f, 0.05f));
	Telegraph->SetVisibility(false);
}

void AAlexiaEnemy::UpdateLabel()
{
	const FString Name = IsBoss() ? DisplayName() : FString(AlexiaData::Enemy(Kind).Title);
	const int32 Bars = FMath::Clamp(FMath::CeilToInt(Hp / MaxHp * 10.f), 0, 10);
	FString Bar;
	for (int32 i = 0; i < 10; ++i) Bar += i < Bars ? TEXT("|") : TEXT(".");
	Label->SetText(FText::FromString(FString::Printf(TEXT("%s Lv%d<br>%s"), *Name, Level, *Bar)));
	const float F = Hp / MaxHp;
	Label->SetTextRenderColor(FLinearColor(1.f, 0.25f + 0.6f * F, 0.25f + 0.4f * F).ToFColor(true));
}

void AAlexiaEnemy::MoveToward(const FVector& Target, float SpeedScale)
{
	FVector D = Target - GetActorLocation();
	D.Z = 0;
	if (D.SizeSquared() < 100.f) return;
	AddMovementInput(D.GetSafeNormal(), SpeedScale);
}

void AAlexiaEnemy::Tick(float Dt)
{
	Super::Tick(Dt);
	Anim += Dt;

	if (APlayerCameraManager* Cam = UGameplayStatics::GetPlayerCameraManager(this, 0))
		Label->SetWorldRotation((-Cam->GetCameraRotation().Vector()).Rotation());

	if (bDying)
	{
		DeathTimer -= Dt;
		Body->SetRelativeScale3D(FVector(FMath::Max(0.01f, DeathTimer / 0.4f)));
		if (DeathTimer <= 0.f) Destroy();
		return;
	}

	AAlexiaHero* Hero = Cast<AAlexiaHero>(UGameplayStatics::GetPlayerPawn(this, 0));
	if (!Hero) return;

	HitFlash -= Dt;
	AttackCd -= Dt;
	SlamCd -= Dt;

	const FEnemyInfo& I = AlexiaData::Enemy(Kind);
	FVector To = Hero->GetActorLocation() - GetActorLocation();
	To.Z = 0;
	const float D = To.Size();
	const float AggroRange = I.bBoss ? 2000.f : 1200.f;
	const float Reach = I.Radius + 40.f + 70.f;

	if (!bAggro && D < AggroRange && Hero->IsAlive()) bAggro = true;
	if (bAggro && (D > 3500.f || FVector::Dist2D(GetActorLocation(), Home) > 4500.f || !Hero->IsAlive()))
	{
		bAggro = false;
		if (I.bBoss) { Hp = MaxHp; UpdateLabel(); }
	}

	// procedurálna animácia
	if (!bUsingArtMesh)
	{
		const float Speed = GetVelocity().Size2D();
		float Bob = Speed > 30.f ? FMath::Abs(FMath::Sin(Anim * 10.f)) * 8.f : FMath::Sin(Anim * 2.f) * 2.f;
		FVector Scale(1.f);
		if (Kind == EEnemyKind::Slime) { const float S = FMath::Sin(Anim * 7.f) * 0.12f; Scale = FVector(1.f + S, 1.f + S, 1.f - S); Bob = 0; }
		if (Windup > 0.f) Scale *= 1.f + 0.15f * FMath::Sin(Anim * 40.f);
		if (HitFlash > 0.f) Scale *= 1.12f;
		Body->SetRelativeLocation(FVector(0, 0, Bob));
		Body->SetRelativeScale3D(Scale);
	}

	if (SlamWindup > 0.f)
	{
		SlamWindup -= Dt;
		const float T = FMath::Clamp(1.f - SlamWindup / 1.1f, 0.f, 1.f);
		Telegraph->SetRelativeScale3D(FVector(11.f * T + 0.2f, 11.f * T + 0.2f, 0.01f));
		if (SlamWindup <= 0.f)
		{
			Telegraph->SetVisibility(false);
			AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, 100), TEXT("BUM!"), FLinearColor(1.f, 0.6f, 0.2f), 120.f);
			if (D < 550.f + 40.f) Hero->ReceiveEnemyDamage(Atk * 1.8f);
		}
	}
	else if (Windup > 0.f)
	{
		Windup -= Dt;
		if (Windup <= 0.f)
		{
			if (D < Reach + 60.f) Hero->ReceiveEnemyDamage(Atk);
			AttackCd = I.bBoss ? 1.2f : 1.4f;
		}
	}
	else if (bAggro)
	{
		if (!To.IsNearlyZero()) SetActorRotation(FMath::RInterpTo(GetActorRotation(), To.Rotation(), Dt, 8.f));
		if (I.bBoss && SlamCd <= 0.f && D < 900.f)
		{
			SlamWindup = 1.1f;
			SlamCd = 6.f;
			Telegraph->SetVisibility(true);
			AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, I.HalfHeight + 200), TEXT("!!!"), FLinearColor(1.f, 0.1f, 0.1f), 120.f);
		}
		else if (D > Reach) MoveToward(Hero->GetActorLocation(), 1.f);
		else if (AttackCd <= 0.f)
		{
			Windup = I.bBoss ? 0.55f : 0.45f;
			AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, I.HalfHeight + 80), TEXT("!"), FLinearColor(1.f, 0.15f, 0.15f), 70.f);
		}
	}
	else
	{
		WanderTimer -= Dt;
		if (WanderTimer <= 0.f)
		{
			WanderTimer = FMath::FRandRange(2.f, 5.f);
			WanderTarget = Home + FVector(FMath::FRandRange(-500.f, 500.f), FMath::FRandRange(-500.f, 500.f), 0.f);
		}
		MoveToward(WanderTarget, 0.35f);
	}

	if (AAlexiaWorldGen* W = AAlexiaWorldGen::Get(GetWorld()))
	{
		FVector L = GetActorLocation();
		if (W->ClampToWorld(L)) SetActorLocation(L);
	}
}

void AAlexiaEnemy::ReceiveHeroDamage(float Damage, const FVector& From, bool bCrit, AAlexiaHero* By)
{
	if (!IsAlive()) return;
	Hp -= Damage;
	HitFlash = 0.12f;
	bAggro = true;
	FVector Dir = GetActorLocation() - From;
	Dir.Z = 0;
	LaunchCharacter(Dir.GetSafeNormal() * (IsBoss() ? 120.f : 650.f) + FVector(0, 0, IsBoss() ? 0.f : 150.f), true, false);
	AAlexiaFloatText::Spawn(GetWorld(), GetActorLocation() + FVector(0, 0, AlexiaData::Enemy(Kind).HalfHeight + 40),
		FString::Printf(TEXT("%d%s"), FMath::RoundToInt(Damage), bCrit ? TEXT("!") : TEXT("")),
		bCrit ? FLinearColor(1.f, 0.82f, 0.2f) : FLinearColor::White, bCrit ? 80.f : 50.f);
	UpdateLabel();
	if (Hp <= 0.f) Die(By);
}

void AAlexiaEnemy::Die(AAlexiaHero* By)
{
	bDying = true;
	DeathTimer = 0.4f;
	SetActorEnableCollision(false);
	Label->SetVisibility(false);
	Telegraph->SetVisibility(false);

	if (bUsingArtMesh)
	{
		if (const FAlexiaCharacterArt* Art = UAlexiaSettings::Get()->EnemyArt.Find(Kind))
		{
			if (UAnimMontage* M = Art->DeathMontage.LoadSynchronous()) { PlayAnimMontage(M); DeathTimer = 1.5f; }
		}
	}

	const FEnemyInfo& I = AlexiaData::Enemy(Kind);
	FRandomStream Rng(FMath::Rand());
	if (By) By->AddXp(FMath::RoundToInt(10.f * I.XpMul * Level));

	UWorld* World = GetWorld();
	FActorSpawnParameters P;
	P.SpawnCollisionHandlingOverride = ESpawnActorCollisionHandlingMethod::AlwaysSpawn;
	const FVector Ground(GetActorLocation().X, GetActorLocation().Y, 2.f);
	auto Drop = [&](const FVector& Off, const FAlexiaItem& Item, int32 Gold, bool bPotion)
	{
		if (AAlexiaPickup* Pk = World->SpawnActor<AAlexiaPickup>(AAlexiaPickup::StaticClass(), Ground + Off, FRotator::ZeroRotator, P))
			Pk->Init(Item, Gold, bPotion);
	};

	const int32 Gold = (Level * 3 + Rng.RandRange(0, Level * 4 + 3)) * (I.bBoss ? 15 : 1);
	Drop(FVector::ZeroVector, FAlexiaItem(), Gold, false);
	if (I.bBoss)
	{
		Drop(FVector(120, 0, 0), AlexiaData::RollItem(Level, Rng, true), 0, false);
		Drop(FVector(-120, 0, 0), AlexiaData::RollItem(Level, Rng, true), 0, false);
		Drop(FVector(0, 120, 0), FAlexiaItem(), 0, true);
		if (By) By->Say(FString::Printf(TEXT("Porazil si: %s!"), *DisplayName()));
		if (AAlexiaWorldGen* W = AAlexiaWorldGen::Get(World)) W->OnBossKilled(this);
	}
	else
	{
		if (Rng.FRand() < 0.22f) Drop(FVector(70, 0, 0), AlexiaData::RollItem(Level, Rng, false), 0, false);
		if (Rng.FRand() < 0.08f) Drop(FVector(-70, 0, 0), FAlexiaItem(), 0, true);
	}
}
