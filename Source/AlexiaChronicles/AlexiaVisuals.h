#pragma once

#include "CoreMinimal.h"

class UStaticMesh;
class UStaticMeshComponent;
class UMaterialInstanceDynamic;
class USceneComponent;

/** Pomocník na stavbu postáv a sveta zo základných tvarov enginu (kocka, guľa, valec, kužeľ). */
namespace AlexiaVisuals
{
	UStaticMesh* Shape(const TCHAR* Name);
	UMaterialInstanceDynamic* ColorMat(UObject* Outer, const FLinearColor& Color);
	UStaticMeshComponent* AddPart(AActor* Owner, USceneComponent* Parent, const TCHAR* ShapeName,
		const FVector& Loc, const FVector& Scale, const FLinearColor& Color, const FRotator& Rot = FRotator::ZeroRotator);
	void SetColor(UStaticMeshComponent* Comp, const FLinearColor& Color);
}
