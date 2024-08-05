plugins {
    id("java")
    id("jacoco")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":discord-connector"))
    implementation("io.github.twonirwana:dice-evaluator:v0.8.2")

    implementation(libs.log4j.to.slf4j)
    implementation(libs.reactor.core)
    implementation(libs.logback.classic)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.guava)
    implementation(libs.micrometer.core)
    implementation(libs.commons.lang3)
    implementation(libs.commons.text)
    implementation(libs.avaje.config)
    implementation(libs.avaje.slf4j)
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.2")
    implementation("com.h2database:h2:2.3.230")
    implementation("org.apache.derby:derby:10.17.1.0")
    implementation("org.apache.derby:derbyclient:10.17.1.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
    implementation("org.apache.xmlgraphics:batik:1.17")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.17")
    implementation("org.apache.xmlgraphics:batik-codec:1.17")
    implementation("net.fellbaum:jemoji:1.4.1")

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation("io.github.origin-energy:java-snapshot-testing-junit5:4.0.8")
    testImplementation("io.github.origin-energy:java-snapshot-testing-plugin-jackson:4.0.8")
    testImplementation("io.projectreactor:reactor-test:3.6.8")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.3")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("commons-io:commons-io:2.16.1")

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
    // set heap size for the test JVM(s)
    minHeapSize = "128m"
    maxHeapSize = "1024m"

    // set JVM arguments for the test JVM(s)
    jvmArgs = listOf("-XX:MaxMetaspaceSize=512m")
}


tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}