# UniCourseHub - Premium Academic Learning Platform

## Demo Login Credentials
- **Student:** `student2@gmail.com`
- **Instructor:** `instructor2@gmail.com`
- **Admin:** `admin2@gmail.com`
- **Password (for all):** `960516`

---

### Video Presentation
[View the project presentation here](https://drive.google.com/file/d/1WtEtMNWC-bQyMroVQQgcrZ24oqjNvfbT/view?usp=sharing)
*(Note: When viewing the video presentation, please double-click the video file to enable repeat playback or watch it multiple times.)*

UniCourseHub is a comprehensive, multi-role educational ecosystem built to provide a professional academic experience. It integrates structured video-based learning with a practical project-sharing economy, overseen by university administrators and enhanced by Google's Gemini AI.

## Project Structure Diagram

```text
UniCourseHub/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/unicoursehub/app/
│   │   │   │   ├── data/                 # Persistence & Data Models
│   │   │   │   │   ├── workers/          # Background WorkManager Logic
│   │   │   │   │   ├── DatabaseHelper.kt # SQLite Schema (v15)
│   │   │   │   │   └── Models.kt         # Domain Entities
│   │   │   │   ├── services/             # Cloud Integration
│   │   │   │   │   └── FCMService.kt     # Firebase Messaging Service
│   │   │   │   ├── ui/                   # Modular UI Components
│   │   │   │   │   ├── adapters/         # Custom List Logic
│   │   │   │   │   ├── admin/            # Quality Control Portal
│   │   │   │   │   ├── auth/             # Identity & Access
│   │   │   │   │   ├── instructor/       # Academic Management
│   │   │   │   │   ├── student/          # The Learning Hub
│   │   │   │   │   ├── splash/           # Entry Point
│   │   │   │   │   └── main/             # Navigation Infrastructure
│   │   │   │   └── util/                 # Cross-cutting Utilities
│   │   │   │       ├── AiHelper.kt       # Gemini AI Logic
│   │   │   │       └── NotificationHelper.kt # Branded Alert System
│   │   │   └── res/                      # XML Resources
│   │   │       ├── layout/               # Dark Academy UI Definitions
│   │   │       ├── values/               # Apple-Inspired Themes
│   │   │       └── values-night/         # Native Dark Mode Support
└── README.md
```

## Detailed File Infrastructure

The following core files drive the primary logic and functionality of the application:

### Data Architecture
- `DatabaseHelper.kt`: The primary SQLite engine for the entire app. It manages 11+ tables using a version 15 schema with complex JOIN queries to link users, courses, and student projects. It includes "auto-repair" logic to ensure tables are always present.
- `Models.kt`: Defines the core Kotlin data structures (User, Course, Project, Lesson) used for type-safe data handling throughout the application layers.
- `VideoCompressWorker.kt`: A specialized background worker using Media3 Transformer to compress high-resolution instructor videos into mobile-optimized formats without blocking the UI.
- `DownloadWorker.kt`: Manages reliable background video downloading, allowing students to save lectures for offline study while providing real-time progress notifications.

### Cloud and Services
- `FCMService.kt`: The entry point for Firebase Cloud Messaging. It interprets cloud payloads to decide whether to show a chat alert, a status update, or a general announcement based on the `type` key.

### Navigation and Core UI
- `MainActivity.kt`: The application's central nervous system. It handles role-based bottom navigation, dynamic fragment switching with backstack management, and automatic FCM topic subscription (subscribing all users to the `all` topic).
- `ModuleAdapter.kt`: A high-complexity RecyclerView adapter that groups flat lesson data into expandable modules, providing a professional hierarchical view of the course syllabus.
- `ProjectReviewAdapter.kt`: Powers the Showcase and Admin lists, integrating video thumbnail playback and role-aware management menus (Approve/Reject/Rate).

### Intelligence and Utilities
- `AiHelper.kt`: The bridge to Google's Gemini Pro AI. It handles API authentication and provides specialized functions for generating note summaries and academic chat responses.
- `NotificationHelper.kt`: A centralized manager for Android Notification Channels. It ensures every alert (Cloud or Local) features the official UniCourseHub branding and the correct prioritized icons (Chat, Trophy, or Download).
- `SessionManager.kt`: A secure wrapper around SharedPreferences that persists the user's ID and role across app restarts, ensuring they are always directed to the correct dashboard (Admin, Instructor, or Student).
- `PushNotificationSender.kt`: A hybrid utility that simulates cloud pushes locally for instant feedback while also sending real REST API requests to the Firebase servers for cross-device synchronization.

## Role-Based Page and Component Architecture

### Student Portal
The student experience focuses on immersive consumption and practical output.

- Student Dashboard (StudentDashboardFragment): Key stats (enrolled courses, progress), recent activities, and recommended courses.
- My Courses (StudentCoursesFragment): List of enrolled courses with progress indicators.
- Academy Video Player (VideoLearningFragment): Immersive Media3 player, 1.0x-2.0x speed control, 15s seek buttons, fullscreen mode, and integrated syllabus navigation.
- Project Showcase (ShowcaseFragment): Public gallery of approved student projects with high-end filtering and rating systems.
- My Projects (MyProjectsFragment): Submission tracker for the student's own work (Pending, Approved, Rejected).
- Post Project (PostProjectFragment): Multipart form for title, demo video upload, GitHub link, and documentation selection.
- Project Detail (ProjectDetailFragment): Immersive detail view of a project with demo video player and peer review timeline.
- SeniorDev AI Insights (InsightsFragment): AI-driven analytics based on student note patterns.
- Academic Competition (CompetitionFragment): Entry portal for university-wide coding challenges.
- Direct Messaging (MessagesFragment): Chat list and thread view for instructor communication.
- Study References (ReferencesFragment): Central hub for bookmarked lessons, saved notes, and AI-generated summaries.
- Settings (SettingsFragment): Dark/Light mode toggle, Password management, and Language localization.
- AI Tutor Chat (AiChatFragment): Lesson-specific AI chatbot powered by Gemini Pro.
- Global Notifications (NotificationsFragment): High-priority system alerts and push notification history.

### Instructor Portal
Instructors manage the curriculum and provide specialized mentorship.

- Instructor Dashboard (InstructorDashboardFragment): Overview of active courses, total students, and pending feedback tasks.
- My Courses (MyCoursesFragment): Management hub for the instructor's personal course catalog.
- Content Management (ContentManagementFragment): Course structure editor for adding/managing modules and lessons.
- Post Course (PostCourseFragment): Curriculum setup (Category, Semester, Title, Description).
- Add Lesson (PostLessonFragment): Media upload portal for Video lessons and supporting documents.
- My Students (MyStudentsFragment): Real-time tracker showing student progress percentages and identifying struggling learners.
- Student Projects (StudentProjectsFragment): Mentorship portal to view student work and provide professional ratings.
- Interaction Dashboard (InstructorInteractionsFragment): Unified feed for student comments, video ratings, and confusing content reports.
- Instructor Profile (InstructorProfileFragment): Academic stats and account credentials management.

### Admin Portal
Admins enforce academic standards and manage the user ecosystem.

- Admin Dashboard (AdminDashboardFragment): Global system analytics including total users, courses, and pending approval volume.
- Manage Users (ManageUsersFragment): Database-wide user list with profile editing, status suspension, and role reassignment.
- Manage Courses (ManageCoursesFragment): Oversight of all course content with global edit and delete authority.
- Manage Projects (ManageProjectsFragment): Global project directory for quality control.
- Pending Approvals Hub (PendingApprovalsFragment): Unified queue for new course releases and student project showcase entries.
- Course Quality Review (CourseReviewFragment): Immersive environment for admins to watch and verify course content before publishing.
- Project Showcase Review (ProjectReviewFragment): Verification portal for student demo videos and project source code.
- Admin Profile (AdminProfileFragment): Administrator access controls and profile settings.

## External API and SDK Integration

UniCourseHub leverages high-performance industry APIs to deliver a premium experience:

- Google Generative AI (Gemini Pro API): Orchestrates the SeniorDev AI features, including contextual lesson chat and automated study note summarization.
- Firebase Authentication: Provides a secure infrastructure for email/password and Google Single Sign-On (SSO).
- Firebase Cloud Messaging (FCM): Facilitates real-time broadcast (topic-based) and targeted push notifications for messaging and status updates.
- Media3 ExoPlayer: Drives the core video learning experience with adaptive bitrate support and immersive controls.
- Media3 Transformer: Executes on-device background video compression for optimized content uploads.
- WorkManager API: Manages persistent background tasks for reliable offline downloads and heavy processing jobs.
- OkHttp3: Handles high-speed networking for REST API communication with the Firebase Cloud infrastructure.
- SQLite Database: Local-first persistence engine using a highly optimized version 15 schema for data speed and reliability.

## AI-Driven Development & Architecture

UniCourseHub was conceptualized, architected, and built with the collaborative assistance of advanced AI (including Google's Gemini Pro API and AI-driven development workflows):

1. **Architectural Scaffolding & Multi-Role Design**: AI assisted in establishing the clean separation of concerns across Admin, Instructor, and Student portals, ensuring robust RBAC (Role-Based Access Control) and unified navigation via `MainActivity`.
2. **Generative AI Features (`AiHelper.kt`)**: AI integration was directly embedded into the core academic experience—powering contextual lesson chatbots (`AiChatFragment`) and automated study note summarization (`ReferencesFragment`) to optimize student exam preparation.
3. **Asynchronous Processing & Coroutines**: AI guidance helped structure and refine complex background operations such as Media3 Transformer video compression (`VideoCompressWorker`) and offline lecture downloading (`DownloadWorker`), guaranteeing a smooth 60fps UI experience without main-thread blocking.

## Database Selection & Architectural Rationale (In-Depth Analysis)

UniCourseHub employs a **Local-First SQLite Database Architecture** via a highly optimized, version 15 custom database engine (`DatabaseHelper.kt`) managing 11+ interconnected relational tables. Below is the in-depth technical rationale for choosing SQLite over alternatives like Room or pure cloud NoSQL databases:

### 1. Local-First Speed and Absolute Offline Availability
- **The Challenge**: Students and instructors frequently operate in environments with intermittent internet connectivity (lecture halls, transit, remote campuses).
- **The SQLite Advantage**: By persisting all user profiles, course structures, modules, lessons, project submissions, saved references, and interactions locally in SQLite, UniCourseHub provides **instant zero-latency data access**. App startup, course syllabus loading, and note-taking happen entirely offline without network round-trips.

### 2. Deep Relational Complexity & Data Integrity
- **The Challenge**: Academic platforms require managing heavily interconnected data structures:
  - Users $\leftrightarrow$ Courses $\leftrightarrow$ Modules $\leftrightarrow$ Lessons
  - Users $\leftrightarrow$ Project Submissions $\leftrightarrow$ Peer Reviews $\leftrightarrow$ Badges
  - Users $\leftrightarrow$ Saved References $\leftrightarrow$ Notes & AI Summaries
  - Users $\leftrightarrow$ Notifications & Direct Messages
- **The SQLite Advantage**: Unlike NoSQL document stores (e.g., Firestore) which require complex client-side joins, data duplication, or eventual-consistency synchronization overhead, SQLite provides **full relational power**. Using native SQL `JOIN`, foreign keys, cascading constraints, and aggregation queries (`COUNT`, `SUM`), the Admin and Instructor portals can instantly compute global statistics (e.g., pending course approvals, student progress percentages, unread notification badges) with 100% data consistency.

### 3. Cost Efficiency & Zero Cloud Query Latency
- **The Rationale**: Relying solely on a cloud database for every local UI read (such as scrolling through video lessons or opening personal notes) incurs continuous bandwidth costs and network latency. SQLite acts as an ultra-fast local cache and source of truth, while Firebase Authentication and Firebase Cloud Messaging (FCM) handle secure identity management and cloud push notifications.

### 4. Self-Healing Schema & Resilience (`DatabaseHelper.kt`)
- **The Rationale**: Version 15 of `DatabaseHelper.kt` incorporates robust defensive programming and auto-repair table creation logic. If tables are missing or require migration across app updates, the database engine programmatically validates and reconstructs schemas on initialization, preventing crashes and data loss.

## Core Uniqueness

- Closed-Loop Learning: UniCourseHub is the only platform that forces a connection between watching a video and publishing a project to a peer-reviewed showcase.
- Adaptive Academy UI: The application features a custom UI engine that ensures "Dark Academy" immersion across all creation and learning flows, regardless of the system theme.
- AI-Driven Study Summaries: Leverages Gemini Pro to automatically generate concise summaries of handwritten student notes, optimizing exam preparation.
- Confusing Content Feedback: Features a specialized "Mark as Confusing" mechanism that provides real-time, lesson-specific feedback to instructors to improve curriculum quality.
- Competitive Academic Environment: Integrates a Project Competition framework, allowing students to elevate their coursework into university-wide showcased entries.
- Professional Mentorship Logic: Replaces standard generic feedback with a role-aware system where instructors provide specific badges and expert ratings.
- Zero-Cost Communication: Fully integrated FCM infrastructure provides real-time global connectivity without any external subscription costs.
