# Implementation Plan - Resend OTP Functionality

I will implement the "Resend OTP" functionality in `OtpVerificationActivity.java`. This will allow users to request a new OTP if they haven't received one.

## User Review Required

> [!NOTE]
> The implementation will use the same endpoint (`auth/getOtp`) as the initial request. It will show a loading dialog during the process and a Toast message indicating success or failure.

## Proposed Changes

### CarpoolClient App

#### [MODIFY] [OtpVerificationActivity.java](file:///C:/Users/Admin/Documents/CarpoolClient/app/src/main/java/com/example/carpoolclient/OtpVerificationActivity.java)
- Add missing imports: `android.widget.Toast` and `com.example.carpoolclient.dtos.EmailDto`.
- In `onCreate`, set an `OnClickListener` for `tvResendOtp`.
- Implement `resendOtp(String email)` method:
    - Show `loadingDialog`.
    - Call `webClient.post("auth/getOtp", ...)` with the user's email.
    - Dismiss `loadingDialog` on response.
    - Show a `Toast` with the message returned from the server.

## Verification Plan

### Automated Tests
- Run `gradle build` to ensure the project compiles with the new changes.

### Manual Verification
- Deploy the app to a device or emulator.
- Navigate to the OTP verification screen.
- Click "Resend OTP".
- Verify that the loading dialog appears.
- Verify that a success message (Toast) appears after the request completes.
- Verify that an error message (Toast) appears if the request fails (e.g., no internet).
