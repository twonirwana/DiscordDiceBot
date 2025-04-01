plugins {
    id("java")
}

dependencies {
    implementation(project(":discord-connector:api"))

    implementation("net.dv8tion:JDA:5.3.1") {
        exclude(module = "opus-java")
        exclude(module = "tink")
    }
    implementation(libs.reactor.core)
    implementation(libs.logback.classic)
    implementation(libs.log4j.to.slf4j)
    implementation(libs.guava)
    implementation(libs.commons.lang3)
    implementation(libs.micrometer.core)
    implementation(libs.commons.text)
    implementation(libs.avaje.config)
    implementation(libs.avaje.slf4j)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.assertj.core)

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.github.origin-energy:java-snapshot-testing-junit5:4.0.8")
    testImplementation("io.github.origin-energy:java-snapshot-testing-plugin-jackson:4.0.8")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
}