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

import com.google.protobuf.Descriptors
import io.grpc.kotlin.generator.protoc.Declarations
import io.grpc.kotlin.generator.protoc.GeneratorConfig
import io.grpc.kotlin.generator.protoc.declarations

abstract class CodeGenerator(protected val config: GeneratorConfig) {

    abstract fun generate(fileDescriptor: Descriptors.FileDescriptor, parameters: Map<String, String>): Declarations

    operator fun plus(other: CodeGenerator): CodeGenerator {
        val me = this
        return object : CodeGenerator(config) {
            override fun generate(
                fileDescriptor: Descriptors.FileDescriptor,
                parameters: Map<String, String>
            ): Declarations = declarations {
                merge(me.generate(fileDescriptor, parameters))
                merge(other.generate(fileDescriptor, parameters))
            }
        }
    }
}
