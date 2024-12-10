plugins {
    id("java")
}

dependencies {
    implementation(project(":discord-connector:api"))

    //todo update on jda release of user install
  /*  implementation("net.dv8tion:JDA:5.2.1") {
        exclude(module = "opus-java")
    }*/
    implementation("com.github.freya022:JDA:4b031a7283")
    implementation(libs.reactor.core)
    implementation(libs.logback.classic)
    implementation(libs.log4j.to.slf4j)
    implementation(libs.guava)
    implementation(libs.commons.lang3)
    implementation(libs.micrometer.core)
    implementation(libs.commons.text)
    implementation(libs.avaje.config)
    implementation(libs.avaje.slf4j)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.assertj.core)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
}