plugins {
    `java-library`

    `maven-publish`

    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    /**
     * Placeholders
     */
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    maven("https://repo.mvdw-software.com/content/groups/public/")

    /**
     * NBT API
     */
    maven("https://repo.codemc.io/repository/maven-public/")

    maven("https://repo.codemc.io/repository/nms")
}

dependencies {
    implementation(project(":api"))

    implementation(project(":worldguard-v6"))
    implementation(project(":worldguard-v7"))

    implementation("de.tr7zw", "nbt-data-api", "2.11.1")

    implementation("org.bstats", "bstats-bukkit", "3.0.0")
    implementation("org.jetbrains", "annotations", "23.0.0")

    compileOnly("org.spigotmc", "spigot-api", "${project.extra["minecraft_version"]}-R0.1-SNAPSHOT")

    compileOnly("me.filoghost.holographicdisplays", "holographicdisplays-api", "3.0.0")

    compileOnly("com.github.decentsoftware-eu", "decentholograms", "2.7.8")

    compileOnly("be.maximvdw", "MVdWPlaceholderAPI", "3.1.1-SNAPSHOT") {
        exclude(group = "org.spigotmc")
        exclude(group = "org.bukkit")
    }

    compileOnly("com.sainttx.holograms", "holograms", "2.12")

    compileOnly("com.github.MilkBowl", "VaultAPI", "1.7")

    compileOnly("me.clip", "placeholderapi", "2.11.2") {
        exclude(group = "org.spigotmc")
        exclude(group = "org.bukkit")
    }
}

val buildNumber: String? = System.getenv("BUILD_NUMBER")
val buildVersion = "${rootProject.version}-b$buildNumber-SNAPSHOT"

tasks {
    shadowJar {
        if (buildNumber != null) {
            archiveFileName.set("${rootProject.name}-${buildVersion}.jar")
        } else {
            archiveFileName.set("${rootProject.name}-${rootProject.version}.jar")
        }

        listOf(
            "de.tr7zw",
            "org.bstats",
            "org.jetbrains"
        ).forEach {
            relocate(it, "${rootProject.group}.plugin.lib.$it")
        }
    }

    compileJava {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }

    processResources {
        filesMatching("plugin.yml") {
            expand(
                "name" to rootProject.name,
                "group" to rootProject.group,
                "version" to if (buildNumber != null) buildVersion else rootProject.version,
                "description" to rootProject.description
            )
        }
    }
}