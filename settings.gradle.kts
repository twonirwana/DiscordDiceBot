rootProject.name = "buttonDiceRoller"
include("discord-connector")
include("discord-connector:api")
include("discord-connector:jda")
include("bot")


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("lombok", "org.projectlombok:lombok:1.18.36")
            library("micrometer-core", "io.micrometer:micrometer-core:1.14.2")
            library("reactor-core", "io.projectreactor:reactor-core:3.7.1")
            library("guava", "com.google.guava:guava:33.4.0-jre")
            library("junit-jupiter-api", "org.junit.jupiter:junit-jupiter-api:5.11.4")
            library("junit-jupiter-engine", "org.junit.jupiter:junit-jupiter-engine:5.11.4")
            library("assertj-core", "org.assertj:assertj-core:3.27.1")
            library("logback-classic", "ch.qos.logback:logback-classic:1.5.15")
            library("log4j-to-slf4j", "org.apache.logging.log4j:log4j-to-slf4j:2.24.3")
            library("commons-lang3", "org.apache.commons:commons-lang3:3.17.0")
            library("commons-text", "org.apache.commons:commons-text:1.13.0")
            library("avaje-config", "io.avaje:avaje-config:4.0")
            library("avaje-slf4j", "io.avaje:avaje-applog-slf4j:1.0")
            library("emoji", "net.fellbaum:jemoji:1.6.0")
        }
    }
}
