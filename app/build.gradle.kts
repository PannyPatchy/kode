// Composition Root + distributable binary. Wires every layer together and builds
// the single native `kode` binary via GraalVM Native Image.
plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.graalvm.native)
    application
}

dependencies {
    implementation(project(":adapter"))
    implementation(project(":application"))
    implementation(project(":domain"))

    implementation(libs.clikt)
    implementation(libs.kotlinx.serialization.json)
}

application {
    applicationName = "kode"
    mainClass.set("dev.kode.bootstrap.MainKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("kode")
            mainClass.set("dev.kode.bootstrap.MainKt")
            buildArgs.add("--no-fallback")
            buildArgs.add("-O2")
        }
    }
}
