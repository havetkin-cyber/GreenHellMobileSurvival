#include "AlexiaEffects.h"
#include "AlexiaHero.h"
#include "AlexiaEnemy.h"
#include "AlexiaVisuals.h"
#include "Components/TextRenderComponent.h"
#include "Components/StaticMeshComponent.h"
#include "Kismet/GameplayStatics.h"
#include "Camera/PlayerCameraManager.h"
#include "EngineUtils.h"

static FRotator FaceCamera(const UObject* Ctx)
{
	if (APlayerCameraManager* Cam = UGameplayStatics::GetPlayerCameraManager(Ctx, 0))
	{
		return (-Cam->GetCameraRotation().Vector()).Rotation();
	}
	return FRotator::ZeroRotator;
}

// ------------------------------------------------------------------ FloatText

AAlexiaFloatText::AAlexiaFloatText()
{
	PrimaryActorTick.bCanEverTick = true;
	Text = CreateDefaultSubobject<UTextRenderComponent>(TEXT("Text"));
	RootComponent = Text;
	Text->SetHorizontalAlignment(EHTA_Center);
	Text->SetVerticalAlignment(EVRTA_TextCenter);
	Text->SetCollisionEnabled(ECollisionEnabled::NoCollision);
	Text->SetCastShadow(false);
}

void AAlexiaFloatText::Spawn(UWorld* World, const FVector& Location, const FString& Str, const FLinearColor& Color, float Size)
{
	if (!World) return;
	FActorSpawnParameters P;
	P.SpawnCollisionHandlingOverride = ESpawnActorCollisionHandlingMethod::AlwaysSpawn;
	const FVector Jitter(FMath::FRandRange(-30.f, 30.f), FMath::FRandRange(-30.f, 30.f), 0.f);
	AAlexiaFloatText* T = World->SpawnActor<AAlexiaFloatText>(AAlexiaFloatText::StaticClass(), Location + Jitter, FRotator::ZeroRotator, P);
	if (!T) return;
	T->Text->SetText(FText::FromString(Str));
	T->Text->SetTextRenderColor(Color.ToFColor(true));
	T->Text->SetWorldSize(Size);
	T->SetActorRotation(FaceCamera(World));
}

void AAlexiaFloatText::Tick(float Dt)
{
	Super::Tick(Dt);
	Life -= Dt;
	AddActorWorldOffset(FVector(0, 0, 140.f * Dt));
	SetActorRotation(FaceCamera(this));
	if (Life < 0.4f) SetActorScale3D(FVector(FMath::Max(0.05f, Life / 0.4f)));
	if (Life <= 0.f) Destroy();
}

// ------------------------------------------------------------------ Projectile

AAlexiaProjectile::AAlexiaProjectile()
{
	PrimaryActorTick.bCanEverTick = true;
	RootComponent = CreateDefaultSubobject<USceneComponent>(TEXT("Root"));
}

void AAlexiaProjectile::Init(AAlexiaHero* InOwner, const FVector& InVelocity, float InDamage, const FLinearColor& Color, float InRadius, bool bInFire, bool bInPierce)
{
	Hero = InOwner;
	Velocity = InVelocity;
	Damage = InDamage;
	Radius = InRadius;
	bFire = bInFire;
	bPierce = bInPierce;
	SetActorRotation(Velocity.Rotation());
	if (bFire)
	{
		AlexiaVisuals::AddPart(this, RootComponent, TEXT("Sphere"), FVector::ZeroVector, FVector(Radius / 50.f), Color);
		AlexiaVisuals::AddPart(this, RootComponent, TEXT("Sphere"), FVector(-Radius, 0, 0), FVector(Radius / 80.f), FLinearColor(1.f, 0.9f, 0.4f));
	}
	else
	{
		AlexiaVisuals::AddPart(this, RootComponent, TEXT("Cylinder"), FVector::ZeroVector, FVector(0.06f, 0.06f, 1.1f), Color, FRotator(90, 0, 0));
		AlexiaVisuals::AddPart(this, RootComponent, TEXT("Cone"), FVector(60, 0, 0), FVector(0.14f, 0.14f, 0.25f), FLinearColor(0.6f, 0.6f, 0.65f), FRotator(-90, 0, 0));
	}
}

void AAlexiaProjectile::Tick(float Dt)
{
	Super::Tick(Dt);
	Life -= Dt;
	AddActorWorldOffset(Velocity * Dt);
	if (Life <= 0.f) { Destroy(); return; }

	AAlexiaHero* H = Hero.Get();
	if (!H) { Destroy(); return; }
	const FVector Loc = GetActorLocation();
	for (TActorIterator<AAlexiaEnemy> It(GetWorld()); It; ++It)
	{
		AAlexiaEnemy* E = *It;
		if (!E->IsAlive() || AlreadyHit.Contains(E)) continue;
		if (FVector::Dist2D(E->GetActorLocation(), Loc) < E->GetRadius() + Radius)
		{
			AlreadyHit.Add(E);
			H->DamageEnemy(E, Damage, 0.f);
			if (bFire)
			{
				H->DamageArea(Loc, 260.f, Damage * 0.5f);
				AAlexiaFloatText::Spawn(GetWorld(), Loc, TEXT("BUM"), FLinearColor(1.f, 0.5f, 0.1f), 90.f);
			}
			if (!bPierce) { Destroy(); return; }
		}
	}
}

// ------------------------------------------------------------------ Pickup

AAlexiaPickup::AAlexiaPickup()
{
	PrimaryActorTick.bCanEverTick = true;
	RootComponent = CreateDefaultSubobject<USceneComponent>(TEXT("Root"));
	Spin = CreateDefaultSubobject<USceneComponent>(TEXT("Spin"));
	Spin->SetupAttachment(RootComponent);
}

void AAlexiaPickup::Init(const FAlexiaItem& InItem, int32 InGold, bool bInPotion)
{
	Item = InItem;
	Gold = InGold;
	bPotion = bInPotion;
	BaseLoc = GetActorLocation();
	if (Item.IsValid())
	{
		const FLinearColor C = AlexiaData::RarityColor(Item.Rarity);
		switch (Item.Slot)
		{
		case EItemSlot::Weapon:
			AlexiaVisuals::AddPart(this, Spin, TEXT("Cube"), FVector(0, 0, 40), FVector(0.12f, 0.12f, 0.9f), C, FRotator(0, 0, 35));
			break;
		case EItemSlot::Armor:
			AlexiaVisuals::AddPart(this, Spin, TEXT("Cube"), FVector(0, 0, 40), FVector(0.5f, 0.2f, 0.55f), C);
			break;
		default:
			AlexiaVisuals::AddPart(this, Spin, TEXT("Sphere"), FVector(0, 0, 40), FVector(0.35f), C);
			break;
		}
		if ((int32)Item.Rarity >= 1)
		{
			AlexiaVisuals::AddPart(this, RootComponent, TEXT("Cylinder"), FVector(0, 0, 200), FVector(0.08f, 0.08f, 4.f), C);
		}
	}
	else if (bPotion)
	{
		AlexiaVisuals::AddPart(this, Spin, TEXT("Sphere"), FVector(0, 0, 35), FVector(0.4f), FLinearColor(0.15f, 0.85f, 0.35f));
		AlexiaVisuals::AddPart(this, Spin, TEXT("Cylinder"), FVector(0, 0, 65), FVector(0.15f, 0.15f, 0.25f), FLinearColor(0.85f, 0.9f, 1.f));
	}
	else
	{
		AlexiaVisuals::AddPart(this, Spin, TEXT("Cylinder"), FVector(0, 0, 35), FVector(0.4f, 0.4f, 0.08f), FLinearColor(1.f, 0.8f, 0.1f), FRotator(0, 0, 90));
	}
}

void AAlexiaPickup::Tick(float Dt)
{
	Super::Tick(Dt);
	Age += Dt;
	Spin->SetRelativeRotation(FRotator(0, Age * 120.f, 0));
	Spin->SetRelativeLocation(FVector(0, 0, FMath::Sin(Age * 4.f) * 12.f));
	if (Age > 120.f) { Destroy(); return; }

	AAlexiaHero* H = Cast<AAlexiaHero>(UGameplayStatics::GetPlayerPawn(this, 0));
	if (!H || !H->IsAlive() || Age < 0.4f) return;
	const float D = FVector::Dist2D(H->GetActorLocation(), GetActorLocation());
	if (D < 450.f)
	{
		FVector Dir = H->GetActorLocation() - GetActorLocation();
		Dir.Z = 0;
		AddActorWorldOffset(Dir.GetSafeNormal() * FMath::Min(D, 900.f * Dt));
	}
	if (D < 130.f)
	{
		if (Item.IsValid()) H->AddItem(Item);
		else if (bPotion) H->AddPotion();
		else H->AddGold(Gold);
		Destroy();
	}
}
