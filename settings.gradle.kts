pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "erii-onebot11"

include("onebot-core")
include("onebot-lib")
include("onebot-sdk")
include("onebot-integration-test")
include("onebot-mock")
