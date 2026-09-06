import java.util.*

plugins {
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

val runFolder = layout.projectDirectory.dir("run").asFile
val pylonBuild = gradle.includedBuild("pylon")
val rebarBuild = gradle.includedBuild("rebar")

val pylonJar = pylonBuild.projectDir.resolve("build/libs/pylon-1.0.0-SNAPSHOT.jar")
val rebarJar = rebarBuild.projectDir.resolve("rebar/build/libs/rebar-1.0.0-SNAPSHOT.jar")

val resetConfig = !System.getProperty("io.github.pylonmc.rebar.disableConfigReset").toBoolean()

tasks.runServer {
    dependsOn(pylonBuild.task(":shadowJar"), rebarBuild.task(":rebar:shadowJar"))

    doFirst {
        runFolder.mkdirs()
        runFolder.resolve("eula.txt").writeText("eula=true")

        val pluginsDir = runFolder.resolve("plugins")
        if (!System.getProperty("io.github.pylonmc.rebar.disableConfigReset").toBoolean()) {
            pluginsDir.resolve("Rebar").deleteRecursively()
            pluginsDir.resolve("Pylon").deleteRecursively()
        }

        pluginsDir.mkdirs()

        pylonJar.copyTo(pluginsDir.resolve(pylonJar.name), overwrite = true)
        rebarJar.copyTo(pluginsDir.resolve(rebarJar.name), overwrite = true)
    }

    maxHeapSize = "2G"

    fun readMinecraftVersion(build: IncludedBuild): String {
        val props = Properties()
        build.projectDir.resolve("gradle.properties").bufferedReader().use(props::load)

        return props["minecraft.version"] as String
    }

    val rebarVersion = readMinecraftVersion(rebarBuild)
    val pylonVersion = readMinecraftVersion(pylonBuild)
    if (rebarVersion != pylonVersion) {
        throw GradleException("Minecraft version mismatch between Rebar ($rebarVersion) and Pylon ($pylonVersion)")
    }

    minecraftVersion(rebarVersion)
}

tasks.register("runStableServer") {
    dependsOn(pylonBuild.task(":runServer"))
    group = "run paper"
}

tasks.register("runTests") {
    dependsOn(rebarBuild.task(":test:runServer"))
    group = "run paper"
}
