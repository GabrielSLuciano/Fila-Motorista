import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

// Ler a chave de API do Google Maps a partir do secrets.properties
val secretsPropertiesFile = rootProject.file("secrets.properties")
val secretsProperties = Properties()

if (secretsPropertiesFile.exists()) {
    try {
        secretsProperties.load(FileInputStream(secretsPropertiesFile))
    } catch (e: Exception) {
        println("Não foi possível carregar o arquivo secrets.properties: ${e.message}")
    }
}

val googleMapsApiKey: String? = secretsProperties.getProperty("GOOGLE_MAPS_API_KEY")

android {
    namespace = "com.gabsistemas.filamotorista"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gabsistemas.filamotorista"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Adicionar a chave de API ao manifesto
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = googleMapsApiKey ?: ""
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Dependências principais
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.location)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore.ktx)
    implementation (libs.firebase.database)
    debugImplementation(libs.androidx.ui.tooling)

    // Dependências de teste
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.material.icons.extended)

}
