dependencies {
    compileOnly("org.spigotmc", "spigot-api", "${project.extra["minecraft_version"]}-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }
}