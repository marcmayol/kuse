import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// Firma de release. Los secretos NUNCA se versionan: salen de keystore.properties
// (en la raíz, gitignored) o de las variables de entorno KUSE_STORE_FILE,
// KUSE_STORE_PASSWORD, KUSE_KEY_ALIAS y KUSE_KEY_PASSWORD. Si no hay ninguna de las
// dos fuentes se cae al debug.keystore, para que cualquiera pueda compilar el
// proyecto; ese APK no sirve para publicar (firma distinta = no actualiza encima).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}

fun datoFirma(clave: String, variableEntorno: String): String? =
    (keystoreProps[clave] as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(variableEntorno)?.takeIf { it.isNotBlank() }

val storeFileKuse = datoFirma("storeFile", "KUSE_STORE_FILE")
val usaKeystoreRelease = storeFileKuse != null

android {
    namespace = "com.marcm.cadencia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.marcm.cadencia"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "2.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            if (usaKeystoreRelease) {
                storeFile = file(storeFileKuse!!)
                storePassword = datoFirma("storePassword", "KUSE_STORE_PASSWORD")
                keyAlias = datoFirma("keyAlias", "KUSE_KEY_ALIAS")
                keyPassword = datoFirma("keyPassword", "KUSE_KEY_PASSWORD")
            } else {
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Mismo applicationId que release para no instalar como app aparte.
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
        // El actualizador compara contra BuildConfig.VERSION_CODE.
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// Room exporta el esquema a app/schemas: sirve de referencia y habilita tests de migración
// el día que la app deje de poder recrear la base desde cero.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":actualizador"))

    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Bloqueo de la app: BiometricPrompt exige una FragmentActivity, de ahí fragment-ktx.
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
