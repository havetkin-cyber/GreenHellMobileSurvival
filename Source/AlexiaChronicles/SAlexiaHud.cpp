#include "SAlexiaHud.h"
#include "AlexiaHero.h"
#include "AlexiaEnemy.h"
#include "GameFramework/PlayerController.h"
#include "Styling/CoreStyle.h"
#include "Widgets/SOverlay.h"
#include "Widgets/SBoxPanel.h"
#include "Widgets/Layout/SBox.h"
#include "Widgets/Layout/SBorder.h"
#include "Widgets/Layout/SScrollBox.h"
#include "Widgets/Layout/SSpacer.h"
#include "Widgets/Input/SButton.h"
#include "Widgets/Text/STextBlock.h"
#include "Widgets/Notifications/SProgressBar.h"

#define LOCTEXT_NAMESPACE "AlexiaHud"

static FSlateFontInfo Font(int32 Size, bool bBold = true)
{
	return FCoreStyle::GetDefaultFontStyle(bBold ? "Bold" : "Regular", Size);
}

static const FLinearColor Gold(1.f, 0.82f, 0.25f);
static const FLinearColor PanelBg(0.03f, 0.05f, 0.08f, 0.72f);

AAlexiaHero* SAlexiaHud::Hero() const
{
	return PC.IsValid() ? Cast<AAlexiaHero>(PC->GetPawn()) : nullptr;
}

TAttribute<FText> SAlexiaHud::Txt(TFunction<FString()> Fn) const
{
	return TAttribute<FText>::CreateLambda([Fn]() { return FText::FromString(Fn()); });
}

void SAlexiaHud::Construct(const FArguments& InArgs)
{
	PC = InArgs._Controller;
	SetVisibility(EVisibility::SelfHitTestInvisible);

	ChildSlot
	[
		SNew(SOverlay)
		.Visibility(EVisibility::SelfHitTestInvisible)
		+ SOverlay::Slot()
		[
			SNew(SBox)
			.Visibility_Lambda([this]() { return bStartOpen ? EVisibility::Collapsed : EVisibility::SelfHitTestInvisible; })
			[
				MakeGameHud()
			]
		]
		+ SOverlay::Slot()
		[
			SNew(SBox)
			.Visibility_Lambda([this]() { return bBagOpen ? EVisibility::Visible : EVisibility::Collapsed; })
			[
				MakeBagPanel()
			]
		]
		+ SOverlay::Slot()
		[
			SNew(SBox)
			.Visibility_Lambda([this]() { return bStartOpen ? EVisibility::Visible : EVisibility::Collapsed; })
			[
				MakeStartPanel()
			]
		]
	];
}

void SAlexiaHud::Tick(const FGeometry& AllottedGeometry, const double InCurrentTime, const float InDeltaTime)
{
	SCompoundWidget::Tick(AllottedGeometry, InCurrentTime, InDeltaTime);
	if (bBagOpen)
	{
		if (AAlexiaHero* H = Hero())
		{
			if (H->InventoryRevision != SeenRevision) RebuildBag();
		}
	}
}

// ======================================================================== stavebné bloky

TSharedRef<SWidget> SAlexiaHud::MakeButton(TAttribute<FText> Label, const FLinearColor& Color, float Width, float Height, int32 FontSize,
	FSimpleDelegate OnPressed, FSimpleDelegate OnReleased)
{
	return SNew(SBox)
		.WidthOverride(Width)
		.HeightOverride(Height)
		[
			SNew(SButton)
			.ButtonColorAndOpacity(Color)
			.HAlign(HAlign_Center)
			.VAlign(VAlign_Center)
			.OnPressed(OnPressed)
			.OnReleased(OnReleased)
			.ClickMethod(EButtonClickMethod::MouseDown)
			.TouchMethod(EButtonTouchMethod::Down)
			[
				SNew(STextBlock)
				.Text(Label)
				.Font(Font(FontSize))
				.ColorAndOpacity(FLinearColor::White)
				.ShadowOffset(FVector2D(1.5f, 1.5f))
				.Justification(ETextJustify::Center)
			]
		];
}

TSharedRef<SWidget> SAlexiaHud::MakeBar(TAttribute<TOptional<float>> Percent, const FLinearColor& Color, float Width, float Height, TAttribute<FText> Label)
{
	return SNew(SBox)
		.WidthOverride(Width)
		.HeightOverride(Height)
		[
			SNew(SOverlay)
			+ SOverlay::Slot()
			[
				SNew(SProgressBar)
				.Percent(Percent)
				.FillColorAndOpacity(Color)
			]
			+ SOverlay::Slot()
			.HAlign(HAlign_Center)
			.VAlign(VAlign_Center)
			[
				SNew(STextBlock)
				.Text(Label)
				.Font(Font(FMath::Max(9, (int32)(Height * 0.55f))))
				.ShadowOffset(FVector2D(1.f, 1.f))
			]
		];
}

// ======================================================================== herný HUD

TSharedRef<SWidget> SAlexiaHud::MakeGameHud()
{
	return SNew(SOverlay)
		.Visibility(EVisibility::SelfHitTestInvisible)

		// ---- život, XP, level, zlato
		+ SOverlay::Slot()
		.HAlign(HAlign_Left)
		.VAlign(VAlign_Top)
		.Padding(20.f)
		[
			SNew(SBorder)
			.BorderImage(&Solid)
			.BorderBackgroundColor(PanelBg)
			.Padding(12.f)
			[
				SNew(SVerticalBox)
				+ SVerticalBox::Slot().AutoHeight()
				[
					MakeBar(
						TAttribute<TOptional<float>>::CreateLambda([this]() -> TOptional<float> { AAlexiaHero* H = Hero(); return H ? H->Hp / FMath::Max(1.f, H->GetMaxHp()) : 0.f; }),
						FLinearColor(0.85f, 0.12f, 0.12f), 380.f, 30.f,
						Txt([this]() { AAlexiaHero* H = Hero(); return H ? FString::Printf(TEXT("%d / %d"), FMath::RoundToInt(H->Hp), FMath::RoundToInt(H->GetMaxHp())) : FString(); }))
				]
				+ SVerticalBox::Slot().AutoHeight().Padding(0, 6, 0, 0)
				[
					MakeBar(
						TAttribute<TOptional<float>>::CreateLambda([this]() -> TOptional<float> { AAlexiaHero* H = Hero(); return H ? (float)H->Xp / FMath::Max(1, H->XpNext()) : 0.f; }),
						FLinearColor(0.2f, 0.55f, 1.f), 380.f, 14.f, FText::GetEmpty())
				]
				+ SVerticalBox::Slot().AutoHeight().Padding(0, 6, 0, 0)
				[
					SNew(SHorizontalBox)
					+ SHorizontalBox::Slot().FillWidth(1.f)
					[
						SNew(STextBlock).Font(Font(18))
						.Text(Txt([this]() { AAlexiaHero* H = Hero(); return H ? FString::Printf(TEXT("%s  Lv %d   ÚT %d  OB %d"), H->Info().Title, H->Level, H->GetAtk(), H->GetDef()) : FString(); }))
					]
					+ SHorizontalBox::Slot().AutoWidth()
					[
						SNew(STextBlock).Font(Font(18)).ColorAndOpacity(Gold)
						.Text(Txt([this]() { AAlexiaHero* H = Hero(); return H ? FString::Printf(TEXT("%d zl."), H->Gold) : FString(); }))
					]
				]
			]
		]

		// ---- oblasť a boss
		+ SOverlay::Slot()
		.HAlign(HAlign_Center)
		.VAlign(VAlign_Top)
		.Padding(0, 16, 0, 0)
		[
			SNew(SVerticalBox)
			.Visibility(EVisibility::SelfHitTestInvisible)
			+ SVerticalBox::Slot().AutoHeight().HAlign(HAlign_Center)
			[
				SNew(STextBlock).Font(Font(20)).ShadowOffset(FVector2D(1.5f, 1.5f))
				.Text(Txt([this]() { AAlexiaHero* H = Hero(); return H ? FString::Printf(TEXT("%s  ·  oblasť Lv %d"), *H->ZoneName, H->ZoneLevel) : FString(); }))
			]
			+ SVerticalBox::Slot().AutoHeight().HAlign(HAlign_Center).Padding(0, 8, 0, 0)
			[
				SNew(SBorder)
				.BorderImage(&Solid)
				.BorderBackgroundColor(PanelBg)
				.Padding(10.f)
				.Visibility_Lambda([this]() { AAlexiaHero* H = Hero(); return H && H->ActiveBoss.IsValid() ? EVisibility::HitTestInvisible : EVisibility::Collapsed; })
				[
					SNew(SVerticalBox)
					+ SVerticalBox::Slot().AutoHeight().HAlign(HAlign_Center)
					[
						SNew(STextBlock).Font(Font(18)).ColorAndOpacity(FLinearColor(1.f, 0.7f, 0.7f))
						.Text(Txt([this]() { AAlexiaHero* H = Hero(); AAlexiaEnemy* B = H ? H->ActiveBoss.Get() : nullptr; return B ? FString::Printf(TEXT("%s  Lv%d"), *B->DisplayName(), B->Level) : FString(); }))
					]
					+ SVerticalBox::Slot().AutoHeight().Padding(0, 4, 0, 0)
					[
						MakeBar(
							TAttribute<TOptional<float>>::CreateLambda([this]() -> TOptional<float> { AAlexiaHero* H = Hero(); AAlexiaEnemy* B = H ? H->ActiveBoss.Get() : nullptr; return B ? B->Hp / B->MaxHp : 0.f; }),
							FLinearColor(0.6f, 0.05f, 0.05f), 560.f, 20.f, FText::GetEmpty())
					]
				]
			]
		]

		// ---- správy
		+ SOverlay::Slot()
		.HAlign(HAlign_Center)
		.VAlign(VAlign_Bottom)
		.Padding(0, 0, 0, 200)
		[
			SNew(SBorder)
			.BorderImage(&Solid)
			.BorderBackgroundColor(PanelBg)
			.Padding(FMargin(20, 10))
			.Visibility_Lambda([this]() { AAlexiaHero* H = Hero(); return H && H->MessageTimer > 0.f ? EVisibility::HitTestInvisible : EVisibility::Collapsed; })
			[
				SNew(STextBlock).Font(Font(22))
				.Text(Txt([this]() { AAlexiaHero* H = Hero(); return H ? H->Message : FString(); }))
			]
		]

		// ---- taška
		+ SOverlay::Slot()
		.HAlign(HAlign_Right)
		.VAlign(VAlign_Top)
		.Padding(20.f)
		[
			MakeButton(
				Txt([this]() { AAlexiaHero* H = Hero(); int32 Up = 0; if (H) for (const FAlexiaItem& It : H->Inventory) if (H->IsUpgrade(It)) Up++; return Up > 0 ? FString::Printf(TEXT("TAŠKA (%d+)"), Up) : FString(TEXT("TAŠKA")); }),
				FLinearColor(0.35f, 0.22f, 0.1f), 190.f, 64.f, 20,
				FSimpleDelegate::CreateLambda([this]() { SetBagOpen(true); }))
		]

		// ---- bojové tlačidlá (pravý dolný roh)
		+ SOverlay::Slot()
		.HAlign(HAlign_Right)
		.VAlign(VAlign_Bottom)
		.Padding(0, 0, 40, 40)
		[
			SNew(SVerticalBox)
			.Visibility(EVisibility::SelfHitTestInvisible)
			+ SVerticalBox::Slot().AutoHeight().HAlign(HAlign_Right).Padding(0, 0, 20, 14)
			[
				MakeButton(
					Txt([this]() { AAlexiaHero* H = Hero(); return H ? (H->PotionCd > 0.f ? FString::Printf(TEXT("%.1f"), H->PotionCd) : FString::Printf(TEXT("ELIXÍR\n%d"), H->Potions)) : FString(); }),
					FLinearColor(0.1f, 0.45f, 0.15f), 100.f, 100.f, 16,
					FSimpleDelegate::CreateLambda([this]() { if (AAlexiaHero* H = Hero()) H->UsePotion(); }))
			]
			+ SVerticalBox::Slot().AutoHeight()
			[
				SNew(SHorizontalBox)
				.Visibility(EVisibility::SelfHitTestInvisible)
				+ SHorizontalBox::Slot().AutoWidth().VAlign(VAlign_Bottom).Padding(0, 0, 14, 0)
				[
					SNew(SVerticalBox)
					.Visibility(EVisibility::SelfHitTestInvisible)
					+ SVerticalBox::Slot().AutoHeight().Padding(0, 0, 0, 14)
					[
						MakeButton(
							Txt([this]() { AAlexiaHero* H = Hero(); return H ? (H->Skill2Cd > 0.f ? FString::Printf(TEXT("%.1f"), H->Skill2Cd) : FString(H->Info().Skill2)) : FString(); }),
							FLinearColor(0.0f, 0.45f, 0.5f), 120.f, 120.f, 18,
							FSimpleDelegate::CreateLambda([this]() { if (AAlexiaHero* H = Hero()) H->UseSkill2(); }))
					]
					+ SVerticalBox::Slot().AutoHeight()
					[
						MakeButton(
							Txt([this]() { AAlexiaHero* H = Hero(); return H ? (H->Skill1Cd > 0.f ? FString::Printf(TEXT("%.1f"), H->Skill1Cd) : FString(H->Info().Skill1)) : FString(); }),
							FLinearColor(0.35f, 0.15f, 0.65f), 120.f, 120.f, 18,
							FSimpleDelegate::CreateLambda([this]() { if (AAlexiaHero* H = Hero()) H->UseSkill1(); }))
					]
				]
				+ SHorizontalBox::Slot().AutoWidth().VAlign(VAlign_Bottom)
				[
					MakeButton(
						Txt([this]() { AAlexiaHero* H = Hero(); if (!H) return FString(); const FString Base = H->Info().bRanged ? TEXT("STRIEĽAJ") : TEXT("ÚTOK"); return H->ComboTimer > 0.f && H->ComboStep > 0 ? FString::Printf(TEXT("%s\nx%d"), *Base, H->ComboStep) : Base; }),
						FLinearColor(0.7f, 0.08f, 0.08f), 190.f, 190.f, 24,
						FSimpleDelegate::CreateLambda([this]() { if (AAlexiaHero* H = Hero()) H->SetAttackHeld(true); }),
						FSimpleDelegate::CreateLambda([this]() { if (AAlexiaHero* H = Hero()) H->SetAttackHeld(false); }))
				]
			]
		];
}

// ======================================================================== výber postavy

void SAlexiaHud::StartAs(EHeroClass Class)
{
	if (AAlexiaHero* H = Hero())
	{
		H->StartNewGame(Class);
		bStartOpen = false;
	}
}

TSharedRef<SWidget> SAlexiaHud::MakeStartPanel()
{
	TSharedRef<SHorizontalBox> Cards = SNew(SHorizontalBox);
	for (int32 i = 0; i < 4; ++i)
	{
		const EHeroClass C = (EHeroClass)i;
		const FHeroClassInfo& I = AlexiaData::Hero(C);
		const FString Stats = FString::Printf(TEXT("Život  %s\nÚtok  %s\nRýchlosť  %s\nKritický  %d %%\n\n%s / %s"),
			*FString::ChrN(FMath::RoundToInt(I.HpMul * 4), TEXT('|')),
			*FString::ChrN(FMath::RoundToInt(I.AtkMul * 4), TEXT('|')),
			*FString::ChrN(FMath::RoundToInt(I.Speed / 150.f), TEXT('|')),
			FMath::RoundToInt(I.Crit * 100), I.Skill1, I.Skill2);

		Cards->AddSlot().Padding(10.f).FillWidth(1.f)
		[
			SNew(SButton)
			.ButtonColorAndOpacity(I.Color * 0.6f + FLinearColor(0.05f, 0.05f, 0.08f, 0.4f))
			.ContentPadding(FMargin(18.f))
			.OnClicked_Lambda([this, C]() { StartAs(C); return FReply::Handled(); })
			[
				SNew(SVerticalBox)
				+ SVerticalBox::Slot().AutoHeight().HAlign(HAlign_Center)
				[
					SNew(SBox).WidthOverride(120.f).HeightOverride(120.f)
					[
						SNew(SBorder).BorderImage(&Solid).BorderBackgroundColor(I.Color)
						.HAlign(HAlign_Center).VAlign(VAlign_Center)
						[
							SNew(SBorder).BorderImage(&Solid).BorderBackgroundColor(I.Accent).Padding(FMargin(26.f, 18.f))
						]
					]
				]
				+ SVerticalBox::Slot().AutoHeight().HAlign(HAlign_Center).Padding(0, 14, 0, 6)
				[
					SNew(STextBlock).Text(FText::FromString(FString(I.Title).ToUpper())).Font(Font(28)).ColorAndOpacity(FLinearColor::White)
				]
				+ SVerticalBox::Slot().AutoHeight().Padding(0, 0, 0, 12)
				[
					SNew(STextBlock).Text(FText::FromString(I.Desc)).Font(Font(15, false)).AutoWrapText(true).Justification(ETextJustify::Center)
				]
				+ SVerticalBox::Slot().AutoHeight()
				[
					SNew(STextBlock).Text(FText::FromString(Stats)).Font(Font(15)).ColorAndOpacity(Gold)
				]
			]
		];
	}

	return SNew(SBorder)
		.BorderImage(&Solid)
		.BorderBackgroundColor(FLinearColor(0.02f, 0.03f, 0.05f, 0.82f))
		.Padding(40.f)
		[
			SNew(SVerticalBox)
			+ SVerticalBox::Slot().AutoHeight().HAlign(HAlign_Center)
			[
				SNew(STextBlock).Text(LOCTEXT("Title", "ALEXIA CHRONICLES")).Font(Font(56)).ColorAndOpacity(Gold).ShadowOffset(FVector2D(3.f, 3.f))
			]
			+ SVerticalBox::Slot().AutoHeight().HAlign(HAlign_Center).Padding(0, 4, 0, 20)
			[
				SNew(STextBlock).Text(FText::FromString(TEXT("Otvorený svet · bossovia · loot · vyber si postavu"))).Font(Font(20, false))
			]
			+ SVerticalBox::Slot().AutoHeight().HAlign(HAlign_Center).Padding(0, 0, 0, 16)
			[
				SNew(SBox)
				.Visibility_Lambda([]() { return AAlexiaHero::HasSave() ? EVisibility::Visible : EVisibility::Collapsed; })
				[
					MakeButton(FText::FromString(TEXT("POKRAČOVAŤ V ULOŽENEJ HRE")), FLinearColor(0.1f, 0.5f, 0.15f), 460.f, 70.f, 22,
						FSimpleDelegate::CreateLambda([this]() { if (AAlexiaHero* H = Hero()) { if (H->LoadGame()) bStartOpen = false; } }))
				]
			]
			+ SVerticalBox::Slot().FillHeight(1.f)
			[
				Cards
			]
			+ SVerticalBox::Slot().AutoHeight().HAlign(HAlign_Center).Padding(0, 10, 0, 0)
			[
				SNew(STextBlock).Text(FText::FromString(TEXT("Ťukni na postavu pre novú hru (prepíše uloženú)"))).Font(Font(16, false)).ColorAndOpacity(FLinearColor(0.8f, 0.8f, 0.85f))
			]
		];
}

// ======================================================================== taška

void SAlexiaHud::ToggleBag()
{
	if (!bStartOpen) SetBagOpen(!bBagOpen);
}

void SAlexiaHud::SetBagOpen(bool bOpen)
{
	bBagOpen = bOpen;
	if (AAlexiaHero* H = Hero())
	{
		H->SetAttackHeld(false);
		if (!bOpen) H->SaveGame();
	}
	if (PC.IsValid()) PC->SetPause(bOpen);
	if (bOpen) RebuildBag();
}

TSharedRef<SWidget> SAlexiaHud::MakeBagPanel()
{
	return SNew(SBorder)
		.BorderImage(&Solid)
		.BorderBackgroundColor(FLinearColor(0.f, 0.f, 0.f, 0.6f))
		.HAlign(HAlign_Center)
		.VAlign(VAlign_Center)
		[
			SNew(SBox)
			.WidthOverride(1200.f)
			.HeightOverride(680.f)
			[
				SNew(SBorder)
				.BorderImage(&Solid)
				.BorderBackgroundColor(FLinearColor(0.06f, 0.09f, 0.13f, 0.97f))
				.Padding(24.f)
				[
					SNew(SHorizontalBox)
					// ľavá strana – výbava a obchod
					+ SHorizontalBox::Slot().FillWidth(0.45f).Padding(0, 0, 20, 0)
					[
						SNew(SVerticalBox)
						+ SVerticalBox::Slot().AutoHeight()
						[
							SNew(STextBlock).Text(FText::FromString(TEXT("VÝBAVA"))).Font(Font(28)).ColorAndOpacity(Gold)
						]
						+ SVerticalBox::Slot().AutoHeight().Padding(0, 10)
						[
							SAssignNew(EquipList, SVerticalBox)
						]
						+ SVerticalBox::Slot().AutoHeight().Padding(0, 6)
						[
							SNew(STextBlock).Font(Font(18)).AutoWrapText(true)
							.Text(Txt([this]() { AAlexiaHero* H = Hero(); return H ? FString::Printf(TEXT("Útok %d   Obrana %d   Život %d   Krit %d%%\nElixíry %d   Zlato %d"), H->GetAtk(), H->GetDef(), FMath::RoundToInt(H->GetMaxHp()), FMath::RoundToInt(H->Info().Crit * 100), H->Potions, H->Gold) : FString(); }))
						]
						+ SVerticalBox::Slot().FillHeight(1.f) [ SNew(SSpacer) ]
						+ SVerticalBox::Slot().AutoHeight().Padding(0, 6)
						[
							MakeButton(FText::FromString(TEXT("PREDAŤ SLABŠIE PREDMETY")), FLinearColor(0.55f, 0.42f, 0.f), 420.f, 64.f, 18,
								FSimpleDelegate::CreateLambda([this]() { if (AAlexiaHero* H = Hero()) H->SellJunk(); }))
						]
						+ SVerticalBox::Slot().AutoHeight().Padding(0, 6)
						[
							MakeButton(Txt([this]() { AAlexiaHero* H = Hero(); return H ? FString::Printf(TEXT("KÚPIŤ ELIXÍR (%d zl.)"), H->PotionPrice()) : FString(); }),
								FLinearColor(0.1f, 0.45f, 0.15f), 420.f, 64.f, 18,
								FSimpleDelegate::CreateLambda([this]() { if (AAlexiaHero* H = Hero()) H->BuyPotion(); }))
						]
						+ SVerticalBox::Slot().AutoHeight().Padding(0, 6)
						[
							MakeButton(FText::FromString(TEXT("ZAVRIEŤ")), FLinearColor(0.5f, 0.12f, 0.12f), 420.f, 64.f, 20,
								FSimpleDelegate::CreateLambda([this]() { SetBagOpen(false); }))
						]
					]
					// pravá strana – predmety v taške
					+ SHorizontalBox::Slot().FillWidth(0.55f)
					[
						SNew(SVerticalBox)
						+ SVerticalBox::Slot().AutoHeight()
						[
							SNew(STextBlock).Font(Font(28)).ColorAndOpacity(Gold)
							.Text(Txt([this]() { AAlexiaHero* H = Hero(); return FString::Printf(TEXT("TAŠKA %d/40   (ťukni = nasadiť)"), H ? H->Inventory.Num() : 0); }))
						]
						+ SVerticalBox::Slot().FillHeight(1.f).Padding(0, 10, 0, 0)
						[
							SNew(SScrollBox)
							+ SScrollBox::Slot()
							[
								SAssignNew(InventoryList, SVerticalBox)
							]
						]
					]
				]
			]
		];
}

void SAlexiaHud::RebuildBag()
{
	AAlexiaHero* H = Hero();
	if (!H || !InventoryList.IsValid() || !EquipList.IsValid()) return;
	SeenRevision = H->InventoryRevision;

	EquipList->ClearChildren();
	for (int32 s = 0; s < 3; ++s)
	{
		const EItemSlot Slot = (EItemSlot)s;
		const FAlexiaItem& It = H->Equipped(Slot);
		const FString Line = It.IsValid()
			? FString::Printf(TEXT("%s:  %s\n%s"), *AlexiaData::SlotName(Slot), *It.Name, *It.StatLine())
			: FString::Printf(TEXT("%s:  — prázdne —"), *AlexiaData::SlotName(Slot));
		EquipList->AddSlot().AutoHeight().Padding(0, 4)
		[
			SNew(SBorder)
			.BorderImage(&Solid)
			.BorderBackgroundColor(FLinearColor(0.02f, 0.03f, 0.05f, 1.f))
			.Padding(10.f)
			[
				SNew(STextBlock).Text(FText::FromString(Line)).Font(Font(17))
				.ColorAndOpacity(It.IsValid() ? AlexiaData::RarityColor(It.Rarity) : FLinearColor(0.5f, 0.5f, 0.5f))
			]
		];
	}

	InventoryList->ClearChildren();
	if (H->Inventory.Num() == 0)
	{
		InventoryList->AddSlot().AutoHeight().Padding(0, 20)
		[
			SNew(STextBlock).Text(FText::FromString(TEXT("Taška je prázdna. Poraz monštrá a zbieraj loot!"))).Font(Font(18, false))
		];
	}
	for (int32 i = 0; i < H->Inventory.Num(); ++i)
	{
		const FAlexiaItem& It = H->Inventory[i];
		const bool bUp = H->IsUpgrade(It);
		const FString Line = FString::Printf(TEXT("%s%s  ·  %s  ·  Lv%d\n%s"), bUp ? TEXT("[+] ") : TEXT(""), *It.Name,
			*AlexiaData::SlotName(It.Slot), It.Level, *It.StatLine());
		InventoryList->AddSlot().AutoHeight().Padding(0, 3)
		[
			SNew(SButton)
			.ButtonColorAndOpacity(FLinearColor(0.08f, 0.1f, 0.14f) + AlexiaData::RarityColor(It.Rarity) * 0.15f)
			.ContentPadding(FMargin(12.f, 8.f))
			.OnClicked_Lambda([this, i]() { if (AAlexiaHero* Hh = Hero()) Hh->EquipFromInventory(i); return FReply::Handled(); })
			[
				SNew(STextBlock).Text(FText::FromString(Line)).Font(Font(17))
				.ColorAndOpacity(AlexiaData::RarityColor(It.Rarity))
			]
		];
	}
}

#undef LOCTEXT_NAMESPACE
