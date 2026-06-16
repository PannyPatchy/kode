// Interface adapters (driving: Clikt CLI; driven: config repository, file system,
// presenter). May depend on application + domain and the respective infrastructure.
plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))

    implementation(libs.clikt)
    implementation(libs.kotlinx.serialization.json)
}
