pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JCenter is shut down. zxing-library:2.2 and glide-transformations:2.0.1
        // were only ever published there and survive solely as JCenter mirror copies.
        // Removable once the barcode scanner is ported off zxing-library.
        maven("https://maven.aliyun.com/repository/public")
    }
}

rootProject.name = "twg-android-test"

include(":app")
