# Security Summary

**Reviewed Files**: 10  
**Issues Resolved**: 6  

### Issues Resolved
| Area | File | Line | What Was Happening | Impact | Status | How It Was Fixed |
|---|---|---|---|---|---|---|
| Unilateral PIN Reset via Edit / Cloud Error / NotFound | `app/src/main/java/com/example/data/repository/SmartOfficerAuthRepository.kt` | 436–631 | Editing an existing officer's profile with an empty PIN or during a transient Cloud `Error` / `NotFound` could synthesize and overwrite `officer_credentials` with a 6-digit NIP default hash and `isCustomPinSet = false`. | High | Fixed | Enforced fail-closed handling on Cloud `Error`, rejected default/6-digit-NIP PINs on existing edits, used metadata-only `.update(...)` for blank-PIN edits, migrated existing hashes on NIP corrections without resetting, and never fabricated default PINs on `NotFound` edits. |
| Post-Login Credential Overwrite Race & Flag Downgrade | `app/src/main/java/com/example/data/repository/SmartOfficerAuthRepository.kt` & `app/src/main/java/com/example/data/local/FirestoreHelper.kt` | 1222–1252, 1773–1856 | Login and biometric unlock re-uploaded full credential objects via `SetOptions.merge()`, risking race overwrites if a PIN changed concurrently, and `needsHashActivation` could trigger even when `isCustomPinSet` was `true`. | High | Fixed | Replaced post-login/biometric credential merges with partial `updateCredentialLoginMetadata` (touching only `role`, `lastLoginAt`, `lastLoginDevice`, `updatedAt`, and backfilling `isPinChanged`), and tightened `needsHashActivation` to require empty `pinHash`, empty `salt`, `!isCustomPinSet`, and no server plaintext PIN. |
| Firestore Rules Anti-Downgrade Bypass | `firestore.rules` | 444–507 | Standalone `ownerUid` branches in `allow update` on `/officer_credentials/{nip}` short-circuited before the `isCustomPinSet` anti-downgrade check, allowing an authenticated owner or `officer_nip_{nip}` client to revert `isCustomPinSet`/`isPinChanged` to `false` or overwrite an already-hashed record. | High | Fixed | Enforced unconditional global anti-downgrade invariants for all callers before evaluating Path A (non-secret metadata/login `.update`), Path B (empty `pinHash`/`salt` first-time activation only), or Path C (personal custom PIN upgrade with `isCustomPinSet == true && isPinChanged == true`). |
| Unilateral Reset-to-Default APIs & UI Triggers | `app/src/main/java/com/example/data/repository/SmartOfficerAuthRepository.kt`, `app/src/main/java/com/example/ui/components/OfficerDashboardConsoleView.kt`, `app/src/main/java/com/example/ui/components/DatabaseExplorerScreen.kt` | 1824–1850, 2257–2585, 4130–4286 | `normalizeAccountToBrandNewInitialState` and `resetToInitialNipSecret` (plus UI buttons/tabs in Change PIN and Restore Session dialogs) reverted custom Cloud PINs back to predictable 6-digit NIP defaults and wiped audit trails. | High | Fixed | Permanently disabled `normalizeAccountToBrandNewInitialState` and `resetToInitialNipSecret` to return `SecurityException` with zero DB/Cloud mutations, removed the UI Reset PIN tab and restore-plus-reset button, and blocked default/6-digit-NIP PIN inputs in edit dialogs. |
| Plaintext PIN Exposure in Security Logs | `app/src/main/java/com/example/security/AdminAccessSecurityManager.kt` | 339, 346, 555 | Diagnostic debug log entries recorded raw user input PINs and Firestore `masterPin` strings in plaintext. | Medium | Fixed | Replaced logged secret strings with redacted markers showing only length/presence status. |
| Citizen Mode Officer Sync Bypass | `app/src/main/java/com/example/security/SecurityManager.kt` | 2235–2241 | `isOfficerDataSyncPermitted()` checked `OfficerAuthManager.isOfficerLoggedIn() || hasAnyActiveLocalVerifiedSession()`, permitting officer sync if stale local verification flags remained on device in Citizen Mode. | Medium | Fixed | Restricted `isOfficerDataSyncPermitted()` strictly to an active `OfficerAuthManager.isOfficerLoggedIn()` session. |

### Safeguards Verified
| Area | File | How It's Protected |
|---|---|---|
| Personal PIN Storage | `app/src/main/java/com/example/data/repository/SmartOfficerAuthRepository.kt` | Raw PINs, PBKDF2 hashes, and salts are stripped via `sanitizeForLocalMetadataCache` before Room caching and are never stored in `SharedPreferences`. |
| Personal PIN Rotation | `app/src/main/java/com/example/data/repository/SmartOfficerAuthRepository.kt` | `changePersonalPin` verifies the current PIN strictly against Cloud Firestore (`Source.SERVER`), blocks `FORBIDDEN_DEFAULT_PINS` and 6-digit NIP defaults, and persists salted PBKDF2 hashes with `isCustomPinSet = true`. |
| Credential Deletion Protection | `firestore.rules` | `allow delete: if false;` on `/officer_credentials/{nip}` and `allow list: if isSignedIn();` prevent credential deletion and unauthenticated NIP enumeration. |
| Two-Tier Module Isolation | `app/src/main/java/com/example/ui/components/OfficerDashboardConsoleView.kt` & `app/src/main/java/com/example/security/SecurityManager.kt` | Officer dashboard dialogs, session listeners, and sensitive activity syncs remain gated behind an active verified officer session (`isOfficerLoggedIn`). |

---

## PoC Verification

### 1. Unilateral PIN Reversion During Profile Edit or Transient Cloud Error
#### Summary
| Field | Value |
|---|---|
| Issue Type | Credential Integrity Downgrade / Silent Default PIN Overwrite |
| Impact | High |
| Affected File | `SmartOfficerAuthRepository.kt:436-631`, `FirestoreHelper.kt:1821-1856` |
| Test Scenario | Call `registerOrUpdateManualOfficer` to edit an existing officer (`NIP: 198605122015031004`, custom PIN `742915`) while Cloud lookup returns `Error`, returns `NotFound`, returns `Found` with blank `customInitialPin`, or submits `031004` (6-digit NIP). |

#### How It Was Fixed
Editing an existing officer now fails closed when Cloud verification errors occur, rejects predictable 6-digit NIP or default PINs, uses partial profile metadata `.update(...)` when no new PIN is provided, and refuses to fabricate default hashes on `NotFound`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | `registerOrUpdateManualOfficer` enters with `isEditingExisting = true` and `candidateCustomPin = "031004"` or `null` | Input enters validation & server lookup |
| 2 | Guard checks `isValidFirstTimeActivationPin` / `FORBIDDEN_DEFAULT_PINS` (`SmartOfficerAuthRepository.kt:281-294`) and `CloudCredentialLookupResult.Error` (`:437-444`) | Rejected (`SecurityException` / `IllegalStateException`) before any write |
| 3 | For `Found` + `candidateCustomPin == null`, invokes `FirestoreHelper.updateCredentialProfileMetadataSuspend` (`:470`) updating only profile metadata fields | **Handled Safely** — `pinHash`, `salt`, and `isCustomPinSet` remain untouched |

---

### 2. Firestore Rules Anti-Downgrade Bypass on `officer_credentials`
#### Summary
| Field | Value |
|---|---|
| Issue Type | Authorization & Schema Rule Short-Circuit Bypass |
| Impact | High |
| Affected File | `firestore.rules:444-507` |
| Test Scenario | Authenticated owner (`alice`) or self-service device (`ownerUid: officer_nip_199807202025061002`) issues `.update(...)` / `.set(..., {merge: true})` setting `isCustomPinSet: false` and `pinHash` to a replacement hash, or unauthenticated caller overwrites an already-hashed `isCustomPinSet: false` record. |

#### How It Was Fixed
Removed standalone `ownerUid` short-circuit clauses in `allow update` and enforced unconditional global anti-downgrade invariants across all callers, restricting updates strictly to Path A (metadata-only), Path B (empty `pinHash`/`salt` activation only), or Path C (custom PIN upgrade with `isCustomPinSet == true && isPinChanged == true`).

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Update request targets `/officer_credentials/{nip}` | Enters `allow update` at `firestore.rules:444` |
| 2 | Unconditional invariants at `firestore.rules:458-467` verify `isCustomPinSet` and `isPinChanged` cannot transition from `true` to `false` | Downgrade rejected immediately (`PERMISSION_DENIED`) |
| 3 | Mutation paths at `firestore.rules:469-504` verify non-empty hashes cannot be replaced while staying `isCustomPinSet == false` | **Handled Safely** — Verified via `firestore.test.js` against Firestore Emulator |

---

### 3. Concurrent Post-Login Sync & Unilateral Reset APIs
#### Summary
| Field | Value |
|---|---|
| Issue Type | Race Overwrite & Destructive Reset API Exposure |
| Impact | High |
| Affected File | `SmartOfficerAuthRepository.kt:1224-1228,1824-1850`, `FirestoreHelper.kt:1773-1814`, `OfficerDashboardConsoleView.kt:2257-2620,4130-4275` |
| Test Scenario | Direct calls to `resetToInitialNipSecret(nip)`, `normalizeAccountToBrandNewInitialState(nip, true)`, `OfficerAuthManager.resetPinToDefault`, or login metadata sync running concurrently with a custom PIN change. |

#### How It Was Fixed
Permanently disabled both repository reset methods (`SecurityException`), removed all UI Reset PIN controls, and switched post-login/biometric updates to partial `.update(...)` metadata writes that never include `pinHash`, `salt`, or `isCustomPinSet`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Caller invokes `resetToInitialNipSecret` or `normalizeAccountToBrandNewInitialState` | Immediately hits disabled guard (`SmartOfficerAuthRepository.kt:1824-1850`) and fails with `SecurityException` |
| 2 | Successful login/biometric unlock calls `FirestoreHelper.updateCredentialLoginMetadata` (`:1773`) | Partial `.update(...)` writes only `role`, `lastLoginAt`, `lastLoginDevice`, `updatedAt` (and `isPinChanged = true` if custom) |
| 3 | Officer custom PIN hash, salt, and local audit logs remain unaltered | **Handled Safely** |

---

### 4. Plaintext PIN Logging & Citizen Mode Sync Isolation
#### Summary
| Field | Value |
|---|---|
| Issue Type | Sensitive Data Logging & Stale Local Session Sync Bypass |
| Impact | Medium |
| Affected File | `AdminAccessSecurityManager.kt:339-346,555`, `SecurityManager.kt:2235-2241` |
| Test Scenario | Admin PIN verification attempt with `WRONG_USER_PIN_5521` / `SECRET_MASTER_7492`, and Citizen Mode check when `bpbd_security_manager_prefs` contains stale `local_verified_<nip> = true` while `isOfficerLoggedIn() == false`. |

#### How It Was Fixed
Redacted both input PINs and `masterPin` strings in `AdminAccessSecurityManager` logs, and removed the `|| hasAnyActiveLocalVerifiedSession()` fallback from `SecurityManager.isOfficerDataSyncPermitted()`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | `verifyMasterPasswordAndIssueToken` logs verification diagnostics (`AdminAccessSecurityManager.kt:339,346`) | Emits `[REDACTED]` instead of secret values |
| 2 | Citizen Mode triggers `SecurityManager.isOfficerDataSyncPermitted()` (`SecurityManager.kt:2235-2241`) | Checks `OfficerAuthManager(context).isOfficerLoggedIn()` -> returns `false` |
| 3 | No secret PIN leaks to logs and no internal officer sync runs in Citizen Mode | **Handled Safely** |
