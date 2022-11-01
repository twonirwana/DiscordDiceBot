plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.reactor.core)
    implementation(libs.guava)
    implementation(libs.commons.lang3)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}