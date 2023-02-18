plugins {
    id("java")
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation( project(":discord-connector:api"))

    implementation("net.dv8tion:JDA:5.0.0-beta.3")
    implementation(libs.reactor.core)
    implementation(libs.logback.classic)
    implementation(libs.log4j.to.slf4j)
    implementation(libs.guava)
    implementation(libs.commons.lang3)
    implementation(libs.micrometer.core)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.assertj.core)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

tasks.test {
    useJUnitPlatform()
}