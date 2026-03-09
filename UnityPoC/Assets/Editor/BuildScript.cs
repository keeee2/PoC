using UnityEditor;

namespace Editor
{
    public static class BuildScript
    {
        private static void SetKeystore()
        {
            PlayerSettings.Android.useCustomKeystore = true;
            PlayerSettings.Android.keystoreName = "PoCKeystore.keystore";
            PlayerSettings.Android.keystorePass = "1q2w3e";
            PlayerSettings.Android.keyaliasName = "1q2w3e";
            PlayerSettings.Android.keyaliasPass = "1q2w3e";
        }

        [MenuItem("Build/APK")]
        public static void BuildAPK()
        {
            SetKeystore();

            var options = new BuildPlayerOptions
            {
                scenes = new[] { "Assets/PoC/Scenes/CharacterView.unity" },
                locationPathName = "Builds/PoC.apk",
                target = BuildTarget.Android,
                options = BuildOptions.None
            };

            EditorUserBuildSettings.exportAsGoogleAndroidProject = false;
            var result = BuildPipeline.BuildPlayer(options);

            if (result.summary.result != UnityEditor.Build.Reporting.BuildResult.Succeeded)
                EditorApplication.Exit(1);
        }

        [MenuItem("Build/Export")]
        public static void ExportAndroidProject()
        {
            SetKeystore();

            var options = new BuildPlayerOptions
            {
                scenes = new[] { "Assets/PoC/Scenes/CharacterView.unity" },
                locationPathName = "Builds",
                target = BuildTarget.Android,
                options = BuildOptions.None
            };

            EditorUserBuildSettings.exportAsGoogleAndroidProject = true;
            var result = BuildPipeline.BuildPlayer(options);

            if (result.summary.result != UnityEditor.Build.Reporting.BuildResult.Succeeded)
                EditorApplication.Exit(1);
        }
    }
}