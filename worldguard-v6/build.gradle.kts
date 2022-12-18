dependencies {
    implementation(project(":api"))

    compileOnly("org.spigotmc", "spigot-api", "1.13.2-R0.1-SNAPSHOT")

    compileOnly("com.sk89q.worldguard", "worldguard-legacy", "6.2")
    compileOnly("com.sk89q", "worldedit", "6.0.0-SNAPSHOT")
}

tasks {
    compileJava {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }
}