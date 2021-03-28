import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.4.30"
}

group = "me.pavel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
//    implementation("ru.tinkoff.invest:openapi-java-sdk:0.6-SNAPSHOT")
//    implementation("ru.tinkoff.invest:openapi-java-sdk-java8:0.6-SNAPSHOT")

    implementation("com.github.TinkoffCreditSystems:invest-openapi-java-sdk:0.5")
//    implementation("com.github.TinkoffCreditSystems:invest-openapi-java-sdk-java8:0.5")

    implementation("org.slf4j:slf4j-jdk14:1.7.30")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.4.2")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}