plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    application
}

dependencies {
    implementation(project(":kotlin:onebot-sdk"))
    implementation(project(":kotlin:onebot-lib"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    testImplementation(project(":kotlin:onebot-mock"))
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass.set("uesugi.onebot.app.MainKt")
}
