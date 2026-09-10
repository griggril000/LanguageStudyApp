# Remove Word Export Implementation

This plan details the steps to remove the Word export functionality from the Journal screen, as it is no longer needed.

## Proposed Changes

### [app](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app)

#### [MODIFY] [JournalExportUtils.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/utils/JournalExportUtils.kt)
- Remove `generateWordBytes` function.
- Remove `generateBatchWordBytes` function.

#### [MODIFY] [JournalScreen.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/ui/screens/JournalScreen.kt)
- Remove `pendingBatchDownloadMimeType` state.
- Update `batchDownloadLauncher` to always use `JournalExportUtils.generateBatchPdfBytes` and set the MIME type to `application/pdf`.
- Simplify the batch export UI by removing the `DropdownMenu` and having the `IconButton` trigger the PDF export directly, since it will be the only option.

#### [MODIFY] [NextSteps.md](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/NextSteps.md)
- Mark "Remove Word export" as completed.

## Verification Plan

### Automated Tests
- Run `gradle_build` to ensure the project still compiles correctly after removing the code.

### Manual Verification
- Deploy the app and navigate to the Journal screen.
- Verify that the batch export button now directly triggers a PDF export (Save as dialog with .pdf extension).
- Verify that there are no remaining UI elements mentioning Word export.
