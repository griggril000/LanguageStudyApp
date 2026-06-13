# Language Study - Development Plan

## Status: IN_PROGRESS

### Completed Tasks
- [x] **Task 1: UI Foundation**
    - Material 3 theme with vibrant orange/amber scheme.
    - Navigation 3 adaptive shell (Nav Rail/Bottom Bar).
    - SDK 37 upgrade.
- [x] **Task 2: Portfolio Section**
    - Firebase integration with `google-services.json`.
    - Firestore repository for student work.
    - Coil 3 for image loading.
    - Infinite scrolling implementation.
- [x] **Task 3: Offline Modules**
    - Room database for Vocab, Skills, and Journal.
    - ViewModels and functional screens for each.
    - Editable journal entries with Firestore sync.
- [x] **Task 4: Authentication**
    - Google Login using Jetpack Credential Manager.
    - AuthViewModel with Firebase Auth integration.
    - LoginScreen UI with Email/Password (Login & Sign Up) and Google Login.
    - Conditional navigation logic in `MainActivity`.
- [x] **Task 5: Refine Portfolio & Infinite Scrolling**
    - Aligned Firestore path with website (`users/{userId}/portfolio`).
    - Implemented real paging/scrolling logic.
    - Added thumbnail extraction for YouTube.
    - Added Portfolio Sharing (Code generation & Public toggle).

### Upcoming Tasks
- [ ] **Task 6: Feature Enhancements**
    - [x] Implement persistent per-page floating search with real-time filtering.
    - [x] Add destructive action warnings (Delete confirmation dialogs).
    - [x] Add Language filters to Portfolio and Vocab.
    - [x] Implement Category CRUD for Vocabulary (Persistent categories, delete cascade).
    - [x] Add edit ability to portfolio items.
    - [ ] Implement "Mentor Mode" (View portfolios via shared code).
    - [ ] Implement notifications for study reminders.
- [ ] **Task 7: Final Polishing**
    - Adaptive icon refinement.
    - Performance optimization.
    - Edge-to-edge UI improvements.

## Technical Notes
- **Authentication**: Using `androidx.credentials` (Credential Manager), the modern standard for 2026.
- **Navigation**: Using `androidx.navigation3` which is state-driven.
- **Data**: Offline-first model. Portfolio (Online/Firebase), Vocab/Skills/Journal (Offline-first with Firebase sync).
