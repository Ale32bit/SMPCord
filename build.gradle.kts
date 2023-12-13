import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("xyz.jpenilla.run-velocity") version "2.2.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.alexdevs"
version = "0.2.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    compileOnly("com.electronwill.night-config:toml:3.6.0")
    annotationProcessor("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.github.discord-jda:JDA:v5.0.0-beta.18")?.let { shadow(it) }
    implementation("club.minnced:discord-webhooks:0.8.4")?.let { shadow(it) }

}


tasks {
    named<ShadowJar>("shadowJar") {
        //archiveClassifier.set(null)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    runVelocity {
        // Configure the Velocity version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        velocityVersion("3.2.0-SNAPSHOT")
    }
}