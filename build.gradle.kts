import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.graalvm.native) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

// Catalog accessors only resolve at the root script's top level, so capture the
// shared test dependencies here and reuse them inside the subprojects block.
val junitBom = libs.junit.bom
val junitJupiter = libs.junit.jupiter
val mockkLib = libs.mockk
val junitLauncher = libs.junit.platform.launcher

// Aggregate coverage from every module into a single root report.
dependencies {
    kover(project(":domain"))
    kover(project(":application"))
    kover(project(":adapter"))
    kover(project(":app"))
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("detekt.yml"))
    }

    dependencies {
        "testImplementation"(platform(junitBom))
        "testImplementation"(junitJupiter)
        "testImplementation"(mockkLib)
        "testRuntimeOnly"(junitLauncher)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
