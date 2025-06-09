plugins {
    id("java")
}

dependencies {
    implementation(libs.reactor.core)
    implementation(libs.guava)
    implementation(libs.commons.lang3)
    implementation(libs.emoji)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}