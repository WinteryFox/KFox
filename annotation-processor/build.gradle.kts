import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jvmVersion = JavaVersion.VERSION_17
val kordVersion = "0.8.0-M15"

plugins {
    kotlin("jvm") version "1.7.10"
    id("maven-publish")
}

group = rootProject.group
version = rootProject.version
java.sourceCompatibility = jvmVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib"))

    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.10-1.0.6")
    implementation("dev.kord:kord-core:$kordVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = jvmVersion.toString()
        freeCompilerArgs = freeCompilerArgs + listOf("-opt-in=kotlin.RequiresOptIn")
    }
}

val sourceJar = task("sourceJar", Jar::class) {
    dependsOn(tasks["classes"])
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        maven {
            url = if (project.version.toString().contains("SNAPSHOT")) {
                uri("https://nexus.zerotwo.bot/repository/m2-public-snapshots/")
            } else {
                uri("https://nexus.zerotwo.bot/repository/m2-public-releases/")
            }
            credentials {
                username = System.getenv("NEXUS_USER")
                password = System.getenv("NEXUS_PASSWORD")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("KFox-Annotation-Processor")
                description.set("Annotation processor for code generation with KFox")
                url.set("https://github.com/ZeroTwo-Bot/KFox/")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com:ZeroTwo-Bot/KFox.git")
                    developerConnection.set("scm:git:ssh://github.com:ZeroTwo-Bot/KFox.git")
                    url.set("https://github.com/ZeroTwo-Bot/KFox/")
                }
            }

            from(components.getByName("java"))

            artifact(sourceJar)
        }
    }
}
