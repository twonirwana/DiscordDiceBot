plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":discord-connector:api"))
    implementation(project(":discord-connector:jda"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}