package io.grpc.kotlin.generator

const val inputPackageParameterName = "inputPackage"
const val outputPackageReplacementParameterName = "outputPackageNameReplacement"
const val outputClassNameReplacementParameterName = "outputClassNameReplacement"

fun String.toPackageName(parameters: Map<String, String>): String =
    if (parameters.containsKey(outputPackageReplacementParameterName)) {
        val replacement = parameters[outputPackageReplacementParameterName]!!.split("/")
        replace(replacement.first(), replacement.last())
    } else {
        this
    }

fun String.toClassName(parameters: Map<String, String>, suffix: String = "ApiDef"): String =
    if (parameters.containsKey(outputClassNameReplacementParameterName)) {
        val replacement = parameters[outputClassNameReplacementParameterName]!!.split("/")
        replace(replacement.first(), replacement.last())
    } else {
        this
    }
        .plus(suffix)

