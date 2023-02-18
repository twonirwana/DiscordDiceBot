plugins {
    id("java-library")
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":discord-connector:api"))
    implementation(project(":discord-connector:jda"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}