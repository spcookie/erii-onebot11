plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    application
}

dependencies {
    implementation(project(":kotlin:onebot-sdk"))
    implementation(project(":kotlin:lib-onebot"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)
}

application {
    mainClass.set("uesugi.onebot.app.MainKt")
}
