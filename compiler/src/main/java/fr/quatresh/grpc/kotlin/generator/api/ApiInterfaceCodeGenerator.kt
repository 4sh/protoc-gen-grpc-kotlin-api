package fr.quatresh.grpc.kotlin.generator.api

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.kotlin.generator.CodeGenerator
import io.grpc.kotlin.generator.protoc.*

class ApiInterfaceCodeGenerator(config: GeneratorConfig) : CodeGenerator(config) {

    private val packageParameterName = "package"

    override fun generate(fileDescriptor: FileDescriptor, parameters: Map<String, String>): Declarations =
        declarations {
            if (!parameters.containsKey(packageParameterName)
                || parameters[packageParameterName] == null
                || fileDescriptor.`package`.startsWith(parameters[packageParameterName]!!)
            ) {
                extractTypes(fileDescriptor)
                    .forEach { addType(it) }
            }
        }

    private fun extractTypes(fileDescriptor: FileDescriptor): List<TypeSpec> {
        val serviceInterfaces = fileDescriptor.services
            .map { buildServiceType(it) }
        val messageDataClasses = fileDescriptor.messageTypes
            .map { buildMessageType(it) }
        val enumClasses = fileDescriptor.enumTypes
            .map { buildEnumType(it) }

        return serviceInterfaces + messageDataClasses + enumClasses
    }

    private fun buildServiceType(service: Descriptors.ServiceDescriptor) =
        TypeSpec.interfaceBuilder(service.name)
            .apply { addFunctions(buildFunctions(service)) }
            .build()

    private fun buildFunctions(service: Descriptors.ServiceDescriptor): List<FunSpec> = with(config) {
        service.methods
            .map { method ->
                FunSpec.builder(method.methodName.toMemberSimpleName())
                    .addModifiers(KModifier.ABSTRACT)
                    .addParameter(buildFunctionParameter(method))
                    .returns(
                        ClassName(
                            "",
                            method.outputType.messageClassSimpleName.name
                        )
                    )
                    .build()
            }
    }

    private fun buildMessageType(descriptor: Descriptors.Descriptor): TypeSpec {
        val constructorParameters = buildMessageTypeConstructorParameters(descriptor)
        val properties = buildMessageTypeProperties(descriptor)
        return TypeSpec.classBuilder(descriptor.simpleName.name)
            .addModifiers(KModifier.DATA)
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

    private fun buildFunctionParameter(method: Descriptors.MethodDescriptor): ParameterSpec = with(config) {
        ParameterSpec(
            "input",
            ClassName(
                "",
                method.inputType.messageClassSimpleName.name
            )
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

    private fun Descriptors.FieldDescriptor.asClassName(): TypeName =
        when (javaType) {
            Descriptors.FieldDescriptor.JavaType.MESSAGE -> if (this.isMapField) {
                ClassName("kotlin.collections", "Map")
                    .parameterizedBy(
                        ClassName("kotlin", "String"),
                        ClassName("kotlin", "String")
                    )
            } else {
                ClassName("", messageType.messageClassSimpleName.name)
            }
            Descriptors.FieldDescriptor.JavaType.ENUM ->
                ClassName("", enumType.enumClassSimpleName.name)
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
}
