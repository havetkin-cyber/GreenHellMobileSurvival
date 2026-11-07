#include "AlexiaGameMode.h"
#include "AlexiaHero.h"
#include "AlexiaPlayerController.h"
#include "AlexiaWorldGen.h"
#include "GameFramework/PlayerStart.h"
#include "EngineUtils.h"

AAlexiaGameMode::AAlexiaGameMode()
{
	DefaultPawnClass = AAlexiaHero::StaticClass();
	PlayerControllerClass = AAlexiaPlayerController::StaticClass();
}

void AAlexiaGameMode::EnsureWorld()
{
	if (AAlexiaWorldGen::Get(GetWorld())) return;
	FActorSpawnParameters P;
	P.SpawnCollisionHandlingOverride = ESpawnActorCollisionHandlingMethod::AlwaysSpawn;
	GetWorld()->SpawnActor<AAlexiaWorldGen>(AAlexiaWorldGen::StaticClass(), FTransform::Identity, P);
}

AActor* AAlexiaGameMode::ChoosePlayerStart_Implementation(AController* Player)
{
	EnsureWorld();
	if (AActor* Found = Super::ChoosePlayerStart_Implementation(Player)) return Found;
	FActorSpawnParameters P;
	P.SpawnCollisionHandlingOverride = ESpawnActorCollisionHandlingMethod::AlwaysSpawn;
	const FVector Loc = AAlexiaWorldGen::Get(GetWorld()) ? AAlexiaWorldGen::Get(GetWorld())->SpawnPoint() : FVector(0, 0, 150);
	return GetWorld()->SpawnActor<APlayerStart>(APlayerStart::StaticClass(), Loc, FRotator::ZeroRotator, P);
}

void AAlexiaGameMode::StartPlay()
{
	EnsureWorld();
	Super::StartPlay();
}
