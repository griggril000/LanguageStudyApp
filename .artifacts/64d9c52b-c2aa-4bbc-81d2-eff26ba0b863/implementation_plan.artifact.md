# Implementation Plan - Optional Tagging for Journal Entries

Add an optional tagging feature to journal entries to help users organize and filter their study reflections (e.g., "struggle", "vocabulary", "grammar", "success").

## Proposed Changes

### Data Layer

#### [MODIFY] [JournalEntryEntity.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/data/local/entity/JournalEntryEntity.kt)
- Add `val tags: List<String> = emptyList()` to the data class.

#### [NEW] [JournalTypeConverters.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/data/local/entity/JournalTypeConverters.kt)
- Create a new file to handle `List<String>` serialization for Room using Gson.

#### [MODIFY] [AppDatabase.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/data/local/AppDatabase.kt)
- Add `JournalTypeConverters::class` to the `@TypeConverters` annotation.
- Increment database version to `7`.
- (Optional) Add a migration from `6` to `7` or rely on destructive migration if acceptable (I'll aim for a simple migration if possible, but since `fallbackToDestructiveMigration` is enabled in some contexts, I'll check if a migration is needed). Actually, the project seems to use `fallbackToDestructiveMigration(false)` but then doesn't provide migrations in the `builder`. I'll add a simple migration.

#### [MODIFY] [JournalRepository.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/data/repository/JournalRepository.kt)
- Update `pushToFirestore` to include the `tags` field.
- Update `syncOneShot` and `startSync` to read the `tags` field from Firestore.

---

### Business Logic Layer

#### [MODIFY] [JournalViewModel.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/ui/viewmodel/JournalViewModel.kt)
- Update `saveEntry` function signature to include `tags: List<String>`.
- Update `filteredEntries` logic to include tags in the search results (if the query matches a tag).

---

### UI Layer

#### [MODIFY] [JournalScreen.kt](file:///C:/Users/grigg/AndroidStudioProjects/LanguageStudyApp/app/src/main/java/io/github/langstudy/ui/screens/JournalScreen.kt)
- **Entry Item**: Display tags as small chips below the content.
- **Entry Sheet**:
    - Add a state variable for `tags` (List<String>).
    - Add a `TagInput` section where users can type a tag and press enter/add to add it to the list.
    - Display current tags as deletable chips.
- **Search**: (Optional Improvement) Allow filtering specifically by tags if time permits, otherwise the global search will include them.

## Verification Plan

### Automated Tests
- N/A (I will verify manually as the project relies on integration with Firebase).

### Manual Verification
1. Open the Journal screen.
2. Create a new entry and add several tags (e.g., "Grammar", "Struggle").
3. Save the entry and verify tags appear in the list.
4. Search for "Grammar" and verify the entry appears.
5. Edit the entry, remove a tag, and save. Verify the change persists.
6. Verify sync with Firestore (if possible in this environment).
