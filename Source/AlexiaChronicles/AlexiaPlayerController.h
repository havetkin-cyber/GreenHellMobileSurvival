#pragma once

#include "CoreMinimal.h"
#include "GameFramework/PlayerController.h"
#include "AlexiaPlayerController.generated.h"

class SAlexiaHud;

UCLASS()
class ALEXIACHRONICLES_API AAlexiaPlayerController : public APlayerController
{
	GENERATED_BODY()

public:
	AAlexiaPlayerController();
	virtual void BeginPlay() override;
	virtual void EndPlay(const EEndPlayReason::Type Reason) override;
	virtual void SetupInputComponent() override;

private:
	void ToggleBag();
	TSharedPtr<SAlexiaHud> Hud;
};
