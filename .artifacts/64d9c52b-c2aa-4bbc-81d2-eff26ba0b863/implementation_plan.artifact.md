# Implementation Plan - Advanced Tag UX: Comma and Delete Key

Enhance the `TagEditor` with advanced keyboard interactions for a more fluid user experience, allowing for faster tag creation and easier editing.

## User Review Required

> [!TIP]
> **Comma Shortcut**: Typing a comma (`,`) now immediately saves the current text as a tag.
> **Delete to Edit**: Pressing Backspace when the text input is empty will remove the last tag and put it back into the input field for quick editing.

## Proposed Changes

### UI Layer

#### [MODIFY] [JournalScreen.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/ui/screens/JournalScreen.kt)

**1. Update `TagEditor` Composable:**
- **Comma Support**:
    - In `onValueChange` of the `OutlinedTextField`, check if the new text ends with a comma.
    - If it does, trim the comma, add the resulting tag (if not empty/duplicate), and clear the input.
- **Backspace/Delete Support**:
    - Use `Modifier.onKeyEvent` on the `OutlinedTextField`.
    - Detect `Key.Backspace` and `KeyEventType.KeyUp`.
    - If `tagInput` is empty and `tags` list is not empty:
        - Get the last tag from the list.
        - Update the tags list to remove the last item.
        - Set `tagInput` to the value of the removed tag.
- **Imports**: Add `androidx.compose.ui.input.key.*` imports.

## Verification Plan

### Manual Verification
1. **Comma Shortcut**:
    - Type "grammar,". Verify "grammar" becomes a chip and the text field clears.
2. **Backspace to Edit**:
    - Add two tags: "vocab" and "lesson".
    - Clear the text field if there's any text.
    - Press Backspace. Verify "lesson" chip disappears and "lesson" text appears in the input field.
    - Press Backspace again (with empty field). Verify "vocab" chip disappears and "vocab" text appears in the input.
3. **Combination**:
    - Type "struggle," (chip created).
    - Press Backspace. Verify "struggle" chip is gone and text is back for editing.
