using UnrealBuildTool;

public class AlexiaChroniclesTarget : TargetRules
{
	public AlexiaChroniclesTarget(TargetInfo Target) : base(Target)
	{
		Type = TargetType.Game;
		DefaultBuildSettings = BuildSettingsVersion.Latest;
		IncludeOrderVersion = EngineIncludeOrderVersion.Latest;
		ExtraModuleNames.Add("AlexiaChronicles");
	}
}
