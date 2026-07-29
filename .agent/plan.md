# Project Plan

A typical browser app for Android named "Surfer" balancing standard web navigation features with native Android OS integration. Core features include web rendering (WebView), Omnibox, tab management, basic controls, default browser handling, custom tabs support, download manager, gesture & UI alignment (M3, Edge-to-Edge), SSL/TLS validation, site permissions, data management, content protections, desktop site toggle, bookmarks & history, reader mode, autofill, PWA support, memory management, and media handling.

## Project Brief

# Project Brief: Surfer - Modern Web Browser

Surfer is a high-performance, Material 3-focused Android browser designed to offer a seamless, edge-to-edge browsing experience. It balances powerful web rendering with deep native Android integration, ensuring a fluid experience across all device form factors from compact phones to large-screen tablets.

### Features
*   **Web Rendering with Omnibox**: A robust integration of Android WebView for high-fidelity web content display, paired with a modern Omnibox for unified URL entry and search.
*   **Adaptive Navigation Controls**: Standard browser controls (Back, Forward, Refresh, Home) that dynamically adjust their layout based on the device's screen size and orientation.
*   **State-Driven Tab Management**: Efficient multi-session handling powered by state-driven logic, allowing users to switch between multiple web pages instantaneously.
*   **Material 3 Edge-to-Edge UI**: A vibrant, energetic design that utilizes the full display area, featuring modern M3 components and a dynamic color scheme that adapts to the user's environment.
*   **Default Browser & Custom Tabs**: Handling http/https intents and implementing CustomTabsIntent API.
*   **Download Manager**: Scoped Storage for background downloads.
*   **Security & Privacy**: SSL/TLS validation, site permissions, data management (clear cache/cookies).
*   **Productivity**: Desktop site toggle, Bookmarks/History, Reader Mode, Autofill, PWA.
*   **Performance**: Tab suspension/discarding, media handling.

### High-Level Technical Stack
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose
*   **Navigation**: Jetpack Navigation 3
*   **Adaptive Strategy**: Compose Material Adaptive library
*   **Concurrency**: Kotlin Coroutines
*   **Web Engine**: Android WebView API

## Implementation Steps

### Task_1_Core_Engine: Implement the core browser engine and Material 3 scaffold. This includes setting up the vibrant M3 theme, Edge-to-Edge display, an Omnibox for URL/search entry, and a WebView for rendering content with basic navigation (Back, Forward, Refresh).
- **Status:** COMPLETED
- **Updates:** Implemented core browser engine with WebView, Omnibox, and M3 scaffold. Set up vibrant M3 theme, Edge-to-Edge display, and basic navigation (Back, Forward, Refresh). Used Navigation 3 for architecture. Added adaptive icon.
- **Acceptance Criteria:**
  - M3 vibrant theme implemented
  - Edge-to-Edge display active
  - Omnibox navigates to URLs
  - WebView renders content correctly
  - Back/Forward/Refresh controls work

### Task_2_Tab_Management: Implement state-driven tab management and adaptive navigation. Use the Compose Material Adaptive library to ensure the browser layout (tabs, controls) adjusts for different screen sizes and orientations.
- **Status:** COMPLETED
- **Updates:** Implemented multi-tab management with BrowserTabState and UUIDs. Integrated NavigationSuiteScaffold for adaptive UI: horizontal tab strip on phones and Navigation Rail on tablets. Used BrowserViewModel for state management. Added "New Tab" functionality and dynamic tab titles.
- **Acceptance Criteria:**
  - Users can create and switch between multiple tabs
  - State-driven tab management works
  - Adaptive layout responds to screen size changes
  - Navigation 3 integrated for tab transitions

### Task_3_Features_Storage: Integrate Bookmarks, History, and the Download Manager. Implement persistence using Room for history/bookmarks and Scoped Storage for downloads. Add support for handling external URL intents (Default Browser) and basic productivity features.
- **Status:** COMPLETED
- **Updates:** Implemented Room database for History and Bookmarks. Added a bottom sheet UI for managing them. Integrated system DownloadManager with Scoped Storage support. Updated AndroidManifest for intent handling (http/https). Added Desktop Site toggle and a vibrant dropdown menu for features.
- **Acceptance Criteria:**
  - Bookmarks and History can be saved and retrieved
  - Downloads work with Scoped Storage
  - App handles http/https intents as a browser
  - Desktop site toggle works

### Task_4_Polish_Performance: Refine the app's visual identity and performance. Create an adaptive app icon, implement tab suspension/discarding for memory management, and ensure SSL/TLS security validations and site permissions are handled.
- **Status:** COMPLETED
- **Updates:** Created a vibrant adaptive app icon. Implemented tab suspension/discarding for memory management. Added SSL/TLS error handling with user warnings. Integrated runtime site permission handling (camera, location, etc.). Finalized M3 UI polish and Edge-to-Edge display.
- **Acceptance Criteria:**
  - Adaptive app icon created and functional
  - Memory management (tab suspension) implemented
  - SSL/TLS warnings handled correctly
  - Site permissions (camera/location) integrated

### Task_5_Final_Verification: Perform a final run and verify the entire application. Ensure stability, M3 compliance, and that all project brief requirements are met without crashes.
- **Status:** IN_PROGRESS
- **Updates:** Reopened verification due to a reported crash: IndexOutOfBoundsException in ScrollableTabRow when adding a new tab. Coder agent has refactored the state management to a unified BrowserState to ensure atomic updates of tab list and selected index.
- **Acceptance Criteria:**
  - Project builds successfully
  - All existing tests pass
  - App does not crash during navigation or tab switching
  - UI aligns with project brief requirements

