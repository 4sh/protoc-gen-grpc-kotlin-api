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
        TypeSpec.interfaceBuilder(service.name)
            .apply { addFunctions(buildFunctions(service)) }
            .apply { addTypes(buildMessageTypes(service)) }
            .apply { addTypes(buildEnumTypes(service)) }
            .build()

    private fun buildFunctions(service: Descriptors.ServiceDescriptor): List<FunSpec> = with(config) {
        service.methods
            .map { method ->
                FunSpec.builder(method.methodName.toMemberSimpleName())
                    .addModifiers(KModifier.ABSTRACT)
                    .addParameter(buildFunctionParameter(method))
                    .returns(method.outputType.messageClass())
                    .build()
            }
    }

    private fun buildMessageTypes(service: Descriptors.ServiceDescriptor): List<TypeSpec> =
        service.file
            .messageTypes
            .map { descriptor ->
                val constructorParameters = buildMessageTypeConstructorParameters(descriptor)
                val properties = buildMessageTypeProperties(descriptor)
                TypeSpec.classBuilder(descriptor.simpleName.name)
                    .addModifiers(KModifier.DATA)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameters(constructorParameters)
                            .build()
                    )
                    .addProperties(properties)
                    .build()
            }

    private fun buildEnumTypes(service: Descriptors.ServiceDescriptor): List<TypeSpec> =
        service.file
            .enumTypes
            .map { descriptor ->
                TypeSpec.enumBuilder(descriptor.simpleName.name)
                    .apply {
                        descriptor.values
                            .forEach { enumValue -> addEnumConstant(enumValue.name) }
                    }
                    .build()
            }

    private fun buildFunctionParameter(method: Descriptors.MethodDescriptor): ParameterSpec = with(config) {
        ParameterSpec(
            "input",
            method.inputType.messageClass()
        )
    }

    private fun buildMessageTypeConstructorParameters(descriptor: Descriptors.Descriptor) = with(config) {
        descriptor
            .fields
            .map { field ->
                ParameterSpec
                    .builder(
                        field.fieldName.javaSimpleName.name,
                        field.asClassName()
                    )
                    .build()
            }
    }

    private fun buildMessageTypeProperties(descriptor: Descriptors.Descriptor): List<PropertySpec> = with(config) {
        descriptor
            .fields
            .map { field ->
                PropertySpec
                    .builder(
                        field.fieldName.javaSimpleName.name,
                        field.asClassName()
                    )
                    .initializer(field.fieldName.javaSimpleName.name)
                    .build()
            }
    }

    private fun Descriptors.FieldDescriptor.asClassName(): ClassName = with(config) {
        when (javaType) {
            Descriptors.FieldDescriptor.JavaType.MESSAGE -> messageType.messageClass()
            Descriptors.FieldDescriptor.JavaType.ENUM -> enumType.enumClass()
            Descriptors.FieldDescriptor.JavaType.INT -> ClassName("kotlin", "Int")
            Descriptors.FieldDescriptor.JavaType.FLOAT -> ClassName("kotlin", "Float")
            Descriptors.FieldDescriptor.JavaType.DOUBLE -> ClassName("kotlin", "Double")
            Descriptors.FieldDescriptor.JavaType.LONG -> ClassName("kotlin", "Long")
            Descriptors.FieldDescriptor.JavaType.STRING -> ClassName("kotlin", "String")
            Descriptors.FieldDescriptor.JavaType.BOOLEAN -> ClassName("kotlin", "Boolean")
            Descriptors.FieldDescriptor.JavaType.BYTE_STRING -> ClassName("kotlin", "ByteString")
            else -> throw IllegalStateException("unable to parse field '${name}' type")
        }
    }
}
