#include "AlexiaVisuals.h"
#include "Components/StaticMeshComponent.h"
#include "Engine/StaticMesh.h"
#include "Materials/MaterialInstanceDynamic.h"
#include "Materials/MaterialInterface.h"
#include "GameFramework/Actor.h"

UStaticMesh* AlexiaVisuals::Shape(const TCHAR* Name)
{
	const FString Path = FString::Printf(TEXT("/Engine/BasicShapes/%s.%s"), Name, Name);
	return LoadObject<UStaticMesh>(nullptr, *Path);
}

UMaterialInstanceDynamic* AlexiaVisuals::ColorMat(UObject* Outer, const FLinearColor& Color)
{
	UMaterialInterface* Base = LoadObject<UMaterialInterface>(nullptr, TEXT("/Engine/BasicShapes/BasicShapeMaterial.BasicShapeMaterial"));
	if (!Base) return nullptr;
	UMaterialInstanceDynamic* M = UMaterialInstanceDynamic::Create(Base, Outer);
	M->SetVectorParameterValue(TEXT("Color"), Color);
	return M;
}

UStaticMeshComponent* AlexiaVisuals::AddPart(AActor* Owner, USceneComponent* Parent, const TCHAR* ShapeName,
	const FVector& Loc, const FVector& Scale, const FLinearColor& Color, const FRotator& Rot)
{
	UStaticMeshComponent* C = NewObject<UStaticMeshComponent>(Owner);
	C->SetStaticMesh(Shape(ShapeName));
	C->SetMaterial(0, ColorMat(C, Color));
	C->SetCollisionEnabled(ECollisionEnabled::NoCollision);
	C->SetGenerateOverlapEvents(false);
	C->SetCastShadow(true);
	C->SetupAttachment(Parent);
	C->SetRelativeLocation(Loc);
	C->SetRelativeRotation(Rot);
	C->SetRelativeScale3D(Scale);
	C->RegisterComponent();
	Owner->AddInstanceComponent(C);
	return C;
}

void AlexiaVisuals::SetColor(UStaticMeshComponent* Comp, const FLinearColor& Color)
{
	if (!Comp) return;
	if (UMaterialInstanceDynamic* M = Cast<UMaterialInstanceDynamic>(Comp->GetMaterial(0)))
	{
		M->SetVectorParameterValue(TEXT("Color"), Color);
	}
}
