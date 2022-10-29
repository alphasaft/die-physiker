import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.7.0"
}

group = "me.alphasaft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.10")
    testImplementation(kotlin("test-junit"))
    implementation(kotlin("stdlib"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = "1.7"
    kotlinOptions.languageVersion = "1.7"
    kotlinOptions.freeCompilerArgs = listOf("-opt-in=kotlin.contracts.ExperimentalContracts")
}
