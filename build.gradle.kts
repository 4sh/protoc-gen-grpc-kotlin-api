import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21" apply false
    id("com.google.protobuf") version "0.8.18" apply false
}

group = "fr.quatresh"
version = "1.0.13"

ext["grpcVersion"] = "1.46.0"
ext["protobufVersion"] = "3.20.1"
ext["coroutinesVersion"] = "1.6.2"

subprojects {

    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("com.google.protobuf")
        plugin("maven-publish")
    }

    // gradle-nexus/publish-plugin needs these here too
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    extensions.getByType<PublishingExtension>().publications {
        create<MavenPublication>("maven") {
            pom {
                url.set("https://github.com/4sh/protoc-gen-grpc-kotlin-api")

                scm {
                    connection.set("scm:git:https://github.com/4sh/protoc-gen-grpc-kotlin-api.git")
                    developerConnection.set("scm:git:git@github.com:4sh/protoc-gen-grpc-kotlin-api.git")
                    url.set("https://github.com/4sh/protoc-gen-grpc-kotlin-api")
                }

                licenses {
                    license {
                        name.set("Apache 2.0")
                        url.set("https://opensource.org/licenses/Apache-2.0")
                    }
                }
            }
        }
    }
}
