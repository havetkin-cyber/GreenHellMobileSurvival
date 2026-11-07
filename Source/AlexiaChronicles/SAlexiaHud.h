#pragma once

#include "CoreMinimal.h"
#include "Widgets/SCompoundWidget.h"
#include "Brushes/SlateColorBrush.h"
#include "AlexiaTypes.h"

class APlayerController;
class AAlexiaHero;
class SVerticalBox;

/** Celé rozhranie hry v Slate (C++): výber postavy, HUD s tlačidlami pre mobil, taška s výbavou. */
class SAlexiaHud : public SCompoundWidget
{
public:
	SLATE_BEGIN_ARGS(SAlexiaHud) {}
		SLATE_ARGUMENT(TWeakObjectPtr<APlayerController>, Controller)
	SLATE_END_ARGS()

	void Construct(const FArguments& InArgs);
	virtual void Tick(const FGeometry& AllottedGeometry, const double InCurrentTime, const float InDeltaTime) override;

	void ToggleBag();

private:
	AAlexiaHero* Hero() const;
	void SetBagOpen(bool bOpen);
	void RebuildBag();
	void StartAs(EHeroClass Class);

	TSharedRef<SWidget> MakeGameHud();
	TSharedRef<SWidget> MakeStartPanel();
	TSharedRef<SWidget> MakeBagPanel();
	TSharedRef<SWidget> MakeButton(TAttribute<FText> Label, const FLinearColor& Color, float Width, float Height, int32 FontSize,
		FSimpleDelegate OnPressed, FSimpleDelegate OnReleased = FSimpleDelegate());
	TSharedRef<SWidget> MakeBar(TAttribute<TOptional<float>> Percent, const FLinearColor& Color, float Width, float Height, TAttribute<FText> Label);
	TAttribute<FText> Txt(TFunction<FString()> Fn) const;

	TWeakObjectPtr<APlayerController> PC;
	TSharedPtr<SVerticalBox> InventoryList;
	TSharedPtr<SVerticalBox> EquipList;
	bool bBagOpen = false;
	bool bStartOpen = true;
	int32 SeenRevision = -1;
	FSlateColorBrush Solid = FSlateColorBrush(FLinearColor::White);
};
