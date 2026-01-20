import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.3.1"
    id("jacoco-report-aggregation")
    id("java")
}

apply(plugin = "java")
apply(plugin = "com.gradleup.shadow")
apply(plugin = "jacoco")


dependencies {
    implementation(project(":bot"))
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
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

