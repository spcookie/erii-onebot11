plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    testImplementation(project(":kotlin:onebot-sdk"))
    testImplementation(project(":kotlin:onebot-lib"))
    testImplementation(project(":kotlin:onebot-mock"))
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
