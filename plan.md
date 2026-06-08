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
    - Infinite scrolling mockup/implementation.
- [x] **Task 3: Offline Modules**
    - Room database for Vocab, Skills, and Journal.
    - ViewModels and functional screens for each.
- [x] **Task 4: Authentication**
    - Google Login using Jetpack Credential Manager.
    - AuthViewModel with Firebase Auth integration.
    - LoginScreen UI with Email/Password (placeholders) and Google Login.
    - Conditional navigation logic in `MainActivity`.

### Upcoming Tasks
- [ ] **Task 5: Refine Portfolio & Infinite Scrolling**
    - Add paging logic to Firestore repository.
    - Implement real video/media playback.
- [ ] **Task 6: Feature Enhancements**
    - Add search/filter to Vocab and Journal.
    - Implement notifications for study reminders.
- [ ] **Task 7: Final Polishing**
    - Adaptive icon refinement.
    - Performance optimization.
    - Edge-to-edge UI improvements.

## Technical Notes
- **Authentication**: Using `androidx.credentials` (Credential Manager) which is the modern standard for 2026.
- **Navigation**: Using `androidx.navigation3` which is state-driven.
- **Data**: Hybrid model - Portfolio (Online/Firebase), Vocab/Skills/Journal (Offline/Room).
