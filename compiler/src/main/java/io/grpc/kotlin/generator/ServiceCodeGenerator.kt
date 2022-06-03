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

package fr.quatresh.kotlin.grpc.api.generator

import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import fr.quatresh.kotlin.grpc.api.generator.protoc.Declarations
import fr.quatresh.kotlin.grpc.api.generator.protoc.GeneratorConfig
import fr.quatresh.kotlin.grpc.api.generator.protoc.declarations
import fr.quatresh.kotlin.grpc.api.generator.protoc.member
import fr.quatresh.kotlin.grpc.api.generator.protoc.methodName
import fr.quatresh.kotlin.grpc.api.generator.protoc.serviceName

/** Generates code based on a [ServiceDescriptor]. */
abstract class ServiceCodeGenerator(protected val config: GeneratorConfig) {
    companion object {
        private const val GRPC_CLASS_NAME_SUFFIX = "Grpc"
    }

    /** Function for subclasses to implement. */
    abstract fun generate(service: ServiceDescriptor): Declarations

    operator fun plus(other: ServiceCodeGenerator): ServiceCodeGenerator {
        val me = this
        return object : ServiceCodeGenerator(config) {
            override fun generate(service: ServiceDescriptor): Declarations = declarations {
                merge(me.generate(service))
                merge(other.generate(service))
            }
        }
    }

    /** Gets the fully qualified name of the Java class generated by gRPC. */
    protected val ServiceDescriptor.grpcClass: ClassName
        get() = with(config) {
            javaPackage(file).nestedClass(
                serviceName.toClassSimpleName().withSuffix(GRPC_CLASS_NAME_SUFFIX)
            )
        }

    /** Gets the name of the function that gets the [io.grpc.ServiceDescriptor]. */
    protected val ServiceDescriptor.grpcDescriptor: MemberName
        get() = grpcClass.member("getServiceDescriptor")

    /** Gets the name of the function that gets the [io.grpc.MethodDescriptor]. */
    protected val MethodDescriptor.descriptorCode: CodeBlock
        get() = CodeBlock.of(
            "%T.%L()",
            service.grpcClass,
            methodName.toMemberSimpleName().withPrefix("get").withSuffix("Method")
        )
}
