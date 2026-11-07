#include "AlexiaWorldGen.h"
#include "AlexiaEnemy.h"
#include "AlexiaHero.h"
#include "AlexiaSettings.h"
#include "AlexiaVisuals.h"
#include "Components/DirectionalLightComponent.h"
#include "Components/ExponentialHeightFogComponent.h"
#include "Components/InstancedStaticMeshComponent.h"
#include "Components/SkyAtmosphereComponent.h"
#include "Components/SkyLightComponent.h"
#include "Engine/DirectionalLight.h"
#include "Engine/StaticMesh.h"
#include "EngineUtils.h"
#include "Kismet/GameplayStatics.h"
#include "Materials/MaterialInterface.h"

TWeakObjectPtr<AAlexiaWorldGen> AAlexiaWorldGen::Instance;

AAlexiaWorldGen::AAlexiaWorldGen()
{
	PrimaryActorTick.bCanEverTick = true;
	RootComponent = CreateDefaultSubobject<USceneComponent>(TEXT("Root"));
}

AAlexiaWorldGen* AAlexiaWorldGen::Get(const UWorld* World)
{
	if (!World) return nullptr;
	if (Instance.IsValid() && Instance->GetWorld() == World) return Instance.Get();
	for (TActorIterator<AAlexiaWorldGen> It(const_cast<UWorld*>(World)); It; ++It)
	{
		Instance = *It;
		return *It;
	}
	return nullptr;
}

void AAlexiaWorldGen::BeginPlay()
{
	Super::BeginPlay();
	Instance = this;
	BuildLights();
	BuildTerrain();
}

// ======================================================================== šum

float AAlexiaWorldGen::Hash(int32 X, int32 Y, int32 S) const
{
	uint32 H = (uint32)X * 374761393u + (uint32)Y * 668265263u + (uint32)S * 1442695041u + (uint32)Seed * 1013904223u;
	H = (H ^ (H >> 13)) * 1274126177u;
	H ^= H >> 16;
	return (H & 0xFFFF) / 65535.f;
}

float AAlexiaWorldGen::ValueNoise(float X, float Y, int32 S) const
{
	const int32 Xi = FMath::FloorToInt(X);
	const int32 Yi = FMath::FloorToInt(Y);
	const float Fx = X - Xi, Fy = Y - Yi;
	const float U = Fx * Fx * (3.f - 2.f * Fx);
	const float V = Fy * Fy * (3.f - 2.f * Fy);
	const float A = Hash(Xi, Yi, S), B = Hash(Xi + 1, Yi, S), C = Hash(Xi, Yi + 1, S), D = Hash(Xi + 1, Yi + 1, S);
	return A + (B - A) * U + (C - A) * V + (A - B - C + D) * U * V;
}

float AAlexiaWorldGen::Fbm(float X, float Y, int32 S) const
{
	float Sum = 0.f, Amp = 0.5f, Freq = 1.f, Norm = 0.f;
	for (int32 O = 0; O < 4; ++O)
	{
		Sum += ValueNoise(X * Freq, Y * Freq, S + O * 31) * Amp;
		Norm += Amp;
		Amp *= 0.5f;
		Freq *= 2.f;
	}
	return Sum / Norm;
}

EBiome AAlexiaWorldGen::BiomeAt(const FVector& L) const
{
	const float V = (L.Y + WorldRadius) / (2.f * WorldRadius);
	const float T = Fbm(L.X / 45000.f + 10.f, L.Y / 45000.f + 10.f, 2) * 0.6f + V * 0.55f;
	const float M = Fbm(L.X / 14000.f + 50.f, L.Y / 14000.f + 50.f, 3);
	if (T < 0.40f) return EBiome::Snow;
	if (T > 0.72f) return EBiome::Sand;
	if (M > 0.54f) return EBiome::Forest;
	return EBiome::Grass;
}

int32 AAlexiaWorldGen::ZoneLevel(const FVector& L) const
{
	return 1 + FMath::FloorToInt(L.Size2D() / 4500.f);
}

bool AAlexiaWorldGen::ClampToWorld(FVector& L) const
{
	const float Max = WorldRadius - 800.f;
	const float D = L.Size2D();
	if (D <= Max) return false;
	const float K = Max / D;
	L.X *= K;
	L.Y *= K;
	return true;
}

// ======================================================================== stavba sveta

UInstancedStaticMeshComponent* AAlexiaWorldGen::MakeIsm(const TCHAR* ShapeName, const FLinearColor& Color, bool bCollide, UStaticMesh* MeshOverride, UMaterialInterface* MatOverride)
{
	UInstancedStaticMeshComponent* C = NewObject<UInstancedStaticMeshComponent>(this);
	C->SetStaticMesh(MeshOverride ? MeshOverride : AlexiaVisuals::Shape(ShapeName));
	if (!MeshOverride)
	{
		if (MatOverride) C->SetMaterial(0, MatOverride);
		else C->SetMaterial(0, AlexiaVisuals::ColorMat(C, Color));
	}
	C->SetMobility(EComponentMobility::Static);
	if (bCollide)
	{
		C->SetCollisionEnabled(ECollisionEnabled::QueryAndPhysics);
		C->SetCollisionProfileName(TEXT("BlockAll"));
	}
	else
	{
		C->SetCollisionEnabled(ECollisionEnabled::NoCollision);
	}
	C->SetupAttachment(RootComponent);
	C->RegisterComponent();
	AddInstanceComponent(C);
	Ism.Add(C);
	return C;
}

void AAlexiaWorldGen::BuildLights()
{
	{
		TActorIterator<ADirectionalLight> It(GetWorld());
		if (It) return; // mapa už má vlastné svetlo
	}

	UDirectionalLightComponent* Sun = NewObject<UDirectionalLightComponent>(this);
	Sun->SetMobility(EComponentMobility::Movable);
	Sun->SetupAttachment(RootComponent);
	Sun->SetIntensity(7.f);
	Sun->SetAtmosphereSunLight(true);
	Sun->SetDynamicShadowDistanceMovableLight(9000.f);
	Sun->RegisterComponent();
	Sun->SetWorldRotation(FRotator(-48.f, 35.f, 0.f));
	AddInstanceComponent(Sun);

	USkyAtmosphereComponent* Sky = NewObject<USkyAtmosphereComponent>(this);
	Sky->SetupAttachment(RootComponent);
	Sky->RegisterComponent();
	AddInstanceComponent(Sky);

	USkyLightComponent* SkyLight = NewObject<USkyLightComponent>(this);
	SkyLight->SetMobility(EComponentMobility::Movable);
	SkyLight->SetupAttachment(RootComponent);
	SkyLight->bRealTimeCapture = true;
	SkyLight->SourceType = ESkyLightSourceType::SLS_CapturedScene;
	SkyLight->SetIntensity(1.2f);
	SkyLight->RegisterComponent();
	SkyLight->RecaptureSky();
	AddInstanceComponent(SkyLight);

	UExponentialHeightFogComponent* Fog = NewObject<UExponentialHeightFogComponent>(this);
	Fog->SetupAttachment(RootComponent);
	Fog->SetFogDensity(0.006f);
	Fog->RegisterComponent();
	AddInstanceComponent(Fog);
}

void AAlexiaWorldGen::BuildTerrain()
{
	if (!UAlexiaSettings::Get()->bGenerateWorld) return;
	const UAlexiaSettings* S = UAlexiaSettings::Get();

	// zem – dve odtiene pre každý bióm
	static const FLinearColor BiomeColor[] = {
		FLinearColor(0.20f, 0.48f, 0.15f), // lúka
		FLinearColor(0.11f, 0.32f, 0.10f), // les
		FLinearColor(0.80f, 0.66f, 0.38f), // púšť
		FLinearColor(0.88f, 0.92f, 0.96f), // sneh
	};
	UInstancedStaticMeshComponent* Ground[8];
	for (int32 b = 0; b < 4; ++b)
	{
		UMaterialInterface* Mat = nullptr;
		if (const TSoftObjectPtr<UMaterialInterface>* M = S->GroundMaterials.Find((EBiome)b)) Mat = M->LoadSynchronous();
		Ground[b * 2] = MakeIsm(TEXT("Cube"), BiomeColor[b], true, nullptr, Mat);
		Ground[b * 2 + 1] = MakeIsm(TEXT("Cube"), BiomeColor[b] * 0.9f, true, nullptr, Mat);
	}

	// voda okolo ostrova
	UInstancedStaticMeshComponent* Water = MakeIsm(TEXT("Cube"), FLinearColor(0.08f, 0.28f, 0.55f), false);
	Water->AddInstance(FTransform(FRotator::ZeroRotator, FVector(0, 0, -160), FVector(WorldRadius * 5.f / 100.f, WorldRadius * 5.f / 100.f, 1.f)));

	// cesty zo stredu na 4 strany
	UInstancedStaticMeshComponent* Road = MakeIsm(TEXT("Cube"), FLinearColor(0.55f, 0.42f, 0.28f), false);
	Road->AddInstance(FTransform(FRotator::ZeroRotator, FVector(0, 0, 1), FVector(WorldRadius * 2.f / 100.f, 7.f, 0.03f)));
	Road->AddInstance(FTransform(FRotator::ZeroRotator, FVector(0, 0, 1), FVector(7.f, WorldRadius * 2.f / 100.f, 0.03f)));

	// dúpätá bossov
	static const TCHAR* BossNames[] = { TEXT("Kamenný golem Bralo"), TEXT("Prastarý ent Morvald"), TEXT("Piesočný kráľ Azhar"), TEXT("Ľadový drak Skaldir") };
	UInstancedStaticMeshComponent* LairDisk = MakeIsm(TEXT("Cylinder"), FLinearColor(0.12f, 0.05f, 0.05f), false);
	UInstancedStaticMeshComponent* Pillar = MakeIsm(TEXT("Cylinder"), FLinearColor(0.3f, 0.28f, 0.3f), true);
	UInstancedStaticMeshComponent* Rune = MakeIsm(TEXT("Sphere"), FLinearColor(1.f, 0.15f, 0.1f), false);
	for (int32 k = 0; k < 4; ++k)
	{
		const float A = FMath::DegreesToRadians(45.f + 90.f * k);
		FLair L;
		L.Loc = FVector(FMath::Cos(A), FMath::Sin(A), 0.f) * WorldRadius * 0.72f;
		switch (BiomeAt(L.Loc))
		{
		case EBiome::Snow: L.Kind = EEnemyKind::FrostWyrm; L.Name = BossNames[3]; break;
		case EBiome::Sand: L.Kind = EEnemyKind::SandKing; L.Name = BossNames[2]; break;
		case EBiome::Forest: L.Kind = EEnemyKind::Treant; L.Name = BossNames[1]; break;
		default: L.Kind = EEnemyKind::Golem; L.Name = BossNames[0]; break;
		}
		Lairs.Add(L);
		LairDisk->AddInstance(FTransform(FRotator::ZeroRotator, L.Loc + FVector(0, 0, 3), FVector(16.f, 16.f, 0.04f)));
		for (int32 p = 0; p < 8; ++p)
		{
			const float PA = FMath::DegreesToRadians(p * 45.f);
			const FVector PL = L.Loc + FVector(FMath::Cos(PA), FMath::Sin(PA), 0.f) * 850.f;
			Pillar->AddInstance(FTransform(FRotator::ZeroRotator, PL + FVector(0, 0, 220), FVector(0.8f, 0.8f, 4.4f)));
			Rune->AddInstance(FTransform(FRotator::ZeroRotator, PL + FVector(0, 0, 470), FVector(0.4f)));
		}
	}

	// dekorácie
	UStaticMesh* TreeMesh = S->TreeMesh.LoadSynchronous();
	UStaticMesh* PineMesh = S->PineMesh.LoadSynchronous();
	UStaticMesh* RockMesh = S->RockMesh.LoadSynchronous();
	UInstancedStaticMeshComponent* Trunk = MakeIsm(TEXT("Cylinder"), FLinearColor(0.32f, 0.2f, 0.1f), true);
	UInstancedStaticMeshComponent* Leaves = MakeIsm(TEXT("Sphere"), FLinearColor(0.12f, 0.42f, 0.12f), false);
	UInstancedStaticMeshComponent* Leaves2 = MakeIsm(TEXT("Sphere"), FLinearColor(0.2f, 0.55f, 0.16f), false);
	UInstancedStaticMeshComponent* Pine = MakeIsm(TEXT("Cone"), FLinearColor(0.08f, 0.3f, 0.2f), false);
	UInstancedStaticMeshComponent* SnowCap = MakeIsm(TEXT("Cone"), FLinearColor(0.95f, 0.97f, 1.f), false);
	UInstancedStaticMeshComponent* Cactus = MakeIsm(TEXT("Cylinder"), FLinearColor(0.2f, 0.5f, 0.2f), true);
	UInstancedStaticMeshComponent* Rock = MakeIsm(TEXT("Sphere"), FLinearColor(0.45f, 0.45f, 0.47f), true, RockMesh);
	UInstancedStaticMeshComponent* Flower = MakeIsm(TEXT("Sphere"), FLinearColor(1.f, 0.4f, 0.6f), false);
	UInstancedStaticMeshComponent* Flower2 = MakeIsm(TEXT("Sphere"), FLinearColor(1.f, 0.9f, 0.3f), false);
	UInstancedStaticMeshComponent* Grass = MakeIsm(TEXT("Cone"), FLinearColor(0.25f, 0.6f, 0.2f), false);
	UInstancedStaticMeshComponent* FabTree = TreeMesh ? MakeIsm(nullptr, FLinearColor::White, true, TreeMesh) : nullptr;
	UInstancedStaticMeshComponent* FabPine = PineMesh ? MakeIsm(nullptr, FLinearColor::White, true, PineMesh) : nullptr;

	const int32 N = FMath::CeilToInt(WorldRadius / TileSize);
	for (int32 ty = -N; ty <= N; ++ty)
	{
		for (int32 tx = -N; tx <= N; ++tx)
		{
			const FVector C(tx * TileSize, ty * TileSize, 0.f);
			if (C.Size2D() > WorldRadius) continue;
			const EBiome B = BiomeAt(C);
			const int32 Variant = Hash(tx, ty, 1) < 0.5f ? 0 : 1;
			Ground[(int32)B * 2 + Variant]->AddInstance(FTransform(FRotator::ZeroRotator, C + FVector(0, 0, -50), FVector(TileSize / 100.f, TileSize / 100.f, 1.f)));

			FRandomStream R(tx * 7919 + ty * 104729 + Seed);
			const int32 Count = B == EBiome::Forest ? 9 : B == EBiome::Snow ? 4 : B == EBiome::Sand ? 2 : 3;
			for (int32 i = 0; i < Count; ++i)
			{
				const FVector P = C + FVector(R.FRandRange(-0.5f, 0.5f) * TileSize, R.FRandRange(-0.5f, 0.5f) * TileSize, 0.f);
				if (P.Size2D() < 3500.f || P.Size2D() > WorldRadius - 600.f) continue;
				if (FMath::Abs(P.X) < 600.f || FMath::Abs(P.Y) < 600.f) continue;
				bool bNearLair = false;
				for (const FLair& L : Lairs) if (FVector::Dist2D(L.Loc, P) < 1400.f) bNearLair = true;
				if (bNearLair) continue;

				const float Roll = R.FRand();
				const float Sz = R.FRandRange(0.8f, 1.4f);
				const FRotator Yaw(0.f, R.FRandRange(0.f, 360.f), 0.f);
				switch (B)
				{
				case EBiome::Forest:
				case EBiome::Grass:
				{
					const bool bTree = B == EBiome::Forest ? Roll < 0.75f : Roll < 0.25f;
					if (bTree)
					{
						if (FabTree) FabTree->AddInstance(FTransform(Yaw, P, FVector(Sz)));
						else
						{
							Trunk->AddInstance(FTransform(Yaw, P + FVector(0, 0, 110 * Sz), FVector(0.45f * Sz, 0.45f * Sz, 2.2f * Sz)));
							Leaves->AddInstance(FTransform(Yaw, P + FVector(0, 0, 300 * Sz), FVector(2.6f * Sz, 2.6f * Sz, 2.2f * Sz)));
							Leaves2->AddInstance(FTransform(Yaw, P + FVector(40, -30, 390) * Sz, FVector(1.6f * Sz)));
						}
					}
					else if (Roll < 0.85f)
					{
						Grass->AddInstance(FTransform(Yaw, P + FVector(0, 0, 20), FVector(0.25f, 0.25f, 0.45f)));
						Grass->AddInstance(FTransform(Yaw, P + FVector(25, 10, 15), FVector(0.2f, 0.2f, 0.35f)));
						(R.FRand() < 0.5f ? Flower : Flower2)->AddInstance(FTransform(Yaw, P + FVector(-20, 20, 25), FVector(0.14f)));
					}
					else Rock->AddInstance(FTransform(Yaw, P + FVector(0, 0, 30), FVector(1.4f * Sz, 1.1f * Sz, 0.8f * Sz)));
					break;
				}
				case EBiome::Snow:
					if (Roll < 0.7f)
					{
						if (FabPine) FabPine->AddInstance(FTransform(Yaw, P, FVector(Sz)));
						else
						{
							Trunk->AddInstance(FTransform(Yaw, P + FVector(0, 0, 70 * Sz), FVector(0.35f * Sz, 0.35f * Sz, 1.4f * Sz)));
							Pine->AddInstance(FTransform(Yaw, P + FVector(0, 0, 230 * Sz), FVector(2.2f * Sz, 2.2f * Sz, 2.6f * Sz)));
							Pine->AddInstance(FTransform(Yaw, P + FVector(0, 0, 380 * Sz), FVector(1.5f * Sz, 1.5f * Sz, 1.9f * Sz)));
							SnowCap->AddInstance(FTransform(Yaw, P + FVector(0, 0, 450 * Sz), FVector(0.6f * Sz, 0.6f * Sz, 0.7f * Sz)));
						}
					}
					else Rock->AddInstance(FTransform(Yaw, P + FVector(0, 0, 30), FVector(1.6f * Sz, 1.2f * Sz, 0.9f * Sz)));
					break;
				case EBiome::Sand:
					if (Roll < 0.5f)
					{
						Cactus->AddInstance(FTransform(Yaw, P + FVector(0, 0, 120), FVector(0.35f, 0.35f, 2.4f)));
						Cactus->AddInstance(FTransform(Yaw, P + FVector(30, 0, 150), FVector(0.22f, 0.22f, 0.9f)));
					}
					else Rock->AddInstance(FTransform(Yaw, P + FVector(0, 0, 30), FVector(1.8f * Sz, 1.4f * Sz, 1.0f * Sz)));
					break;
				}
			}
		}
	}
}

// ======================================================================== nepriatelia

AAlexiaEnemy* AAlexiaWorldGen::SpawnEnemy(EEnemyKind Kind, int32 Level, const FVector& Loc, const FString& BossName)
{
	const FEnemyInfo& I = AlexiaData::Enemy(Kind);
	FActorSpawnParameters P;
	P.SpawnCollisionHandlingOverride = ESpawnActorCollisionHandlingMethod::AdjustIfPossibleButDontSpawnIfColliding;
	const FVector At(Loc.X, Loc.Y, I.HalfHeight + 15.f);
	AAlexiaEnemy* E = GetWorld()->SpawnActor<AAlexiaEnemy>(AAlexiaEnemy::StaticClass(), At, FRotator(0.f, FMath::FRandRange(0.f, 360.f), 0.f), P);
	if (E) E->Init(Kind, Level, BossName);
	return E;
}

void AAlexiaWorldGen::Tick(float Dt)
{
	Super::Tick(Dt);
	AAlexiaHero* Hero = Cast<AAlexiaHero>(UGameplayStatics::GetPlayerPawn(this, 0));
	if (!Hero || !Hero->HasStarted()) return;
	UpdateSpawns(Dt);
	UpdateLairs(Dt);
}

void AAlexiaWorldGen::UpdateSpawns(float Dt)
{
	AAlexiaHero* Hero = Cast<AAlexiaHero>(UGameplayStatics::GetPlayerPawn(this, 0));
	const FVector HL = Hero->GetActorLocation();

	int32 Near = 0;
	TArray<AAlexiaEnemy*> ToRemove;
	for (TActorIterator<AAlexiaEnemy> It(GetWorld()); It; ++It)
	{
		if (It->IsBoss()) continue;
		const float D = FVector::Dist2D(It->GetActorLocation(), HL);
		if (D > 7000.f && !It->bAggro) ToRemove.Add(*It);
		else if (D < 5000.f) Near++;
	}
	for (AAlexiaEnemy* E : ToRemove) E->Destroy();

	SpawnTimer -= Dt;
	if (SpawnTimer > 0.f || Near >= MaxEnemiesNearPlayer || !Hero->IsAlive()) return;
	SpawnTimer = 0.6f;

	for (int32 Try = 0; Try < 6; ++Try)
	{
		const float A = FMath::FRandRange(0.f, 2.f * PI);
		const float Dist = FMath::FRandRange(2500.f, 4200.f);
		const FVector P = HL + FVector(FMath::Cos(A), FMath::Sin(A), 0.f) * Dist;
		if (P.Size2D() < 3500.f || P.Size2D() > WorldRadius - 1200.f) continue;
		bool bNearLair = false;
		for (const FLair& L : Lairs) if (FVector::Dist2D(L.Loc, P) < 1800.f) bNearLair = true;
		if (bNearLair) continue;

		EEnemyKind Kinds[4];
		int32 NumKinds = 3;
		switch (BiomeAt(P))
		{
		case EBiome::Forest: Kinds[0] = EEnemyKind::Wolf; Kinds[1] = EEnemyKind::Spider; Kinds[2] = EEnemyKind::Bandit; break;
		case EBiome::Sand: Kinds[0] = EEnemyKind::Scorpion; Kinds[1] = EEnemyKind::Skeleton; Kinds[2] = EEnemyKind::Bandit; break;
		case EBiome::Snow: Kinds[0] = EEnemyKind::Yeti; Kinds[1] = EEnemyKind::Wolf; Kinds[2] = EEnemyKind::Skeleton; break;
		default: Kinds[0] = EEnemyKind::Slime; Kinds[1] = EEnemyKind::Wolf; Kinds[2] = EEnemyKind::Bandit; Kinds[3] = EEnemyKind::Slime; NumKinds = 4; break;
		}
		const EEnemyKind Kind = Kinds[FMath::RandRange(0, NumKinds - 1)];
		const int32 Level = ZoneLevel(P) + FMath::RandRange(0, 1);
		if (SpawnEnemy(Kind, Level, P, FString()))
		{
			if (Kind == EEnemyKind::Wolf || Kind == EEnemyKind::Slime) SpawnEnemy(Kind, Level, P + FVector(180.f, 120.f, 0.f), FString());
			return;
		}
	}
}

void AAlexiaWorldGen::UpdateLairs(float Dt)
{
	AAlexiaHero* Hero = Cast<AAlexiaHero>(UGameplayStatics::GetPlayerPawn(this, 0));
	for (FLair& L : Lairs)
	{
		if (L.Alive.IsValid()) continue;
		L.Respawn -= Dt;
		if (L.Respawn <= 0.f && FVector::Dist2D(L.Loc, Hero->GetActorLocation()) < 5000.f)
		{
			if (AAlexiaEnemy* Boss = SpawnEnemy(L.Kind, ZoneLevel(L.Loc) + 3, L.Loc, L.Name))
			{
				L.Alive = Boss;
				Hero->Say(FString::Printf(TEXT("Cítiš prítomnosť: %s"), *L.Name));
			}
		}
	}
}

void AAlexiaWorldGen::OnBossKilled(AAlexiaEnemy* Boss)
{
	for (FLair& L : Lairs)
	{
		if (L.Alive.Get() == Boss)
		{
			L.Alive = nullptr;
			L.Respawn = 240.f;
		}
	}
}
