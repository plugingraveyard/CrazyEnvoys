plugins {
    `java-library`
}

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
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}