plugins {
    id("java")
    id("jacoco")
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation(project(":discord-connector"))
    implementation("com.github.twonirwana:dice-parser:0.7.1")
    implementation("io.github.twonirwana:dice-evaluator:v0.3.4")

    implementation(libs.log4j.to.slf4j)
    implementation(libs.reactor.core)
    implementation(libs.logback.classic)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.guava)
    implementation(libs.micrometer.core)
    implementation(libs.commons.lang3)
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.6")
    implementation("com.h2database:h2:2.1.214")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.2")

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation("io.projectreactor:reactor-test:3.5.5")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("commons-io:commons-io:2.11.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}