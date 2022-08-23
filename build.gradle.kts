import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jvmVersion = JavaVersion.VERSION_17
val kordVersion = "0.8.0-M16"

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"

    id("maven-publish")
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

group = "dev.bitflow"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = jvmVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":annotations"))

    ksp(project(":annotation-processor"))

    api("org.reflections:reflections:0.10.2")
    api("com.ibm.icu:icu4j:71.1")
    api("dev.kord:kord-core:$kordVersion")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")

    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
    runtimeOnly("org.slf4j:slf4j-api:1.7.36")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.11")
    runtimeOnly("ch.qos.logback:logback-core:1.2.11")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
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
                name.set("KFox")
                description.set("A discord command library in kotlin")
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
