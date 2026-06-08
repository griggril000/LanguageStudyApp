# Project Plan

The Language Study app is a mobile application designed for language learners. It features an online-only Portfolio section that uses infinite scrolling to display student work from a Firebase backend. The rest of the app, including Vocabulary, Skills, and Journaling features, is designed to work offline using a local Room database. The app will follow Material Design 3 guidelines, featuring a vibrant and energetic color scheme, edge-to-edge display, and adaptive layouts for different screen sizes. Priority: Build the Portfolio section first.

## Project Brief

# Language Study - Project Brief

## Features
1. **Infinite Scrolling Portfolio (Online-Only)**: A dynamic feed fetching student work and project data from the existing Firebase backend. It supports featured media playback and continuous scrolling for an effortless browsing experience.
2. **Offline Study Modules**: Core learning features including **Vocabulary**, **Skills**, and **Journal** sections are engineered to work entirely offline, ensuring study sessions are never interrupted by connectivity issues.
3. **Adaptive Multi-Pane Layout**: A responsive interface that automatically adjusts between a mobile-first view and a multi-pane tablet/foldable layout, utilizing a navigation rail or sidebar as seen in the desktop version.
4. **Material 3 Vibrant UI**: A high-energy, energetic aesthetic implementation of Material Design 3, featuring a full edge-to-edge display and a custom adaptive app icon.

## High-Level Technical Stack
* **Language**: Kotlin
* **UI Framework**: Jetpack Compose with **Material Design 3**
* **Navigation**: **Jetpack Navigation 3** (State-driven)
* **Layout Strategy**: **Compose Material Adaptive** library
* **Database**: **Room** (Required for offline Vocab, Skills, and Journal functionality)
* **Networking & Backend**: Firebase SDK (for online Portfolio data)
* **Asynchronous Logic**: Kotlin Coroutines & Flow
* **Image Loading**: Coil (for portfolio thumbnail rendering)

## Implementation Steps

### Task_1_UI_Foundation: Set up the Material 3 theme with a vibrant color scheme, implement edge-to-edge display, and establish the Navigation 3 shell using adaptive layout components (Navigation Rail/Bottom Bar).
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Project builds successfully
  - M3 vibrant theme applied
  - Navigation shell works on different screen sizes
  - The implemented UI must match the design provided in input_images/image_0.png
- **StartTime:** 2026-06-08 00:54:09 MDT

### Task_2_Portfolio_Section: Integrate Firebase and implement the infinite scrolling Portfolio section, including image loading with Coil and featured media playback support.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Firebase integration complete
  - Portfolio displays data with infinite scrolling
  - Images load correctly using Coil
  - The implemented UI must match the design provided in input_images/image_0.png

### Task_3_Offline_Modules: Setup Room database for local persistence and implement the Vocabulary, Skills, and Journal modules to work fully offline.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Room database stores and retrieves data correctly
  - Vocabulary, Skills, and Journal screens are functional offline
  - The implemented UI must match the design provided in input_images/image_0.png

### Task_4_Finalization_and_Verification: Create an adaptive app icon, refine the energetic UI aesthetics, and perform a final verification of the application stability and requirements.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Adaptive app icon is present
  - App does not crash
  - Full compliance with Material 3 and adaptive layout guidelines
  - The implemented UI must match the design provided in input_images/image_0.png
  - Build pass

