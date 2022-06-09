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
import com.squareup.kotlinpoet.FileSpec
import io.grpc.kotlin.generator.protoc.*
import java.util.*

/**
 * Given a list of [CodeGenerator] factories, generates (optionally) a [FileSpec] of the
 * generated code.
 */
class ProtoFileCodeGenerator(
    generators: List<(GeneratorConfig) -> CodeGenerator>,
    private val config: GeneratorConfig,
    private val topLevelSuffix: String
) {
    private val generators = generators.map { it(config) }

    fun generateCodeForFile(fileDescriptor: FileDescriptor, parameters: Map<String, String>): FileSpec? = with(config) {
        val fileName = ClassSimpleName(buildFileName(fileDescriptor)).withSuffix(topLevelSuffix)

        val packageName = javaPackage(fileDescriptor)
            .let { it.copy(pkg = it.pkg.toPackageName(parameters)) }
        val fileBuilder = FileSpec.builder(packageName, fileName)

        val decls = declarations {
            for (generator in generators) {
                merge(generator.generate(fileDescriptor, parameters))
            }
        }

        return if (decls.hasEnclosingScopeDeclarations || decls.hasTopLevelDeclarations) {
            decls.writeAllAtTopLevel(fileBuilder)
            fileBuilder.build()
        } else {
            null
        }
    }

    private fun buildFileName(fileDescriptor: FileDescriptor) =
        fileDescriptor.file.fileName.name
            .replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }
}
