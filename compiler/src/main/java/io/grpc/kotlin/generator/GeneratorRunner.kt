/*
 * Copyright 2020 gRPC authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.kotlin.generator

import com.google.protobuf.Descriptors.FileDescriptor
import com.izivia.grpc.kotlin.generator.api.ApiInterfaceCodeGenerator
import com.squareup.kotlinpoet.FileSpec
import io.grpc.kotlin.generator.protoc.AbstractGeneratorRunner
import io.grpc.kotlin.generator.protoc.GeneratorConfig
import io.grpc.kotlin.generator.protoc.JavaPackagePolicy

/** Main runner for code generation for Kotlin gRPC APIs. */
object GeneratorRunner : AbstractGeneratorRunner() {
    @JvmStatic
    fun main(args: Array<String>) = super.doMain(args)

    private val config = GeneratorConfig(JavaPackagePolicy.OPEN_SOURCE, false)

    val generator = ProtoFileCodeGenerator(
        generators = listOf(
            ::ApiInterfaceCodeGenerator
        ),
        config = config,
        topLevelSuffix = "Api"
    )

    override fun generateCodeForFile(file: FileDescriptor, parameters: Map<String, String>): List<FileSpec> =
        listOfNotNull(generator.generateCodeForFile(file, parameters))
}
