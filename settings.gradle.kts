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

include(":kotlin:onebot-core")
include(":kotlin:onebot-lib")
include(":kotlin:onebot-sdk")
include(":kotlin:onebot-app")
include(":kotlin:onebot-mock")
