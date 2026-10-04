import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val yourlsSignature: String = localProps.getProperty("YOURLS_SIGNATURE", "")
val yourlsEndpoint: String = localProps.getProperty("YOURLS_ENDPOINT", "https://10101110.xyz/yourls-api.php")

android {
    namespace = "dev.nullcode.shrink"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.nullcode.shrink"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "YOURLS_ENDPOINT", "\"$yourlsEndpoint\"")
        buildConfigField("String", "YOURLS_SIGNATURE", "\"$yourlsSignature\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // sideload-friendly: signed with the debug key so `assembleRelease` installs directly
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        disable += listOf(
            "NullSafeMutableLiveData",
            "RememberInComposition",
            "FrequentlyChangingValue",
            "AutoboxingStateCreation"
        )
        abortOnError = false
        checkReleaseBuilds = false
    }
}

configurations.all {
    exclude(group = "androidx.compose.runtime", module = "runtime-lint")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3:1.4.0-alpha08")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
