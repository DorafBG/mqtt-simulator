plugins {
    kotlin("jvm") version "1.9.23"
    application
}

group = "jp.ac.tmu.sakailab"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib")) // bibliothèque standard de Kotlin
}

application {
    mainClass.set("jp.ac.tmu.sakailab.MainDPSimKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "jp.ac.tmu.sakailab.MainDPSimKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}