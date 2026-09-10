# Implementation Plan - Simplified Tag Management

Simplify the tagging UX in both the journal list and the entry editor. Implement a unified "Tag Editor" pattern that allows quick additions and provides clear visibility of existing tags, with immediate persistence.

## User Review Required

> [!IMPORTANT]
> The tagging UI will change from a static list/separate input to an interactive "Plus" button that reveals a text field. Existing tags will appear below the input in a scrollable list.

## Proposed Changes

### UI Layer

#### [NEW] `TagEditorComponent.kt` (or internal to `JournalScreen.kt`)
- Create a reusable Composable `TagEditor`:
    - **Trigger**: A tag icon next to a plus button.
    - **Expanded State**: When the plus is clicked, shows an `OutlinedTextField` for entering a new tag.
    - **Tag List**: Below the text field, show a `LazyRow` or scrollable list of current tags as deletable chips.
    - **Action**: Pressing Enter or clicking a confirm icon adds the tag.
    - **Persistence**: If editing an existing entry, each add/delete operation triggers an immediate save to Room (local storage).

#### [MODIFY] [JournalScreen.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/ui/screens/JournalScreen.kt)
- **Main List (`JournalItem`)**:
    - Integrate the `TagEditor` directly into the `JournalItem` card. This allows users to add/remove tags without opening the full entry sheet.
- **Entry Sheet**:
    - Replace the existing tag input/list with the new `TagEditor` component.
- **State Management**:
    - Ensure the UI reacts to Room updates immediately. The `filteredEntries` flow from `JournalViewModel` already provides this, as it's backed by the Room DAO.

### Business Logic Layer

#### [MODIFY] [JournalViewModel.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/ui/viewmodel/JournalViewModel.kt)
- (Done) `updateTags(entry: JournalEntryEntity, newTags: List<String>)` handles the persistence to Room first, then Firestore.

## Verification Plan

### Manual Verification
1. **Main Page Tagging**:
    - Find an entry in the list. Click the plus button.
    - Add a tag. Verify it appears immediately in the list.
    - Re-launch the app to verify it persisted in local storage.
2. **Entry Sheet Tagging**:
    - Open an entry. Add/Remove tags using the new editor.
    - Verify changes are reflected without needing to click the main "Save" button.
3. **Data Integrity**:
    - Verify that adding a tag locally eventually syncs to Firestore (background process).
