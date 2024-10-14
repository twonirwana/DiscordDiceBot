rootProject.name = "buttonDiceRoller"
include("discord-connector")
include("discord-connector:api")
include("discord-connector:jda")
include("bot")


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("lombok", "org.projectlombok:lombok:1.18.34")
            library("micrometer-core", "io.micrometer:micrometer-core:1.13.5")
            library("reactor-core", "io.projectreactor:reactor-core:3.6.10")
            library("guava", "com.google.guava:guava:33.3.1-jre")
            library("junit-jupiter-api", "org.junit.jupiter:junit-jupiter-api:5.11.2")
            library("junit-jupiter-engine", "org.junit.jupiter:junit-jupiter-engine:5.11.2")
            library("assertj-core", "org.assertj:assertj-core:3.26.3")
            library("logback-classic", "ch.qos.logback:logback-classic:1.5.10")
            library("log4j-to-slf4j", "org.apache.logging.log4j:log4j-to-slf4j:2.24.1")
            library("commons-lang3", "org.apache.commons:commons-lang3:3.17.0")
            library("commons-text", "org.apache.commons:commons-text:1.12.0")
            library("avaje-config", "io.avaje:avaje-config:4.0")
            library("avaje-slf4j", "io.avaje:avaje-applog-slf4j:1.0")
            library("emoji", "net.fellbaum:jemoji:1.5.2")
        }
    }
}
