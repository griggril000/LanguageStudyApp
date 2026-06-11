# Language Study - Development Plan

## Status: IN_PROGRESS

### Completed Tasks
- [x] **Task 1: UI Foundation**
    - **(todo)** Material 3 theme with vibrant orange/amber scheme.
    - Navigation 3 adaptive shell (Nav Rail/Bottom Bar).
    - SDK 37 upgrade.
- [x] **Task 2: Portfolio Section**
    - Firebase integration with `google-services.json`.
    - Firestore repository for student work.
    - Coil 3 for image loading.
    - Infinite scrolling mockup/implementation.
- [x] **Task 3: Offline Modules**
    - Room database for Vocab, Skills, and Journal.
    - ViewModels and functional screens for each.
- [x] **Task 4: Authentication**
    - **(todo)** Google Login using Jetpack Credential Manager.
    - AuthViewModel with Firebase Auth integration.
    - LoginScreen UI with Email/Password (Login & Sign Up) and Google Login.
    - Conditional navigation logic in `MainActivity`.

### Upcoming Tasks
- [x] **Task 5: Refine Portfolio & Infinite Scrolling**
    - Aligned Firestore path with website (`users/{userId}/portfolio`).
    - Implemented real paging/scrolling logic.
    - Added thumbnail extraction for YouTube.
    - Added Portfolio Sharing (Code generation & Public toggle).
- [ ] **Task 6: Feature Enhancements**
    - **(done)** Implement Search Bar in Top Bar (Global Search) - Implemented persistent per-page floating search with real-time filtering.
    - Add Language filters to Portfolio and Vocab.
    - Separate out "OtherScreens.kt"
    - Add destructive actions warning before performing delete operations.
    - Implement "Mentor Mode" (View portfolios via code).
    - Implement notifications for study reminders.
- [ ] **Task 7: Final Polishing**
    - Adaptive icon refinement.
    - Performance optimization.
    - Edge-to-edge UI improvements.

## Technical Notes
- **Authentication**: Using `androidx.credentials` (Credential Manager) which is the modern standard for 2026.
- **Navigation**: Using `androidx.navigation3` which is state-driven.
- **Data**: Offline first model - Portfolio (Online/Firebase), Vocab/Skills/Journal (Offline first, stored in Firebase).
