# Walkthrough - Resend OTP Functionality

I have implemented the "Resend OTP" functionality in `OtpVerificationActivity.java`. This allows users to request a new OTP code if they didn't receive the first one.

## Changes Made

### CarpoolClient App

#### [OtpVerificationActivity.java](file:///C:/Users/Admin/Documents/CarpoolClient/app/src/main/java/com/example/carpoolclient/OtpVerificationActivity.java)
- **Added Click Listener**: Set up `tvResendOtp` to trigger the resend logic when clicked.
- **Implemented `resendOtp` method**:
    - Shows a loading dialog.
    - Sends a POST request to `auth/getOtp` with the user's email.
    - Provides feedback via a Toast message (success or error).
    - Dismisses the loading dialog once the response is received.
- **Improved Context Handling**: Captured the email from the intent and ensured it's used for the resend request.

## Verification Results

### Automated Tests
- Successfully ran `:app:assembleDebug` to ensure no compilation errors.

### Manual Verification
1.  Navigate to the OTP Verification screen.
2.  Click on the "Resend OTP" text.
3.  Observe the loading dialog appearing.
4.  Confirm that a Toast message appears with the result from the server.
