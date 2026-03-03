# NotiReply Assistant (MVP)

An Android notification organizer and reply assistant. This is the MVP version (Steps 1-8).

## Features
- **Notification Inbox**: Captures notifications (using `NotificationListenerService`), groups them into conversations, and displays an active/archived list.
- **Quick Reply**: Allows you to reply directly from the app using `RemoteInput`.
- **Reminders**: Schedule reminders (10min, 1hr) to handle a conversation later.
- **Privacy-First**: All data is stored locally using Room.

## Architecture
- **Tech Stack**: Kotlin, Jetpack Compose (Material 3), Room, DataStore, WorkManager, Hilt, Coroutines.
- **Data Flow**: Unidirectional Data Flow (UDF) with `StateFlow`.
- **Ingestion**: Two-stage parsing with Burst Suppression.

## Important Limitations & Privacy Notes
1. **Not a "Ghost Read" Tool**: This app uses the official Notification APIs. Reading a message here does not send a "read receipt" to the original app, but we do not guarantee "ghost reading" if you interact with the notification. Terminology uses "Handled" instead of "Read".
2. **Ephemeral Quick Reply**: `RemoteInput` (Quick Reply) relies on the original system notification. It is stored in memory and is only available as long as the original notification has not been dismissed by the system.
3. **Local Storage Only**: No data is uploaded. You can clear all data at any time via Settings.
4. **Privacy Logs**: Message contents are not logged to logcat. Reminder notifications do not leak the full message text.

## How to Test
1. Run Unit Tests:
   `./gradlew :app:testDebugUnitTest`
2. Build Debug APK:
   `./gradlew :app:assembleDebug`

## QA Checklist (For Real Device Testing)
- [ ] Grant Notification Access permission in Android Settings.
- [ ] Receive a message (e.g., from WhatsApp/Telegram) and verify it appears in "Active".
- [ ] Swipe/Click to "Mark Handled" and verify the badge disappears.
- [ ] Swipe to "Archive" and verify it moves to the Archived tab.
- [ ] Open a conversation and type a Quick Reply. Verify the original app sends it.
- [ ] Set a 10min reminder, verify a local notification fires in 10 minutes.
- [ ] Go to Settings and "Clear All Data", verify inbox is empty.
