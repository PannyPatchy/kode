rootProject.name = "kode"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

include(":domain")
include(":application")
include(":adapter")
include(":app")
