package fr.quatresh.grpc.kotlin.generator.api

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.kotlin.generator.CodeGenerator
import io.grpc.kotlin.generator.inputPackageParameterName
import io.grpc.kotlin.generator.protoc.*
import io.grpc.kotlin.generator.toPackageName

class ApiInterfaceCodeGenerator(config: GeneratorConfig) : CodeGenerator(config) {

    override fun generate(fileDescriptor: FileDescriptor, parameters: Map<String, String>): Declarations =
        declarations {
            if (!parameters.containsKey(inputPackageParameterName)
                || parameters[inputPackageParameterName] == null
                || fileDescriptor.`package`.startsWith(parameters[inputPackageParameterName]!!)
            ) {
                extractTypes(fileDescriptor, parameters)
                    .forEach { addType(it) }
            }
        }

    private fun extractTypes(fileDescriptor: FileDescriptor, parameters: Map<String, String>): List<TypeSpec> {
        val messageDataClasses = fileDescriptor.messageTypes
            .map { buildMessageType(it, parameters) }
        val enumClasses = fileDescriptor.enumTypes
            .map { buildEnumType(it) }
        val serviceInterfaces = fileDescriptor.services
            .map { buildServiceType(it, parameters) }
        return messageDataClasses + enumClasses + serviceInterfaces
    }

    private fun buildServiceType(service: Descriptors.ServiceDescriptor, parameters: Map<String, String>) =
        TypeSpec.interfaceBuilder(service.name)
            .apply { addFunctions(buildFunctions(service, parameters)) }
            .build()

    private fun buildFunctions(service: Descriptors.ServiceDescriptor, parameters: Map<String, String>): List<FunSpec> =
        service.methods
            .map { method ->
                FunSpec.builder(method.methodName.toMemberSimpleName())
                    .addModifiers(KModifier.ABSTRACT)
                    .addParameter(buildFunctionParameter(method, parameters))
                    .apply {
                        val className = ClassName(
                            method.outputType.file.`package`.toPackageName(parameters),
                            method.outputType.messageClassSimpleName.name
                        )
                        if (!className.isProtobufEmptyType()) {
                            returns(className)
                        }
                    }
                    .build()
            }

    private fun buildMessageType(descriptor: Descriptors.Descriptor, parameters: Map<String, String>): TypeSpec {
        val constructorParameters = buildMessageTypeConstructorParameters(descriptor, parameters)
        val properties = buildMessageTypeProperties(descriptor, parameters)
        return TypeSpec.classBuilder(descriptor.simpleName.name)
            .apply {
                if (constructorParameters.isNotEmpty()) {
                    addModifiers(KModifier.DATA)
                }
            }
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(constructorParameters)
                    .build()
            )
            .addProperties(properties)
            .build()
    }

    private fun buildEnumType(descriptor: EnumDescriptor): TypeSpec =
        TypeSpec.enumBuilder(descriptor.simpleName.name)
            .apply {
                descriptor.values
                    .forEach { enumValue -> addEnumConstant(enumValue.name) }
            }
            .build()

    private fun buildFunctionParameter(method: Descriptors.MethodDescriptor, parameters: Map<String, String>): ParameterSpec =
        ParameterSpec(
            "input",
            ClassName(
                method.inputType.file.`package`.toPackageName(parameters),
                method.inputType.messageClassSimpleName.name
            )
        )

    private fun buildMessageTypeConstructorParameters(
        descriptor: Descriptors.Descriptor,
        parameters: Map<String, String>
    ) =
        descriptor
            .fields
            .map { field ->
                ParameterSpec
                    .builder(
                        field.fieldName.javaSimpleName.name,
                        field.asClassName(parameters)
                    )
                    .build()
            }

    private fun buildMessageTypeProperties(descriptor: Descriptors.Descriptor, parameters: Map<String, String>): List<PropertySpec> =
        descriptor
            .fields
            .map { field ->
                PropertySpec
                    .builder(
                        field.fieldName.javaSimpleName.name,
                        field.asClassName(parameters)
                    )
                    .initializer(field.fieldName.javaSimpleName.name)
                    .build()
            }

    private fun Descriptors.FieldDescriptor.asClassName(parameters: Map<String, String>): TypeName =
        when (javaType) {
            Descriptors.FieldDescriptor.JavaType.MESSAGE -> if (isMapField) {
                ClassName("kotlin.collections", "Map")
                    .parameterizedBy(
                        messageType.findFieldByName("key").asClassName(parameters),
                        messageType.findFieldByName("value").asClassName(parameters)
                    )
            } else {
                ClassName(
                    messageType.file.`package`.toPackageName(parameters),
                    messageType.messageClassSimpleName.name
                )
            }
            Descriptors.FieldDescriptor.JavaType.ENUM ->
                ClassName(
                    enumType.file.`package`.toPackageName(parameters),
                    enumType.enumClassSimpleName.name
                )
            Descriptors.FieldDescriptor.JavaType.INT ->
                ClassName("kotlin", "Int")
            Descriptors.FieldDescriptor.JavaType.FLOAT ->
                ClassName("kotlin", "Float")
            Descriptors.FieldDescriptor.JavaType.DOUBLE ->
                ClassName("kotlin", "Double")
            Descriptors.FieldDescriptor.JavaType.LONG ->
                ClassName("kotlin", "Long")
            Descriptors.FieldDescriptor.JavaType.STRING ->
                ClassName("kotlin", "String")
            Descriptors.FieldDescriptor.JavaType.BOOLEAN ->
                ClassName("kotlin", "Boolean")
            Descriptors.FieldDescriptor.JavaType.BYTE_STRING ->
                ClassName("kotlin", "ByteString")
            else -> throw IllegalStateException("unable to parse field '${name}' type")
        }

    private fun ClassName.isProtobufEmptyType() =
        packageName == "google.protobuf"
                && simpleName == "Empty"
}
