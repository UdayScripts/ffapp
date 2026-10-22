# Private Notification Feed & Notification Clearing Controls

Isolates each player's notification feed so they only see their own personal updates and alerts for tournaments they have joined since signing up, while adding both bulk "Clear All" and individual per-card delete controls.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product decisions were confirmed during clarification and govern this implementation:

- **Confirmed Decision 1 — Strict Per-User & Joined-Match Scoping**: Players will only see notifications explicitly addressed to their account (`userId == currentUserId`) or broadcast alerts (such as Room ID & Password releases) for tournaments they have joined, plus global announcements sent after their signup. Notifications belonging to other users are strictly excluded.
- **Confirmed Decision 2 — Post-Signup Timestamp Cutoff**: Each user profile tracks an account creation timestamp (`createdAt`). Any notification with a timestamp older than the player's signup time is automatically filtered out so new accounts start with a clean feed.
- **Confirmed Decision 3 — Dual Clearing Controls (Clear All + Per-Item Delete)**: The Notification Center includes a **Clear All** action in the top app bar and an individual **Delete (Trash/Dismiss)** icon on every notification card, persisting cleared IDs both locally and in Cloud Firestore so deleted alerts never reappear.

---

## 1. Overview & Core Concept

- **What It Does**: Eliminates cross-user notification leakage and historical clutter in the real-time notification engine and Notification Center screen, while giving players full control to delete individual alerts or clear their entire notification inbox.
- **Target Audience / Persona**: Competitive Free Fire tournament players who need a private, distraction-free inbox containing only their own registration confirmations, wallet updates, prize payouts, and room credentials for matches they are registered in.
- **Key Value**: Protects user privacy (preventing players from seeing other players' deposits, registrations, or winnings), ensures newly registered accounts are not flooded with old alerts, and lets players keep their inbox clean.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Clean First-Time Inbox**: When a new user signs up or logs in, their account creation timestamp is recorded. Only notifications occurring at or after their signup that belong to them (or their joined matches) appear in the Notification Center and real-time heads-up banner.
  2. **Per-Item Notification Deletion**: On any notification card in the Notification Center, tapping the subtle trash/delete icon immediately removes that notification from the list with a confirmation toast (`"Notification removed"`).
  3. **Bulk Clear All**: Tapping the **Clear All** button in the Notification Center top bar clears all currently visible notifications for the user and transitions the screen to the clean empty state (`"No notifications yet"`).
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: High-contrast tactical esports interface matching the Free Fire Tournament Arena theme.
  - *Color Palette & Mood*: Crisp light slate background (`#F8FAFC`) paired with white read cards, warm unread highlight cards (`#FFF7ED`), `#FF5722` (Fire Orange) accents, and `#EF4444` (Crimson Red) for destructive clear/delete actions.
  - *Typography & Hierarchy*: Bold 13sp card titles, 12sp secondary body copy, and compact 11sp action pills with clear iconography.
  - *Component Styling & Layout*: Material 3 `TopAppBar` action buttons (`Mark Read` and `Clear All`) with minimum 48dp accessible touch targets on per-card delete icons.
- **Interactive Feedback & Motion**:
  - Instant optimistic UI removal when deleting a single notification or clearing all notifications, accompanied by a lightweight toast confirmation.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Client + Cloud Hybrid Filtering with Signup Timestamp Gate**
  - *Chosen Approach*: Combine real-time streams of the user's profile (`createdAt`), the user's joined tournament IDs (`userParticipations`), Cleared Notification IDs, and the Firestore notifications feed. Filter both the Notification Center list and the real-time push banner using the same privacy predicate.
  - *Why*: Guarantees that personal notifications for User A are never shown to User B in either the inbox list or the live floating banner, while still allowing multi-recipient tournament room key broadcasts to reach only the participants of that match.
  - *Alternatives Considered*: Separate subcollections per user for all notifications—passed over because tournament room credentials are broadcast once per match and matched dynamically against registered participants.
- **Decision 2: Optimistic Local + Cloud Persistence for Cleared Notifications**
  - *Chosen Approach*: Store cleared notification IDs in `SharedPreferences` scoped per user ID for instant zero-latency UI removal, while also recording the user ID in a `clearedByUsers` array on shared Firestore notification documents (or deleting personal notification documents outright).
  - *Why*: Ensures cleared items disappear immediately on tap and stay hidden across app restarts.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       Cloud Firestore Backend                           │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌───────────────┐  │
│  │       users          │  │    notifications     │  │ participations│  │
│  │ • createdAt: Long    │  │ • userId: String?    │  │ • tournamentId│  │
│  │ • id: String         │  │ • tournamentId: Str? │  │ • userId      │  │
│  └──────────┬───────────┘  │ • timestamp: Long    │  └───────┬───────┘  │
│             │              │ • clearedByUsers: [] │          │          │
└─────────────┼──────────────┴──────────┬───────────┴──────────┼──────────┘
              │                         │                      │
              ▼                         ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     AppViewModel Reactive Filter                        │
│  • Privacy Gate:                                                        │
│    1. Exclude if id in clearedIds or userId in notif.clearedByUsers     │
│    2. Exclude if notif.timestamp < user.createdAt                       │
│    3. If notif.userId != null -> require notif.userId == currentUserId  │
│    4. If notif.tournamentId != null (Room Key / Match Update) ->        │
│       require notif.tournamentId in joinedTournamentIds (or Announcement│
│       after signup)                                                     │
└──────────────────┬───────────────────────────────────┬──────────────────┘
                   │                                   │
                   ▼                                   ▼
┌─────────────────────────────────────┐ ┌─────────────────────────────────┐
│      NotificationCenterScreen       │ │   Real-Time Push & Banner Gate  │
│  • TopBar: [Mark Read] [Clear All]  │ │  • Only triggers system tray &  │
│  • NotificationCard:                │ │    in-app banner if notification│
│    - [Mark as Read] pill            │ │    passes the user privacy &    │
│    - [Delete / Clear] icon button   │ │    signup timestamp filter      │
└─────────────────────────────────────┘ └─────────────────────────────────┘
```

- **Data Model & State**:
  - `UserDto`: Add `createdAt: Long = 0L` (populated with `System.currentTimeMillis()` on initial account creation; existing accounts without `createdAt` default to `0L` or first-seen timestamp stored locally so they don't lose legitimate post-registration alerts).
  - `NotificationDto`: Add `clearedByUsers: List<String> = emptyList()` alongside `readByUsers: List<String>`.
  - Local Persistence: Track `_locallyClearedNotifIds` in `SharedPreferences` (`cleared_notifs_$uid`) and record a per-user `account_first_seen_$uid` timestamp fallback.
- **Interactive Component & State Mapping**:
  - `clearNotification(id: String)`: Immediately adds `id` to `_locallyClearedNotifIds`, updates Firestore (`clearedByUsers` via `FieldValue.arrayUnion(userId)` or deletes if owned by `userId`), and emits a confirmation toast.
  - `clearAllNotifications()`: Immediately adds all currently visible notification IDs to `_locallyClearedNotifIds`, batch-updates Firestore for the current user, and transitions the UI to the empty state.
