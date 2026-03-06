# Bunny's List Android

Native Android implementation of the existing Bunny's List v3 grocery web app.

## Current Scope

This repo now contains the first Android MVP scaffold for the current v3 product surface:

- one primary home screen
- real-time Firestore subscriptions for `items` and `categories`
- search plus `All` / `Needed` filtering
- add, edit, delete, and undo item flows
- inline category creation
- offline banner

The Android implementation intentionally mirrors the current web app at `/Users/jonfletcher/Desktop/Personal_Projects/bunnys-list-v3` before expanding the product beyond parity.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Firebase Firestore

## Local Setup

1. Install a JDK and Android SDK.
2. Copy `local.properties.example` to `local.properties`.
3. Set `sdk.dir` to your Android SDK path.
4. Add an Android `google-services.json` file at `app/google-services.json`.

## Firebase Notes

- The web app uses the same Firestore collections directly: `items` and `categories`.
- This Android project now uses the standard Android Google Services setup.
- The package name in `google-services.json` must match the app's package: `com.bunnyslist.android`.
- The currently checked-in source expects a replacement `google-services.json`, because the file presently in the repo is registered for different package names.

## Current Limitation

The project now has a Gradle wrapper and Android Studio metadata, but I still cannot complete a successful build until `app/google-services.json` is replaced with one registered for `com.bunnyslist.android`.
