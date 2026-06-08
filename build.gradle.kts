import java.util.*

plugins {
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val pylonBuild = gradle.includedBuild("pylon")
val rebarBuild = gradle.includedBuild("rebar")

tasks.runServer {
    dependsOn(pylonBuild.task(":shadowJar"), rebarBuild.task(":rebar:shadowJar"))

    doFirst {
        val runFolder = project.projectDir.resolve("run")
        runFolder.mkdirs()
        runFolder.resolve("eula.txt").writeText("eula=true")

        val pluginsDir = runFolder.resolve("plugins")
        if (!System.getProperty("io.github.pylonmc.rebar.disableConfigReset").toBoolean()) {
            pluginsDir.resolve("Rebar").deleteRecursively()
            pluginsDir.resolve("Pylon").deleteRecursively()
        }
        pluginsDir.mkdirs()
        copy {
            from(pylonBuild.projectDir.resolve("build/libs")) {
                include("pylon-1.0.0-SNAPSHOT.jar")
            }
            into(pluginsDir)
        }
        copy {
            from(rebarBuild.projectDir.resolve("rebar/build/libs")) {
                include("rebar-1.0.0-SNAPSHOT.jar")
            }
            into(pluginsDir)
        }
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
