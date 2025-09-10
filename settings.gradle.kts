rootProject.name = "buttonDiceRoller"
include("discord-connector")
include("discord-connector:api")
include("discord-connector:jda")
include("bot")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            library("lombok", "org.projectlombok:lombok:1.18.40")
            library("micrometer-core", "io.micrometer:micrometer-core:1.15.4")
            library("reactor-core", "io.projectreactor:reactor-core:3.7.11")
            library("guava", "com.google.guava:guava:33.4.8-jre")
            library("assertj-core", "org.assertj:assertj-core:3.27.4")
            library("logback-classic", "ch.qos.logback:logback-classic:1.5.18")
            library("log4j-to-slf4j", "org.apache.logging.log4j:log4j-to-slf4j:2.25.1")
            library("commons-lang3", "org.apache.commons:commons-lang3:3.18.0")
            library("commons-text", "org.apache.commons:commons-text:1.14.0")
            library("avaje-config", "io.avaje:avaje-config:4.1")
            library("avaje-slf4j", "io.avaje:avaje-applog-slf4j:1.0")
            library("emoji", "net.fellbaum:jemoji:1.7.4")
        }
    }
}
