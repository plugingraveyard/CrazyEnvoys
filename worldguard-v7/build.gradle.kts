dependencies {
    implementation(project(":api"))

    compileOnly("org.spigotmc", "spigot-api", "${project.extra["minecraft_version"]}-R0.1-SNAPSHOT")

    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.1-SNAPSHOT")

    compileOnly("com.sk89q.worldedit", "worldedit-bukkit", "7.0.1-SNAPSHOT")
}