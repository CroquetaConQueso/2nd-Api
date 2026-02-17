plugins {
    id("com.android.application")
    // Activa los servicios de Google para Firebase (google-services.json).
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.trabajoapi"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.trabajoapi"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Define el runner de tests instrumentados.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Mantiene el release sin minificación para evitar problemas de entrega rápida.
            isMinifyEnabled = false
            // Aplica reglas ProGuard/R8 base + reglas del proyecto.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        // Compila el proyecto con Java 8 para compatibilidad con librerías y lambdas.
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Base UI.
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Google Maps (si se usa en alguna pantalla o dependencia indirecta).
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Cliente REST + parseo JSON.
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Tareas en segundo plano para recordatorios.
    implementation("androidx.work:work-runtime:2.9.0")

    // Mapa OSM (osmdroid) usado en el panel admin de ubicación/radio.
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // GPS / ubicación actual.
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // BOM para alinear versiones de Firebase.
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    // Telemetría básica del proyecto (recomendado por Firebase).
    implementation("com.google.firebase:firebase-analytics")
    // Notificaciones push (FCM).
    implementation("com.google.firebase:firebase-messaging")
}
