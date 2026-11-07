using UnrealBuildTool;

public class AlexiaChronicles : ModuleRules
{
	public AlexiaChronicles(ReadOnlyTargetRules Target) : base(Target)
	{
		PCHUsage = PCHUsageMode.UseExplicitOrSharedPCHs;
		PublicDependencyModuleNames.AddRange(new string[] {
			"Core", "CoreUObject", "Engine", "InputCore", "AIModule", "DeveloperSettings"
		});
		PrivateDependencyModuleNames.AddRange(new string[] { "Slate", "SlateCore" });
	}
}
