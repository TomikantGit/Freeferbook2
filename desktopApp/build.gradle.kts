import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    testImplementation("junit:junit:4.13.2")
}

compose.desktop {
    application {
        mainClass = "com.livrohub.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "Freeferbook"
            packageVersion = providers.gradleProperty("desktopPackageVersion")
                .orNull
                ?: "0.1.0"
            description = "Editor de manuscritos Freeferbook"
            vendor = "Freeferbook"
        }
    }
}
