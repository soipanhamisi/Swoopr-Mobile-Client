# Walkthrough - Enhanced Auth Flow and Network Connectivity

This update improves the app's startup experience by adding network detection and optimizing the authentication sequence.

## Changes Made

### 1. Network Connectivity Detection
- **[NEW] [NetworkUtils.java](file:///C:/Users/Admin/Documents/CarpoolClient/app/src/main/java/com/example/carpoolclient/utils/NetworkUtils.java)**: A utility to check if the device is connected to Wi-Fi or Mobile Data.
- **[MODIFY] [AndroidManifest.xml](file:///C:/Users/Admin/Documents/CarpoolClient/app/src/main/AndroidManifest.xml)**: Added `ACCESS_NETWORK_STATE` permission.
- **[MODIFY] [LandingPageActivity.java](file:///C:/Users/Admin/Documents/CarpoolClient/app/src/main/java/com/example/carpoolclient/LandingPageActivity.java)**: Now checks for internet connectivity immediately upon launch. If offline, a toast message is displayed and auto-login is skipped.

### 2. Optimized Authentication Flow
- **Auto-Login with Validity Check**: The `LandingPageActivity` now validates existing JWT tokens against `/auth/testEndpoint`. If the token is expired but refreshable, the `WebClient` handles the refresh transparently.
- **FCM Token Submission**: FCM tokens are now submitted to the server immediately after they are generated.
- **Conditional Redirection**: The registration flow now correctly handles returning users vs. new users based on the action taken on the landing page.

### 3. WebClient Enhancements
- Added support for `401 Unauthorized` in the automatic token refresh mechanism to ensure better compatibility with server responses.

## Verification

### Automated Tests
- Verified file structure and imports.

### Manual Verification Required
- [ ] **Connectivity Test**: Disable Wi-Fi and Mobile Data, launch the app. Verify the "No internet connection" toast appears.
- [ ] **Auto-Login Test**: Log in, close the app, and reopen it. Verify it goes directly to the Main Map (if online).
- [ ] **FCM Test**: Verify FCM token submission logs in Logcat.
