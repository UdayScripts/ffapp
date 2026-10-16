# Free Fire Tournament Platform & Admin Management System

A comprehensive Free Fire tournament and esports management Android application featuring player registration, instant simulated wallet deposits, detailed match and transaction logs, real-time match room notification updates, and a full-featured Admin Control Panel for tournament lifecycle handling, user accounts, and customizable app settings in a high-contrast minimalist light aesthetic.

## User Review & Critical Decisions

> [!IMPORTANT]
> The implementation plan incorporates the answers confirmed during the initial consultation:

- **Confirmed Match Modes**: Battle Royale supporting **Solo, Duo, and Squad** game modes across Free Fire maps (Bermuda, Purgatory, Kalahari, Alpine).
- **Confirmed Wallet & Deposit Model**: **Instant simulated top-up wallet** for smooth demo and tournament testing, coupled with realistic deposit history, simulated transaction IDs, and live balance bookkeeping logs.
- **Confirmed Visual Direction**: **Minimalist modern light theme** featuring crisp stark backgrounds (`#F8FAFC`, `#FFFFFF`), deep obsidian high-contrast cards (`#0F172A`), sharp amber/fire accents (`#FF5722`, `#EA580C`), clean typography, and elevated tactile surfaces.

---

## 1. Overview & Core Concept

- **What It Does**:
  - **Player Experience**: 
    - Browse upcoming, ongoing, and completed Free Fire tournaments with mode filters (Solo, Duo, Squad, Map).
    - Register for tournaments by entering Free Fire In-Game Name (IGN) and Player UID.
    - Inspect prize pools, entry fees, per-kill rewards, and rules.
    - Real-time match room notification system: receives instant alerts when the Admin publishes the Room ID & Password, with one-tap copy.
    - In-app simulated wallet: instant top-up, deposit history logs, balance ledger (entry fee deductions, prize payouts).
    - Player Profile & Match History: comprehensive record of entered tournaments, results, placements, kills, and earnings.
  - **Admin Control Panel**:
    - Admin Login & Registration with secure PIN/credentials.
    - Tournament Management: Create new tournaments, edit schedules/rules, update status (Upcoming -> Live -> Completed), and release Room ID & Password with instant notification broadcast.
    - Match Result Settlement: Record participant kills and placements, automatically credit winners' wallets, and mark matches completed.
    - User Management: View registered players, inspect their FF UIDs, edit wallet balances, and manage status.
    - System Settings Customization: Customize app name, announcement marquee, support links (Discord/WhatsApp), platform fee, and maintenance mode toggle.
    - Real-time Notification Broadcast: Admin can broadcast custom alerts (e.g. room credentials, match delays, tournament announcements) directly to players.

- **Target Audience / Persona**: Free Fire esports enthusiasts, competitive squads, and tournament organizers.
- **Key Value**: A complete, self-contained tournament management ecosystem where admins can run tournaments end-to-end and players enjoy a professional tournament experience.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│                              USER FLOW                                 │
└────────────────────────────────────────────────────────────────────────┘
 [Home Dashboard] ──► [Tournament Details] ──► [Join Match (UID & IGN)]
         │                                               │
         ├───────────────────────────────────────────────┘
         ▼
 [Room Credentials Alert] ──► [Copy Room ID/Pass] ──► [Play in Free Fire]
         │
         ▼
 [Match Completed] ──► [Check Results & History] ──► [Wallet Balance Log]

┌────────────────────────────────────────────────────────────────────────┐
│                              ADMIN FLOW                                │
└────────────────────────────────────────────────────────────────────────┘
 [Admin Portal Auth] ──► [Admin Overview Dashboard (Metrics & Quick Actions)]
         │
         ├─► [Tournaments Manager] ──► [Create / Edit / Publish Room ID]
         ├─► [Result Settlement]   ──► [Input Kills, Rank & Disburse Prizes]
         ├─► [User Management]     ──► [Inspect Players & Adjust Balances]
         └─► [App Settings]        ──► [Configure Announcement, Rules & Fees]
```

### Visual Identity & Theme
- **Aesthetic Direction**: Minimalist modern light theme with deep high-contrast cards and vibrant esports fire-orange accents.
- **Color Palette**:
  - `Background`: `#F8FAFC` (Slate 50) & `#FFFFFF`
  - `High-Contrast Surface`: `#0F172A` (Slate 900) for hero tournament cards and esports banners.
  - `Surface Cards`: `#FFFFFF` with `#E2E8F0` crisp outlines and `#F1F5F9` subtle highlights.
  - `Primary / Accent`: `#FF5722` (Vibrant Flame Orange) and `#EA580C` (Deep Fire).
  - `Success`: `#10B981` (Emerald for deposits, prize credits, and verified status).
  - `Live Status`: `#EF4444` (Pulse Red for live matches).
  - `Text`: `#0F172A` (High-contrast Primary Text), `#64748B` (Secondary Muted Text).
- **Typography & Hierarchy**:
  - Bold headline numbers for entry fees, coin balances, and countdown timers.
  - Clean monospaced styling for Room IDs and Free Fire UIDs for instant legibility.
- **Interactive Feedback & Motion**:
  - Interactive chip filters for game modes (All, Solo, Duo, Squad).
  - One-tap "Copy Room ID" with visual checkmark indicator and haptic/toast feedback.
  - Real-time in-app notification badge and floating snackbar alert when room credentials drop.
  - Modal sheets for Instant Wallet Top-Up and Tournament Registration.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Persistence Strategy via Room Database**
  - *Chosen Approach*: Implement a structured Room Database with pre-seeded initial tournaments, sample players, deposit logs, and settings, providing instantaneous reactive queries (`Flow`) to both user and admin views.
  - *Why*: Room provides zero-latency updates; when an admin creates a tournament or releases room credentials, all user UI components instantly reflect the new state without requiring cloud backend configuration.
  - *Alternative Considered*: Static in-memory lists (lost on app process death) or external cloud setup (adds latency and setup barriers).
- **Decision 2: Dual Role Mode (Player & Admin Switcher)**
  - *Chosen Approach*: Include dedicated Admin Authentication (Login & Register) along with an intuitive profile mode-switch toggle so reviewers and users can seamlessly experience both sides of the app.
  - *Why*: Guarantees easy evaluation of tournament creation, room credential alerts, and result payouts.
- **Decision 3: Real-Time Notification System**
  - *Chosen Approach*: An in-app Notification Center backed by Room Database, combined with an active Kotlin Coroutine StateFlow event bus that triggers immediate high-priority banner alerts in the top bar whenever a room ID is published or a tournament status changes.
  - *Why*: Gives players the critical real-time cue to join the custom room before the match starts without missing room keys.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
├──────────────────────────────────┬─────────────────────────────────────┤
│          Player UI               │               Admin UI              │
│  - DashboardScreen               │  - AdminDashboardScreen             │
│  - TournamentDetailScreen        │  - AdminTournamentEditorScreen      │
│  - WalletScreen & DepositDialog  │  - AdminMatchSettlementScreen       │
│  - MatchHistoryScreen            │  - AdminUserManagementScreen        │
│  - NotificationCenterScreen      │  - AdminAppSettingsScreen           │
└─────────────────┬────────────────┴──────────────────┬──────────────────┘
                  │                                   │
                  ▼                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          VIEWMODEL LAYER                               │
│  - TournamentViewModel (Player match browsing, joining, filtering)     │
│  - WalletViewModel (Balance, instant deposit, transaction log)         │
│  - NotificationViewModel (Live notifications, room alerts, badges)     │
│  - AdminViewModel (CRUD tournaments, user moderation, app settings)   │
└─────────────────────────────────┬──────────────────────────────────────┘
                                  │
                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          REPOSITORY LAYER                              │
│  - TournamentRepository                                                │
│  - WalletRepository                                                    │
│  - NotificationRepository                                              │
│  - AppSettingsRepository                                               │
└─────────────────────────────────┬──────────────────────────────────────┘
                                  │
                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   LOCAL DATABASE LAYER (ROOM)                          │
│  - Entities:                                                           │
│      • TournamentEntity (id, title, mode, map, entryFee, prizePool,    │
│                          perKill, maxPlayers, registeredCount,         │
│                          scheduleTime, status, roomId, roomPass)       │
│      • MatchParticipantEntity (tournamentId, userId, ign, ffUid, kills,│
│                                placement, prizeWon)                    │
│      • UserEntity (id, username, email, ffUid, walletBalance, isAdmin) │
│      • DepositTransactionEntity (id, userId, amount, type, status,     │
│                                  timestamp, referenceId)               │
│      • NotificationEntity (id, title, message, type, timestamp, isRead)│
│      • AppSettingsEntity (id, appName, bannerText, platformFee, etc.)  │
└────────────────────────────────────────────────────────────────────────┘
```

### Core Entities & Data Schema
1. **Tournament**:
   - `id: String`, `title: String`, `matchType: MatchType (SOLO, DUO, SQUAD)`, `map: String (Bermuda, Purgatory, Kalahari, Alpine)`
   - `entryFee: Int`, `prizePool: Int`, `perKillReward: Int`, `maxSlots: Int`, `joinedSlots: Int`
   - `matchTime: String`, `status: MatchStatus (UPCOMING, LIVE, COMPLETED, CANCELLED)`
   - `roomId: String?`, `roomPassword: String?`, `roomNotice: String?`
2. **Participant**:
   - `tournamentId: String`, `userId: String`, `ffIgn: String`, `ffUid: String`
   - `kills: Int`, `rank: Int`, `payout: Int`
3. **Wallet & Balance Log**:
   - `transactionId: String`, `userId: String`, `amount: Int`, `type: TxType (DEPOSIT, ENTRY_FEE, PRIZE_WIN)`
   - `title: String`, `timestamp: Long`, `status: String (SUCCESS)`
4. **Notification**:
   - `id: String`, `title: String`, `body: String`, `type: String (ROOM_UPDATE, MATCH_RESULT, SYSTEM)`
   - `timestamp: Long`, `isRead: Boolean`
5. **App Settings**:
   - `appName: String`, `announcementText: String`, `supportContact: String`, `maintenanceMode: Boolean`, `minDeposit: Int`

---

## 5. Implementation Stages (To Be Executed Upon Approval)

1. **Step 1: Project Metadata & Dependencies**:
   - Set app name in `metadata.json` ("Free Fire Tournaments") and `strings.xml`.
   - Verify Room & Compose dependencies in `app/build.gradle.kts`.
2. **Step 2: Database & Data Layer**:
   - Create Room Entities, DAOs, Database, and pre-seeded database callback with realistic Free Fire tournaments and demo match histories.
   - Implement Repositories providing reactive `Flow` streams.
3. **Step 3: Business Logic & ViewModels**:
   - Create `TournamentViewModel`, `WalletViewModel`, `NotificationViewModel`, and `AdminViewModel`.
4. **Step 4: Design System & Theme**:
   - Set up the Minimalist Modern Light theme tokens, high-contrast dark card styles, typography, and accent colors in `Theme.kt` and `Color.kt`.
5. **Step 5: Player UI Components**:
   - Home Dashboard with mode filters, active ticker, and balance pill.
   - Tournament details sheet with live prize breakdown and registration dialog (IGN + UID).
   - Instant Wallet top-up dialog, deposit history list, and balance ledger.
   - Real-time notification center and room credential popups.
   - User profile and match results history.
6. **Step 6: Admin Panel UI Components**:
   - Admin Login/Register screen and auth guard.
   - Admin Tournament management (create match, publish Room ID/Pass, broadcast notification).
   - Match settlement screen (record kills, declare winners, auto-disburse winnings).
   - User details and wallet adjustments.
   - Custom app settings screen (app name, announcement marquee, support links).
7. **Step 7: Verification**:
   - Compile applet with `compile_applet` and verify zero errors.
