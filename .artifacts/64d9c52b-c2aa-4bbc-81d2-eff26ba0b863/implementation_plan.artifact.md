# Implementation Plan - Auto-Populating Journal Tags

Ensure that previously used tags "auto-populate" in the tag editor, allowing users to quickly select existing tags instead of re-typing them.

## User Review Required

> [!IMPORTANT]
> The `TagEditor` will now show a list of "Suggested Tags" (all unique tags used across your journal) below the text input when it is expanded. Tapping a suggestion will immediately add it to the entry.

## Proposed Changes

### Business Logic Layer

#### [MODIFY] [JournalViewModel.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/ui/viewmodel/JournalViewModel.kt)
- Added `allUniqueTags` StateFlow that derives a sorted list of all unique tags from `allEntries`. (Already added).

---

### UI Layer

#### [MODIFY] [JournalScreen.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/ui/screens/JournalScreen.kt)

**1. Update `TagEditor` Component:**
- Add `allSystemTags: List<String>` as a parameter.
- When `isExpanded` is true, show a "Suggested" section below the `OutlinedTextField`.
- **Filtering Logic**:
    - If the `TextField` input is **blank**, show a list of all tags from `allSystemTags` that are *not* already on the current entry.
    - If the `TextField` input has **text** (e.g., "str"), filter the suggestions to only show tags that *contain* or *start with* that text (e.g., "struggle").
- Tapping a suggested tag triggers `onTagsChanged` with the new tag added and clears the text input.

**2. Update `JournalScreen` and `JournalItem`:**
- Collect `allUniqueTags` from the `viewModel` in `JournalScreen`.
- Pass this list down to all `TagEditor` instances (in the main list items and the entry sheet).

**3. Persistence:**
- Continue using the local-first `updateTags` method to ensure suggestions are updated immediately after a new tag is saved.

## Verification Plan

### Manual Verification
1. **Suggestion Visibility**:
    - Open the tag editor on an entry.
    - Verify that tags used in *other* entries appear in the "Suggested" list.
2. **Adding via Suggestion**:
    - Tap a suggested tag.
    - Verify it is added to the entry's tags and immediately persisted to local storage.
    - Verify it disappears from the suggestions list (since it's now applied).
3. **Filtering**:
    - (Optional) Filter the suggestions based on the current text input in the `TextField`.
