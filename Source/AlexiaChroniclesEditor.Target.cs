using UnrealBuildTool;

public class AlexiaChroniclesEditorTarget : TargetRules
{
	public AlexiaChroniclesEditorTarget(TargetInfo Target) : base(Target)
	{
		Type = TargetType.Editor;
		DefaultBuildSettings = BuildSettingsVersion.Latest;
		IncludeOrderVersion = EngineIncludeOrderVersion.Latest;
		ExtraModuleNames.Add("AlexiaChronicles");
	}
}
