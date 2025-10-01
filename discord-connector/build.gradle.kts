plugins {
    id("java-library")
}


dependencies {
    api(project(":discord-connector:api"))
    implementation(project(":discord-connector:jda"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}