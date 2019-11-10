import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "space.sentinel"
version = "1.0-SNAPSHOT"

plugins {
    application
    kotlin("jvm") version "1.3.40"
    `maven-publish`
}

application {
    mainClassName = "space.hajnal.App"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.pi4j:pi4j-core:1.2")
    implementation("io.projectreactor:reactor-core:3.3.0.RELEASE")
    implementation("org.slf4j:slf4j-api:1.7.26")
    implementation("ch.qos.logback:logback-classic:0.9.26")
    implementation("ch.qos.logback:logback-core:0.9.26")
    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("org.bytedeco:javacv-platform:1.5.2")
    implementation("io.github.config4k:config4k:0.4.1")

    implementation("$group:PIRReader:$version")
    implementation("$group:CameraReader:$version")

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.9")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("io.projectreactor:reactor-test:3.3.0.RELEASE")
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
}

repositories {
    jcenter()
    mavenLocal()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava"
        ) {
            artifactId = "SentinelPi"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("Sentinel Pi")
                description.set("Motion Detector for Raspberry Pi")
                url.set("https://github.com/314t0n/SentinelPi")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("314t0n")
                        name.set("Hajnal David")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = uri("$buildDir/repos/releases")
            val snapshotsRepoUrl = uri("$buildDir/repos/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}


val fatJar = task("fatJar", type = Jar::class) {
    manifest {
        attributes["Implementation-Title"] = "Sentinel Pi"
        attributes["Implementation-Version"] = "1.0"
        attributes["Main-Class"] = "space.sentinel.AppKt"
    }
    from(configurations.runtime.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks {

    "build" {
        dependsOn(fatJar)
    }
    "test"(Test::class) {
        useJUnitPlatform()
    }

}