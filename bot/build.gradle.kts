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
    implementation("io.github.twonirwana:dice-evaluator:v0.5.3")

    implementation(libs.log4j.to.slf4j)
    implementation(libs.reactor.core)
    implementation(libs.logback.classic)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.guava)
    implementation(libs.micrometer.core)
    implementation(libs.commons.lang3)
    implementation("org.apache.commons:commons-text:1.11.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.1")
    implementation("com.h2database:h2:2.2.224")
    implementation("org.apache.derby:derby:10.17.1.0")
    implementation("org.apache.derby:derbyclient:10.17.1.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0")
    implementation("org.apache.xmlgraphics:batik:1.17")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.17")
    implementation("org.apache.xmlgraphics:batik-codec:1.17")

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation("io.github.origin-energy:java-snapshot-testing-junit5:4.0.6")
    testImplementation("io.github.origin-energy:java-snapshot-testing-plugin-jackson:4.0.6")
    testImplementation("io.projectreactor:reactor-test:3.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("commons-io:commons-io:2.15.1")

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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