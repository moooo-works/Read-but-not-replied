# Codex Git Sync Recovery Guide

## Problem

Codex runs in an isolated sandbox. If the sandbox initializes without a git remote
(`.git/config` has only `[core]`, no `[remote "origin"]`), its commits cannot be pushed
to GitHub — even when the ChatGPT Codex Connector GitHub App is installed with write access.

Symptoms in the Codex conversation:
- `git remote -v` returns nothing
- `make_pr` completes but no PR appears on GitHub
- `git push` fails with "could not read Username"

## Prevention

At the start of each Codex task, ask Codex to verify the remote before starting work:

```
Before you start, run: git remote -v
If origin is missing, run: git remote add origin https://github.com/moooo-works/Read-but-not-replied.git
```

## Recovery (if Codex commits are stranded in the sandbox)

If Codex has uncommitted or unpushed changes, ask it to run:

```bash
git diff HEAD~1..HEAD        # or git diff if uncommitted
```

Copy the output, then apply locally:

```bash
# Save the patch Codex gave you to a file, then:
git apply codex.patch

# Or apply manually from the diff output
```

If Codex already committed but cannot push:

```bash
# Inside Codex conversation, ask it to run:
git format-patch HEAD~1 --stdout
```

Paste the output and apply locally:

```bash
git am < codex.patch
git push -u origin <branch-name>
```

## Running Tests Without Robolectric

Robolectric-based tests are slow. To skip them during development or CI:

```bash
./gradlew testDebugUnitTest -PskipRobolectric
```

Tests excluded by `-PskipRobolectric`:
- `DaoTest`
- `NotificationRepositoryTest`
- `SettingsRepositoryTest`
- `BurstFilterTest`
- `NotificationListenerServiceImplTest`
- `ReminderWorkerTest`
- `NotificationProcessorTest`

To run the full suite including Robolectric:

```bash
./gradlew testDebugUnitTest
```
