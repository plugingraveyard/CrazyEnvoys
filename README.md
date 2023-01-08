# Crazy Envoys
The legacy source for CrazyEnvoys ( 1.8 -> 1.16.5 )

## Modrinth:
https://modrinth.com/plugin/crazyenvoys/version/1.4.17

## Repository:
https://repo.crazycrew.us/#/releases

# Developer API

## Groovy
<details>
 <summary>
   Gradle (Groovy)
 </summary>

```gradle
repositories {
    maven {
        url = "https://repo.crazycrew.us/releases"
    }
}
```

```gradle
dependencies {
    compileOnly "me.badbones69.crazyenvoys:crazyenvoys:1.4.17"
}
```
</details>

## Kotlin
<details>
 <summary>
   Gradle (Kotlin)
 </summary>

```gradle
repositories {
    maven("https://repo.crazycrew.us/releases")
}
```

```gradle
dependencies {
    compileOnly("me.badbones69.crazyenvoys", "crazyenvoys", "1.4.17")
}
```
</details>

## Maven
<details>
 <summary>
   Maven
 </summary>

```xml
<repository>
  <id>crazycrew</id>
  <url>https://repo.crazycrew.us/releases</url>
</repository>
```

```xml
<dependency>
  <groupId>me.badbones69.crazyenvoys</groupId>
  <artifactId>crazyenvoys</artifactId>
  <version>1.4.17</version>
 </dependency>
```
</details>