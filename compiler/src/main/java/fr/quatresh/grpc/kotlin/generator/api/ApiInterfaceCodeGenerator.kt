package fr.quatresh.grpc.kotlin.generator.api

import com.google.protobuf.Descriptors.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.kotlin.generator.*
import io.grpc.kotlin.generator.protoc.*

class ApiInterfaceCodeGenerator(config: GeneratorConfig) : CodeGenerator(config) {

    private val flowClassName = ClassName(
        "kotlinx.coroutines.flow",
        "Flow"
    )

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
            .map { buildEnumType(it, parameters) }
        val serviceInterfaces = fileDescriptor.services
            .map { buildServiceType(it, parameters) }
        return messageDataClasses + enumClasses + serviceInterfaces
    }

    private fun buildServiceType(service: ServiceDescriptor, parameters: Map<String, String>) =
        TypeSpec.interfaceBuilder(service.name.toClassName(parameters = parameters, suffix = "Def"))
            .apply { addFunctions(buildFunctions(service, parameters)) }
            .build()

    private fun buildFunctions(service: ServiceDescriptor, parameters: Map<String, String>): List<FunSpec> =
        service.methods
            .map { method ->
                FunSpec.builder(method.methodName.toMemberSimpleName())
                    .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
                    .addParameters(buildFunctionParameters(method, parameters))
                    .apply {
                        if (!method.isProtobufEmptyType()) {
                            val returnType = ClassName(
                                method.outputType.file.`package`.toPackageName(parameters),
                                method.outputType.messageClassSimpleName.name.toClassName(parameters)
                            )
                            if (method.isServerStreaming) {
                                returns(flowClassName.parameterizedBy(returnType))
                            } else {
                                returns(returnType)
                            }
                        }
                    }
                    .build()
            }

    private fun buildMessageType(descriptor: Descriptor, parameters: Map<String, String>): TypeSpec {
        val constructorParameters = buildMessageTypeConstructorParameters(descriptor, parameters)
        val properties = buildMessageTypeProperties(descriptor, parameters)
        return TypeSpec.classBuilder(descriptor.simpleName.name.toClassName(parameters))
            .apply {
                if (constructorParameters.isNotEmpty()) {
                    addModifiers(KModifier.DATA)
                }
            }
            .apply {
                if (constructorParameters.any { it.name == "id" }) {
                    buildClassSuperInterfaceName(parameters)
                        ?.also { addSuperinterface(it) }
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

    private fun buildEnumType(descriptor: EnumDescriptor, parameters: Map<String, String>): TypeSpec =
        TypeSpec.enumBuilder(descriptor.simpleName.name.toClassName(parameters))
            .apply {
                descriptor.values
                    .forEach { enumValue -> addEnumConstant(enumValue.name) }
            }
            .apply {
                addEnumConstant("UNRECOGNIZED")
            }
            .apply {
                buildEnumSuperInterfaceName(parameters)
                    ?.also { addSuperinterface(it) }
            }
            .build()

    private fun buildFunctionParameters(
        method: MethodDescriptor,
        parameters: Map<String, String>
    ): List<ParameterSpec> =
        if (method.isClientStreaming) {
            listOf(
                ParameterSpec(
                    "input",
                    flowClassName
                        .parameterizedBy(
                            ClassName(
                                method.inputType.file.`package`.toPackageName(parameters),
                                method.inputType.messageClassSimpleName.name.toClassName(parameters)
                            )
                        )
                )
            )
        } else {
            method.inputType
                .fields
                .map { field ->
                    ParameterSpec(
                        field.fieldName.javaSimpleName.name,
                        field.asClassName(parameters)
                    )
                }
        }

    private fun buildMessageTypeConstructorParameters(
        descriptor: Descriptor,
        parameters: Map<String, String>
    ) =
        descriptor
            .fields
            .map { field ->
                val fieldName = field.fieldName.javaSimpleName.name
                ParameterSpec
                    .builder(
                        fieldName,
                        field.asClassName(parameters)
                            .copy(nullable = field.isOptional || field.isRepeated)
                    )
                    .build()
            }

    private fun buildMessageTypeProperties(
        descriptor: Descriptor,
        parameters: Map<String, String>
    ): List<PropertySpec> =
        descriptor
            .fields
            .map { field ->
                val fieldName = field.fieldName.javaSimpleName.name
                PropertySpec
                    .builder(
                        fieldName,
                        field.asClassName(parameters)
                            .copy(nullable = field.isOptional || field.isRepeated)
                    )
                    .run {
                        if (fieldName == "id") {
                            addModifiers(KModifier.OVERRIDE)
                        } else this
                    }
                    .initializer(fieldName)
                    .build()
            }

    private fun FieldDescriptor.asClassName(parameters: Map<String, String>): TypeName =
        if (isMapField) {
            ClassName("kotlin.collections", "Map")
                .parameterizedBy(
                    messageType.findFieldByName("key").asClassName(parameters),
                    messageType.findFieldByName("value").asClassName(parameters)
                )
        } else {
            when (javaType) {
                FieldDescriptor.JavaType.MESSAGE ->
                    ClassName(
                        messageType.file.`package`.toPackageName(parameters),
                        messageType.messageClassSimpleName.name.toClassName(parameters)
                    )
                FieldDescriptor.JavaType.ENUM ->
                    ClassName(
                        enumType.file.`package`.toPackageName(parameters),
                        enumType.enumClassSimpleName.name.toClassName(parameters)
                    )
                FieldDescriptor.JavaType.INT ->
                    ClassName("kotlin", "Int")
                FieldDescriptor.JavaType.FLOAT ->
                    ClassName("kotlin", "Float")
                FieldDescriptor.JavaType.DOUBLE ->
                    ClassName("kotlin", "Double")
                FieldDescriptor.JavaType.LONG ->
                    ClassName("kotlin", "Long")
                FieldDescriptor.JavaType.STRING ->
                    ClassName("kotlin", "String")
                FieldDescriptor.JavaType.BOOLEAN ->
                    ClassName("kotlin", "Boolean")
                FieldDescriptor.JavaType.BYTE_STRING ->
                    ClassName("kotlin", "ByteString")
                else -> throw IllegalStateException("unable to parse field '${name}' type")
            }
                .run {
                    if (isRepeated) {
                        ClassName("kotlin.collections", "List")
                            .parameterizedBy(
                                this
                            )
                    } else this
                }
        }

    private fun MethodDescriptor.isProtobufEmptyType() =
        ClassName(
            outputType.file.`package`,
            outputType.messageClassSimpleName.name
        )
            .run {
                packageName == "google.protobuf"
                        && simpleName == "Empty"
            }
}
