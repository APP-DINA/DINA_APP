import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/** dina_backend publicado en AWS EC2 (Elastic IP de la instancia DINA_BACKEND). */
val URL_BACKEND_AWS = "http://34.201.191.7:8080/"

android {
    namespace = "cl.app.dina"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cl.app.dina"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Backend local (dina_backend en tu PC). Antes de abrir la app, en una terminal:
            //     adb reverse tcp:8080 tcp:8080
            // Así 127.0.0.1:8080 del emulador/teléfono USB llega a tu PC. No se usa 10.0.2.2 porque
            // Android 17 bloquea la red local (10.x, 192.168.x) sin el permiso ACCESS_LOCAL_NETWORK.
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:8080/\"")
            // El backend local no usa HTTPS: se permite HTTP solo en debug
            manifestPlaceholders["permitirHttp"] = "true"
        }
        // Igual que debug, pero contra el backend publicado en AWS EC2 (Elastic IP). Se instala aparte
        // ("DINA", id cl.app.dina.aws), así convive en el teléfono con la de pruebas ("DINA P").
        // Es la variante por defecto al abrir el proyecto (Build > Select Build Variant para cambiarla).
        create("aws") {
            initWith(getByName("debug"))
            isDefault = true
            applicationIdSuffix = ".aws"
            versionNameSuffix = "-aws"
            buildConfigField("String", "API_BASE_URL", "\"$URL_BACKEND_AWS\"")
            // EC2 todavía no tiene HTTPS
            manifestPlaceholders["permitirHttp"] = "true"
            // Las librerías sin variante "aws" usan la de debug
            matchingFallbacks += listOf("debug")
        }
        release {
            optimization {
                enable = false
            }
            // Backend publicado en AWS EC2 (sin HTTPS por ahora, por eso se permite HTTP)
            buildConfigField("String", "API_BASE_URL", "\"$URL_BACKEND_AWS\"")
            manifestPlaceholders["permitirHttp"] = "true"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Permite usar java.time (LocalDate) en Android 7 (minSdk 24)
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    // Material Icons
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // MVVM + navegación
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Room (SQLite): room-compiler va con ksp(), nunca con implementation()
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (Preferences)
    implementation(libs.androidx.datastore.preferences)

    // Consumo API REST
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Imágenes desde URL
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
// ── Backend local: adb reverse automático ────────────────────────────────────
// En debug la app llama a http://127.0.0.1:8080/ (ver API_BASE_URL). Esta tarea redirige ese
// puerto de cada emulador/teléfono conectado al 8080 de tu PC, donde corre dina_backend.
// Se ejecuta sola al compilar o instalar la versión debug (también con el botón Run).
// Sin dispositivos conectados no hace nada y no falla el build.
abstract class AdbReverseTask : DefaultTask() {
    @get:InputFile
    abstract val adb: RegularFileProperty

    @get:Input
    abstract val puerto: Property<Int>

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun redirigir() {
        val adbPath = adb.get().asFile.absolutePath
        val salida = ByteArrayOutputStream()
        val lista = exec.exec {
            commandLine(adbPath, "devices")
            standardOutput = salida
            isIgnoreExitValue = true
        }
        if (lista.exitValue != 0) {
            logger.warn("adb reverse: no se pudo ejecutar adb")
            return
        }
        val dispositivos = salida.toString().lines()
            .drop(1)
            .map { it.trim().split(Regex("""\s+""")) }
            .filter { it.size >= 2 && it[1] == "device" }
            .map { it[0] }
        if (dispositivos.isEmpty()) {
            logger.lifecycle("adb reverse: no hay dispositivos conectados, se omite")
            return
        }
        val regla = "tcp:${puerto.get()}"
        dispositivos.forEach { serial ->
            val r = exec.exec {
                commandLine(adbPath, "-s", serial, "reverse", regla, regla)
                isIgnoreExitValue = true
            }
            if (r.exitValue == 0) logger.lifecycle("adb reverse: $serial $regla → PC $regla")
            else logger.warn("adb reverse: falló en $serial")
        }
    }
}

val adbReverseDebug = tasks.register<AdbReverseTask>("adbReverseDebug") {
    group = "dina"
    description = "Redirige el puerto 8080 de los dispositivos conectados al backend local"
    adb.set(androidComponents.sdkComponents.adb)
    puerto.set(8080)
    // Siempre se ejecuta: el emulador pudo reiniciarse desde el último build
    outputs.upToDateWhen { false }
}

tasks.configureEach {
    if (name == "assembleDebug" || name == "installDebug") finalizedBy(adbReverseDebug)
}
