plugins {
    id("java")
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.reactor.core)
    implementation(libs.guava)
    implementation(libs.commons.lang3)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}