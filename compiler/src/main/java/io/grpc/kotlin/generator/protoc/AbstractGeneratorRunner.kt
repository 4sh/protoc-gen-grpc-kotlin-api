/*
 * Copyright 2020 Google LLC
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

package io.grpc.kotlin.generator.protoc

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.FileSpec
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Superclass for generators. */
abstract class AbstractGeneratorRunner {
    abstract fun generateCodeForFile(file: FileDescriptor, parameters: Map<String, String>): List<FileSpec>

    @VisibleForTesting
    fun mainAsProtocPlugin(input: InputStream, output: OutputStream) {
        val generatorRequest = try {
            input.buffered().use {
                PluginProtos.CodeGeneratorRequest.parseFrom(it)
            }
        } catch (failure: Exception) {
            throw IOException(
                """
        Attempted to run proto extension generator as protoc plugin, but could not read
        CodeGeneratorRequest.
        """.trimIndent(),
                failure
            )
        }
        output.buffered()
            .use {
                CodeGenerators
                    .codeGeneratorResponse {
                        val descriptorMap =
                            CodeGenerators.descriptorMap(generatorRequest.protoFileList)
                        val parameters = generatorRequest.parameter.split(",")
                            .map { param -> param.split("=") }
                            .associate { (key, value) -> key to value }
                        generatorRequest.filesToGenerate
                            .map(descriptorMap::getValue) // compiled descriptors to generate code for
                            .flatMap { fileDescriptor ->
                                generateCodeForFile(
                                    fileDescriptor,
                                    parameters
                                )
                            } // generated extensions
                    }
                    .writeTo(it)
            }
    }

    fun doMain(args: Array<String>) {
        if (args.isEmpty()) {
            mainAsProtocPlugin(System.`in`, System.out)
        } else {
            throw IllegalStateException("unsupported execution mode")
        }
    }
}
