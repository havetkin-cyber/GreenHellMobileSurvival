#include "AlexiaPlayerController.h"
#include "AlexiaHero.h"
#include "SAlexiaHud.h"
#include "Engine/GameViewportClient.h"
#include "Engine/Engine.h"
#include "Widgets/SWeakWidget.h"

AAlexiaPlayerController::AAlexiaPlayerController()
{
	bShowMouseCursor = true;
	bEnableClickEvents = false;
	bEnableTouchEvents = false;
}

void AAlexiaPlayerController::BeginPlay()
{
	Super::BeginPlay();
	if (!IsLocalController() || !GEngine || !GEngine->GameViewport) return;

	Hud = SNew(SAlexiaHud).Controller(this);
	GEngine->GameViewport->AddViewportWidgetContent(SNew(SWeakWidget).PossiblyNullContent(Hud.ToSharedRef()), 10);

	FInputModeGameAndUI Mode;
	Mode.SetHideCursorDuringCapture(false);
	Mode.SetLockMouseToViewportBehavior(EMouseLockMode::DoNotLock);
	SetInputMode(Mode);
}

void AAlexiaPlayerController::EndPlay(const EEndPlayReason::Type Reason)
{
	if (AAlexiaHero* Hero = Cast<AAlexiaHero>(GetPawn())) Hero->SaveGame();
	if (Hud.IsValid() && GEngine && GEngine->GameViewport)
	{
		GEngine->GameViewport->RemoveAllViewportWidgets();
	}
	Hud.Reset();
	Super::EndPlay(Reason);
}

void AAlexiaPlayerController::SetupInputComponent()
{
	Super::SetupInputComponent();
	InputComponent->BindAction(TEXT("Bag"), IE_Pressed, this, &AAlexiaPlayerController::ToggleBag);
}

void AAlexiaPlayerController::ToggleBag()
{
	if (Hud.IsValid()) Hud->ToggleBag();
}
