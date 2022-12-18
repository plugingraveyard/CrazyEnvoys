plugins {
    `java-library`
}

rootProject.group = "me.badbones69.crazyenvoys"
rootProject.version = "${extra["plugin_version"]}"
rootProject.description = "Drop custom crates with any prize you want all over spawn for players to fight over."

allprojects {
    apply(plugin = "java-library")

    repositories {
        /**
         * Spigot Team
         */
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

        maven("https://maven.enginehub.org/repo/")

        maven("https://jitpack.io")

        /**
         * Everything else we need.
         */
        mavenCentral()
        mavenLocal()
    }
}