# Watch Ads & Earn Balance (Unity Rewarded Ads)

Add a rewarded video ads feature to the Wallet page powered by the Unity Ads SDK, allowing players to watch up to 5 video ads daily to earn a random reward between ₹1 and ₹5 added directly to their deposit wallet balance.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following parameters were confirmed during interactive clarification and are locked into the design:

- **Confirmed Decision 1 (Ad SDK & Environment)**: Integrated with the official Unity Ads Android SDK (`com.unity3d.ads:unity-ads:4.12.5` or compatible) configured in test mode with standard Android placement ID (`Rewarded_Android`). Includes graceful test fallback for local execution environments.
- **Confirmed Decision 2 (Daily Allowance Reset)**: Daily quota resets automatically at midnight (12:00 AM) based on the user's localized calendar date.
- **Confirmed Decision 3 (Reward Crediting)**: Rewards (random ₹1 to ₹5) are credited directly to the player's **Deposit Balance** (`walletBalance`), making them immediately available for tournament entry fees.

---

### 1. Overview & Core Concept

- **What It Does**: Adds an interactive "Watch Ads & Earn" section to `WalletScreen.kt`. Players can view their daily progress (e.g., 2/5 ads watched), tap "Watch Ad", experience a rewarded video via Unity Ads, and celebrate receiving an instant ₹1 to ₹5 bonus added to their wallet with an updated ledger entry.
- **Target Audience / Persona**: Free Fire tournament players who need small top-ups to join matches without needing to pay via UPI every time.
- **Key Value**: Increases daily engagement, provides an accessible pathway for players to earn match entry fees, and monetizes non-paying players.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Wallet Page Discovery**:
   - Right beneath the Balance & Quick Actions header and above the Transaction History, users see the **"Watch Ads & Earn Cash"** card.
   - The card displays:
     - Dynamic gradient banner with glowing play icon and "Daily Video Rewards".
     - Segmented dot progress bar: 5 indicators representing the 5 daily ads (e.g., 🟢 🟢 ⚪ ⚪ ⚪ - "2/5 Watched Today").
     - Reward guarantee: *"Earn ₹1 – ₹5 random bonus per video • Resets daily at 12:00 AM"*.
     - Call-to-action button: **"Watch Ad (+₹1–₹5)"** with active ripple and touch feedback.
2. **Watching the Ad**:
   - Tapping the button shows a momentary loading/buffering state: *"Loading Unity Video Ad..."*.
   - Unity Ads opens full-screen. If test mode is active, the Unity test ad displays with timer and close button.
   - Upon completion callback (`onUnityAdsShowComplete` with `COMPLETED`), the app calculates a random reward between 1 and 5 (`(1..5).random()`).
3. **Reward Celebration Dialog**:
   - A modal celebration dialog pops up with celebratory Lottie or particle glow:
     - Headline: *"Reward Unlocked! 🎉"*
     - Prominent amount: *"+₹X Added to Wallet"*
     - Next steps: *"X ads remaining today • Resets tomorrow at midnight"*
     - Action button: *"Claim & Continue"*
4. **Daily Quota Reached (5/5)**:
   - When 5 ads have been watched for the day, the button transitions to a disabled badge state:
     - *"Daily Limit Reached (5/5)"*
     - Subtitle: *"Come back tomorrow after 12:00 AM for 5 more rewarded ads!"*
5. **Ledger & History Reflection**:
   - A new transaction is logged: `type = "REWARD_AD"` with badge `"AD REWARD 🎬"`, reference ID `AD-XXXXXX`, amount `+₹X`, and new updated balance.
   - An in-app push notification is generated informing the user of the credit.

#### Visual Styling
- Dark gaming aesthetic aligned with existing slate palette (`SlateCardBg` #1E293B, `SlateSurfaceDark` #0F172A).
- Gradient accent for the Ads Card: Violet-to-Amber linear gradient (`0xFF8B5CF6` to `0xFFF59E0B`) distinguishing promotional reward opportunities from standard deposit/withdrawal cards.
- High contrast, 48dp touch targets, and clear state indicators for loading, available, and completed states.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Daily Limit Tracking Architecture**:
  - *Chosen Approach*: Store `dailyAdsWatchedCount` and `dailyAdsWatchedDate` directly on the user's Firestore document (e.g., `YYYY-MM-DD`).
  - *Why*: Prevents client-side manipulation, guarantees cross-device sync (e.g. if the user reinstalls the app or switches devices), and enforces atomic server-side resets when a new calendar day arrives.
- **Decision 2: Transaction Logging**:
  - *Chosen Approach*: Record a dedicated `REWARD_AD` transaction type in the user's transaction history subcollection.
  - *Why*: Total transparency in the wallet ledger so players can audit where their balance came from, avoiding confusion with Paytm deposits or tournament prizes.
- **Decision 3: Resilience & Safety Simulation Fallback**:
  - *Chosen Approach*: `UnityAdsManager` will initialize Unity Ads SDK with real test configuration, and if the SDK is unavailable or blocked by network sandboxing in the streaming container, provide a graceful interactive simulation fallback that lets the user experience the full watch countdown and reward crediting.
  - *Why*: Ensures the application is 100% testable and operable in all cloud emulator environments without crashing or dead-ending.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                      WalletScreen                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │         "Watch Ads & Earn" UI Card               │  │
│  │   - 5-dot daily progress bar (2/5 watched)       │  │
│  │   - "Watch Ad (+₹1-₹5)" interactive CTA          │  │
│  │   - Resets daily at midnight badge               │  │
│  └────────────────────────┬─────────────────────────┘  │
└───────────────────────────┼────────────────────────────┘
                            │ On Click
                            ▼
┌────────────────────────────────────────────────────────┐
│                   UnityAdsManager                      │
│   - UnityAds.initialize(context, gameId, testMode)     │
│   - UnityAds.load("Rewarded_Android")                  │
│   - UnityAds.show(activity, "Rewarded_Android")        │
│   - Listener: onUnityAdsShowComplete(...)              │
└───────────────────────────┬────────────────────────────┘
                            │ onCompleted -> random(1..5)
                            ▼
┌────────────────────────────────────────────────────────┐
│                     AppViewModel                       │
│   - claimAdReward(rewardAmount: Int)                   │
│   - Optimistic local state update for 0ms latency      │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                 FirestoreRepository                    │
│   - Run atomic transaction on userDoc:                 │
│     - Verify today's date and count < 5                │
│     - Increment count and update last watch date       │
│     - Add reward to walletBalance                      │
│     - Write TransactionDto (type = "REWARD_AD")        │
│     - Trigger NotificationDto                          │
└────────────────────────────────────────────────────────┘
```

#### Data Entities & Schema Additions

1. **`UserDto` Additions**:
   ```kotlin
   val dailyAdsWatchedCount: Int = 0
   val dailyAdsWatchedDate: String = "" // e.g. "2026-10-23"
   ```
2. **`TransactionDto`**:
   - `type = "REWARD_AD"`
   - `description = "Watched Unity Video Ad Reward (+₹$amount)"`
   - `referenceId = "AD-${UUID.randomUUID().toString().take(8).uppercase()}"`
3. **`UnityAdsManager`**:
   - Singleton manager handling initialization, state machine (idle, loading, showing, completed, error), and reward dispatch callbacks.
