// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Repositories live in settings.gradle.kts under dependencyResolutionManagement.

plugins {
    alias(libs.plugins.android.application) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
