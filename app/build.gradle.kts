plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

val testKeystorePath = providers.environmentVariable("TEST_KEYSTORE_PATH").orNull
val testKeystorePassword = providers.environmentVariable("TEST_KEYSTORE_PASSWORD").orNull
val testKeyAlias = providers.environmentVariable("TEST_KEY_ALIAS").orNull
val testKeyPassword = providers.environmentVariable("TEST_KEY_PASSWORD").orNull
val publicTestBuild = providers.gradleProperty("publicTestBuild")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false
val testSigningConfigured = listOf(
    testKeystorePath,
    testKeystorePassword,
    testKeyAlias,
    testKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.livrohub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.livrohub"
        minSdk = 24
        targetSdk = 35
        versionCode = providers.gradleProperty("testVersionCode")
            .orNull
            ?.toIntOrNull()
            ?: 1
        versionName = providers.gradleProperty("testVersionName")
            .orNull
            ?: "0.1.0"
    }

    signingConfigs {
        if (testSigningConfigured) {
            create("test") {
                storeFile = file(requireNotNull(testKeystorePath))
                storePassword = requireNotNull(testKeystorePassword)
                keyAlias = requireNotNull(testKeyAlias)
                keyPassword = requireNotNull(testKeyPassword)
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (publicTestBuild) {
                applicationIdSuffix = ".test"
                signingConfig = null
                resValue("string", "app_name", "Freeferbook")
            } else {
                signingConfigs.findByName("test")?.let { signingConfig = it }
            }
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.java.diff.utils)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)

    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
