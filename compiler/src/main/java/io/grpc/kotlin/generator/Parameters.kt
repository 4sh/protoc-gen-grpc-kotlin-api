package io.grpc.kotlin.generator

const val inputPackageParameterName = "inputPackage"
const val outputPackageReplacementParameterName = "outputPackageReplacement"

fun String.toPackageName(parameters: Map<String, String>): String =
    if (parameters.containsKey(outputPackageReplacementParameterName)) {
        val regex = parameters[outputPackageReplacementParameterName]!!.split("/")
        replace(Regex(regex.first()), regex.last())
    } else {
        this
    }

