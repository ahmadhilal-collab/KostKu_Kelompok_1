plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    // Tahap H: Firebase. WAJIB ada file app/google-services.json (unduh dari Firebase Console).
    id("com.google.gms.google-services")
}

android {
    namespace = "com.kostku.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kostku.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Alamat KostKu REST API (wajib diakhiri garis miring "/").
        buildConfigField("String", "API_BASE_URL", "\"https://kostku.sovereigns.my.id/api/v1/\"")
        // Nomor WhatsApp pengelola untuk tombol "Hubungi pengelola" di halaman login penghuni.
        buildConfigField("String", "OWNER_WHATSAPP", "\"081234567890\"")
        // Sandi rahasia pendaftaran admin kini diperiksa di server (ADMIN_SECRET_CODE di config/.env),
        // jadi tidak lagi disimpan di dalam aplikasi.
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room (salinan data dari server di HP). Sesi login disimpan di SharedPreferences (bawaan Android).
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Notifikasi push (Firebase Cloud Messaging)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Koneksi ke KostKu REST API
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Memuat foto kamar / fasilitas (dipakai mulai Milestone 2)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
