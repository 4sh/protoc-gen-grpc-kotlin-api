package io.grpc.kotlin.generator

const val inputPackageParameterName = "inputPackage"
const val outputPackageReplacementParameterName = "outputPackageReplacement"

fun String.toPackageName(parameters: Map<String, String>): String =
    if (parameters.containsKey(outputPackageReplacementParameterName)) {
        val replacement = parameters[outputPackageReplacementParameterName]!!.split("/")
        replace(replacement.first(), replacement.last())
    } else {
        this
    }

