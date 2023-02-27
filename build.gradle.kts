import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "8.0.0"
    id("jacoco-report-aggregation")
    id("java")
    id("io.freefair.github.dependency-submission") version "6.6.3"
}

apply(plugin = "java")
apply(plugin = "com.github.johnrengelman.shadow")
apply(plugin = "jacoco")

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation(project(":bot"))
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

tasks.test {
    useJUnitPlatform()
}

// This task will generate your fat JAR and put it in the ./build/libs/ directory
tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            attributes(mapOf("Main-Class" to "de.janno.discord.bot.Bot"))
        }
    }
}
tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}

