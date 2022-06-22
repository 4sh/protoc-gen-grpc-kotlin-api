import com.google.protobuf.gradle.*

plugins {
    base
    java
    kotlin("jvm")
    id("com.google.protobuf")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    compileOnly("com.google.protobuf:protobuf-kotlin:3.19.4")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("io.strikt:strikt-core:0.34.1")
}

tasks.getByPath("test").doFirst {
    with(this as Test) {
        useJUnit()
        useJUnitPlatform()
    }
}

kotlin {
    sourceSets.getByName("main").kotlin.srcDir("build/generated/source/proto/main/grpckt-api")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.19.4"
    }
    plugins {
        id("grpckt-api") {
            path = project(":compiler").tasks.jar.get().archiveFile.get().asFile.absolutePath
        }
    }
    generateProtoTasks {
        all().forEach {
            if (it.name.startsWith("generateTestProto") || it.name.startsWith("generateProto")) {
                it.dependsOn(":compiler:jar")
            }
            it.builtins {
                remove("java")
            }
            it.plugins {
                id("grpckt-api") {
                    option("inputPackage=com.test.rpc")
                    option("outputPackageNameReplacement=.rpc/")
                    option("outputClassNameReplacement=Dto/")
                    option("baseClassSuperinterfaceName=com.test.BaseApiElement")
                    option("baseEnumSuperinterfaceName=com.test.BaseApiEnum")
                }
            }
        }
    }
}
