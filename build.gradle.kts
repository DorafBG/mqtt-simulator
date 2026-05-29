plugins {
    // Indique à IntelliJ que c'est un projet Kotlin
    kotlin("jvm") version "1.9.23"
    // Ajoute la capacité de "Run" (lancer) le projet
    application
}

group = "jp.ac.tmu.sakailab"
version = "1.0-SNAPSHOT"

repositories {
    // Indique où télécharger les outils de base
    mavenCentral()
}

dependencies {
    // La bibliothèque standard de Kotlin
    implementation(kotlin("stdlib"))
}

application {
    // C'est la ligne magique qui dit où se trouve ton point de départ !
    mainClass.set("jp.ac.tmu.sakailab.MainDPSimKt")
}

tasks.withType<Jar> {
    manifest {
        // Remplace par le chemin exact de ton Main si ce n'est pas celui-ci
        attributes["Main-Class"] = "jp.ac.tmu.sakailab.MainDPSimKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}