# Implementation Plan - Update to target Android 16 (API 36)

The app is currently targeting API 37, which already satisfies the "API 36 or higher" requirement. However, some project metadata (`site-data.json`) still references API 34, and there are several Android 16-specific platform optimizations and behavior changes that should be addressed to ensure full compatibility and leverage new features.

## User Review Required

> [!IMPORTANT]
> The app's `build.gradle.kts` is already targeting API 37. I will maintain this target (as it is "higher" than 36) but will update other parts of the project to ensure Android 16 features like 16 KB page size support and intent matching are correctly configured.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/build.gradle.kts)
- Verify `compileSdk` and `targetSdk` remain at least 36 (currently 37).
- Ensure AGP and other dependencies are compatible with Android 16.

#### [MODIFY] [site-data.json](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/site-data.json)
- Update `targetSdk` to 37 to match the actual build configuration.

### Manifest and Security

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/AndroidManifest.xml)
- Add `android:pageSizeCompat="true"` to the `<application>` tag to support 16 KB page sizes in compatibility mode.
- Consider adding `android:intentMatchingFlags="enforceIntentFilter"` to the `<application>` tag to opt-in to Safer Intents (Android 16 security feature), after verifying intent filters.

### UI and Behavior

#### [VERIFY] [MainActivity.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/MainActivity.kt)
- Ensure edge-to-edge is fully implemented (already present via `enableEdgeToEdge()`).
- Verify predictive back handling via `navigation3`.

## Verification Plan

### Automated Tests
- Run `gradlew lint` to check for any new API 36/37 related warnings.
- Run existing unit and instrumentation tests to ensure no regressions.

### Manual Verification
- Deploy to an Android 16 (Baklava) emulator or device.
- Verify that the app launches and navigates correctly.
- Check logcat for any `PackageManager` warnings related to intent matching if opted-in.
