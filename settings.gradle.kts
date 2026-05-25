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
include(":kotlin:lib-onebot")
include(":kotlin:onebot-sdk")
include(":kotlin:onebot-app")
include(":kotlin:mock-bot-server")
