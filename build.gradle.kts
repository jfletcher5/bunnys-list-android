plugins {
    id("com.android.application") version "8.13.2" apply false
    // 2.3.21 is the newest Kotlin that can read Firebase Auth 24.x metadata
    // and still has a KSP release. Auth 25 (Firebase BOM 35) needs Kotlin 2.4,
    // and there is no KSP build for that yet.
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
    id("com.google.dagger.hilt.android") version "2.58" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
