plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
val maxAppId = providers.gradleProperty("max.appId").orElse("").get()
require(maxAppId.matches(Regex("[A-Za-z0-9_.-]*"))) { "max.appId contains unsupported characters" }
android {
    namespace = "io.github.lubara7.tgmax"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.lubara7.tgmax.authharness"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        resourceConfigurations += listOf("en", "ru", "es", "uz", "pt", "pt-rBR", "fr", "be")
        buildConfigField("String", "MAX_APP_ID", "\"$maxAppId\"")
    }
    flavorDimensions += "backend"
    productFlavors {
        create("demo") {
            dimension = "backend"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            buildConfigField("boolean", "IS_DEMO", "true")
        }
        create("production") {
            dimension = "backend"
            buildConfigField("boolean", "IS_DEMO", "false")
        }
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:localization"))
    implementation(project(":feature:auth"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
}
