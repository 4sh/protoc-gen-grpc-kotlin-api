import com.google.protobuf.gradle.*

plugins {
    application
}

application {
    mainClass.set("io.grpc.kotlin.generator.GeneratorRunner")
}

dependencies {
    // Kotlin and Java
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.ext["coroutinesVersion"]}")

    // Grpc and Protobuf
    implementation("io.grpc:grpc-protobuf:${rootProject.ext["grpcVersion"]}")

    // Misc
    implementation(kotlin("reflect"))
    implementation("com.squareup:kotlinpoet:1.11.0")
    implementation("com.google.truth:truth:1.1.3")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)

    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            pom {
                name.set("gRPC Kotlin Compiler")
                artifactId = "protoc-gen-grpc-kotlin-api"
                description.set("gRPC Kotlin API protoc compiler plugin")
            }

            artifact(tasks.jar) {
                classifier = "jdk8"
            }
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${rootProject.ext["protobufVersion"]}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${rootProject.ext["grpcVersion"]}"
        }
        id("grpckt") {
            path = tasks.jar.get().archiveFile.get().asFile.absolutePath
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}
