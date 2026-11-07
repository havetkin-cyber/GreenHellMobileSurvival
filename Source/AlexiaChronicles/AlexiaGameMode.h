#pragma once

#include "CoreMinimal.h"
#include "GameFramework/GameModeBase.h"
#include "AlexiaGameMode.generated.h"

UCLASS()
class ALEXIACHRONICLES_API AAlexiaGameMode : public AGameModeBase
{
	GENERATED_BODY()

public:
	AAlexiaGameMode();
	virtual AActor* ChoosePlayerStart_Implementation(AController* Player) override;
	virtual void StartPlay() override;

private:
	void EnsureWorld();
};
