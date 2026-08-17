# Edit Vocab Category Plan

Re-use the `ModalBottomSheet` from the "Add Vocabulary" flow for editing existing vocabulary items, enabling category editing which was previously missing.

## Proposed Changes

### UI Layer

#### [MODIFY] [VocabScreen.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/ui/screens/VocabScreen.kt)
- Add `editingVocab` state to track the vocabulary item being edited.
- Update `showAddSheet` logic to handle both adding and editing (consider renaming to `showEntrySheet`).
- Update `ModalBottomSheet` content:
    - Change title based on add/edit mode.
    - Change button text (e.g., "Add to list" vs "Save changes").
    - Call `viewModel.updateVocab` when in edit mode.
- Update `VocabItem` call to pass a callback that opens the edit sheet.

#### [MODIFY] [VocabItem.kt] (part of VocabScreen.kt)
- Remove `showEditDialog` and its `AlertDialog`.
- Replace `onEdit` callback with `onEditRequest: (VocabEntity) -> Unit`.
- Update click listener to trigger `onEditRequest`.

## Verification Plan

### Manual Verification
1. Open the Vocab screen.
2. Tap on an existing vocabulary item.
3. Verify that the bottom sheet opens with the item's details pre-filled.
4. Change the category using the dropdown.
5. Save changes and verify the category is updated in the list.
6. Verify "Add" flow still works correctly and defaults to "General" or current filter.
