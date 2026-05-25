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

    testImplementation(project(":kotlin:mock-bot-server"))
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass.set("uesugi.onebot.app.MainKt")
}
