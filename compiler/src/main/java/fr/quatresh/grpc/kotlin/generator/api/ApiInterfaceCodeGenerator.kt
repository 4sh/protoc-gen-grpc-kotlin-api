package fr.quatresh.grpc.kotlin.generator.api

import com.google.protobuf.Descriptors
import com.squareup.kotlinpoet.*
import io.grpc.kotlin.generator.ServiceCodeGenerator
import io.grpc.kotlin.generator.protoc.*

class ApiInterfaceCodeGenerator(config: GeneratorConfig) : ServiceCodeGenerator(config) {

    override fun generate(service: Descriptors.ServiceDescriptor): Declarations = declarations {
        addType(buildApiInterfaceType(service))
    }

    private fun buildApiInterfaceType(service: Descriptors.ServiceDescriptor): TypeSpec =
        TypeSpec.interfaceBuilder(buildApiInterfaceName(service))
            .apply { addFunctions(buildFunctions(service)) }
            .apply { addTypes(buildMessageTypes(service)) }
            .build()

    private fun buildFunctions(service: Descriptors.ServiceDescriptor): List<FunSpec> =
        service.methods
            .map { method ->
                FunSpec.builder(method.methodName.toMemberSimpleName())
                    .addModifiers(KModifier.ABSTRACT)
                    .addParameter(buildFunctionParameter(method))
                    .returns(
                        ClassName(
                            "",
                            method.outputType.simpleName.name.asApiClassName()
                        )
                    )
                    .build()
            }

    private fun buildFunctionParameter(method: Descriptors.MethodDescriptor): ParameterSpec =
        ParameterSpec(
            "input",
            ClassName(
                "",
                method.inputType.simpleName.name.asApiClassName()
            )
        )

    private fun buildMessageTypes(service: Descriptors.ServiceDescriptor): List<TypeSpec> =
        service.file
            .messageTypes
            .map { descriptor ->
                val constructorParameters = buildMessageTypeConstructorParameters(descriptor)
                val properties = buildMessageTypeProperties(descriptor)
                TypeSpec.classBuilder(descriptor.simpleName.name.asApiClassName())
                    .addModifiers(KModifier.DATA)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameters(constructorParameters)
                            .build()
                    )
                    .addProperties(properties)
                    .build()
            }

    private fun buildMessageTypeConstructorParameters(descriptor: Descriptors.Descriptor) =
        descriptor
            .fields
            .map { field ->
                ParameterSpec
                    .builder(
                        field.fieldName.javaSimpleName.name.asApiClassName(),
                        ClassName("kotlin", "String")
                    )
                    .build()
            }

    private fun buildMessageTypeProperties(descriptor: Descriptors.Descriptor): List<PropertySpec> =
        descriptor
            .fields
            .map { field ->
                PropertySpec
                    .builder(
                        field.fieldName.javaSimpleName.name.asApiClassName(),
                        ClassName("kotlin", "String")
                    )
                    .initializer(field.fieldName.javaSimpleName.name)
                    .build()
            }

    private fun buildApiInterfaceName(service: Descriptors.ServiceDescriptor): String =
        service.name.replace("Service", "")

    private fun String.asApiClassName() = replace("Dto", "")
}
