plugins {
    id("java-library")
}


dependencies {
    api(project(":discord-connector:api"))
    implementation(project(":discord-connector:jda"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}