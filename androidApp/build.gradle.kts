import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

kotlin {
    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    dependencies {
        implementation(projects.composeApp)

        implementation(platform(libs.androidx.compose.bom))
        debugImplementation(libs.androidx.compose.ui.tooling)
        debugImplementation(libs.androidx.compose.ui.tooling.preview)

        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.material3)
        //implementation(libs.ktor.client.android)

//        implementation(libs.material.icons.core)
//        implementation(libs.androidx.material.icons.core)
//        implementation(libs.androidx.material.icons.extended)
//
//        // Firebase
//        implementation(project.dependencies.platform(libs.firebase.bom))
//        implementation(libs.firebase.auth)
//        implementation(libs.firebase.firestore)
//
//        // Google Play Services
//        implementation(libs.play.services.location)
//        implementation(libs.play.services.maps)
//
//        // Maps Compose
//        implementation(libs.maps.compose)
//
//        // Accompanist (for permissions)
//        implementation(libs.accompanist.permissions)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.testExt.junit)
        androidTestImplementation(libs.androidx.espresso.core)
    }
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}


android {
    namespace = "com.maciel.androidapp"
    compileSdk {
        version = release(36)
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }


    defaultConfig {
        applicationId = "com.maciel.androidapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 4
        versionName = "1.0.3"


        // Load properties
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")


        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }


        // Get API key (with fallback for CI/CD)
        val mapsApiKey = properties.getProperty("MAPS_API_KEY", "")


        // Add to manifest placeholders
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey


        // Optional: Add to BuildConfig for access in code
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        //consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}
