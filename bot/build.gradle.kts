plugins {
    id("java")
    id("jacoco")
}

dependencies {
    implementation(project(":discord-connector"))
    implementation("io.github.twonirwana:dice-evaluator:v0.10.2")

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
    implementation(libs.emoji)
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.2")
    implementation("com.h2database:h2:2.3.232")
    implementation("org.apache.derby:derby:10.17.1.0")
    implementation("org.apache.derby:derbyclient:10.17.1.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.2")
    implementation("org.apache.xmlgraphics:batik:1.19")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.19")
    implementation("org.apache.xmlgraphics:batik-codec:1.19")

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation("io.github.origin-energy:java-snapshot-testing-junit5:4.0.8")
    testImplementation("io.github.origin-energy:java-snapshot-testing-plugin-jackson:4.0.8")
    testImplementation("io.projectreactor:reactor-test:3.7.8")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("commons-io:commons-io:2.19.0")

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