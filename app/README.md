# Swoopr Android Client

This repo is the source code for the android client for the campus carpooling application and an extension of the server logic, [here](https://github.com/soipanhamisi/swooprServer).
The app connects users with verified peers traveling along similar routes, allowing them to create or join carpools, coordinate pickup and drop-off points, and share the journey with confidence.
## Features
- Secure signup and email/OTP verification
- Map-based origin and destination selection
- Create and join carpools
- Trip matching and status tracking
- In-app chat for trip coordination
- Firebase-powered notifications

## Getting started
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Run the app on an emulator or Android device

## App look and feel

<p align="center">
  <img src="../readmeGifs/ezgif.com-video-to-gif-converter.gif" width="180" alt="Swoopr onboarding flow" />
  <img src="../readmeGifs/ezgif.com-video-to-gif-converter (1).gif" width="180" alt="Swoopr map and route flow" />
  <img src="../readmeGifs/ezgif.com-video-to-gif-converter (2).gif" width="180" alt="Swoopr carpool interaction" />
  <img src="../readmeGifs/ezgif.com-video-to-gif-converter (3).gif" width="180" alt="Swoopr trip details" />
  <img src="../readmeGifs/ezgif.com-video-to-gif-converter (4).gif" width="180" alt="Swoopr route and matching" />
  <img src="../readmeGifs/ezgif.com-video-to-gif-converter (5).gif" width="180" alt="Swoopr app experience" />
</p>


## Tech stack
- Android (Kotlin/Java)
- Google Maps / Places
- Firebase Messaging
- OkHttp
- Gson
- Secure token storage

## Project structure
- `app/` — Android application source
- `readmeGifs/` — app screenshots and motion previews