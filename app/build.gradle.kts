plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.devtools.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "io.github.piyushdaiya.vaachak"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.piyushdaiya.vaachak"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            storeFile = file("vaachak-key.jks")
            storePassword = "#YqMPEY7KwFP7J" // The password you just created
            keyAlias = "vaachak_alias"
            keyPassword = "#YqMPEY7KwFP7J"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        // --- TURNED BACK ON FOR READIUM METADATA ---
        isCoreLibraryDesugaringEnabled = true
        // -------------------------------------------
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // ADD THIS BLOCK TO RENAME THE APK
    applicationVariants.all {
        val variantName = name.replaceFirstChar { it.uppercase() }
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "Vaachak-$variantName.apk"
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
// --- ADD THIS LINE ---
    coreLibraryDesugaring(libs.android.desugar)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    // --- ADD THIS LINE FOR AndroidViewBinding ---
    implementation("androidx.compose.ui:ui-viewbinding")

    // --- PRE-COMPILED READIUM BINARIES (From your GitHub Fork) ---
    // Format: com.github.[username].[repo-name]:[module-name]:[branch-name]
    implementation("org.readium.kotlin-toolkit:readium-shared:3.1.2")
    implementation("org.readium.kotlin-toolkit:readium-streamer:3.1.2")
    implementation("org.readium.kotlin-toolkit:readium-navigator:3.1.2")

    // Google Gemini AI SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")

    // Jetpack DataStore (For User Settings / API Keys)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coil (For displaying the Cloudflare AI generated images)
    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
}