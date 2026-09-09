import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Azure APIM subscription key. Resolved from local.properties (developer machines,
// gitignored) first, then the environment (CI). Never hardcode it in source.
// Takes Project explicitly: top-level functions in a Kotlin DSL script cannot reach
// the script's implicit Project receiver.
fun resolveSubscriptionKey(project: Project): String {
    val props = Properties()
    val propsFile = project.rootProject.file("local.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use { props.load(it) }
    }
    val key = props.getProperty("twg.subscriptionKey") ?: System.getenv("TWG_SUBSCRIPTION_KEY")
    if (key.isNullOrBlank()) {
        project.logger.warn(
            "WARNING: twg.subscriptionKey is not set in local.properties and " +
                "TWG_SUBSCRIPTION_KEY is not in the environment. API calls will fail with 401."
        )
        return ""
    }
    return key.trim()
}

android {
    namespace = "nz.co.warehouseandroidtest"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "nz.co.warehouseandroidtest"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUBSCRIPTION_KEY", "\"${resolveSubscriptionKey(project)}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })

    implementation(libs.support.appcompat)
    implementation(libs.support.constraint.layout)
    implementation(libs.support.design)

    // retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.adapter.rxjava2)

    // okhttp
    implementation(libs.okhttp)
    implementation(libs.okio)
    implementation(libs.okhttp.logging.interceptor)

    // zxing
    implementation(libs.zxing.library)

    // gson
    implementation(libs.gson)

    // glide
    implementation(libs.glide)
    implementation(libs.glide.transformations)

    testImplementation(libs.junit)
    androidTestImplementation(libs.support.test.runner)
    androidTestImplementation(libs.espresso.core)
}
