rootProject.name = "buttonDiceRoller"
include("discord-connector")
include("discord-connector:api")
include("discord-connector:jda")
include("bot")


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("lombok", "org.projectlombok:lombok:1.18.32")
            library("micrometer-core", "io.micrometer:micrometer-core:1.12.4")
            library("reactor-core", "io.projectreactor:reactor-core:3.6.4")
            library("guava", "com.google.guava:guava:33.1.0-jre")
            library("junit-jupiter-api", "org.junit.jupiter:junit-jupiter-api:5.10.2")
            library("junit-jupiter-engine", "org.junit.jupiter:junit-jupiter-engine:5.10.2")
            library("assertj-core", "org.assertj:assertj-core:3.25.3")
            library("logback-classic", "ch.qos.logback:logback-classic:1.5.3")
            library("log4j-to-slf4j", "org.apache.logging.log4j:log4j-to-slf4j:2.23.1")
            library("commons-lang3", "org.apache.commons:commons-lang3:3.14.0")
            library("commons-text", "org.apache.commons:commons-text:1.11.0")
            library("avaje-config", "io.avaje:avaje-config:3.12")
            library("avaje-slf4j", "io.avaje:avaje-applog-slf4j:1.0")
        }
    }
}
