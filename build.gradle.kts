import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    jacoco
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

apply( plugin = "java")
apply(  plugin= "com.github.johnrengelman.shadow")
apply( plugin= "jacoco")

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.2.2")
    implementation("ch.qos.logback:logback-classic:1.2.10")
    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("io.micrometer:micrometer-core:1.8.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.8.3")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.github.twonirwana:dice-parser:0.4.1")
    implementation("io.undertow:undertow-core:2.2.16.Final")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation ("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("org.mockito:mockito-core:4.3.1")
    testImplementation ("org.mockito:mockito-inline:4.3.1")
    testCompileOnly ("org.projectlombok:lombok:1.18.22")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.22")
    testImplementation ("io.projectreactor:reactor-test")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// This task will generate your fat JAR and put it in the ./build/libs/ directory
tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            attributes(mapOf("Main-Class" to "de.janno.discord.BaseBot"))
        }
    }
}
tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}