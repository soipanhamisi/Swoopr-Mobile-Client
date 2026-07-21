# Implementation Plan - Network Connectivity Service

This plan outlines the creation of a utility service to check and monitor the internet connectivity (Wi-Fi and Mobile Data) of the Android client.

## User Review Required

> [!NOTE]
> I will implement this as a utility class `NetworkUtils` for simple checks and a `ConnectivityService` (singleton) that can provide real-time updates if needed. I will start with a utility class as it's the most common requirement for pre-request checks.

## Proposed Changes

### Permissions

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Admin/Documents/CarpoolClient/app/src/main/AndroidManifest.xml)
- Add `android.permission.ACCESS_NETWORK_STATE` permission.

### Utilities

#### [NEW] [NetworkUtils.java](file:///C:/Users/Admin/Documents/CarpoolClient/app/src/main/java/com/example/carpoolclient/utils/NetworkUtils.java)
- Implement `isNetworkAvailable(Context context)` using `ConnectivityManager`.
- Support modern API levels (using `NetworkCapabilities`).

### Application Logic (Optional Integration)

#### [MODIFY] [LandingPageActivity.java](file:///C:/Users/Admin/Documents/CarpoolClient/app/src/main/java/com/example/carpoolclient/LandingPageActivity.java)
- Add a check at the start of `onCreate` to ensure internet is available before proceeding with token checks or API calls.
- Show a Toast or Dialog if the user is offline.

## Verification Plan

### Manual Verification
1. **Offline Mode**:
   - Turn off Wi-Fi and Mobile Data.
   - Launch the app.
   - Verify that the app detects the offline state (e.g., shows a message).
2. **Wi-Fi Only**:
   - Turn on Wi-Fi, keep Mobile Data off.
   - Verify connectivity is detected.
3. **Mobile Data Only**:
   - Turn on Mobile Data, keep Wi-Fi off.
   - Verify connectivity is detected.
